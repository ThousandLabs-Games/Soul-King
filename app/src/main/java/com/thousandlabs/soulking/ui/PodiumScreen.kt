package com.thousandlabs.soulking.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thousandlabs.soulking.game.GameViewModel
import com.thousandlabs.soulking.game.PlayerState

@Composable
fun PodiumScreen(vm: GameViewModel) {
    val g by vm.game.collectAsState()
    BackHandler { vm.home() }
    val ranked = g.players.withIndex().sortedByDescending { it.value.score }
    var grown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { grown = true }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        IconBadge(CrownIcon, size = 72.dp)
        Spacer(Modifier.height(12.dp))
        val champion = ranked.firstOrNull()
        if (g.solo) {
            Text("Partie terminée", style = MaterialTheme.typography.headlineMedium, color = Sk.Text)
            Spacer(Modifier.height(6.dp))
            Text("${champion?.value?.score ?: 0} points", style = MaterialTheme.typography.headlineSmall, color = Sk.Gold)
            Text(
                "${champion?.value?.correct ?: 0} bonnes réponses sur ${g.roundCount}",
                style = MaterialTheme.typography.bodyLarge,
                color = Sk.TextDim,
            )
            Spacer(Modifier.height(10.dp))
            when {
                g.newRecord -> Text(
                    if (g.bestBefore != null) "Nouveau record ! L'ancien était de ${g.bestBefore} points." else "Premier record sur ce thème !",
                    style = MaterialTheme.typography.titleMedium,
                    color = Sk.Gold,
                    textAlign = TextAlign.Center,
                )
                g.bestBefore != null -> Text(
                    "Votre record sur ce thème : ${g.bestBefore} points",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Sk.TextDim,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            val tie = ranked.size > 1 && ranked[0].value.score == ranked[1].value.score
            Text(
                if (tie) "Égalité au sommet" else "${champion?.value?.name} est le Soul King",
                style = MaterialTheme.typography.headlineMedium,
                color = Sk.Text,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            // podium : 2e, 1er, 3e
            Row(
                Modifier.fillMaxWidth().height(230.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                val order = listOf(1, 0, 2)
                val heights = listOf(130.dp, 180.dp, 95.dp)
                order.forEachIndexed { slot, rank ->
                    val entry = ranked.getOrNull(rank)
                    Box(Modifier.weight(1f)) {
                        if (entry != null) Pillar(entry.value, Sk.player(entry.index), rank + 1, heights[slot], grown)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ranked.forEachIndexed { rank, (i, p) -> ScoreRow(rank + 1, p.name, p.score, Sk.player(i), highlight = rank == 0) }
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("Les records de la partie", Modifier.fillMaxWidth())
        val fastest = g.players.withIndex().filter { it.value.fastestMs != null }.minByOrNull { it.value.fastestMs ?: Long.MAX_VALUE }
        val streak = g.players.withIndex().maxByOrNull { it.value.bestStreak }
        val sharp = g.players.withIndex().maxByOrNull { it.value.correct }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Stat(
                Icons.Outlined.Bolt,
                "Réponse la plus rapide",
                fastest?.let { (if (g.solo) "" else "${it.value.name} · ") + formatSeconds(it.value.fastestMs ?: 0) } ?: "Aucune",
            )
            Stat(
                Icons.Outlined.Whatshot,
                "Meilleure série",
                streak?.takeIf { it.value.bestStreak > 0 }?.let { (if (g.solo) "" else "${it.value.name} · ") + "${it.value.bestStreak} d'affilée" } ?: "Aucune",
            )
            if (!g.solo) {
                Stat(
                    Icons.Outlined.CheckCircle,
                    "Le plus de bonnes réponses",
                    sharp?.takeIf { it.value.correct > 0 }?.let { "${it.value.name} · ${it.value.correct}" } ?: "Aucune",
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        GoldButton("Revanche", Icons.Outlined.Replay, onClick = vm::startGame, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        GhostButton("Accueil", Icons.Outlined.Home, onClick = vm::home, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Pillar(p: PlayerState, color: Color, rank: Int, height: Dp, grown: Boolean) {
    val h by animateFloatAsState(if (grown) 1f else 0f, tween(700 + rank * 150), label = "pillar")
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(p.name, style = MaterialTheme.typography.titleMedium, color = Sk.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${p.score}", style = MaterialTheme.typography.bodyMedium, color = Sk.TextDim)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(height * h)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(color.copy(alpha = 0.85f)),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text("$rank", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun Stat(icon: ImageVector, label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Sk.Surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Sk.Gold, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Sk.TextDim, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = Sk.Text)
    }
}
