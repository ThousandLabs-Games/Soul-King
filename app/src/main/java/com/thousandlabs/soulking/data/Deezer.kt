package com.thousandlabs.soulking.data

import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Catalogue en ligne : extraits officiels de 30 secondes mis à disposition par Deezer.
 * Aucun compte, aucune donnée personnelle envoyée : seulement la recherche tapée.
 * Toutes les fonctions sont bloquantes : à appeler depuis Dispatchers.IO.
 */
object Deezer {

    private const val BASE = "https://api.deezer.com"

    data class ArtistInfo(val id: Long, val name: String, val picture: String?, val fans: Int)
    data class PlaylistInfo(val id: Long, val title: String, val picture: String?, val count: Int)

    // Deezer limite à 50 requêtes toutes les 5 secondes : on espace les appels.
    private val lock = Any()
    private var nextAt = 0L

    private fun throttle() {
        val wait: Long
        synchronized(lock) {
            val now = SystemClock.elapsedRealtime()
            val at = maxOf(now, nextAt)
            nextAt = at + 115
            wait = at - now
        }
        if (wait > 0) Thread.sleep(wait)
    }

    private fun get(path: String): JSONObject {
        var attempt = 0
        while (true) {
            throttle()
            val conn = (URL(BASE + path).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("User-Agent", "SoulKing")
            }
            val json = try {
                if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
                JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            } finally {
                conn.disconnect()
            }
            val err = json.optJSONObject("error")
            if (err != null) {
                // code 4 : trop de requêtes, on patiente un peu
                if (err.optInt("code") == 4 && attempt < 4) {
                    attempt++
                    Thread.sleep(1500L * attempt)
                    continue
                }
                throw IOException(err.str("message").ifBlank { "Erreur du catalogue" })
            }
            return json
        }
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    private fun JSONObject.str(k: String): String = if (isNull(k)) "" else optString(k)

    private fun JSONObject.data(): JSONArray = optJSONArray("data") ?: JSONArray()

    fun parseTrack(o: JSONObject): Track? {
        val preview = o.str("preview")
        if (preview.isBlank() || !o.optBoolean("readable", true)) return null
        val id = o.optLong("id")
        val title = o.str("title_short").ifBlank { o.str("title") }.trim()
        val artist = o.optJSONObject("artist")?.str("name")?.trim().orEmpty()
        if (id == 0L || title.isBlank() || artist.isBlank()) return null
        val album = o.optJSONObject("album")
        val cover = album?.str("cover_xl")?.ifBlank { null } ?: album?.str("cover_big")?.ifBlank { null }
        return Track(
            key = "dz:$id",
            title = title,
            artist = artist,
            coverUrl = cover,
            previewUrl = preview,
            durationMs = 30_000,
            deezerId = id,
        )
    }

    private fun tracks(json: JSONObject): List<Track> {
        val a = json.data()
        return (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.let { o -> parseTrack(o) } }
    }

    fun searchTracks(q: String, limit: Int = 50): List<Track> =
        tracks(get("/search?q=${enc(q)}&limit=$limit"))

    fun searchArtists(q: String, limit: Int = 5): List<ArtistInfo> {
        val a = get("/search/artist?q=${enc(q)}&limit=$limit").data()
        return (0 until a.length()).mapNotNull { i ->
            val o = a.optJSONObject(i) ?: return@mapNotNull null
            val id = o.optLong("id")
            if (id == 0L) null
            else ArtistInfo(id, o.str("name"), o.str("picture_medium").ifBlank { null }, o.optInt("nb_fan"))
        }
    }

    fun searchPlaylists(q: String, limit: Int = 8): List<PlaylistInfo> {
        val a = get("/search/playlist?q=${enc(q)}&limit=$limit").data()
        return (0 until a.length()).mapNotNull { i ->
            val o = a.optJSONObject(i) ?: return@mapNotNull null
            val id = o.optLong("id")
            if (id == 0L) null
            else PlaylistInfo(id, o.str("title"), o.str("picture_medium").ifBlank { null }, o.optInt("nb_tracks"))
        }
    }

    fun artistTop(id: Long, limit: Int = 25): List<Track> =
        tracks(get("/artist/$id/top?limit=$limit"))

    fun playlistTracks(id: Long, limit: Int = 200): List<Track> =
        tracks(get("/playlist/$id/tracks?limit=$limit"))

    /** Redemande un morceau : les liens d'extraits expirent au bout d'un moment. */
    fun track(id: Long): Track? = runCatching { parseTrack(get("/track/$id")) }.getOrNull()
}
