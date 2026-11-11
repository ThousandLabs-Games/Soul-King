package com.thousandlabs.soulking.data

import android.net.Uri
import java.text.Normalizer

/** Un morceau jouable : extrait officiel en ligne ou fichier du téléphone. */
data class Track(
    /** « dz:123 » pour Deezer, « local:456 » pour un fichier du téléphone. */
    val key: String,
    val title: String,
    val artist: String,
    /** Animé d'origine, pour les openings. */
    val anime: String? = null,
    /** Précision affichée à la révélation, ex. « Opening 25 ». */
    val label: String? = null,
    val coverUrl: String? = null,
    val previewUrl: String? = null,
    val localUri: Uri? = null,
    val artUri: Uri? = null,
    val durationMs: Long = 30_000,
    val deezerId: Long? = null,
    /** Album et titre complet : servent seulement à vérifier les morceaux choisis à la main. */
    val album: String? = null,
    val rawTitle: String? = null,
) {
    val isLocal: Boolean get() = localUri != null

    /** Ce qu'il faut deviner à la place de l'artiste : l'animé quand il est connu. */
    val who: String get() = anime ?: artist

    val playUri: Uri get() = localUri ?: Uri.parse(previewUrl ?: "")

    val cover: Any? get() = coverUrl ?: artUri

    /** Deux versions du même morceau comptent pour un seul. */
    val dedupeKey: String get() = norm(title) + "|" + norm(artist.substringBefore(",").substringBefore(" feat")) + "|" + norm(anime ?: "")
}

private val marks = Regex("\\p{M}+")
private val brackets = Regex("\\(.*?\\)|\\[.*?]")
private val nonAlnum = Regex("[^a-z0-9]+")

/** Texte simplifié pour comparer : minuscules, sans accents, sans parenthèses ni ponctuation. */
fun norm(s: String): String =
    Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
        .replace(marks, "")
        .replace(brackets, " ")
        .replace(nonAlnum, " ")
        .trim()

/** Comme [norm], mais garde le contenu des parenthèses (« Générique de Code Lyoko »). */
fun normKeep(s: String): String =
    Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
        .replace(marks, "")
        .replace(nonAlnum, " ")
        .trim()
