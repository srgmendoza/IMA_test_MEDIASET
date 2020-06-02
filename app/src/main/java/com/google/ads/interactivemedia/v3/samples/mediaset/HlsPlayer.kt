package com.google.ads.interactivemedia.v3.samples.mediaset

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.View
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.metadata.id3.TextInformationFrame
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory

class HlsPlayer(private val context: Context, private val playerView: PlayerView, override var metadataListener: MetadataListener? = null)
    : Player.EventListener, IPlayer {

    companion object {
        private val LOG_TAG = "MitelePlayer"
        private val USER_AGENT = "MitelePlayer (Android " + Build.VERSION.RELEASE + ")"
    }

    private var startSeekingPosition: Long = 0L
    private var seeking: Boolean = false
    override val videoEventListeners: MutableList<OnPlayerEventListener> = mutableListOf()

    private var playerFormat: Format? = null

    var player: SimpleExoPlayer? = null

    private var idle = true

    private val progressHandler = Handler()

    private val progressRunnable = object : Runnable {
        override fun run() {
            videoEventListeners.forEach { it.onPlayerEvent(PlayerEvent(PlayerEvent.Type.PROGRESS, currentPositionMs, durationMs)) }
            progressHandler.postDelayed(this, 1000)
        }
    }

    override var trackSelector: DefaultTrackSelector? = DefaultTrackSelector()

    override val durationMs: Long
        get() = playerView.player?.duration ?: -1

    override val currentPositionMs: Long
        get() {
            var position = playerView.player?.currentPosition ?: 0
            val currentTimeline = playerView.player?.currentTimeline
            if (currentTimeline?.isEmpty == false) {
                position -= currentTimeline.getPeriod(playerView.player?.currentPeriodIndex
                        ?: 0, Timeline.Period()).positionInWindowMs
            }
            return if (seeking) startSeekingPosition else position
        }

    init {
        playerView.useController = false
    }


    override fun initPlayer(url: String, drm: String?, cid: String?) {

        trackSelector?.parameters = DefaultTrackSelector.ParametersBuilder().setPreferredTextLanguage("es").build()

        player = ExoPlayerFactory.newSimpleInstance(context, DefaultRenderersFactory(context), trackSelector)


        // Register for ID3 events.
        player?.addMetadataOutput { metadata ->
            for (i in 0 until metadata.length()) {
                val entry = metadata.get(i)
                if (entry is TextInformationFrame) {
                    if ("TXXX" == entry.id) {
                        if ("event".equals(entry.description, ignoreCase = true)) {
                            metadataListener?.onID3TagReceived(entry.value)
                        } else {
                            metadataListener?.onUserTextReceived(entry.value)
                        }
                    }
                }
            }
        }

        player?.playWhenReady = true
        player?.addListener(this)


        player?.prepare(buildMediaSource(Uri.parse(url)))

        playerView.player = player

    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        return if (uri.lastPathSegment?.contains("mp3") == true || uri.lastPathSegment?.contains("mp4") == true) {
            ExtractorMediaSource.Factory(DefaultHttpDataSourceFactory(USER_AGENT))
                    .createMediaSource(uri)
        } else if (uri.lastPathSegment?.contains("m3u8") == true) {
            HlsMediaSource.Factory(DefaultHttpDataSourceFactory(USER_AGENT)).createMediaSource(uri)
        } else {
            val dashChunkSourceFactory = DefaultDashChunkSource.Factory(
                    DefaultHttpDataSourceFactory(USER_AGENT))
            val manifestDataSourceFactory = DefaultHttpDataSourceFactory(USER_AGENT)
            DashMediaSource.Factory(dashChunkSourceFactory, manifestDataSourceFactory).createMediaSource(uri)
        }
    }

    override fun start() {
        if (playerView.player != null) {
            progressHandler.postDelayed(progressRunnable, 1000)
            videoEventListeners.forEach { it.onPlayerEvent(PlayerEvent(PlayerEvent.Type.PLAY, currentPositionMs, durationMs)) }
            playerView.player?.playWhenReady = true

        }
    }

    override fun pause() {
        if (playerView.player != null) {
            progressHandler.removeCallbacks(progressRunnable)
            videoEventListeners.forEach { it.onPlayerEvent(PlayerEvent(PlayerEvent.Type.PAUSE, currentPositionMs, durationMs)) }
            playerView.player?.playWhenReady = false
        }
    }

    override fun stop() {
        if (playerView.player != null) {
            progressHandler.removeCallbacks(progressRunnable)
            videoEventListeners.forEach { it.onPlayerEvent(PlayerEvent(PlayerEvent.Type.STOP, currentPositionMs, durationMs)) }
            playerView.player?.stop()
            playerView.player?.release()
            playerView.player = null
            idle = true
        }
    }

    override fun seekTo(mSec: Long) {
        if (playerView.player != null) {
            if (!seeking) {
                startSeekingPosition = playerView.player.currentPosition
                seeking = true
            }
            playerView.player?.seekTo(mSec)
            videoEventListeners.forEach { it.onPlayerEvent(PlayerEvent(PlayerEvent.Type.SEEK, currentPositionMs, durationMs)) }
        }
    }

    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {}

    override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {}

    override fun onLoadingChanged(isLoading: Boolean) {}

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
            Player.STATE_READY -> {
                if (idle) {
                    videoEventListeners.forEach { it.onPlayerEvent(PlayerEvent(PlayerEvent.Type.READY, currentPositionMs, durationMs)) }
                    if (playWhenReady) {
                        videoEventListeners.forEach { it.onPlayerEvent(PlayerEvent(PlayerEvent.Type.PLAY, currentPositionMs, durationMs)) }
                        progressHandler.postDelayed(progressRunnable, 1000)
                    } else {
                        videoEventListeners.forEach { it.onPlayerEvent(PlayerEvent(PlayerEvent.Type.PAUSE, currentPositionMs, durationMs)) }
                        progressHandler.removeCallbacks(progressRunnable)
                    }
                }
                idle = false
                seeking = false
            }
            Player.STATE_IDLE -> idle = true
            Player.STATE_ENDED -> {
                idle = true
                videoEventListeners.forEach { it.onPlayerEvent(PlayerEvent(PlayerEvent.Type.COMPLETED, currentPositionMs, durationMs)) }
            }
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {}

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}

    override fun onPlayerError(error: ExoPlaybackException) {
        if(error.type == ExoPlaybackException.TYPE_SOURCE){
            videoEventListeners.forEach{it.onChangeUrlEvent()}
        }else{
            videoEventListeners.forEach { it.onErrorEvent()}
        }

    }

    override fun onPositionDiscontinuity(reason: Int) {}

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {}

    override fun onSeekProcessed() {}

    override fun isPlaying() = playerView.player?.playWhenReady ?: false

    override fun isSeeking() = seeking

    override fun setOnClickListener(onClickListener: View.OnClickListener) {
        playerView.setOnClickListener(onClickListener)
    }

    override fun getExoPlayer() = if (playerView.player is ExoPlayer) playerView.player as ExoPlayer else null

    override fun getAudioTracks(): Map<String, String> {


        Log.e("TRACKS", trackSelector?.currentMappedTrackInfo.toString())

        return mutableMapOf()
    }

    override fun getVideoFormat(): Format? {
        var format = player?.videoFormat
        return format
    }

    override fun getDroppedFrames(): Int? {
        var droppedFrames = player?.videoDecoderCounters?.droppedBufferCount
        return droppedFrames
    }

}