package com.thousandlabs.soulking.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thousandlabs.soulking.data.LocalMusic
import com.thousandlabs.soulking.data.Source
import com.thousandlabs.soulking.data.ThemeDef
import com.thousandlabs.soulking.data.Themes
import com.thousandlabs.soulking.game.GameViewModel
import com.thousandlabs.soulking.game.Screen
import com.thousandlabs.soulking.update.Updater

private val quickSearches = listOf("Eminem", "Drill UK", "Michael Jackson")

@Composable
fun HomeScreen(vm: GameViewModel, ensureAudio: (() -> Unit) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val update by Updater.state.collectAsState()
    val dismissed by Updater.bannerDismissed.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                IconBadge(CrownIcon, size = 48.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Soul King", style = MaterialTheme.typography.headlineMedium, color = Sk.Text)
                    Text("Le blind test des soirées", style = MaterialTheme.typography.bodyMedium, color = Sk.TextDim)
                }
                IconButton(onClick = { vm.open(Screen.HOWTO) }) {
                    Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "Comment jouer", tint = Sk.TextDim)
                }
                IconButton(onClick = { vm.open(Screen.ABOUT) }) {
                    Icon(Icons.Outlined.Info, contentDescription = "À propos", tint = Sk.TextDim)
                }
            }
        }

        val available = update as? Updater.State.Available
        if (available != null && !dismissed) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Sk.GoldDim)
                        .clickable { vm.open(Screen.ABOUT) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.SystemUpdate, null, tint = Sk.Gold)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Nouvelle version ${available.versionName} disponible",
                        style = MaterialTheme.typography.titleMedium,
                        color = Sk.Gold,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Outlined.ChevronRight, null, tint = Sk.Gold)
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Un artiste, un genre, un animé…", color = Sk.TextDim) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Sk.TextDim) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focus.clearFocus()
                    vm.search(query)
                }),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Sk.Gold,
                    unfocusedBorderColor = Sk.Divider,
                    cursorColor = Sk.Gold,
                    focusedContainerColor = Sk.Surface,
                    unfocusedContainerColor = Sk.Surface,
                    focusedTextColor = Sk.Text,
                    unfocusedTextColor = Sk.Text,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                quickSearches.take(3).forEach { q ->
                    SkChip(q, selected = false, onClick = { vm.search(q) }, modifier = Modifier.weight(1f))
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            MyMusicCard(onClick = { ensureAudio { vm.loadLocal(); vm.open(Screen.LOCAL) } })
        }

        item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Thèmes") }

        items(Themes.all, key = { it.id }) { def ->
            ThemeCard(def) { vm.choose(Source.Theme(def)) }
        }
    }
}

@Composable
private fun MyMusicCard(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(Sk.GoldDim, Sk.Surface)))
            .border(1.dp, Sk.GoldDim, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(Icons.Outlined.LibraryMusic, size = 52.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Ma musique", style = MaterialTheme.typography.titleLarge, color = Sk.Text, fontWeight = FontWeight.Bold)
            Text(
                "Jouez avec les morceaux de votre téléphone, même sans connexion",
                style = MaterialTheme.typography.bodySmall,
                color = Sk.TextDim,
            )
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = Sk.Gold)
    }
}

@Composable
private fun ThemeCard(def: ThemeDef, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(142.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Sk.Surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        IconBadge(def.icon, size = 40.dp)
        Column {
            Text(def.title, style = MaterialTheme.typography.titleMedium, color = Sk.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(def.subtitle, style = MaterialTheme.typography.bodySmall, color = Sk.TextDim, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
