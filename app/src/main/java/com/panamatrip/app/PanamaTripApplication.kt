package com.panamatrip.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.panamatrip.app.di.AppContainer
import okio.Path.Companion.toOkioPath

class PanamaTripApplication : Application(), SingletonImageLoader.Factory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.warmUp()
    }

    /**
     * Caché de imágenes persistente en disco: el feed y los destinos se
     * mantienen visibles sin señal. El respaldo tonal cubre el primer arranque.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, MEMORY_CACHE_FRACTION)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve(IMAGE_CACHE_DIR).toOkioPath())
                    .maxSizeBytes(IMAGE_CACHE_BYTES)
                    .build()
            }
            .components {
                add(OkHttpNetworkFetcherFactory())
            }
            .crossfade(true)
            .build()

    private companion object {
        const val MEMORY_CACHE_FRACTION = 0.2
        const val IMAGE_CACHE_BYTES = 64L * 1024 * 1024
        const val IMAGE_CACHE_DIR = "image_cache"
    }
}
