package com.margelo.nitro.nitrovideo.cache

import android.content.Context
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.margelo.nitro.nitrovideo.managers.VideoManager
import java.io.File
import java.lang.ref.WeakReference
import java.util.UUID

private const val SHARED_PREFERENCES_NAME = "NitroVideoCache"
private const val CACHE_SIZE_KEY = "cacheSize"
private const val VIDEO_CACHE_PARENT_DIR = "NitroVideoCache"
private const val VIDEO_CACHE_DIR_KEY = "cacheDir"
private const val DEFAULT_CACHE_SIZE = 1024 * 1024 * 1024L // 1GB

@OptIn(UnstableApi::class)
class VideoCache(context: Context) {
    private val weakContext = WeakReference(context)
    private val context: Context
        get() = weakContext.get() ?: throw IllegalStateException("Context lost")

    private val databaseProvider: DatabaseProvider = StandaloneDatabaseProvider(context)
    private val sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
    private var cacheEvictor = LeastRecentlyUsedCacheEvictor(getMaxCacheSize())
    var instance = SimpleCache(getCacheDir(), cacheEvictor, databaseProvider)

    private fun getMaxCacheSize(): Long {
        return sharedPreferences.getLong(CACHE_SIZE_KEY, DEFAULT_CACHE_SIZE)
    }

    fun release() {
        instance.release()
    }

    fun setMaxCacheSize(size: Long) {
        assertModificationReleaseConditions()
        instance.release()
        sharedPreferences.edit().putLong(CACHE_SIZE_KEY, size).apply()
        cacheEvictor = LeastRecentlyUsedCacheEvictor(size)
        instance = SimpleCache(getCacheDir(), cacheEvictor, databaseProvider)
    }

    fun getCurrentCacheSize(): Long {
        return getFileSize(getCacheDir())
    }

    private fun getCacheDir(): File {
        val videoCacheDirName = sharedPreferences.getString(VIDEO_CACHE_DIR_KEY, null) ?: run {
            val newCacheDirName = UUID.randomUUID().toString()
            sharedPreferences.edit().putString(VIDEO_CACHE_DIR_KEY, newCacheDirName).commit()
            newCacheDirName
        }
        val cacheParentDir = File(context.cacheDir, VIDEO_CACHE_PARENT_DIR)
        val cacheDir = File(cacheParentDir, videoCacheDirName)

        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return cacheDir
    }

    fun clear() {
        assertModificationReleaseConditions()

        val oldCacheDirectory = getCacheDir()
        val oldCache = instance
        val newCacheName = UUID.randomUUID().toString()

        sharedPreferences.edit().putString(VIDEO_CACHE_DIR_KEY, newCacheName).apply()
        instance = SimpleCache(getCacheDir(), cacheEvictor, databaseProvider)
        oldCache.release()
        oldCacheDirectory.deleteRecursively()
        CacheVariantIndex.clearAll(context)
    }

    private fun getFileSize(file: File): Long {
        return file
            .walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
    }

    private fun assertModificationReleaseConditions() {
        if (VideoManager.hasRegisteredPlayers()) {
            throw IllegalStateException("Cannot clear or modify cache while there are active players")
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            Log.w("NitroVideo", "Clearing cache on the main thread, this might cause performance issues")
        }
    }
}
