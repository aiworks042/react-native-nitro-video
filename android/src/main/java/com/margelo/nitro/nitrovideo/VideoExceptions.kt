package com.margelo.nitro.nitrovideo

import com.margelo.nitro.nitrovideo.enums.DRMType

private const val defaultServiceBindingTip = "Make sure that the nitro-video playback service is properly configured to avoid issues with displaying the now playing notification and sustaining background playback."

open class NitroVideoException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class FullScreenVideoViewNotFoundException :
    NitroVideoException("VideoView id wasn't passed to the activity")

class FullScreenOptionsNotFoundException :
    NitroVideoException("Fullscreen options were not passed to the activity")

class VideoViewNotFoundException(id: String) :
    NitroVideoException("VideoView with id: $id not found")

class MethodUnsupportedException(methodName: String) :
    NitroVideoException("Method `$methodName` is not supported on Android")

class PictureInPictureEnterException(message: String?) :
    NitroVideoException("Failed to enter Picture in Picture mode${message?.let { ". $message" } ?: ""}")

class PictureInPictureConfigurationException :
    NitroVideoException("Current activity does not support picture-in-picture. Make sure you have configured AndroidManifest.xml correctly.")

class PictureInPictureUnsupportedException :
    NitroVideoException("Picture in Picture mode is not supported on this device")

class UnsupportedDRMTypeException(type: DRMType) :
    NitroVideoException("DRM type `$type` is not supported on Android")

class PlaybackException(reason: String?, cause: Throwable? = null) :
    NitroVideoException("A playback exception has occurred: ${reason ?: "reason unknown"}", cause)

class FailedToGetAudioFocusManagerException :
    NitroVideoException("Failed to get AudioFocusManager service")

class VideoCacheException(message: String?, cause: Throwable? = null) :
    NitroVideoException(message ?: "Unexpected video cache error", cause)

class NowPlayingException(message: String?, cause: Throwable? = null) :
    NitroVideoException(message ?: "Unexpected video now playing exception", cause)

fun getPlaybackServiceErrorMessage(message: String?, tip: String = defaultServiceBindingTip) =
    (message ?: "Nitro-video playback service binder error") + ". $tip"
