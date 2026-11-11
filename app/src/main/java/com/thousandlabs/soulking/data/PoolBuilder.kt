package com.thousandlabs.soulking.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil

/** D'où viennent les morceaux d'une partie. */
sealed interface Source {
    val name: String
    val description: String

    data class Theme(val def: ThemeDef) : Source {
        override val name: String get() = def.title
        override val description: String get() = def.subtitle
    }

    data class Artist(val id: Long, val artistName: String, val picture: String?) : Source {
        override val name: String get() = artistName
        override val description: String get() = "Ses morceaux les plus connus"
    }

    data class Playlist(val id: Long, val title: String, val picture: String?, val count: Int) : Source {
        override val name: String get() = title
        override val description: String get() = "Playlist en ligne · $count titres"
    }

    data class Search(val query: String) : Source {
        override val name: String get() = "« $query »"
        override val description: String get() = "Morceaux liés à votre recherche"
    }

    data class Local(val filter: LocalMusic.Filter) : Source {
        override val name: String get() = filter.label
        override val description: String get() = "Depuis la musique de votre téléphone"
    }
}

/** Prépare la liste de morceaux d'une partie, en coulisses. */
object PoolBuilder {

    /** Versions qui gâchent un blind test. */
    private val junk = Regex(
        """\b(karaok[eé]|instrumental|8d audio|sped up|slowed|nightcore|piano version|lofi|lo-fi|cover|tribute|made famous)\b""",
        RegexOption.IGNORE_CASE,
    )

    fun matchesLocal(def: ThemeDef, t: Track): Boolean {
        def.localAnime?.let { anime ->
            val n = norm(anime)
            return norm(t.anime ?: t.artist).contains(n) || norm(t.title).contains(n)
        }
        if (def.localAnyAnime) return t.anime != null
        if (def.artists.isEmpty()) return false
        val a = norm(t.artist)
        return def.artists.any { name ->
            val n = norm(name)
            n.isNotEmpty() && (a == n || a.startsWith("$n "))
        }
    }

    suspend fun build(
        source: Source,
        local: List<Track>,
        includeLocal: Boolean,
        needed: Int,
        progress: (String) -> Unit,
    ): List<Track> = withContext(Dispatchers.IO) {
        val online: List<Track> = when (source) {
            is Source.Theme -> fromTheme(source.def, needed, progress)
            is Source.Artist -> Deezer.artistTop(source.id, 100)
            is Source.Playlist -> Deezer.playlistTracks(source.id, 300)
            is Source.Search -> Deezer.searchTracks(source.query, 100)
            is Source.Local -> emptyList()
        }.filterNot { junk.containsMatchIn(it.title) || junk.containsMatchIn(it.artist) }

        val locals: List<Track> = when (source) {
            is Source.Local -> LocalMusic.filter(local, source.filter)
            is Source.Theme -> if (includeLocal) local.filter { matchesLocal(source.def, it) } else emptyList()
            else -> emptyList()
        }
        // les fichiers du téléphone passent en premier : en cas de doublon, c'est eux qu'on garde
        (locals + online).distinctBy { it.dedupeKey }
    }

    private suspend fun fromTheme(def: ThemeDef, needed: Int, progress: (String) -> Unit): List<Track> = coroutineScope {
        // grandes listes : on ne cherche qu'un échantillon, assez pour la partie et les remplaçants
        val curatedPick = def.curated.shuffled().take(needed * 2 + 12)
        val total = def.artists.size + def.playlists.size + curatedPick.size
        val done = AtomicInteger(0)
        progress("Préparation de la playlist…")
        fun tick() {
            val n = done.incrementAndGet()
            progress("Préparation de la playlist… ${(n * 100 / total.coerceAtLeast(1))} %")
        }
        val gate = Semaphore(6)
        val perArtist = if (def.artists.isEmpty()) 0
        else ceil(needed * 2.5 / def.artists.size).toInt().coerceIn(4, 12)

        val artistJobs = def.artists.map { name ->
            async {
                gate.withPermit {
                    val r = runCatching {
                        val id = Prefs.artistId(name)
                            ?: Deezer.searchArtists(name, 1).firstOrNull()?.id?.also { Prefs.putArtistId(name, it) }
                        if (id == null) emptyList() else Deezer.artistTop(id, perArtist)
                    }.getOrDefault(emptyList())
                    tick()
                    r
                }
            }
        }
        val playlistJobs = def.playlists.map { q ->
            async {
                gate.withPermit {
                    val r = runCatching {
                        Deezer.searchPlaylists(q, 5).filter { it.count >= 10 }.take(2)
                            .flatMap { Deezer.playlistTracks(it.id, 150) }
                    }.getOrDefault(emptyList())
                    tick()
                    r
                }
            }
        }
        val curatedJobs = curatedPick.map { c ->
            async {
                gate.withPermit {
                    val r = runCatching {
                        Deezer.searchTracks(c.query ?: "${c.title} ${c.artist}".trim(), 10).firstOrNull { matchesCurated(it, c) }
                            ?.let { listOf(it.copy(anime = c.anime ?: def.forceAnime)) }
                    }.getOrNull() ?: emptyList()
                    tick()
                    r
                }
            }
        }
        val all = (artistJobs + playlistJobs + curatedJobs).awaitAll().flatten()
        if (def.forceAnime != null) all.map { if (it.anime == null) it.copy(anime = def.forceAnime) else it } else all
    }

    private val stopWords = setOf("les", "le", "la", "et", "de", "des", "du", "the", "and", "l", "d")

    /** Le résultat trouvé est-il bien le morceau demandé (et pas une reprise) ? */
    private fun matchesCurated(t: Track, c: Curated): Boolean {
        if (c.loose) {
            // générique : le nom de l'émission doit apparaître dans le titre complet, l'album ou l'artiste
            val hay = normKeep(listOfNotNull(t.rawTitle, t.title, t.album, t.artist).joinToString(" "))
            return (listOf(c.title) + c.alts).any { name ->
                val words = normKeep(name).split(" ").filter { it.length >= 2 && it !in stopWords }
                words.isNotEmpty() && words.all { w -> Regex("\\b${Regex.escape(w)}").containsMatchIn(hay) }
            }
        }
        val got = norm(t.title)
        val titleOk = (listOf(c.title) + c.alts).any { title ->
            val want = norm(title)
            want.isNotEmpty() && (got == want || got.startsWith("$want ") || (want.length >= 5 && got.contains(want)))
        }
        if (!titleOk) return false
        val wantArtist = norm(c.artist).split(" ").filter { it.length >= 2 }
        val gotArtist = norm(t.artist)
        return wantArtist.isEmpty() || wantArtist.any { gotArtist.contains(it) }
    }
}
