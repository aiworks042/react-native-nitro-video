package com.margelo.nitro.nitrovideo.utils

import android.content.Context
import android.content.res.Configuration
import android.hardware.SensorManager
import android.provider.Settings
import android.view.OrientationEventListener
import com.margelo.nitro.nitrovideo.enums.FullscreenOrientation
import com.margelo.nitro.nitrovideo.records.FullscreenOptions

/**
 * Helper for the auto-exit fullscreen functionality. Once the user has rotated the phone to the desired orientation,
 * the orientation lock should be released, so that once rotation to a perpendicular orientation is detected, the fullscreen can be exited.
 */
class FullscreenActivityOrientationHelper(
    val context: Context,
    val options: FullscreenOptions,
    val onShouldAutoExit: () -> Unit,
    val onShouldReleaseOrientation: () -> Unit
) {
    private var userHasRotatedToVideoOrientation = false
    private val isLockedToLandscape = options.orientation == FullscreenOrientation.LANDSCAPE ||
        options.orientation == FullscreenOrientation.LANDSCAPE_LEFT ||
        options.orientation == FullscreenOrientation.LANDSCAPE_RIGHT

    private val isLockedToPortrait = options.orientation == FullscreenOrientation.PORTRAIT ||
        options.orientation == FullscreenOrientation.PORTRAIT_UP ||
        options.orientation == FullscreenOrientation.PORTRAIT_DOWN

    /**
     * Checks if the system's auto-rotation setting is currently enabled.
     * Returns true if auto-rotation is unlocked (enabled), false otherwise (locked or error).
     */
    val isAutoRotationEnabled: Boolean
        get() = try {
            val rotationStatus = Settings.System.getInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                0
            )
            rotationStatus == 1
        } catch (_: Exception) {
            false
        }

    private val orientationEventListener by lazy {
        object : OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
            override fun onOrientationChanged(orientation: Int) {
                // Use narrower ranges to determine the orientation. Using a 90 degree range is too sensitive to small tilts.
                val newPhysicalOrientation = when {
                    (orientation in 0..10) || (orientation in 350 until 360) -> {
                        Configuration.ORIENTATION_PORTRAIT
                    }
                    orientation in 80..100 -> {
                        Configuration.ORIENTATION_LANDSCAPE
                    }
                    orientation in 170..190 -> {
                        Configuration.ORIENTATION_PORTRAIT
                    }
                    orientation in 260..280 -> {
                        Configuration.ORIENTATION_LANDSCAPE
                    }
                    else -> {
                        Configuration.ORIENTATION_UNDEFINED
                    }
                }

                if (!options.autoExitOnRotate) {
                    return
                }

                val canReleaseFromLandscape = newPhysicalOrientation == Configuration.ORIENTATION_PORTRAIT && isLockedToLandscape && userHasRotatedToVideoOrientation
                val canReleaseFromPortrait = newPhysicalOrientation == Configuration.ORIENTATION_LANDSCAPE && isLockedToPortrait && userHasRotatedToVideoOrientation

                if (canReleaseFromPortrait || canReleaseFromLandscape) {
                    if (!isAutoRotationEnabled) {
                        return
                    }
                    onShouldReleaseOrientation()
                    this@FullscreenActivityOrientationHelper.stopOrientationEventListener()
                }

                val hasRotatedToVideoOrientationPortrait = newPhysicalOrientation == Configuration.ORIENTATION_PORTRAIT && isLockedToPortrait && !userHasRotatedToVideoOrientation
                val hasRotatedToVideoOrientationLandscape = newPhysicalOrientation == Configuration.ORIENTATION_LANDSCAPE && isLockedToLandscape && !userHasRotatedToVideoOrientation

                if (hasRotatedToVideoOrientationPortrait || hasRotatedToVideoOrientationLandscape) {
                    userHasRotatedToVideoOrientation = true
                }
            }
        }
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        val orientation = newConfig.orientation
        if (!options.autoExitOnRotate) {
            return
        }

        if (isLockedToPortrait && orientation == Configuration.ORIENTATION_LANDSCAPE) {
            onShouldAutoExit()
        } else if (isLockedToLandscape && orientation == Configuration.ORIENTATION_PORTRAIT) {
            onShouldAutoExit()
        }
    }

    fun startOrientationEventListener() {
        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable()
        }
    }

    fun stopOrientationEventListener() {
        orientationEventListener.disable()
    }
}
