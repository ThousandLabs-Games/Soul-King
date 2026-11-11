package com.thousandlabs.soulking

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.thousandlabs.soulking.data.Prefs
import java.io.File

class SoulKingApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        // ancien cache d'images sans limite (versions 1.1 et 1.2)
        runCatching { File(cacheDir, "image_cache").deleteRecursively() }
    }

    /** Pochettes : 5 Mo maximum sur le téléphone, les plus anciennes sont effacées d'elles-mêmes. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.10).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "covers"))
                    .maxSizeBytes(COVER_CACHE_BYTES)
                    .build()
            }
            .crossfade(true)
            .build()

    companion object {
        const val COVER_CACHE_BYTES = 5L * 1024 * 1024
    }
}
