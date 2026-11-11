package com.thousandlabs.soulking.data

import org.json.JSONObject
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Origine d'un morceau qui a sa propre image : un animé ou un dessin animé. */
enum class ArtKind { ANIME, CARTOON }

/**
 * Image officielle de l'animé (MyAnimeList, via Jikan) ou du dessin animé (TVmaze),
 * pour la révélation. Aucun compte, aucune donnée personnelle : seulement le nom de l'émission.
 * Les adresses trouvées sont gardées sur le téléphone. Fonctions bloquantes : Dispatchers.IO.
 */
object ArtSearch {

    /** Noms sous lesquels les bases d'images connaissent les émissions. */
    private val searchNames: Map<String, String> = mapOf(
        // animés
        "L'Attaque des Titans" to "Shingeki no Kyojin",
        "Naruto Shippuden" to "Naruto: Shippuuden",
        "Demon Slayer" to "Kimetsu no Yaiba",
        "Your Lie in April" to "Shigatsu wa Kimi no Uso",
        "My Hero Academia" to "Boku no Hero Academia",
        "Erased" to "Boku dake ga Inai Machi",
        "Fire Force" to "Enen no Shouboutai",
        "Hunter x Hunter" to "Hunter x Hunter (2011)",
        "Code Geass" to "Code Geass: Hangyaku no Lelouch",
        "Fullmetal Alchemist: Brotherhood" to "Fullmetal Alchemist: Brotherhood",
        // dessins animés
        "Miraculous" to "Miraculous: Tales of Ladybug & Cat Noir",
        "Les Lapins Crétins" to "Rabbids Invasion",
        "Souvenirs de Gravity Falls" to "Gravity Falls",
        "Kung Fu Panda" to "Kung Fu Panda: Legends of Awesomeness",
        "Totally Spies" to "Totally Spies!",
        "Bob l'éponge" to "SpongeBob SquarePants",
        "Oggy et les Cafards" to "Oggy and the Cockroaches",
        "Phinéas et Ferb" to "Phineas and Ferb",
        "Les Supers Nanas" to "The Powerpuff Girls",
        "Le Laboratoire de Dexter" to "Dexter's Laboratory",
        "Danny Fantôme" to "Danny Phantom",
        "Avatar, le dernier maître de l'air" to "Avatar: The Last Airbender",
        "Martin Mystère" to "Martin Mystery",
        "Les Razmoket" to "Rugrats",
        "Hé Arnold" to "Hey Arnold!",
        "Jimmy Neutron" to "The Adventures of Jimmy Neutron: Boy Genius",
        "Digimon" to "Digimon Adventure",
        "Olive et Tom" to "Captain Tsubasa",
        "Zig et Sharko" to "Zig & Sharko",
        "Lilo et Stitch" to "Lilo & Stitch: The Series",
        "Les Tortues Ninja" to "Teenage Mutant Ninja Turtles",
        "Les Zinzins de l'espace" to "Space Goofs",
        "Foster, la maison des amis imaginaires" to "Foster's Home for Imaginary Friends",
        "Ed, Edd et Eddy" to "Ed, Edd n Eddy",
        "Courage, le chien froussard" to "Courage the Cowardly Dog",
        "Samouraï Jack" to "Samurai Jack",
        "Les Pingouins de Madagascar" to "The Penguins of Madagascar",
        "Bakugan" to "Bakugan Battle Brawlers",
        "Le Monde incroyable de Gumball" to "The Amazing World of Gumball",
        "Mes parrains sont magiques" to "The Fairly OddParents",
        "Détective Conan" to "Detective Conan",
        "Les Simpson" to "The Simpsons",
        "Dora l'exploratrice" to "Dora the Explorer",
        "Yu-Gi-Oh!" to "Yu-Gi-Oh!",
    ).mapKeys { norm(it.key) }

    private fun cacheKey(kind: ArtKind, name: String) = "${kind.name}:${norm(name)}"

    @Synchronized
    private fun cache(): JSONObject = runCatching { JSONObject(Prefs.getString("art_cache") ?: "{}") }.getOrDefault(JSONObject())

    @Synchronized
    private fun remember(key: String, url: String) {
        val o = cache()
        o.put(key, url)
        Prefs.putString("art_cache", o.toString())
    }

    /** Adresse de l'image, ou null si l'émission est introuvable ou sans connexion. */
    fun find(kind: ArtKind, name: String): String? {
        val key = cacheKey(kind, name)
        val cached = cache()
        if (cached.has(key)) return cached.optString(key).ifBlank { null }
        val query = searchNames[norm(name)] ?: name
        val result = runCatching {
            when (kind) {
                ArtKind.ANIME -> jikan(query)
                ArtKind.CARTOON -> tvmaze(query)
            }
        }
        // introuvable : on le note pour ne pas redemander ; erreur réseau : on réessaiera plus tard
        result.onSuccess { remember(key, it ?: "") }
        return result.getOrNull()
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    /** null : la base n'a rien trouvé (404). Une erreur réseau lève une exception. */
    private fun get(url: String): JSONObject? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 10_000
            setRequestProperty("User-Agent", "SoulKing")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            when (conn.responseCode) {
                404 -> null
                in 200..299 -> JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                else -> error("HTTP ${conn.responseCode}")
            }
        } catch (_: FileNotFoundException) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun jikan(q: String): String? {
        val data = get("https://api.jikan.moe/v4/anime?q=${enc(q)}&limit=1&sfw=true")?.optJSONArray("data") ?: return null
        val first = data.optJSONObject(0) ?: return null
        val images = first.optJSONObject("images") ?: return null
        val jpg = images.optJSONObject("jpg")
        return jpg?.optString("large_image_url")?.takeIf { it.startsWith("http") }
            ?: jpg?.optString("image_url")?.takeIf { it.startsWith("http") }
    }

    private fun tvmaze(q: String): String? {
        val show = get("https://api.tvmaze.com/singlesearch/shows?q=${enc(q)}") ?: return null
        val image = show.optJSONObject("image") ?: return null
        return image.optString("original").takeIf { it.startsWith("http") }
            ?: image.optString("medium").takeIf { it.startsWith("http") }
    }
}
