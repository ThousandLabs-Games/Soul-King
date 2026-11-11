package com.thousandlabs.soulking.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thousandlabs.soulking.data.Source
import com.thousandlabs.soulking.game.AnswerMode
import com.thousandlabs.soulking.game.GameConfig
import com.thousandlabs.soulking.game.GameViewModel
import com.thousandlabs.soulking.game.GuessTarget

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupScreen(vm: GameViewModel, ensureAudio: (() -> Unit) -> Unit) {
    val source by vm.source.collectAsState()
    val cfg by vm.config.collectAsState()
    val names by vm.playerNames.collectAsState()
    val local by vm.local.collectAsState()
    val src = source ?: return

    Column(Modifier.fillMaxSize()) {
        TopBar("Nouvelle partie", onBack = vm::back)
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            item {
                InfoCard {
                    Text(src.name, style = MaterialTheme.typography.headlineSmall, color = Sk.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(src.description, style = MaterialTheme.typography.bodyMedium, color = Sk.TextDim)
                }
            }

            if (src is Source.Theme) {
                item {
                    val count = if (local != null) vm.localMatches(src) else null
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Sk.Surface)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.LibraryMusic, null, tint = Sk.Gold)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Ajouter mes morceaux", style = MaterialTheme.typography.titleMedium, color = Sk.Text)
                            Text(
                                when {
                                    !cfg.includeLocal -> "Les morceaux de votre téléphone qui vont avec ce thème"
                                    count == null -> "Recherche dans votre téléphone…"
                                    count == 0 -> "Aucun morceau du téléphone ne correspond"
                                    else -> "$count morceau${if (count > 1) "x" else ""} du téléphone en plus"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Sk.TextDim,
                            )
                        }
                        Switch(
                            checked = cfg.includeLocal && vm.hasAudioPermission(),
                            onCheckedChange = { on ->
                                if (on) ensureAudio {
                                    vm.updateConfig { it.copy(includeLocal = true) }
                                    vm.loadLocal()
                                } else vm.updateConfig { it.copy(includeLocal = false) }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Sk.OnGold, checkedTrackColor = Sk.Gold),
                        )
                    }
                }
            }

            item { SectionTitle("Joueurs") }
            itemsIndexed(names) { i, name ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(16.dp).clip(CircleShape).background(Sk.player(i)))
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { vm.setPlayerName(i, it) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Sk.player(i),
                            unfocusedBorderColor = Sk.Divider,
                            cursorColor = Sk.player(i),
                            focusedContainerColor = Sk.Surface,
                            unfocusedContainerColor = Sk.Surface,
                            focusedTextColor = Sk.Text,
                            unfocusedTextColor = Sk.Text,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    if (names.size > 1) {
                        IconButton(onClick = { vm.removePlayer(i) }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Retirer", tint = Sk.TextDim)
                        }
                    }
                }
            }
            if (names.size < 4) {
                item {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { vm.addPlayer() }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Add, null, tint = Sk.Gold)
                        Spacer(Modifier.width(8.dp))
                        Text("Ajouter un joueur", style = MaterialTheme.typography.labelLarge, color = Sk.Gold)
                    }
                }
            }
            item {
                Text(
                    if (names.size == 1) "Seul : les propositions s'affichent directement pendant l'extrait."
                    else "Chaque joueur a sa zone de buzzer. Le premier qui appuie répond.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Sk.TextDim,
                )
            }

            item { SectionTitle("Manches") }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GameConfig.ROUND_CHOICES.forEach { n ->
                        SkChip("$n", cfg.rounds == n, onClick = { vm.updateConfig { it.copy(rounds = n) } })
                    }
                }
            }

            item { SectionTitle("Durée de l'extrait") }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GameConfig.CLIP_CHOICES.forEach { n ->
                        SkChip("$n s", cfg.clipSec == n, onClick = { vm.updateConfig { it.copy(clipSec = n) } })
                    }
                }
            }

            item { SectionTitle("Réponse") }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnswerMode.entries.forEach { m ->
                        SkChip(m.label, cfg.answer == m, onClick = { vm.updateConfig { it.copy(answer = m) } })
                    }
                }
            }
            item {
                Text(
                    if (cfg.answer == AnswerMode.QCM) "Le joueur choisit parmi 4 propositions."
                    else "Le joueur répond à voix haute, celui qui tient le téléphone valide.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Sk.TextDim,
                )
            }

            item { SectionTitle("À deviner") }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GuessTarget.entries.forEach { t ->
                        SkChip(t.label, cfg.target == t, onClick = { vm.updateConfig { it.copy(target = t) } })
                    }
                }
            }

            item { SectionTitle("Extrait") }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkChip("Au hasard dans le morceau", cfg.randomStart, onClick = { vm.updateConfig { it.copy(randomStart = true) } })
                    SkChip("Depuis le début", !cfg.randomStart, onClick = { vm.updateConfig { it.copy(randomStart = false) } })
                }
            }

            item { SectionTitle("Ambiance") }
            item {
                InfoCard {
                    ToggleLine("Sons du jeu", cfg.sounds) { on -> vm.updateConfig { it.copy(sounds = on) } }
                    ToggleLine("Vibrations", cfg.vibrations) { on -> vm.updateConfig { it.copy(vibrations = on) } }
                }
            }
        }

        GoldButton(
            text = "Lancer la partie",
            icon = Icons.Outlined.PlayArrow,
            onClick = vm::startGame,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}

@Composable
private fun ToggleLine(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = Sk.Text, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Sk.OnGold, checkedTrackColor = Sk.Gold),
        )
    }
}
