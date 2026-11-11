package com.thousandlabs.soulking

import android.app.Application
import com.thousandlabs.soulking.data.Prefs

class SoulKingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
    }
}
