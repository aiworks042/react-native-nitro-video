package com.margelo.nitro.nitrovideo.cache

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheKeyFactory

@OptIn(UnstableApi::class)
internal class ExpoVideoCacheKeyFactory(
    private val context: Context,
    private val requestHeaders: Map<String, String> = emptyMap(),
    private val sourceUrl: String? = null,
    private val sourceStorageKey: String? = null
) : CacheKeyFactory {
    override fun buildCacheKey(dataSpec: DataSpec): String {
        val url = dataSpec.uri.toString()
        val variantKey = if (url == sourceUrl && sourceStorageKey != null) {
            sourceStorageKey
        } else {
            CacheVariantIndex.storageKey(context, url, requestHeaders)
        }
        return if (variantKey.isEmpty()) url else "$url#$variantKey"
    }
}
