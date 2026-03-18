package com.skulpt.app.util

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator
import com.bumptech.glide.module.AppGlideModule

@GlideModule
class SkulptGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // Limit Disk Cache to 5 MB
        val diskCacheSizeBytes = 1024 * 1024 * 5L
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, "skulpt_image_cache", diskCacheSizeBytes))

        // Limit Memory Cache (RAM) explicitly
        val calculator = MemorySizeCalculator.Builder(context)
            .setMemoryCacheScreens(1f)
            .setBitmapPoolScreens(1f)
            .build()
        builder.setMemoryCache(LruResourceCache(calculator.memoryCacheSize.toLong()))
    }
}
