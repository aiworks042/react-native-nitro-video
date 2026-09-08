package com.margelo.nitro.nitrovideo.utils

import android.app.Activity
import android.app.PictureInPictureParams
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.util.Rational
import androidx.annotation.OptIn
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.margelo.nitro.nitrovideo.enums.ContentFit

@OptIn(UnstableApi::class)
fun calculateRectHint(playerView: PlayerView): Rect {
    val hint = Rect()
    playerView.videoSurfaceView?.getGlobalVisibleRect(hint)
    val location = IntArray(2)
    playerView.videoSurfaceView?.getLocationOnScreen(location)

    // getGlobalVisibleRect doesn't take into account the offset for the notch, we use the screen location
    // of the view to calculate the rectHint.
    val height = hint.bottom - hint.top
    hint.top = location[1]
    hint.bottom = hint.top + height
    return hint
}

fun calculatePiPAspectRatio(videoSize: VideoSize, viewWidth: Int, viewHeight: Int, contentFit: ContentFit): Rational {
    var aspectRatio = if (contentFit == ContentFit.CONTAIN) {
        Rational(videoSize.width.coerceAtLeast(1), videoSize.height.coerceAtLeast(1))
    } else {
        Rational(viewWidth.coerceAtLeast(1), viewHeight.coerceAtLeast(1))
    }
    // AspectRatio for the activity in picture-in-picture, must be between 2.39:1 and 1:2.39 (inclusive).
    val maximumRatio = Rational(239, 100)
    val minimumRatio = Rational(100, 239)

    if (aspectRatio.toFloat() > maximumRatio.toFloat()) {
        aspectRatio = maximumRatio
    } else if (aspectRatio.toFloat() < minimumRatio.toFloat()) {
        aspectRatio = minimumRatio
    }
    return aspectRatio
}

fun isPictureInPictureSupported(activity: Activity): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity.packageManager.hasSystemFeature(
        android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE
    )
}

fun applyRectHint(activity: Activity, rectHint: Rect) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isPictureInPictureSupported(activity)) {
        runWithPiPMisconfigurationSoftHandling {
            activity.setPictureInPictureParams(PictureInPictureParams.Builder().setSourceRectHint(rectHint).build())
        }
    }
}

fun runWithPiPMisconfigurationSoftHandling(shouldThrow: Boolean = false, block: () -> Any?) {
    try {
        block()
    } catch (e: IllegalStateException) {
        Log.e("NitroVideo", "Current activity does not support picture-in-picture.", e)
        if (shouldThrow) throw e
    }
}

fun applyPiPParams(activity: Activity, autoEnterPiP: Boolean, aspectRatio: Rational? = null) {
    val safeAspectRatio = aspectRatio?.takeIf { it.toFloat() in 0.41841..2.39 }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isPictureInPictureSupported(activity)) {
        val paramsBuilder = PictureInPictureParams.Builder()

        safeAspectRatio?.let {
            paramsBuilder.setAspectRatio(it)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                paramsBuilder.setAutoEnterEnabled(autoEnterPiP)
            } catch (e: NoSuchMethodError) {
                Log.w("NitroVideo", "PictureInPictureParams.Builder.setAutoEnterEnabled is missing on this device, skipping auto-enter PiP setup", e)
            }
        }
        runWithPiPMisconfigurationSoftHandling {
            activity.setPictureInPictureParams(paramsBuilder.build())
        }
    }
}

object PictureInPictureUtils {
    fun enterPictureInPicture(activity: Activity, width: Int, height: Int) {
        if (!isPictureInPictureSupported(activity)) return
        val ratio = if (width > 0 && height > 0) {
            val raw = Rational(width, height)
            val maxR = Rational(239, 100)
            val minR = Rational(100, 239)
            if (raw.toFloat() > maxR.toFloat()) maxR else if (raw.toFloat() < minR.toFloat()) minR else raw
        } else {
            Rational(16, 9)
        }
        applyPiPParams(activity, false, ratio)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(ratio).build())
        }
    }

    fun setAutoEnterEnabled(activity: Activity, enabled: Boolean) {
        applyPiPParams(activity, enabled)
    }
}
