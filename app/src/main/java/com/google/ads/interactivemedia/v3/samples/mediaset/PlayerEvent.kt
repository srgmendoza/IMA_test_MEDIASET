package com.google.ads.interactivemedia.v3.samples.mediaset

class PlayerEvent(val type: Type, val positionMS: Long, val durationMS: Long) {
    enum class Type {
        READY,
        PLAY,
        PAUSE,
        SEEK,
        COMPLETED,
        PROGRESS,
        STOP,
        ERROR
    }
}