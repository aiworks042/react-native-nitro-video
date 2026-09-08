package com.margelo.nitro.nitrovideo.records

import android.graphics.Rect
import android.util.Rational

data class PiPParams(
    val autoEnter: Boolean = false,
    val canEnter: Boolean = false,
    val willEnter: Boolean = false,
    val blocksAppFromEntering: Boolean = false,
    val aspectRatio: Rational? = Rational(16, 9),
    val rectHint: Rect = Rect(0, 0, 0, 0)
) {
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is PiPParams) return false
        return this.canEnter == other.canEnter &&
            this.willEnter == other.willEnter &&
            this.autoEnter == other.autoEnter &&
            this.blocksAppFromEntering == other.blocksAppFromEntering &&
            this.aspectRatio == other.aspectRatio &&
            this.rectHint == other.rectHint
    }

    override fun hashCode(): Int {
        var result = autoEnter.hashCode()
        result = 31 * result + canEnter.hashCode()
        result = 31 * result + willEnter.hashCode()
        result = 31 * result + blocksAppFromEntering.hashCode()
        result = 31 * result + (aspectRatio?.hashCode() ?: 0)
        result = 31 * result + rectHint.hashCode()
        return result
    }
}
