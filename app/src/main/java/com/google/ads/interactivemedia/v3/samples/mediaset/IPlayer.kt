package com.google.ads.interactivemedia.v3.samples.mediaset

import android.content.Context
import android.view.View
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector


interface IPlayer {
    val durationMs: Long
    val currentPositionMs: Long
    val videoEventListeners: MutableList<OnPlayerEventListener>
    var metadataListener: MetadataListener?
    var trackSelector: DefaultTrackSelector?

    fun initPlayer(url: String, drm: String? = "", cid: String? = "")

    fun isPlaying(): Boolean

    fun isSeeking(): Boolean

    fun start()

    fun pause()

    fun stop()

    fun seekTo(mSec: Long)

    fun setOnClickListener(onClickListener: View.OnClickListener)

    fun addPlayerEventListener(listener: OnPlayerEventListener) {
        videoEventListeners.add(listener)
    }

    fun getExoPlayer(): ExoPlayer?

    fun getAudioTracks(): Map<String, String>

    fun getVideoFormat(): Format?

    fun getDroppedFrames(): Int?
}

interface OnPlayerEventListener {
    fun onPlayerEvent(playerEvent: PlayerEvent)
    fun onErrorEvent()
    fun onChangeUrlEvent()
    fun onConcurrentErrorEvent()
}

interface MetadataListener {
    fun onID3TagReceived(tagValue: String)
    fun onUserTextReceived(userText: String)
}
