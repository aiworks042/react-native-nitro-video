package com.margelo.nitro.nitrovideo.enums

enum class AudioMixingMode(val value: String) {
    MIX_WITH_OTHERS("mixWithOthers"),
    DUCK_OTHERS("duckOthers"),
    AUTO("auto"),
    DO_NOT_MIX("doNotMix");

    val priority: Int
        get() = when (this) {
            DO_NOT_MIX -> 3
            AUTO -> 2
            DUCK_OTHERS -> 1
            MIX_WITH_OTHERS -> 0
        }

    companion object {
        fun fromString(str: String?): AudioMixingMode {
            return entries.firstOrNull { it.value.equals(str, ignoreCase = true) } ?: AUTO
        }
    }
}
