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

/** Thème ou sélection où l'on peut deviner l'animé (ou le dessin animé). */
val Source.showLike: Boolean
    get() = when (this) {
        is Source.Theme -> def.forceAnime != null || def.localAnyAnime || def.whoOnly
        is Source.Local -> filter.kind == LocalMusic.Kind.ANIME || filter.kind == LocalMusic.Kind.ALL
        else -> false
    }

/** Prépare la liste de morceaux d'une partie, en coulisses. */
object PoolBuilder {

    /** Versions qui gâchent un blind test : reprises, remix, versions refaites par IA… */
    private val junk = Regex(
        """\b(karaok[eé]|instrumental version|8d audio|sped up|slowed|nightcore|piano version|lofi|lo-fi|cover|covers|tribute|made famous|remix|remixed|phonk|mashup|bass boosted|type beat|8 ?bit|ai cover|ai version|ai generated|version ia|ia version|suno|udio|acapella|a cappella|music box|lullaby|berceuse|parody|parodie)\b""",
        RegexOption.IGNORE_CASE,
    )

    /** Pour les génériques, encore plus strict : aucune version retravaillée. */
    private val looseJunk = Regex(
        """\b(instrumental|instru|trap|drill|funk|techno|house|hardstyle|dubstep|rock version|metal|epic|orchestral|piano|guitar|guitare|beat|vibe|chill)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val instrumentalWord = Regex("""\b(instrumental|instru|karaoke|playback)\b""", RegexOption.IGNORE_CASE)

    /** Un générique officiel est présenté comme tel (les bandes originales de films ne comptent pas). */
    private val themeMarker = Regex(
        """\b(generique|generiques|theme|theme song|opening|intro|main title|serie|series|tv|dessin anime|dessins animes|cartoon|cartoons)\b""",
    )

    @Volatile
    private var banned: Set<String> = emptySet()

    private fun isJunk(t: Track): Boolean =
        t.key in banned || listOfNotNull(t.rawTitle, t.title, t.artist, t.album).any { junk.containsMatchIn(it) }

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
        banned = Prefs.banned()
        val online: List<Track> = when (source) {
            is Source.Theme -> fromTheme(source.def, needed, progress)
            is Source.Artist -> Deezer.artistTop(source.id, 100)
            is Source.Playlist -> Deezer.playlistTracks(source.id, 300)
            is Source.Search -> Deezer.searchTracks(source.query, 100)
            is Source.Local -> emptyList()
        }.filterNot { isJunk(it) }

        val locals: List<Track> = when (source) {
            is Source.Local -> LocalMusic.filter(local, source.filter).filter { it.key !in banned }
            is Source.Theme -> if (includeLocal) local.filter { it.key !in banned && matchesLocal(source.def, it) } else emptyList()
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
                        // version française d'abord ; si elle est introuvable, la version originale anglaise
                        val english = ArtSearch.englishName(c.title)
                        val attempts = buildList {
                            add((c.query ?: "${c.title} ${c.artist}".trim()) to c)
                            if (c.loose && c.needsMarker && !english.equals(c.title, ignoreCase = true)) {
                                add("$english theme song" to c.copy(alts = c.alts + english))
                            }
                        }
                        // la version déjà retenue, sauf si elle a été écartée (« Mauvaise version ? »)
                        val choiceKey = "${def.id}|${norm(c.title)}|${norm(c.artist)}"
                        var found: Track? = Prefs.choice(choiceKey)
                            ?.let { id -> Deezer.track(id) }
                            ?.takeIf { !isJunk(it) }
                        if (found == null) {
                            for ((q, check) in attempts) {
                                // parmi les résultats valides : la version officielle et ancienne d'abord, puis la plus écoutée
                                found = Deezer.searchTracks(q, 15)
                                    .filter { !isJunk(it) && matchesCurated(it, check) }
                                    .maxWithOrNull(compareBy<Track>({ versionScore(it) }, { it.rank }))
                                if (found != null) break
                            }
                            found?.deezerId?.let { Prefs.setChoice(choiceKey, it) }
                        }
                        found
                            ?.let { listOf(it.copy(anime = c.anime ?: def.forceAnime)) }
                            ?.map { if (it.anime != null) it.copy(artKind = if (def.whoOnly) ArtKind.CARTOON else ArtKind.ANIME) else it }
                    }.getOrNull() ?: emptyList()
                    tick()
                    r
                }
            }
        }
        val all = (artistJobs + playlistJobs + curatedJobs).awaitAll().flatten()
        if (def.forceAnime != null) all.map { if (it.anime == null) it.copy(anime = def.forceAnime, artKind = ArtKind.ANIME) else it } else all
    }

    /** Artistes de compilations qui réenregistrent les génériques : souvent des reprises. */
    private val coverMaker = Regex(
        """\b(kids?|kidz|enfants|comptines|tv themes?|theme players|tv theme|cartoon kids|band|orchestra|orchestre|singers|allstars|all stars|music factory|anime kei|studio|chorale|la bande|baby|les petits|crew|ensemble|project|academy|tribute|cover)\b""",
    )

    /** Indices d'une version officielle : studio, chaîne, bande originale. */
    private val officialHint = Regex("""\b(disney|nickelodeon|cartoon network|warner|cast|original|soundtrack|bande originale|ost|officiel|official)\b""")

    /** Indices d'un générique récent (nouvelle saison, reboot) : moins connu de la Gen Z. */
    private val recentHint = Regex("""\b(202[3-9]|nouvelle version|new version|reboot|nouveau generique|new theme)\b""")

    /** Plus le score est haut, plus la version a de chances d'être l'officielle connue de tous. */
    private fun versionScore(t: Track): Int {
        val artist = normKeep(t.artist)
        val rest = normKeep(listOfNotNull(t.rawTitle, t.album).joinToString(" "))
        var score = 0
        if (coverMaker.containsMatchIn(artist)) score -= 4
        if (officialHint.containsMatchIn("$artist $rest")) score += 2
        if (recentHint.containsMatchIn(rest)) score -= 3
        return score
    }

    private val stopWords = setOf("les", "le", "la", "et", "de", "des", "du", "the", "and", "l", "d")

    /** Le résultat trouvé est-il bien le morceau demandé (et pas une reprise) ? */
    private fun matchesCurated(t: Track, c: Curated): Boolean {
        if (c.loose) {
            // générique : le nom de l'émission doit apparaître dans le titre complet, l'album ou l'artiste
            val texts = listOfNotNull(t.rawTitle, t.title, t.album, t.artist)
            if (texts.any { looseJunk.containsMatchIn(it) }) return false
            if (!c.instrumentalOk && texts.any { instrumentalWord.containsMatchIn(it) }) return false
            // le nom de l'émission doit être dans le titre ou l'album, jamais seulement dans le nom de l'artiste
            // (sinon « Les Simpson » trouverait une chanson de Jessica Simpson)
            val hay = normKeep(listOfNotNull(t.rawTitle, t.title, t.album).joinToString(" "))
            if (c.needsMarker && !themeMarker.containsMatchIn(hay)) return false
            if (hasExcluded(t, c)) return false
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
        if (!titleOk || hasExcluded(t, c)) return false
        val wantArtist = norm(c.artist).split(" ").filter { it.length >= 2 }
        val gotArtist = norm(t.artist)
        return wantArtist.isEmpty() || wantArtist.any { gotArtist.contains(it) }
    }

    private fun hasExcluded(t: Track, c: Curated): Boolean {
        if (c.exclude.isEmpty()) return false
        val all = normKeep(listOfNotNull(t.rawTitle, t.title, t.album, t.artist).joinToString(" "))
        return c.exclude.any { w -> Regex("\\b${Regex.escape(normKeep(w))}\\b").containsMatchIn(all) }
    }
}
