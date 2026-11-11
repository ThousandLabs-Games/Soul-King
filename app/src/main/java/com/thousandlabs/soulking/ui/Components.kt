package com.thousandlabs.soulking.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour", tint = Sk.Text)
            }
        } else Spacer(Modifier.width(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = Sk.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = Sk.TextDim,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

/** Puce de choix sobre, dorée quand elle est sélectionnée. */
@Composable
fun SkChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Sk.Gold else Sk.Surface)
            .border(1.dp, if (selected) Sk.Gold else Sk.Divider, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Sk.OnGold else Sk.Text,
            maxLines = 1,
        )
    }
}

@Composable
fun GoldButton(text: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Row(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) Sk.Gold else Sk.SurfaceHigh)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (enabled) Sk.OnGold else Sk.TextDim, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, color = if (enabled) Sk.OnGold else Sk.TextDim, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GhostButton(text: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Sk.Text) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Sk.Divider, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

/** Pastille ronde contenant une icône. */
@Composable
fun IconBadge(icon: ImageVector, tint: Color = Sk.Gold, background: Color = Sk.GoldDim, size: Dp = 44.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun Cover(model: Any?, size: Dp, corner: Dp = 16.dp) {
    val fallback = rememberVectorPainter(Icons.Outlined.Album)
    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        placeholder = fallback,
        error = fallback,
        fallback = fallback,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(Sk.SurfaceHigh),
    )
}

/** Barres d'égaliseur animées pendant l'écoute. */
@Composable
fun Equalizer(active: Boolean, color: Color, modifier: Modifier = Modifier, bars: Int = 9) {
    val t = rememberInfiniteTransition(label = "eq")
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "phase",
    )
    Canvas(modifier) {
        val w = size.width / (bars * 2f)
        for (i in 0 until bars) {
            val amp = if (active) 0.25f + 0.75f * abs(sin(phase + i * 0.75f)) * (0.55f + 0.45f * abs(cos(phase * 0.5f + i)))
            else 0.10f
            val h = (size.height * amp).coerceAtLeast(w)
            val x = w / 2f + i * 2f * w
            drawRoundRect(
                color = color,
                topLeft = Offset(x, (size.height - h) / 2f),
                size = Size(w, h),
                cornerRadius = CornerRadius(w / 2f, w / 2f),
            )
        }
    }
}

/** Anneau de progression de l'extrait. */
@Composable
fun ProgressRing(progress: Float, modifier: Modifier = Modifier, color: Color = Sk.Gold, strokeWidth: Dp = 6.dp) {
    Canvas(modifier) {
        val s = strokeWidth.toPx()
        val arcSize = Size(size.width - s, size.height - s)
        val topLeft = Offset(s / 2f, s / 2f)
        drawArc(Sk.SurfaceHigh, 0f, 360f, false, topLeft = topLeft, size = arcSize, style = Stroke(s))
        drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, topLeft = topLeft, size = arcSize, style = Stroke(s, cap = StrokeCap.Round))
    }
}

/** Petit tableau des scores. */
@Composable
fun ScoreRow(rank: Int?, name: String, score: Int, color: Color, highlight: Boolean = false, delta: Int? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (highlight) color.copy(alpha = 0.16f) else Sk.Surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (rank != null) {
            Text("$rank", style = MaterialTheme.typography.titleMedium, color = Sk.TextDim, modifier = Modifier.width(26.dp))
        }
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Text(name, style = MaterialTheme.typography.titleMedium, color = Sk.Text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (delta != null && delta != 0) {
            Text(
                if (delta > 0) "+$delta" else "−${-delta}",
                style = MaterialTheme.typography.labelLarge,
                color = if (delta > 0) Sk.Good else Sk.Bad,
            )
            Spacer(Modifier.width(10.dp))
        }
        Text("$score", style = MaterialTheme.typography.titleMedium, color = Sk.Text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InfoCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Sk.Surface)
            .padding(16.dp),
    ) { content() }
}

fun formatSeconds(ms: Long): String {
    val tenth = (ms / 100).toInt()
    return "${tenth / 10},${tenth % 10} s"
}
