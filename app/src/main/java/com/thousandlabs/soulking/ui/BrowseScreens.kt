package com.thousandlabs.soulking.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.MusicOff
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.thousandlabs.soulking.data.LocalMusic
import com.thousandlabs.soulking.data.Source
import com.thousandlabs.soulking.game.GameViewModel

// ---------------------------------------------------------------- recherche

@Composable
fun SearchScreen(vm: GameViewModel) {
    val s by vm.search.collectAsState()
    Column(Modifier.fillMaxSize()) {
        TopBar("Recherche", onBack = vm::back)
        when {
            s.loading -> CenterMessage(null, "Recherche de « ${s.query} »…", loading = true)
            s.error != null -> CenterMessage(Icons.Outlined.WifiOff, s.error ?: "") {
                GhostButton("Réessayer", Icons.Outlined.Refresh, onClick = { vm.search(s.query) })
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    SourceRow(
                        icon = Icons.Outlined.Search,
                        picture = null,
                        title = "Tout ce qui correspond à « ${s.query} »",
                        subtitle = "Une partie avec les morceaux trouvés",
                        onClick = { vm.choose(Source.Search(s.query)) },
                    )
                }
                if (s.artists.isNotEmpty()) {
                    item { SectionTitle("Artistes") }
                    items(s.artists, key = { "a${it.id}" }) { a ->
                        SourceRow(
                            icon = Icons.Outlined.Person,
                            picture = a.picture,
                            round = true,
                            title = a.name,
                            subtitle = "Uniquement ses morceaux les plus connus",
                            onClick = { vm.choose(Source.Artist(a.id, a.name, a.picture)) },
                        )
                    }
                }
                if (s.playlists.isNotEmpty()) {
                    item { SectionTitle("Playlists") }
                    items(s.playlists, key = { "p${it.id}" }) { p ->
                        SourceRow(
                            icon = Icons.AutoMirrored.Outlined.QueueMusic,
                            picture = p.picture,
                            title = p.title,
                            subtitle = "${p.count} titres",
                            onClick = { vm.choose(Source.Playlist(p.id, p.title, p.picture, p.count)) },
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- musique du téléphone

@Composable
fun LocalScreen(vm: GameViewModel) {
    val tracks by vm.local.collectAsState()
    Column(Modifier.fillMaxSize()) {
        TopBar("Ma musique", onBack = vm::back, actions = {
            IconButton(onClick = { vm.loadLocal(force = true) }) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Actualiser", tint = Sk.TextDim)
            }
        })
        val list = tracks
        when {
            list == null -> CenterMessage(null, "Lecture de votre bibliothèque…", loading = true)
            list.size < 4 -> CenterMessage(
                Icons.Outlined.MusicOff,
                "Aucun morceau trouvé sur ce téléphone, ou pas assez pour une partie (il en faut au moins 4).",
            )
            else -> {
                val (animes, artists) = LocalMusic.groups(list)
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        SourceRow(
                            icon = Icons.Outlined.LibraryMusic,
                            picture = null,
                            title = "Toute ma musique",
                            subtitle = "${list.size} morceaux",
                            onClick = { vm.choose(Source.Local(LocalMusic.Filter("Toute ma musique", LocalMusic.Kind.ALL))) },
                        )
                    }
                    if (animes.isNotEmpty()) {
                        item { SectionTitle("Animés") }
                        items(animes, key = { "an" + it.filter.label }) { g ->
                            SourceRow(Icons.Outlined.LiveTv, null, title = g.filter.label, subtitle = "${g.count} morceaux", onClick = { vm.choose(Source.Local(g.filter)) })
                        }
                    }
                    if (artists.isNotEmpty()) {
                        item { SectionTitle("Artistes") }
                        items(artists, key = { "ar" + it.filter.label }) { g ->
                            SourceRow(Icons.Outlined.Person, null, title = g.filter.label, subtitle = "${g.count} morceaux", onClick = { vm.choose(Source.Local(g.filter)) })
                        }
                    }
                    item {
                        Text(
                            "Vos fichiers ne quittent jamais le téléphone. Les animés sont reconnus quand l'artiste est écrit « Nom de l'animé Opening 12 ».",
                            style = MaterialTheme.typography.bodySmall,
                            color = Sk.TextDim,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- éléments partagés

@Composable
fun SourceRow(
    icon: ImageVector,
    picture: String?,
    title: String,
    subtitle: String,
    round: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Sk.Surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (picture != null) {
            AsyncImage(
                model = picture,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(if (round) CircleShape else RoundedCornerShape(10.dp))
                    .background(Sk.SurfaceHigh),
            )
        } else IconBadge(icon, size = 48.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Sk.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Sk.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = Sk.TextDim)
    }
}

@Composable
fun CenterMessage(
    icon: ImageVector?,
    text: String,
    loading: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (loading) CircularProgressIndicator(color = Sk.Gold)
            else if (icon != null) IconBadge(icon, size = 64.dp)
            Spacer(Modifier.height(18.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge, color = Sk.TextDim, textAlign = TextAlign.Center)
            if (action != null) {
                Spacer(Modifier.height(18.dp))
                action()
            }
        }
    }
}
