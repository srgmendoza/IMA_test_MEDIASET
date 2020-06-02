package com.google.ads.interactivemedia.v3.samples.mediaset

import android.content.Context
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import com.google.ads.interactivemedia.v3.samples.videoplayerapp.R
import kotlinx.android.synthetic.main.player_controller.view.*

import java.lang.ref.WeakReference
import java.util.Formatter
import java.util.Locale



data class VideoInfo(val title: String = "", val subtitle: String = "", val extratitle: String = "")

class VideoControllerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "VideoControllerView"
        private const val sDefaultTimeout = 3000
        private const val FADE_OUT = 1
        private const val SHOW_PROGRESS = 2
    }

    interface PlayerOptionClickListener {
        fun showInfo()
        fun share()
        fun close()
        fun audioOption()
        fun onControllerVisibilityChanged(isVisible: Boolean)
    }

    private var isShowing: Boolean = false
    private var dragging: Boolean = false
    private var formatBuilder: StringBuilder
    private var formatter: Formatter
    private val handler = MessageHandler(this)

    var playerControlsEnabled: Boolean = true
        set(value) {
            field = value
            forward_btn.visibility = if (value && seekable) View.VISIBLE else View.GONE
            replay_btn.visibility = if (value && seekable) View.VISIBLE else View.GONE
            btn_play_big.visibility = if (value) View.VISIBLE else View.GONE
        }


    var seekable = true
        set(value) {
            field = value
            seekBar.visibility = if (value) View.VISIBLE else View.GONE
            forward_btn.visibility = if (value) View.VISIBLE else View.GONE
            replay_btn.visibility = if (value) View.VISIBLE else View.GONE
            remaining_time.visibility = if (value) View.VISIBLE else View.GONE
            current_time.visibility = if (value) View.VISIBLE else View.GONE
        }


    var optionClickListener: PlayerOptionClickListener? = null

    var player: IPlayer? = null
        set(value) {
            field = value
            field?.setOnClickListener(OnClickListener { performClick() })
        }


    private val pauseListener = OnClickListener {
        doPauseResume()
        show(sDefaultTimeout)
    }

    private val forwardListener = OnClickListener {
        doSeek(30000)
        show(sDefaultTimeout)
    }

    private val replayListener = OnClickListener {
        doSeek(-30000)
        show(sDefaultTimeout)
    }

    private val seekListener = object : OnSeekBarChangeListener {
        override fun onStartTrackingTouch(bar: SeekBar) {
            show(3600000)
            dragging = true
            handler.removeMessages(SHOW_PROGRESS)
        }

        override fun onProgressChanged(bar: SeekBar, progress: Int, fromuser: Boolean) {
            if (player == null || !fromuser) return

            val duration = player?.durationMs ?: 0
            val newPosition = duration * progress / 1000L
            player?.seekTo(newPosition)
            current_time.text = stringForTime(newPosition.toInt())
        }

        override fun onStopTrackingTouch(bar: SeekBar) {
            dragging = false
            setProgress()
            updatePausePlay()
            show(sDefaultTimeout)
            handler.sendEmptyMessage(SHOW_PROGRESS)
        }
    }

    init {
        View.inflate(context, R.layout.player_controller, this)
        btn_play_big?.requestFocus()
        btn_play_big?.setOnClickListener(pauseListener)
        seekBar?.setOnSeekBarChangeListener(seekListener)
        seekBar?.max = 1000

        forward_btn?.setOnClickListener(forwardListener)
        replay_btn?.setOnClickListener(replayListener)


        formatBuilder = StringBuilder()
        formatter = Formatter(formatBuilder, Locale.getDefault())
    }

    fun show(timeout: Int = sDefaultTimeout) {
        if (!isShowing) {
            setProgress()
            btn_play_big.requestFocus()
            visibility = View.VISIBLE
            isShowing = true
            bringToFront()
        }
        updatePausePlay()

        handler.sendEmptyMessage(SHOW_PROGRESS)

        val msg = handler.obtainMessage(FADE_OUT)
        if (timeout != 0) {
            handler.removeMessages(FADE_OUT)
            handler.sendMessageDelayed(msg, timeout.toLong())
        }
        optionClickListener?.onControllerVisibilityChanged(isShowing)
    }

    fun hide() {
        try {
            visibility = View.INVISIBLE
            handler.removeMessages(SHOW_PROGRESS)
        } catch (ex: IllegalArgumentException) {
            Log.w(TAG, "already removed")
        }

        isShowing = false
        optionClickListener?.onControllerVisibilityChanged(isShowing)
    }

    override fun performClick(): Boolean {
        if (isShowing) hide() else show(sDefaultTimeout)
        return super.performClick()
    }

    private fun stringForTime(timeMs: Int): String {
        val totalSeconds = timeMs / 1000

        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600

        formatBuilder.setLength(0)
        return if (hours > 0) {
            formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            formatter.format("%02d:%02d", minutes, seconds).toString()
        }
    }

    private fun setProgress(): Int {
        if (player == null || dragging) {
            return 0
        }

        val position = player?.currentPositionMs ?: 0
        val duration = player?.durationMs ?: 0

        if (duration > 0) {
            val pos = 1000L * position / duration
            seekBar.progress = pos.toInt()
        }

        remaining_time.text = stringForTime(duration.toInt())
        current_time.text = stringForTime(position.toInt())

        return position.toInt()
    }

    override fun onTrackballEvent(ev: MotionEvent): Boolean {
        show(sDefaultTimeout)
        return false
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (player == null) {
            return true
        }

        val keyCode = event.keyCode
        val uniqueDown = event.repeatCount == 0 && event.action == KeyEvent.ACTION_DOWN
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (uniqueDown) {
                doPauseResume()
                show(sDefaultTimeout)
                btn_play_big.requestFocus()
            }
            return true
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            if (uniqueDown && player?.isPlaying() == false) {
                player?.start()
                updatePausePlay()
                show(sDefaultTimeout)
            }
            return true
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if (uniqueDown && player?.isPlaying() == true) {
                player?.pause()
                updatePausePlay()
                show(sDefaultTimeout)
            }
            return true
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
            // don't show the controls for volume adjustment
            return super.dispatchKeyEvent(event)
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            if (uniqueDown) {
                hide()
            }
            return true
        }

        show(sDefaultTimeout)
        return super.dispatchKeyEvent(event)
    }

    fun updatePausePlay() {
        if (btn_play_big == null || player == null) {
            return
        }

        if (player?.isPlaying() == true) {
            btn_play_big.setImageResource(R.drawable.pause_btn)
        } else {
            btn_play_big.setImageResource(R.drawable.play_btn)
        }
    }

    private fun doPauseResume() {
        if (player == null) {
            return
        }

        if (player?.isPlaying() == true) {
            player?.pause()
        } else {
            player?.start()
        }
        updatePausePlay()
    }

    private fun doSeek(offset: Int) {
        if (player == null) {
            return
        }

        var newPosition = (player?.currentPositionMs ?: 0) + offset

        if (newPosition < 0) newPosition = 0
        else if (newPosition > player?.durationMs ?: 0) newPosition = player?.durationMs
                ?: 0

        player?.seekTo(newPosition)

    }


    private class MessageHandler internal constructor(view: VideoControllerView) : Handler() {
        private val mView: WeakReference<VideoControllerView> = WeakReference(view)

        override fun handleMessage(message: Message) {
            var msg = message
            val view = mView.get()
            if (null == view?.player) {
                return
            }

            val pos: Int
            when (msg.what) {
                FADE_OUT -> view.hide()
                SHOW_PROGRESS -> {
                    pos = view.setProgress()
                    if (!view.dragging && view.isShowing && view.player?.isPlaying() == true) {
                        msg = obtainMessage(SHOW_PROGRESS)
                        sendMessageDelayed(msg, (1000 - pos % 1000).toLong())
                    }
                }
            }
        }
    }
}