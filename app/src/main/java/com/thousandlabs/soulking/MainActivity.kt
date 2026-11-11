package com.thousandlabs.soulking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.thousandlabs.soulking.game.GameViewModel
import com.thousandlabs.soulking.ui.AppRoot
import com.thousandlabs.soulking.ui.SoulKingTheme
import com.thousandlabs.soulking.update.Updater

class MainActivity : ComponentActivity() {

    private val vm: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Updater.autoCheck()
        setContent {
            SoulKingTheme { AppRoot(vm) }
        }
    }

    override fun onResume() {
        super.onResume()
        Updater.onAppResumed(this)
    }
}
