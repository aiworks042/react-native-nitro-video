package com.margelo.nitro.nitrovideo.records

import androidx.media3.common.PlaybackException
import java.io.Serializable

class PlaybackError(
    var message: String? = null
) : Serializable {
    constructor(exception: PlaybackException) : this(errorMessageFromException(exception))

    companion object {
        private fun errorMessageFromException(exception: PlaybackException): String {
            val reason = "${exception.localizedMessage} ${exception.cause?.localizedMessage ?: ""}"
            return "A playback exception has occurred: $reason"
        }
    }
}
