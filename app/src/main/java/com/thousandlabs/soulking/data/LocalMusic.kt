package com.thousandlabs.soulking.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat

/** Morceaux enregistrés sur le téléphone. Rien ne quitte l'appareil. */
object LocalMusic {

    const val UNKNOWN = "Artiste inconnu"

    val permission: String
        get() = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    fun granted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /** « One Piece Opening 25 » → animé « One Piece », précision « Opening 25 ». */
    private val animeRe = Regex("""^(.+?)\s+((?:opening|op|ending|ed)\s*\d{1,3})$""", RegexOption.IGNORE_CASE)

    /** Forme affichée par Tone Dial : artiste « One Piece », titre « Opening 25 - The Peak ». */
    private val themeTitleRe = Regex("""^((?:opening|op|ending|ed)\s*\d{1,3})\s*[-–:]\s*(.+)$""", RegexOption.IGNORE_CASE)

    fun load(context: Context): List<Track> {
        if (!granted(context)) return emptyList()
        val td = ToneDialCatalog.get(context)
        val out = ArrayList<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        @Suppress("DEPRECATION")
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
        )
        // au moins 30 secondes, comme Tone Dial : écarte les notes vocales et les sonneries
        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        try {
            context.contentResolver.query(collection, projection, selection, arrayOf("30000"), null)?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                @Suppress("DEPRECATION")
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val path = c.getString(dataCol)
                    val display = c.getString(nameCol)?.substringBeforeLast('.')?.trim().orEmpty()
                    val tagTitle = c.getString(titleCol)?.trim().orEmpty().ifBlank { display }
                    val tagArtist = c.getString(artistCol)?.trim()

                    // 1. le nom affiché dans Tone Dial (corrections comprises), s'il a été importé
                    val tdSong = td?.find(id, path)
                    val override = if (td != null && tdSong != null) td.overrides[tdSong.id] else null
                    var title: String
                    var artist: String?
                    if (tdSong != null && (override != null || tdSong.title.isNotBlank())) {
                        title = override?.first ?: tdSong.title
                        artist = (override?.second ?: tdSong.artist).takeUnless { TagCleaner.isUnknown(it) }
                    } else {
                        // 2. nettoyage des noms de téléchargement : sites, (Official Video), numéros, noms collés…
                        val fromTags = TagCleaner.clean(tagTitle, tagArtist)
                        val fromFile = if (fromTags.artist == null && display.isNotBlank()) TagCleaner.clean(display, null) else null
                        val best = if (fromFile?.artist != null) fromFile else fromTags
                        title = best.title
                        artist = best.artist
                    }
                    if (title.isBlank()) continue

                    // opening reconnu : par Tone Dial, ou par le nom « One Piece Opening 25 »
                    val openingName = (if (td != null && tdSong != null) td.openings[tdSong.id] else null) ?: artist
                    var anime: String? = null
                    var label: String? = null
                    if (openingName != null) {
                        animeRe.matchEntire(openingName.trim())?.let { m ->
                            anime = m.groupValues[1].trim()
                            label = m.groupValues[2].trim().replaceFirstChar { it.uppercase() }
                        }
                    }
                    if (anime == null && artist != null) {
                        themeTitleRe.matchEntire(title)?.let { m ->
                            anime = artist
                            label = m.groupValues[1].trim().replaceFirstChar { it.uppercase() }
                            title = m.groupValues[2].trim()
                        }
                    }

                    out += Track(
                        key = "local:$id",
                        title = title,
                        artist = artist ?: UNKNOWN,
                        anime = anime,
                        label = label,
                        localUri = ContentUris.withAppendedId(collection, id),
                        artUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), c.getLong(albumCol)),
                        durationMs = c.getLong(durCol),
                        artKind = if (anime != null) ArtKind.ANIME else null,
                    )
                }
            }
        } catch (_: SecurityException) {
            return emptyList()
        }
        return inferArtists(out)
    }

    private val stopWords = setOf(
        "the", "a", "an", "le", "la", "les", "l", "un", "une", "des", "i", "my", "you", "we", "it", "in", "on",
        "of", "de", "du", "to", "and", "et", "je", "tu", "me", "mon", "ma", "mes", "this", "that", "no", "oh",
    )

    /**
     * Comme Tone Dial : repère l'artiste des fichiers sans tiret.
     * Si plusieurs titres commencent par les mêmes mots (« Eminem Lose Yourself », « Eminem Mockingbird »),
     * ou par le nom d'un artiste déjà connu, ces mots sont l'artiste.
     */
    private fun inferArtists(tracks: List<Track>): List<Track> {
        val unknown = tracks.filter { it.artist == UNKNOWN }
        if (unknown.isEmpty()) return tracks
        val known = tracks.asSequence().filter { it.artist != UNKNOWN }.map { it.artist }
            .filter { it.length >= 2 }.distinctBy { it.lowercase() }.sortedByDescending { it.length }.toList()
        fun words(t: Track) = TagCleaner.prepare(t.title).split(Regex("""\s+""")).filter { it.isNotBlank() }
        val counts = HashMap<String, Pair<String, Int>>()
        unknown.forEach { t ->
            val w = words(t)
            if (w.isEmpty() || w[0].lowercase() in stopWords) return@forEach
            for (n in 1..3) {
                if (w.size <= n) break
                val prefix = w.take(n).joinToString(" ")
                val key = prefix.lowercase()
                val cur = counts[key]
                counts[key] = (cur?.first ?: prefix) to ((cur?.second ?: 0) + 1)
            }
        }
        return tracks.map { t ->
            if (t.artist != UNKNOWN) return@map t
            val prepared = TagCleaner.prepare(t.title)
            var artist: String? = known.firstOrNull { k ->
                prepared.length > k.length + 1 && prepared.startsWith(k, ignoreCase = true) &&
                    !prepared[k.length].isLetterOrDigit()
            }?.let { prepared.substring(0, it.length) }
            if (artist == null) {
                val w = words(t)
                for (n in 3 downTo 1) {
                    if (w.size <= n) continue
                    val c = counts[w.take(n).joinToString(" ").lowercase()] ?: continue
                    if (c.second >= 3) {
                        artist = c.first
                        break
                    }
                }
            }
            val a = artist ?: return@map t
            val title = prepared.substring(a.length).trim(' ', '-', '–', '—', ':', '|').ifBlank { t.title }
            t.copy(artist = a, title = title)
        }
    }

    enum class Kind { ALL, ANIME, ARTIST, FAVORITES, MOST_PLAYED, PLAYLIST }

    /** [keys] : morceaux précis (favoris, playlist Tone Dial). */
    data class Filter(val label: String, val kind: Kind, val value: String? = null, val keys: Set<String> = emptySet())

    data class Group(val filter: Filter, val count: Int)

    fun filter(tracks: List<Track>, f: Filter): List<Track> = when (f.kind) {
        Kind.ALL -> tracks
        Kind.ANIME -> tracks.filter { it.anime.equals(f.value, ignoreCase = true) }
        Kind.ARTIST -> tracks.filter { it.anime == null && it.artist.equals(f.value, ignoreCase = true) }
        Kind.FAVORITES, Kind.MOST_PLAYED, Kind.PLAYLIST -> tracks.filter { it.key in f.keys }
    }

    /** Regroupements proposés : chaque animé (3 morceaux ou plus), chaque artiste (3 ou plus). */
    fun groups(tracks: List<Track>): Pair<List<Group>, List<Group>> {
        val animes = tracks.filter { it.anime != null }
            .groupBy { it.anime!!.lowercase() }.values
            .filter { it.size >= 3 }
            .sortedByDescending { it.size }
            .map { Group(Filter(it.first().anime!!, Kind.ANIME, it.first().anime), it.size) }
        val artists = tracks.filter { it.anime == null && it.artist != UNKNOWN }
            .groupBy { it.artist.lowercase() }.values
            .filter { it.size >= 3 }
            .sortedByDescending { it.size }
            .map { Group(Filter(it.first().artist, Kind.ARTIST, it.first().artist), it.size) }
        return animes to artists
    }

    /** Favoris, plus écoutés et playlists du catalogue Tone Dial, retrouvés parmi les morceaux du téléphone. */
    fun toneDialGroups(context: Context, tracks: List<Track>): List<Group> {
        val td = ToneDialCatalog.get(context) ?: return emptyList()
        val present = tracks.map { it.key }.toHashSet()
        fun keysOf(ids: Collection<Long>): Set<String> = ids.map { "local:$it" }.filter { it in present }.toSet()
        val out = ArrayList<Group>()
        keysOf(td.favorites).takeIf { it.size >= 4 }?.let { out += Group(Filter("Mes favoris Tone Dial", Kind.FAVORITES, keys = it), it.size) }
        val top = td.playCounts.entries.filter { it.value > 0 }.sortedByDescending { it.value }.take(60).map { it.key }
        keysOf(top).takeIf { it.size >= 4 }?.let { out += Group(Filter("Mes plus écoutés", Kind.MOST_PLAYED, keys = it), it.size) }
        td.playlists.forEach { (name, ids) ->
            keysOf(ids).takeIf { it.size >= 4 }?.let { out += Group(Filter(name, Kind.PLAYLIST, value = name, keys = it), it.size) }
        }
        return out
    }
}
