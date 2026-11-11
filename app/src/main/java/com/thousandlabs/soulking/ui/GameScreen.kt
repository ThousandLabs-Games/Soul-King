package com.thousandlabs.soulking.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thousandlabs.soulking.game.AnswerMode
import com.thousandlabs.soulking.game.GameUi
import com.thousandlabs.soulking.game.GameViewModel
import com.thousandlabs.soulking.game.GuessTarget
import com.thousandlabs.soulking.game.Stage

@Composable
fun GameScreen(vm: GameViewModel) {
    val g by vm.game.collectAsState()
    var confirmQuit by remember { mutableStateOf(false) }

    // l'écran reste allumé pendant toute la partie
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    BackHandler { confirmQuit = true }

    Column(Modifier.fillMaxSize()) {
        GameHeader(g, onClose = { confirmQuit = true })
        when (g.stage) {
            Stage.LOADING -> CenterMessage(null, g.loadingText, loading = true)
            Stage.ERROR -> CenterMessage(Icons.Outlined.WifiOff, g.error ?: "Erreur") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostButton("Retour", null, onClick = vm::quitGame)
                    GoldButton("Réessayer", Icons.Outlined.Refresh, onClick = vm::retryLoading)
                }
            }
            else -> Board(g, vm)
        }
    }

    if (confirmQuit) {
        AlertDialog(
            onDismissRequest = { confirmQuit = false },
            containerColor = Sk.SurfaceHigh,
            title = { Text("Quitter la partie ?", color = Sk.Text) },
            text = { Text("Les scores de cette partie seront perdus.", color = Sk.TextDim) },
            confirmButton = {
                TextButton(onClick = { confirmQuit = false; vm.quitGame() }) { Text("Quitter", color = Sk.Bad) }
            },
            dismissButton = {
                TextButton(onClick = { confirmQuit = false }) { Text("Continuer", color = Sk.Gold) }
            },
        )
    }
}

@Composable
private fun GameHeader(g: GameUi, onClose: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, contentDescription = "Quitter", tint = Sk.TextDim) }
        Column(Modifier.weight(1f)) {
            Text(g.sourceName, style = MaterialTheme.typography.titleMedium, color = Sk.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (g.roundCount > 0) {
                Text("Manche ${g.roundIndex + 1} sur ${g.roundCount}", style = MaterialTheme.typography.bodySmall, color = Sk.TextDim)
            }
        }
        Icon(CrownIcon, null, tint = Sk.Gold, modifier = Modifier.padding(end = 14.dp).size(22.dp))
    }
}

@Composable
private fun Board(g: GameUi, vm: GameViewModel) {
    val listening = g.stage == Stage.COUNTDOWN || g.stage == Stage.PLAYING || g.stage == Stage.CLIP_OVER
    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            when (g.stage) {
                Stage.COUNTDOWN -> Countdown(g)
                Stage.PLAYING, Stage.CLIP_OVER -> Listening(g, vm)
                Stage.ANSWERING -> Answering(g, vm)
                Stage.REVEAL -> Reveal(g, vm)
                else -> Unit
            }
        }
        if (listening) {
            when {
                !g.solo -> BuzzerZones(g, onBuzz = vm::buzz, modifier = Modifier.weight(1.05f))
                g.answerMode == AnswerMode.ORAL -> SoloFoundButton(enabled = g.stage != Stage.COUNTDOWN) { vm.buzz(0) }
                else -> Unit
            }
        }
    }
}

// ---------------------------------------------------------------- compte à rebours

@Composable
private fun Countdown(g: GameUi) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
        Text(g.prompt, style = MaterialTheme.typography.titleMedium, color = Sk.TextDim)
        Spacer(Modifier.height(8.dp))
        AnimatedContent(
            targetState = g.countdown,
            transitionSpec = { (scaleIn(initialScale = 1.6f) + fadeIn()) togetherWith fadeOut() },
            label = "countdown",
        ) { n ->
            Text("$n", style = MaterialTheme.typography.displayLarge, color = Sk.Gold)
        }
        g.flash?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = Sk.TextDim, textAlign = TextAlign.Center)
        }
        if (g.roundIndex == 0) g.notice?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = Sk.TextDim, textAlign = TextAlign.Center)
        }
    }
}

// ---------------------------------------------------------------- écoute

@Composable
private fun Listening(g: GameUi, vm: GameViewModel) {
    val over = g.stage == Stage.CLIP_OVER
    val showChoices = g.solo && g.answerMode == AnswerMode.QCM
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(g.prompt, style = MaterialTheme.typography.titleMedium, color = Sk.TextDim)
        Spacer(Modifier.height(14.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(if (showChoices) 120.dp else 170.dp)) {
            ProgressRing(g.progress, Modifier.fillMaxSize(), color = if (over) Sk.TextDim else Sk.Gold)
            Equalizer(active = !over, color = if (over) Sk.TextDim else Sk.Gold, modifier = Modifier.size(if (showChoices) 60.dp else 84.dp))
        }
        Spacer(Modifier.height(10.dp))
        g.flash?.let {
            Text(it, style = MaterialTheme.typography.labelLarge, color = Sk.Bad, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
        }
        if (over) {
            Text(
                if (g.solo) "L'extrait est fini. Vous avez encore le temps de répondre." else "L'extrait est fini. Les buzzers restent ouverts.",
                style = MaterialTheme.typography.bodySmall,
                color = Sk.TextDim,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!g.replayUsed) GhostButton("Réécouter (points ÷ 2)", Icons.Outlined.Replay, onClick = vm::replay)
                GhostButton("Révéler", Icons.Outlined.Flag, onClick = vm::giveUp, color = Sk.TextDim)
            }
        }
        if (showChoices) {
            Spacer(Modifier.height(14.dp))
            Choices(g, onPick = vm::pick)
        }
    }
}

@Composable
private fun Choices(g: GameUi, onPick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        g.choices.forEachIndexed { i, c ->
            val out = i in g.eliminated
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (out) Sk.Bad.copy(alpha = 0.12f) else Sk.Surface)
                    .border(1.dp, if (out) Sk.Bad.copy(alpha = 0.4f) else Sk.Divider, RoundedCornerShape(16.dp))
                    .clickable(enabled = !out) { onPick(i) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "ABCD".getOrElse(i) { ' ' }.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (out) Sk.Bad else Sk.Gold,
                    modifier = Modifier.width(28.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        c.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (out) Sk.TextDim else Sk.Text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    c.sub?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Sk.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                if (out) Icon(Icons.Outlined.Close, null, tint = Sk.Bad)
            }
        }
    }
}

// ---------------------------------------------------------------- buzzers

@Composable
private fun BuzzerZones(g: GameUi, onBuzz: (Int) -> Unit, modifier: Modifier = Modifier) {
    val n = g.players.size
    val active = g.stage == Stage.PLAYING || g.stage == Stage.CLIP_OVER
    Column(modifier.fillMaxWidth().padding(8.dp)) {
        if (n == 2) {
            // face à face : la zone du haut est retournée pour le joueur assis en face
            Zone(g, 0, active, onBuzz, Modifier.weight(1f).fillMaxWidth(), flipped = true)
            Zone(g, 1, active, onBuzz, Modifier.weight(1f).fillMaxWidth())
        } else {
            for (row in 0 until (n + 1) / 2) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (col in 0..1) {
                        val i = row * 2 + col
                        if (i < n) Zone(g, i, active, onBuzz, Modifier.weight(1f).fillMaxHeight())
                        else Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun Zone(g: GameUi, i: Int, active: Boolean, onBuzz: (Int) -> Unit, modifier: Modifier, flipped: Boolean = false) {
    val p = g.players[i]
    val color = Sk.player(i)
    val locked = i in g.locked
    val enabled = active && !locked
    val pulse by animateFloatAsState(if (enabled) 1f else 0.97f, tween(200), label = "pulse")
    Box(
        modifier
            .padding(5.dp)
            .scale(pulse)
            .clip(RoundedCornerShape(24.dp))
            .background(if (locked) color.copy(alpha = 0.12f) else color.copy(alpha = if (active) 0.92f else 0.35f))
            .pointerInput(enabled) {
                if (enabled) awaitEachGesture {
                    awaitFirstDown()
                    onBuzz(i)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.rotate(if (flipped) 180f else 0f).padding(12.dp),
        ) {
            if (locked) {
                Icon(Icons.Outlined.Lock, null, tint = color, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(4.dp))
            }
            Text(
                p.name,
                style = MaterialTheme.typography.headlineSmall,
                color = if (locked) color else Color.White,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (locked) "Bloqué pour cette manche" else "${p.score} pts",
                style = MaterialTheme.typography.bodyMedium,
                color = if (locked) color else Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun SoloFoundButton(enabled: Boolean, onClick: () -> Unit) {
    GoldButton(
        text = "J'ai trouvé",
        icon = Icons.Outlined.Bolt,
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(72.dp),
    )
}

// ---------------------------------------------------------------- réponse d'un joueur

@Composable
private fun Answering(g: GameUi, vm: GameViewModel) {
    val p = g.answering ?: return
    val color = Sk.player(p)
    val track = g.track
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(color)
                .padding(horizontal = 22.dp, vertical = 10.dp),
        ) {
            Text("${g.players[p].name} répond", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Text(g.prompt, style = MaterialTheme.typography.bodyMedium, color = Sk.TextDim)
        Spacer(Modifier.height(18.dp))

        if (g.answerMode == AnswerMode.QCM) {
            Choices(g, onPick = vm::pick)
        } else {
            Text(
                "Réponse à voix haute. Celui qui tient le téléphone valide.",
                style = MaterialTheme.typography.bodyMedium,
                color = Sk.TextDim,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { vm.toggleAnswer() }) {
                    Icon(if (g.showAnswer) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null, tint = Sk.Gold)
                    Spacer(Modifier.width(10.dp))
                    if (g.showAnswer && track != null) {
                        Column(Modifier.weight(1f)) {
                            Text(track.title, style = MaterialTheme.typography.titleMedium, color = Sk.Text)
                            Text(
                                listOfNotNull(track.artist, track.anime?.takeIf { !it.equals(track.artist, true) }, track.label).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = Sk.TextDim,
                            )
                        }
                    } else {
                        Text("Afficher la réponse (arbitre)", style = MaterialTheme.typography.titleMedium, color = Sk.Text, modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                VerdictButton("Mauvaise", Icons.Outlined.Close, Sk.Bad, Modifier.weight(1f)) { vm.oralVerdict(false) }
                VerdictButton("Bonne", Icons.Outlined.Check, Sk.Good, Modifier.weight(1f)) { vm.oralVerdict(true) }
            }
        }
    }
}

@Composable
private fun VerdictButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.18f))
            .border(1.dp, color, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
    }
}

// ---------------------------------------------------------------- révélation

@Composable
private fun Reveal(g: GameUi, vm: GameViewModel) {
    val track = g.track ?: return
    val o = g.outcome
    var shown by remember(track.key) { mutableStateOf(false) }
    LaunchedEffect(track.key) { shown = true }
    val appear by animateFloatAsState(if (shown) 1f else 0.8f, tween(450), label = "appear")
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        Box(Modifier.scale(appear)) { Cover(track.cover, 210.dp, corner = 22.dp) }
        Spacer(Modifier.height(16.dp))
        // quand on devine l'animé ou le dessin animé, c'est lui qu'on met en avant
        val showFirst = g.target == GuessTarget.SHOW && track.anime != null
        Text(if (showFirst) track.anime ?: track.title else track.title, style = MaterialTheme.typography.headlineSmall, color = Sk.Text, textAlign = TextAlign.Center)
        Text(
            buildString {
                if (showFirst) append(track.title).append(" · ")
                append(track.artist)
                if (!showFirst) track.anime?.let { if (!it.equals(track.artist, ignoreCase = true)) append(" · ").append(it) }
                track.label?.let { append(" · ").append(it) }
            },
            style = MaterialTheme.typography.bodyLarge,
            color = Sk.TextDim,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))

        val winner = o?.winner
        if (winner != null) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Sk.player(winner).copy(alpha = 0.18f))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.EmojiEvents, null, tint = Sk.player(winner))
                Spacer(Modifier.width(8.dp))
                Text(
                    "${g.players[winner].name}  +${o?.points ?: 0}" + (o?.timeMs?.let { "  ·  ${formatSeconds(it)}" } ?: ""),
                    style = MaterialTheme.typography.titleMedium,
                    color = Sk.Text,
                    fontWeight = FontWeight.Bold,
                )
            }
            val streak = g.players[winner].streak
            if (streak >= 3) {
                Spacer(Modifier.height(6.dp))
                Text("Série de $streak bonnes réponses", style = MaterialTheme.typography.bodySmall, color = Sk.Gold)
            }
        } else {
            Text("Personne n'a trouvé", style = MaterialTheme.typography.titleMedium, color = Sk.TextDim)
        }

        Spacer(Modifier.height(18.dp))
        if (!g.solo) {
            val ranked = g.players.withIndex().sortedByDescending { it.value.score }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ranked.forEachIndexed { rank, (i, p) ->
                    ScoreRow(
                        rank = rank + 1,
                        name = p.name,
                        score = p.score,
                        color = Sk.player(i),
                        highlight = i == winner,
                        delta = if (i == winner) o?.points else null,
                    )
                }
            }
        } else {
            Text("Score : ${g.players.first().score} pts", style = MaterialTheme.typography.titleLarge, color = Sk.Gold)
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Sk.Surface)
                .clickable { if (g.hookMarked) vm.clearHook() else vm.markHook() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(if (g.hookMarked) Icons.Outlined.BookmarkAdded else Icons.Outlined.BookmarkAdd, null, tint = Sk.Gold)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (g.hookMarked) "Passage retenu" else "Retenir ce passage",
                    style = MaterialTheme.typography.titleMedium,
                    color = Sk.Text,
                )
                Text(
                    if (g.hookMarked) "Touchez pour l'oublier. Touchez à nouveau pendant la bonne partie pour le remplacer."
                    else "Touchez pendant le moment le plus connu : la prochaine fois, l'extrait démarrera là",
                    style = MaterialTheme.typography.bodySmall,
                    color = Sk.TextDim,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        GoldButton(
            text = if (g.isLastRound) "Voir le podium" else "Manche suivante",
            icon = if (g.isLastRound) Icons.Outlined.EmojiEvents else Icons.AutoMirrored.Outlined.ArrowForward,
            onClick = vm::next,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
    }
}
