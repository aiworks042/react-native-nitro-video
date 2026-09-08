package com.margelo.nitro.nitrovideo.records

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.source.MediaSource
import com.margelo.nitro.nitrovideo.enums.ContentType
import com.margelo.nitro.nitrovideo.utils.DataSourceUtils
import java.io.Serializable

@OptIn(UnstableApi::class)
class VideoSource(
    var uri: Uri? = null,
    var drm: DRMOptions? = null,
    var metadata: VideoMetadata? = null,
    var headers: Map<String, String>? = null,
    var useCaching: Boolean = false,
    val contentType: ContentType = ContentType.AUTO
) : Serializable {

    fun toMediaSource(context: Context): MediaSource? {
        val uri = this.uri ?: return null
        val dataSourceFactory = DataSourceUtils.buildDataSourceFactory(
            context = context,
            uri = uri.toString(),
            useCaching = useCaching,
            headers = headers
        )
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)
        val mediaItem = toMediaItem(context)
        return mediaSourceFactory.createMediaSource(mediaItem)
    }

    fun toMediaItem(context: Context): MediaItem {
        return MediaItem.Builder().apply {
            setUri(parseLocalAssetId(uri, context))
            contentType.toMimeTypeString()?.let {
                setMimeType(it)
            }
            drm?.let {
                if (it.type.isSupported()) {
                    setDrmConfiguration(it.toDRMConfiguration())
                }
            }
            setMediaMetadata(
                MediaMetadata.Builder().apply {
                    metadata?.let { data ->
                        setTitle(data.title)
                        setArtist(data.artist)
                        data.artwork?.let { setArtworkUri(it) }
                    }
                }.build()
            )
        }.build()
    }

    @SuppressLint("DiscouragedApi")
    private fun parseLocalAssetId(uri: Uri?, context: Context): Uri? {
        if (uri == null || uri.scheme != null) {
            return uri
        }
        try {
            val resourceId: Int = context.resources.getIdentifier(
                uri.toString(),
                "raw",
                context.packageName
            )
            val parsedUri = Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .appendPath(resourceId.toString())
                .build()
            val dataSpec = DataSpec(parsedUri)
            val rawResourceDataSource = RawResourceDataSource(context)
            rawResourceDataSource.open(dataSpec)
            return rawResourceDataSource.uri
        } catch (e: Exception) {
            Log.e("NitroVideo", "Error parsing local asset id, falling back to original uri", e)
            return uri
        }
    }
}
