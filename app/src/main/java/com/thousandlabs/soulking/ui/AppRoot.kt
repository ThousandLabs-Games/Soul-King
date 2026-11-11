package com.thousandlabs.soulking.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.thousandlabs.soulking.data.LocalMusic
import com.thousandlabs.soulking.game.GameViewModel
import com.thousandlabs.soulking.game.Screen

@Composable
fun AppRoot(vm: GameViewModel) {
    val screen by vm.screen.collectAsState()
    val ctx = LocalContext.current

    // accès aux morceaux du téléphone : demandé seulement quand on en a besoin
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    var denied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        val action = pending
        pending = null
        if (ok) action?.invoke() else denied = true
    }
    val ensureAudio: (() -> Unit) -> Unit = { action ->
        if (LocalMusic.granted(ctx)) action()
        else {
            pending = action
            launcher.launch(LocalMusic.permission)
        }
    }

    BackHandler(enabled = screen != Screen.HOME && screen != Screen.GAME && screen != Screen.PODIUM) { vm.back() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Sk.Bg)
            .safeDrawingPadding(),
    ) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen",
        ) { s ->
            when (s) {
                Screen.HOME -> HomeScreen(vm, ensureAudio)
                Screen.LOCAL -> LocalScreen(vm)
                Screen.SEARCH -> SearchScreen(vm)
                Screen.SETUP -> SetupScreen(vm, ensureAudio)
                Screen.GAME -> GameScreen(vm)
                Screen.PODIUM -> PodiumScreen(vm)
                Screen.HOWTO -> HowToScreen(vm)
                Screen.ABOUT -> AboutScreen(vm)
            }
        }
    }

    if (denied) {
        AlertDialog(
            onDismissRequest = { denied = false },
            containerColor = Sk.SurfaceHigh,
            title = { Text("Accès à la musique refusé", color = Sk.Text) },
            text = {
                Text(
                    "Pour jouer avec vos morceaux, autorisez Soul King à lire les fichiers audio : Paramètres de l'appli › Autorisations › Musique et audio.",
                    color = Sk.TextDim,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    denied = false
                    ctx.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }) { Text("Ouvrir les paramètres", color = Sk.Gold) }
            },
            dismissButton = { TextButton(onClick = { denied = false }) { Text("Plus tard", color = Sk.TextDim) } },
        )
    }
}
