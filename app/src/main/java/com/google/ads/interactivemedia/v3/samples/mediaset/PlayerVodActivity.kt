package com.google.ads.interactivemedia.v3.samples.mediaset

import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AlertDialog
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.ads.interactivemedia.v3.api.*
import com.google.ads.interactivemedia.v3.api.AdEvent.AdEventType.*
import com.google.ads.interactivemedia.v3.samples.videoplayerapp.R
import com.google.ads.interactivemedia.v3.samples.videoplayerapp.VideoMetadata
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.ui.TrackSelectionView
import com.google.android.gms.cast.framework.*
import com.google.android.gms.cast.framework.CastSession
import kotlinx.android.synthetic.main.activity_player_vod.*


class PlayerVodActivity : AppCompatActivity(),
    AdEvent.AdEventListener,
    AdErrorEvent.AdErrorListener,
    VideoControllerView.PlayerOptionClickListener,
    OnPlayerEventListener
{

    companion object {
        @JvmField
        var VIDEO: String = "video"

        @JvmField
        var CONTENT: String = "content"

        @JvmField
        var XDR: String = "xdr"
        const val RECOMMENDATIONS_OFFSET_TIME = 20000
        var isAdComing: Boolean = false
    }

    private var contentCompleted: Boolean = false
    private var isAdDisplayed: Boolean = false

//    private lateinit var infoFragment: PlayerInfoFragment

    private lateinit var sdkFactory: ImaSdkFactory
    private lateinit var adsLoader: AdsLoader
    var adsManager: AdsManager? = null
    private var adUiContainer: ViewGroup? = null

    private var videoPlayer: IPlayer? = null

    //private var mCastContext: CastContext? = null
    private var mCastSession: CastSession? = null
    //private var mSessionManagerListener: SessionManagerListener<CastSession>? = null

    private var currentlUrlIndex = 0
    private var streams: List<String> = listOf()
    private var drms: List<String> = listOf()
    private var cids: List<String> = listOf()

    private var xdrPositionMs: Int = 0

    private var isMidrollReady = false
    private var isPostrollReady = false
    private var isPrerollPlayed = false
    private var adsCuePoints = mutableListOf<Float>()

    private var totalPlayTime = 0L

    private lateinit var timer: CountDownTimer

    enum class TimerState{
        STOPPED,RUNNING
    }
    private var timerState = TimerState.STOPPED
    private var totalLength = 0L
    private var playedTime = 0L

    private var playerFormat: Format? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_vod)

        adUiContainer = ads_layout_container

        createAdsLoader()

        startPlaying()

    }

    private fun createAdsLoader() {
        sdkFactory = ImaSdkFactory.getInstance()
        val settings = sdkFactory.createImaSdkSettings()
        settings.autoPlayAdBreaks = false
        settings?.language = "es"
        val adDisplayContainer: AdDisplayContainer = sdkFactory.createAdDisplayContainer()
        adDisplayContainer.adContainer = adUiContainer
        adsLoader = sdkFactory.createAdsLoader(this, settings, adDisplayContainer)

        adsLoader.addAdErrorListener(this)
        adsLoader.addAdsLoadedListener { adsManagerLoadedEvent ->
            adsManager = adsManagerLoadedEvent?.adsManager
            adsManager?.addAdErrorListener(this@PlayerVodActivity)
            adsManager?.addAdEventListener(this@PlayerVodActivity)
            adsManager?.init()

            setMidrollsTimeList(adsManager?.adCuePoints as MutableList<Float>)
        }
    }

    private fun setMidrollsTimeList(adCuePoints: MutableList<Float>) {
        adsCuePoints = adsManager?.adCuePoints as MutableList<Float>

        if(!adsCuePoints.isNullOrEmpty()){
            if(!isPrerollPlayed && adsCuePoints.isNotEmpty()) adsCuePoints.removeAt(0)
        }

    }


    fun startPlaying() {
        videoPlayer = HlsPlayer(this, playerView)
        videoPlayer?.addPlayerEventListener(this)


        videoPlayer?.initPlayer(VideoMetadata.PRE_ROLL_NO_SKIP.videoUrl)

        requestAds()
    }

    private fun requestAds() {
        val request = sdkFactory.createAdsRequest()
        request?.adTagUrl = VideoMetadata.PRE_ROLL_NO_SKIP.adTagUrl
        request?.setContentProgressProvider {
            val duration = videoPlayer?.durationMs ?: Long.MAX_VALUE

            /* Sending current playtime not current position to get
            the proper ad for the user playtime  */
            val position = (totalPlayTime * 1000) //videoPlayer?.currentPositionMs ?: 0

            if (isAdDisplayed || videoPlayer == null || duration <= 0) {
                VideoProgressUpdate.VIDEO_TIME_NOT_READY
            }
            else {
                VideoProgressUpdate(position, duration)
            }
        }
        adsLoader.requestAds(request)
    }

    public override fun onResume() {
        if (adsManager != null && isAdDisplayed) {
            adsManager?.resume()
        } else {
            videoPlayer?.start()
        }


        super.onResume()
    }

    public override fun onPause() {
        if (adsManager != null && isAdDisplayed) {
            adsManager?.pause()
        } else {
            videoPlayer?.pause()
        }
        super.onPause()
    }

    override fun close() {
        finish()
    }

    override fun showInfo() {
    }

    override fun share() {
    }

    override fun audioOption() {
        var trackSelectionDialog =
                TrackSelectionView.getDialog(this, "Seleccionar track", videoPlayer?.trackSelector, C.TRACK_TYPE_AUDIO)
        trackSelectionDialog.first.show()
        /*TODO: Necessary if in future it is updated to ExoPlayer version 2.10.X
        val trackSelectionDialog =
                TrackSelectionDialogBuilder(this, "Seleccionar track", videoPlayer?.trackSelector, C.TRACK_TYPE_AUDIO).build()
        trackSelectionDialog.show()*/
    }

    override fun onControllerVisibilityChanged(isVisible: Boolean) {
    }


    override fun onPlayerEvent(playerEvent: PlayerEvent) {
        Log.i("Event", "Player event: " + playerEvent.type)
        when (playerEvent.type) {
            PlayerEvent.Type.PLAY -> {
                rv_progress_bar.visibility = View.GONE

            }
            PlayerEvent.Type.COMPLETED -> {
                Log.d("TotalPLayTime", playedTime.toString())
                isPostrollReady = true
                stopTimer()

                contentCompleted = true

                if (adsManager == null) {
                    onAdsAndContentCompleted()
                    isAdComing = false
                }
                else {
                    adsLoader.contentComplete()
                    isAdComing = true
                }
            }
            PlayerEvent.Type.PROGRESS -> {
                startTimer()

                if(!adsCuePoints.isNullOrEmpty()) {
                    if (totalPlayTime >= adsCuePoints[0] && isMidrollReady) {
                        //totalPlayTime = 0
                        isMidrollReady = false
                        adsCuePoints.removeAt(0)
                        playMidroll()
                    }
                }
            }
            PlayerEvent.Type.PAUSE -> {
                stopTimer()
            }
            PlayerEvent.Type.SEEK -> {
                stopTimer()
            }
            else -> {
            }
        }
    }

    private fun startTimer(){
        if(timerState == TimerState.RUNNING) return

        timerState = TimerState.RUNNING

        totalLength = videoPlayer?.durationMs ?: 0

        timer = object : CountDownTimer(totalLength, 1000) {
            override fun onFinish() {
                timerState = TimerState.STOPPED
            }

            override fun onTick(millisUntilFinished: Long) {
                timerState = TimerState.RUNNING
                playedTime = (totalLength - millisUntilFinished) / 1000
                if((totalLength - millisUntilFinished) != 0L)
                    totalPlayTime += 1
                Log.d("totalPlayTime", totalPlayTime.toString())
            }
        }

        timer.start()
    }

    private fun stopTimer() {
        if(timerState == TimerState.STOPPED) return

        timerState = TimerState.STOPPED
//        totalPlayTime += playedTime
        timer.cancel()
        Log.d("TotalPlayedTime", totalPlayTime.toString())
    }

    private fun playMidroll() {
        if (adsManager != null)
            adsManager?.start()
    }

    override fun onErrorEvent() {

    }

    override fun onConcurrentErrorEvent() {
    }

    override fun onAdEvent(adEvent: AdEvent) {
        Log.i("Event", "AD_EVENT: ${adEvent.type}")

        try {
            when (adEvent.type) {

                LOADED -> {
                    if(!isPrerollPlayed || isPostrollReady) {
                        if (adsManager != null) {
                            isPrerollPlayed = true
                            isPostrollReady = false
                            adsManager?.start()
                        }
                    }
                } //Ads are ready to be played.
                CONTENT_PAUSE_REQUESTED -> { // Immediately before a video ad is played.
                    rv_progress_bar.visibility = View.VISIBLE
                    //progress_bar.bringToFront()
                    ads_layout_container.bringToFront()
                    isAdDisplayed = true
                    videoPlayer?.pause()
                }
                CONTENT_RESUME_REQUESTED -> {//Ad is contentCompleted
                    rv_progress_bar.visibility = View.GONE
                    isAdDisplayed = false
                    videoPlayer?.start()
                }
                ALL_ADS_COMPLETED -> {
                    adsManager?.destroy()
                    adsManager = null
                    if (contentCompleted) onAdsAndContentCompleted()
                }
                COMPLETED -> {
                    ads_layout_container.visibility = View.GONE
                    rv_progress_bar.visibility = View.VISIBLE
                    progress_bar.visibility = View.GONE
                }
                STARTED -> {
                    ads_layout_container.visibility = View.VISIBLE
                    rv_progress_bar.visibility = View.GONE
                    progress_bar.visibility = View.VISIBLE
                }
                CLICKED -> {

                }
                AD_BREAK_READY -> {
                    if(isPrerollPlayed)
                        isMidrollReady = true
                }
                else -> {
                }
            }
        } catch (e: Exception) {
            Log.e("Error with Ads", e.localizedMessage)
            videoPlayer?.start()
        }
    }

    override fun onAdError(adErrorEvent: AdErrorEvent) {
        Log.e("TAG", "Ad Error: ${adErrorEvent.error.message}")
        videoPlayer?.start()
    }


    override fun onChangeUrlEvent() {
    }

    private fun showErrorDialog(message: String) {
        val errorAlertBuilder = AlertDialog.Builder(this)
        errorAlertBuilder.setMessage(message)
        errorAlertBuilder.setCancelable(false)
        errorAlertBuilder.setPositiveButton("OK") { _, _ -> close() }
        val alertDialog = errorAlertBuilder.create()
        try {
            alertDialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun onAdsAndContentCompleted() {
        close()
    }

    private fun stop() {
        adsManager?.destroy()
        adsManager = null
        videoPlayer?.stop()
        videoPlayer = null
        stopTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        stop()
    }

}
