package com.thousandlabs.soulking.data

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Catalogue Tone Dial importé depuis sa sauvegarde (Menu › Réglages › « Sauvegarder mes données ») :
 * titres et artistes tels qu'affichés dans Tone Dial (corrections comprises), openings reconnus,
 * playlists, favoris et nombre d'écoutes. Le fichier reste sur le téléphone, dans l'espace privé de Soul King.
 */
object ToneDialCatalog {

    data class Song(val id: Long, val path: String, val title: String, val artist: String)

    data class Data(
        val date: Long,
        /** Par identifiant Android du morceau (le même dans Tone Dial et Soul King sur ce téléphone). */
        val songs: Map<Long, Song>,
        val byPath: Map<String, Song>,
        /** Corrections faites à la main dans Tone Dial : titre, artiste. */
        val overrides: Map<Long, Pair<String?, String?>>,
        /** Openings reconnus par Tone Dial : « One Piece Opening 25 ». */
        val openings: Map<Long, String>,
        val playlists: List<Pair<String, List<Long>>>,
        val favorites: Set<Long>,
        val playCounts: Map<Long, Int>,
    ) {
        /** Retrouve le morceau Tone Dial : par identifiant, sinon par emplacement du fichier. */
        fun find(id: Long, path: String?): Song? = songs[id] ?: path?.let { byPath[it] }
    }

    private const val FILE = "tonedial_catalog.json"

    @Volatile
    private var cached: Data? = null
    @Volatile
    private var loaded = false

    private fun file(context: Context) = File(context.filesDir, FILE)

    fun exists(context: Context): Boolean = file(context).exists()

    fun clear(context: Context) {
        file(context).delete()
        cached = null
        loaded = true
    }

    /** Lit la sauvegarde choisie (.zip, ou ancien .json) et garde son contenu. */
    fun import(context: Context, uri: Uri): Data {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            val bytes = input.readBytes()
            // une sauvegarde .zip commence par « PK »
            if (bytes.size > 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()) {
                var json: String? = null
                ZipInputStream(bytes.inputStream()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (entry.name.endsWith("tonedial.json")) {
                            json = zip.readBytes().toString(Charsets.UTF_8)
                            break
                        }
                    }
                }
                json
            } else bytes.toString(Charsets.UTF_8)
        } ?: error("Fichier illisible")
        val root = JSONObject(text ?: error("Ce fichier n'est pas une sauvegarde Tone Dial"))
        if (root.optString("app") != "Tone Dial" && !root.has("songs")) error("Ce fichier n'est pas une sauvegarde Tone Dial")
        val data = parse(root)
        file(context).writeText(root.toString())
        cached = data
        loaded = true
        return data
    }

    fun get(context: Context): Data? {
        if (loaded) return cached
        cached = runCatching { parse(JSONObject(file(context).readText())) }.getOrNull()
        loaded = true
        return cached
    }

    private fun parse(root: JSONObject): Data {
        val songs = HashMap<Long, Song>()
        root.optJSONObject("songs")?.let { o ->
            o.keys().forEach { k ->
                val id = k.toLongOrNull() ?: return@forEach
                val s = o.optJSONObject(k) ?: return@forEach
                songs[id] = Song(id, s.optString("p"), s.optString("t"), s.optString("a"))
            }
        }
        val overrides = HashMap<Long, Pair<String?, String?>>()
        root.optJSONObject("overrides")?.let { o ->
            o.keys().forEach { k ->
                val id = k.toLongOrNull() ?: return@forEach
                val v = o.optJSONObject(k) ?: return@forEach
                overrides[id] = v.optString("t").ifBlank { null } to v.optString("a").ifBlank { null }
            }
        }
        val openings = HashMap<Long, String>()
        root.optJSONObject("openings")?.let { o ->
            o.keys().forEach { k ->
                val id = k.toLongOrNull() ?: return@forEach
                o.optString(k).takeIf { it.isNotBlank() }?.let { openings[id] = it }
            }
        }
        val playlists = ArrayList<Pair<String, List<Long>>>()
        root.optJSONArray("playlists")?.let { arr ->
            for (i in 0 until arr.length()) {
                val p = arr.optJSONObject(i) ?: continue
                val ids = p.optJSONArray("songs") ?: continue
                playlists += p.optString("name", "Playlist") to List(ids.length()) { ids.optLong(it) }
            }
        }
        val favorites = HashSet<Long>()
        root.optJSONArray("favorites")?.let { arr -> for (i in 0 until arr.length()) favorites += arr.optLong(i) }
        val counts = HashMap<Long, Int>()
        root.optJSONObject("playCounts")?.let { o ->
            o.keys().forEach { k -> k.toLongOrNull()?.let { counts[it] = o.optInt(k) } }
        }
        return Data(
            date = root.optLong("date"),
            songs = songs,
            byPath = songs.values.filter { it.path.isNotBlank() }.associateBy { it.path },
            overrides = overrides,
            openings = openings,
            playlists = playlists,
            favorites = favorites,
            playCounts = counts,
        )
    }
}
