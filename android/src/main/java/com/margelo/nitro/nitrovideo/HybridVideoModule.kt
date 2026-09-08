package com.margelo.nitro.nitrovideo

import android.os.Build
import com.margelo.nitro.core.Promise

class HybridVideoModule : HybridVideoModuleSpec() {
    override fun isPictureInPictureSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    override fun clearVideoCacheAsync(): Promise<Unit> {
        return Promise.resolved(Unit)
    }

    override fun setVideoCacheSizeAsync(sizeBytes: Double): Promise<Unit> {
        return Promise.resolved(Unit)
    }

    override fun getCurrentVideoCacheSize(): Double {
        return 0.0
    }
}
