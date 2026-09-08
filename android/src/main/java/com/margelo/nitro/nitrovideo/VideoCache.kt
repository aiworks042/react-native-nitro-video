package com.margelo.nitro.nitrovideo

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.margelo.nitro.nitrovideo.cache.VideoCache as BaseVideoCache

@OptIn(UnstableApi::class)
typealias VideoCache = BaseVideoCache
