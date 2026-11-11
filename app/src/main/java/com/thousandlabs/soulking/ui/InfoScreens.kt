package com.thousandlabs.soulking.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import coil.imageLoader
import com.thousandlabs.soulking.BuildConfig
import com.thousandlabs.soulking.data.Prefs
import com.thousandlabs.soulking.game.GameViewModel
import com.thousandlabs.soulking.update.Updater

const val REPO_URL = "https://github.com/ThousandLabs-Games/Soul-King"
const val RELEASES_URL = "https://github.com/ThousandLabs-Games/Soul-King/releases"
const val SITE_URL = "https://thousandlabs-games.github.io"

// ---------------------------------------------------------------- guide intégré

@Composable
fun HowToScreen(vm: GameViewModel) {
    Column(Modifier.fillMaxSize()) {
        TopBar("Comment jouer", onBack = vm::back)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GuideCard(Icons.Outlined.Search, "1. Choisir la musique", listOf(
                "Touchez un thème (Rap US, Drill, Hits mondiaux, Openings cultes, Spécial One Piece, Génériques…).",
                "Pour les génériques de dessins animés, il faut trouver le dessin animé.",
                "Ou tapez ce que vous voulez dans la recherche : un artiste, un genre, un animé. Soul King trouve les morceaux tout seul.",
                "Ou « Ma musique » pour jouer avec les morceaux du téléphone, même sans connexion.",
            ))
            GuideCard(Icons.Outlined.Groups, "2. Les joueurs", listOf(
                "De 1 à 4 joueurs ou équipes, chacun avec sa couleur.",
                "À deux, posez le téléphone entre vous : la zone du haut est retournée pour celui qui est en face.",
                "Les noms sont gardés pour la soirée suivante.",
            ))
            GuideCard(Icons.Outlined.Tune, "3. Les réglages", listOf(
                "Nombre de manches, durée de l'extrait (2 à 15 secondes).",
                "Réponse en 4 propositions, ou à l'oral avec un arbitre qui valide.",
                "Deviner le titre, l'artiste (le chanteur), les deux, ou l'animé pour les thèmes animés.",
                "Extrait : le moment le plus connu, au hasard dans le morceau, ou depuis le début.",
            ))
            GuideCard(Icons.Outlined.Bolt, "4. Pendant la manche", listOf(
                "Le premier qui appuie sur sa zone répond. La musique se met en pause.",
                "Mauvaise réponse : 50 points en moins et le joueur est bloqué jusqu'à la manche suivante. Les autres peuvent encore tenter.",
                "Extrait fini sans réponse : les buzzers restent ouverts. « Réécouter » rejoue l'extrait, mais les points sont divisés par deux.",
            ))
            GuideCard(Icons.Outlined.Bookmark, "Le moment le plus connu", listOf(
                "Avec ce réglage, l'extrait part du refrain : en ligne, l'extrait officiel est souvent pris autour de lui ; pour vos fichiers, il est estimé.",
                "Pendant la révélation, le morceau continue : touchez « Retenir ce passage » au bon moment.",
                "La fois suivante, ce morceau démarrera exactement là. Touchez à nouveau pour oublier le passage.",
            ))
            GuideCard(Icons.Outlined.EmojiEvents, "5. Les points", listOf(
                "Bonne réponse : 100 points, plus jusqu'à 100 points de bonus selon la rapidité.",
                "La dernière manche compte double (réglage « Dernière manche à points doubles »).",
                "En solo, Soul King garde votre record pour chaque thème.",
                "À la fin : podium, réponse la plus rapide, meilleure série. « Revanche » relance avec de nouveaux morceaux.",
                "Un morceau déjà joué ne revient pas avant longtemps.",
                "Mauvaise version (instrumental, reprise, remix) ? Touchez « Mauvaise version ? » à la révélation : elle ne sera plus jamais proposée.",
            ))
            GuideCard(Icons.Outlined.LibraryMusic, "Votre catalogue Tone Dial", listOf(
                "Dans Tone Dial : Menu › Réglages › « Sauvegarder mes données ».",
                "Dans Soul King : Ma musique › « Importer ma sauvegarde Tone Dial », puis choisissez le fichier.",
                "Soul King reprend vos noms corrigés et vos openings reconnus, et propose de jouer avec vos favoris, vos plus écoutés et vos playlists.",
                "Après de nouvelles corrections dans Tone Dial, refaites une sauvegarde et importez-la à nouveau.",
            ))
            GuideCard(Icons.Outlined.LibraryMusic, "Astuce : vos openings", listOf(
                "Nommez vos fichiers « One Piece Opening 25 - The Peak » : Soul King reconnaît l'animé et le numéro de l'opening.",
                "Dans « Spécial One Piece » et « Openings cultes », vos fichiers s'ajoutent automatiquement aux morceaux en ligne.",
            ))
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun GuideCard(icon: ImageVector, title: String, lines: List<String>) {
    InfoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = Sk.Text)
        }
        Spacer(Modifier.height(10.dp))
        lines.forEach { line ->
            Row(Modifier.padding(vertical = 3.dp)) {
                Text("•", color = Sk.Gold, modifier = Modifier.width(16.dp))
                Text(line, style = MaterialTheme.typography.bodyMedium, color = Sk.TextDim)
            }
        }
    }
}

// ---------------------------------------------------------------- à propos, mises à jour, nouveautés

@OptIn(coil.annotation.ExperimentalCoilApi::class)
@Composable
fun AboutScreen(vm: GameViewModel) {
    val uri = LocalUriHandler.current
    val ctx = LocalContext.current
    val state by Updater.state.collectAsState()
    var auto by remember { mutableStateOf(Updater.autoEnabled()) }
    var recent by remember { mutableStateOf(Prefs.recentCount()) }
    var bannedCount by remember { mutableStateOf(Prefs.bannedCount()) }
    var cacheBytes by remember { mutableStateOf(cacheSize(ctx.cacheDir)) }

    Column(Modifier.fillMaxSize()) {
        TopBar("À propos", onBack = vm::back)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(CrownIcon, size = 52.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Soul King", style = MaterialTheme.typography.headlineSmall, color = Sk.Text)
                        Text("Version ${BuildConfig.VERSION_NAME} · Thousand Labs Games", style = MaterialTheme.typography.bodySmall, color = Sk.TextDim)
                    }
                }
            }

            SectionTitle("Mises à jour")
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.SystemUpdate, null, tint = Sk.Gold)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        when (val s = state) {
                            Updater.State.Idle -> "Touchez « Vérifier » pour chercher une nouvelle version."
                            Updater.State.Checking -> "Vérification…"
                            Updater.State.UpToDate -> "Soul King est à jour."
                            is Updater.State.Available -> "Version ${s.versionName} disponible."
                            is Updater.State.Downloading -> "Téléchargement… ${(s.progress * 100).toInt()} %"
                            is Updater.State.Installing -> "Installation en cours…"
                            is Updater.State.Error -> s.message
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Sk.Text,
                        modifier = Modifier.weight(1f),
                    )
                    if (state is Updater.State.Checking || state is Updater.State.Downloading || state is Updater.State.Installing) {
                        CircularProgressIndicator(color = Sk.Gold, strokeWidth = 2.dp, modifier = Modifier.width(20.dp).height(20.dp))
                    }
                }
                val s = state
                if (s is Updater.State.Available && s.notes.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(s.notes, style = MaterialTheme.typography.bodySmall, color = Sk.TextDim)
                }
                Spacer(Modifier.height(12.dp))
                val installable = s is Updater.State.Available || (s is Updater.State.Error && s.release != null)
                if (installable) {
                    GoldButton("Installer la mise à jour", Icons.Outlined.SystemUpdate, onClick = {
                        if (Updater.canInstall(ctx)) Updater.downloadAndInstall(ctx)
                        else ctx.startActivity(Updater.installPermissionIntent(ctx))
                    }, modifier = Modifier.fillMaxWidth())
                } else {
                    GhostButton("Vérifier", Icons.Outlined.SystemUpdate, onClick = { Updater.check() }, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Vérifier automatiquement au lancement", style = MaterialTheme.typography.bodyMedium, color = Sk.TextDim, modifier = Modifier.weight(1f))
                    Switch(
                        checked = auto,
                        onCheckedChange = { auto = it; Updater.setAutoEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Sk.OnGold, checkedTrackColor = Sk.Gold),
                    )
                }
                Text(
                    "Les mises à jour viennent uniquement du dépôt officiel et sont signées : Android refuse toute version qui ne vient pas de là.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Sk.TextDim,
                )
            }

            SectionTitle("Nouveautés de la version ${BuildConfig.VERSION_NAME}")
            ReleaseNotes(listOf(
                "Dernière manche à points doubles, pour garder le suspense jusqu'au bout (désactivable dans les réglages)",
                "En solo : record personnel pour chaque thème, et « Nouveau record » sur le podium",
                "Ma musique : noms de téléchargement nettoyés comme dans Tone Dial, beaucoup plus d'artistes reconnus",
                "Import du catalogue Tone Dial : noms corrigés, openings, favoris, plus écoutés et playlists",
                "Génériques : versions officielles et d'avant 2023 privilégiées, bouton « Mauvaise version ? »",
            ))

            SectionTitle("Vos données")
            InfoCard {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Lock, null, tint = Sk.Gold)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Aucun compte, aucune publicité, aucun suivi. Vos fichiers musicaux ne quittent jamais le téléphone. " +
                            "En ligne, seule votre recherche est envoyée au catalogue Deezer pour obtenir les extraits officiels de 30 secondes, " +
                        "et le nom de l'animé ou du dessin animé à MyAnimeList (Jikan) ou TVmaze pour afficher son image.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Sk.TextDim,
                    )
                }
                Spacer(Modifier.height(12.dp))
                GhostButton(
                    if (recent > 0) "Oublier les $recent morceaux déjà joués" else "Aucun morceau joué pour l'instant",
                    null,
                    onClick = { Prefs.clearRecent(); recent = 0 },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (bannedCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    GhostButton(
                        "Rétablir les $bannedCount versions écartées",
                        null,
                        onClick = { Prefs.unbanAll(); bannedCount = 0 },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            SectionTitle("Stockage")
            InfoCard {
                Text(
                    "Fichiers temporaires : ${formatBytes(cacheBytes)}. Les pochettes sont limitées à 5 Mo et les plus anciennes s'effacent d'elles-mêmes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Sk.TextDim,
                )
                Spacer(Modifier.height(12.dp))
                GhostButton("Vider les fichiers temporaires", null, onClick = {
                    runCatching { ctx.imageLoader.diskCache?.clear() }
                    runCatching { ctx.imageLoader.memoryCache?.clear() }
                    runCatching { ctx.cacheDir.listFiles()?.forEach { it.deleteRecursively() } }
                    cacheBytes = cacheSize(ctx.cacheDir)
                }, modifier = Modifier.fillMaxWidth())
            }

            SectionTitle("Liens officiels")
            LinkRow(Icons.Outlined.Code, "Dépôt GitHub de Soul King", REPO_URL) { uri.openUri(REPO_URL) }
            LinkRow(Icons.Outlined.SystemUpdate, "Toutes les versions", RELEASES_URL) { uri.openUri(RELEASES_URL) }
            LinkRow(CrownIcon, "Thousand Labs Games", SITE_URL) { uri.openUri(SITE_URL) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun cacheSize(dir: java.io.File): Long =
    runCatching { dir.walkTopDown().filter { it.isFile }.sumOf { it.length() } }.getOrDefault(0L)

private fun formatBytes(b: Long): String = when {
    b < 1024 * 1024 -> "${b / 1024} Ko"
    else -> "%.1f Mo".format(b / (1024.0 * 1024.0)).replace('.', ',')
}

@Composable
private fun ReleaseNotes(lines: List<String>) {
    InfoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.NewReleases, null, tint = Sk.Gold)
            Spacer(Modifier.width(10.dp))
            Text("Ce qui change", style = MaterialTheme.typography.titleMedium, color = Sk.Text)
        }
        Spacer(Modifier.height(8.dp))
        lines.forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium, color = Sk.TextDim, modifier = Modifier.padding(vertical = 2.dp)) }
    }
}

@Composable
private fun LinkRow(icon: ImageVector, title: String, url: String, onClick: () -> Unit) {
    InfoCard {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Sk.Gold)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Sk.Text)
                Text(url.removePrefix("https://"), style = MaterialTheme.typography.bodySmall, color = Sk.TextDim)
            }
            Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, tint = Sk.TextDim)
        }
    }
}
