package com.thousandlabs.soulking.game

import android.media.AudioManager
import android.media.ToneGenerator

/** Petits sons du jeu, générés par le téléphone : aucun fichier audio à embarquer. */
object Sfx {
    var enabled = true

    private var tg: ToneGenerator? = null

    private fun gen(): ToneGenerator? {
        if (tg == null) tg = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 75) }.getOrNull()
        return tg
    }

    private fun play(tone: Int, ms: Int) {
        if (!enabled) return
        runCatching { gen()?.startTone(tone, ms) }
    }

    fun tick() = play(ToneGenerator.TONE_PROP_BEEP, 80)
    fun go() = play(ToneGenerator.TONE_PROP_BEEP2, 180)
    fun buzz() = play(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 220)
    fun success() = play(ToneGenerator.TONE_PROP_ACK, 280)
    fun error() = play(ToneGenerator.TONE_SUP_ERROR, 400)
    fun fanfare() = play(ToneGenerator.TONE_CDMA_ALERT_INCALL_LITE, 600)

    fun release() {
        runCatching { tg?.release() }
        tg = null
    }
}
