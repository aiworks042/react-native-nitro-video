package com.margelo.nitro.nitrovideo.utils

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.margelo.nitro.nitrovideo.cache.CachePolicy
import com.margelo.nitro.nitrovideo.cache.CacheVariantIndex
import com.margelo.nitro.nitrovideo.cache.ExpoVideoCacheKeyFactory
import com.margelo.nitro.nitrovideo.managers.VideoManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.ForwardingSource
import okio.buffer
import java.util.concurrent.ConcurrentHashMap

@OptIn(UnstableApi::class)
object DataSourceUtils {

    fun buildDataSourceFactory(
        context: Context,
        uri: String,
        useCaching: Boolean,
        headers: Map<String, String>? = null
    ): DataSource.Factory {
        val requestHeaders = headers ?: emptyMap()

        if (useCaching && (uri.startsWith("http://") || uri.startsWith("https://"))) {
            val storageKeyResult = CacheVariantIndex.storageKeyResult(context, uri, requestHeaders)
            val storageKey = storageKeyResult.storageKey

            if (!storageKeyResult.cacheable || !storageKeyResult.canReadFromCache) {
                evictCacheEntry(uri, storageKey)
            }

            val upstreamFactory = buildOkHttpDataSourceFactory(context, uri, requestHeaders, storageKey, useCaching = true)

            return CacheDataSource.Factory().apply {
                setCache(VideoManager.cache.instance)
                setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                setCacheKeyFactory(ExpoVideoCacheKeyFactory(context, requestHeaders, uri, storageKey))
                setUpstreamDataSourceFactory(upstreamFactory)
            }
        }

        return if (uri.startsWith("http://") || uri.startsWith("https://")) {
            buildOkHttpDataSourceFactory(context, uri, requestHeaders, null, useCaching = false)
        } else {
            DefaultDataSource.Factory(context)
        }
    }

    private fun buildOkHttpDataSourceFactory(
        context: Context,
        sourceUrl: String,
        requestHeaders: Map<String, String>,
        cacheStorageKey: String?,
        useCaching: Boolean
    ): OkHttpDataSource.Factory {
        val clientBuilder = OkHttpClient.Builder()
        if (useCaching) {
            clientBuilder.addNetworkInterceptor(buildCacheVariantRecorder(context, sourceUrl, requestHeaders, cacheStorageKey))
        }
        val client = clientBuilder.build()

        val applicationName = context.applicationInfo.loadLabel(context.packageManager).toString().filter { it.code in 0..127 }
        val defaultUserAgent = Util.getUserAgent(context, applicationName)
        val userAgent = requestHeaders["User-Agent"] ?: defaultUserAgent

        return OkHttpDataSource.Factory(client).apply {
            if (requestHeaders.isNotEmpty()) {
                setDefaultRequestProperties(requestHeaders)
            }
            setUserAgent(userAgent)
        }
    }

    private fun buildCacheVariantRecorder(
        context: Context,
        sourceUrl: String,
        requestHeaders: Map<String, String>,
        cacheStorageKey: String?
    ): Interceptor {
        val recordedKeys = ConcurrentHashMap.newKeySet<String>()
        return Interceptor { chain ->
            val response = chain.proceed(chain.request())
            val responseHeaders = response.headers.toMultimap().mapValues { it.value.joinToString(separator = ",") }
            val policy = CachePolicy.evaluate(responseHeaders, response.code)
            val storageKey = cacheStorageKey ?: CacheVariantIndex.storageKey(context, sourceUrl, requestHeaders)

            if (recordedKeys.add(storageKey)) {
                CacheVariantIndex.recordVariant(context, sourceUrl, storageKey, requestHeaders, policy)
            }
            if (!policy.isCacheable) {
                evictCacheEntry(sourceUrl, storageKey)
                return@Interceptor response.evictAfterClose {
                    evictCacheEntry(sourceUrl, storageKey)
                }
            }
            response
        }
    }

    private fun Response.evictAfterClose(onClose: () -> Unit): Response {
        val originalBody = body ?: return this
        val wrappedSource = object : ForwardingSource(originalBody.source()) {
            private var fired = false
            override fun close() {
                try {
                    super.close()
                } finally {
                    if (!fired) {
                        fired = true
                        onClose()
                    }
                }
            }
        }
        return newBuilder()
            .body(wrappedSource.buffer().asResponseBody(originalBody.contentType(), originalBody.contentLength()))
            .build()
    }

    private fun evictCacheEntry(url: String, storageKey: String) {
        val cacheKey = if (storageKey.isEmpty()) url else "$url#$storageKey"
        try {
            VideoManager.cache.instance.removeResource(cacheKey)
        } catch (_: Exception) {}
    }
}
