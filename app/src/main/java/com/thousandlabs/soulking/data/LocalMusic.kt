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

    val permission: String
        get() = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    fun granted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /** « One Piece Opening 25 » → animé « One Piece », précision « Opening 25 ». */
    private val animeRe = Regex("""^(.+?)\s+((?:opening|op|ending|ed)\s*\d{1,3})$""", RegexOption.IGNORE_CASE)

    private val unknownArtists = setOf("<unknown>", "unknown", "artiste inconnu", "unknown artist")

    fun load(context: Context): List<Track> {
        if (!granted(context)) return emptyList()
        val out = ArrayList<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
        )
        // au moins 45 secondes : écarte les notes vocales et les sonneries
        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        try {
            context.contentResolver.query(collection, projection, selection, arrayOf("45000"), null)?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                while (c.moveToNext()) {
                    val display = c.getString(nameCol)?.substringBeforeLast('.')?.trim().orEmpty()
                    var title = c.getString(titleCol)?.trim().orEmpty().ifBlank { display }
                    var artist = c.getString(artistCol)?.trim().orEmpty()
                    if (artist.lowercase() in unknownArtists) artist = ""

                    // fichiers nommés « Artiste - Titre »
                    if (artist.isBlank()) {
                        val src = if (title.contains(" - ")) title else display
                        if (src.contains(" - ")) {
                            artist = src.substringBefore(" - ").trim()
                            title = src.substringAfter(" - ").trim()
                        }
                    } else if (title.contains(" - ") && title.substringBefore(" - ").trim().equals(artist, ignoreCase = true)) {
                        title = title.substringAfter(" - ").trim()
                    }
                    if (artist.isBlank() || title.isBlank()) continue

                    var anime: String? = null
                    var label: String? = null
                    animeRe.matchEntire(artist)?.let { m ->
                        anime = m.groupValues[1].trim()
                        label = m.groupValues[2].trim().replaceFirstChar { it.uppercase() }
                    }
                    val id = c.getLong(idCol)
                    out += Track(
                        key = "local:$id",
                        title = title,
                        artist = artist,
                        anime = anime,
                        label = label,
                        localUri = ContentUris.withAppendedId(collection, id),
                        artUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), c.getLong(albumCol)),
                        durationMs = c.getLong(durCol),
                    )
                }
            }
        } catch (_: SecurityException) {
            return emptyList()
        }
        return out
    }

    enum class Kind { ALL, ANIME, ARTIST }

    data class Filter(val label: String, val kind: Kind, val value: String? = null)

    data class Group(val filter: Filter, val count: Int)

    fun filter(tracks: List<Track>, f: Filter): List<Track> = when (f.kind) {
        Kind.ALL -> tracks
        Kind.ANIME -> tracks.filter { it.anime.equals(f.value, ignoreCase = true) }
        Kind.ARTIST -> tracks.filter { it.anime == null && it.artist.equals(f.value, ignoreCase = true) }
    }

    /** Regroupements proposés : toute la musique, chaque animé (3 morceaux ou plus), chaque artiste (4 ou plus). */
    fun groups(tracks: List<Track>): Pair<List<Group>, List<Group>> {
        val animes = tracks.filter { it.anime != null }
            .groupBy { it.anime!!.lowercase() }.values
            .filter { it.size >= 3 }
            .sortedByDescending { it.size }
            .map { Group(Filter(it.first().anime!!, Kind.ANIME, it.first().anime), it.size) }
        val artists = tracks.filter { it.anime == null }
            .groupBy { it.artist.lowercase() }.values
            .filter { it.size >= 4 }
            .sortedByDescending { it.size }
            .map { Group(Filter(it.first().artist, Kind.ARTIST, it.first().artist), it.size) }
        return animes to artists
    }
}
