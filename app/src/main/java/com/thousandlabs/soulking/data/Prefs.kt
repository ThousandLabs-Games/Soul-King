package com.thousandlabs.soulking.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** Réglages et petites données gardées sur le téléphone uniquement. */
object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        if (!::sp.isInitialized) sp = context.getSharedPreferences("soulking", Context.MODE_PRIVATE)
    }

    fun getBool(key: String, def: Boolean): Boolean = sp.getBoolean(key, def)
    fun putBool(key: String, v: Boolean) = sp.edit().putBoolean(key, v).apply()
    fun getInt(key: String, def: Int): Int = sp.getInt(key, def)
    fun putInt(key: String, v: Int) = sp.edit().putInt(key, v).apply()
    fun getString(key: String): String? = sp.getString(key, null)
    fun putString(key: String, v: String) = sp.edit().putString(key, v).apply()

    private fun list(key: String): List<String> = runCatching {
        val a = JSONArray(getString(key) ?: "[]")
        List(a.length()) { a.getString(it) }
    }.getOrDefault(emptyList())

    // ---- joueurs de la dernière soirée ----
    fun playerNames(): List<String> = list("players").ifEmpty { listOf("Joueur 1", "Joueur 2") }
    fun savePlayerNames(names: List<String>) = putString("players", JSONArray(names).toString())

    // ---- morceaux déjà joués : ne reviennent pas avant longtemps ----
    private const val RECENT_MAX = 400
    fun recent(): Set<String> = list("recent").toHashSet()
    fun addRecent(key: String) {
        val merged = (list("recent").filter { it != key } + key).takeLast(RECENT_MAX)
        putString("recent", JSONArray(merged).toString())
    }
    fun clearRecent() = putString("recent", "[]")
    fun recentCount(): Int = list("recent").size

    // ---- identifiants d'artistes déjà trouvés : chargement plus rapide la fois suivante ----
    private fun artistCache(): JSONObject = runCatching { JSONObject(getString("artist_ids") ?: "{}") }.getOrDefault(JSONObject())

    @Synchronized
    fun artistId(name: String): Long? {
        val o = artistCache()
        val k = name.lowercase()
        return if (o.has(k)) o.optLong(k) else null
    }

    @Synchronized
    fun putArtistId(name: String, id: Long) {
        val o = artistCache()
        o.put(name.lowercase(), id)
        putString("artist_ids", o.toString())
    }

    // ---- moment le plus connu de chaque morceau, marqué par l'utilisateur ----
    private fun hooks(): JSONObject = runCatching { JSONObject(getString("hooks") ?: "{}") }.getOrDefault(JSONObject())

    fun hook(key: String): Long? {
        val o = hooks()
        return if (o.has(key)) o.optLong(key) else null
    }

    fun setHook(key: String, ms: Long) {
        val o = hooks()
        o.put(key, ms)
        putString("hooks", o.toString())
    }

    fun clearHook(key: String) {
        val o = hooks()
        o.remove(key)
        putString("hooks", o.toString())
    }

    fun hookCount(): Int = hooks().length()

    // ---- versions écartées par l'utilisateur : plus jamais proposées ----
    fun banned(): Set<String> = list("banned").toHashSet()

    fun ban(key: String) {
        val merged = (list("banned").filter { it != key } + key).takeLast(2000)
        putString("banned", JSONArray(merged).toString())
    }

    fun unbanAll() = putString("banned", "[]")
    fun bannedCount(): Int = list("banned").size
}
