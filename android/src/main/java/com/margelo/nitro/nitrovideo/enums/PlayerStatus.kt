package com.margelo.nitro.nitrovideo.enums

enum class PlayerStatus(val value: String) {
    IDLE("idle"),
    LOADING("loading"),
    READY_TO_PLAY("readyToPlay"),
    ERROR("error");

    companion object {
        fun fromString(str: String?): PlayerStatus {
            return entries.firstOrNull { it.value.equals(str, ignoreCase = true) } ?: IDLE
        }
    }
}
