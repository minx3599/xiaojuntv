package top.yogiczy.mytv.tv.ui.screensold.videoplayer.player

import android.content.Context
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import top.yogiczy.mytv.core.data.entities.channel.ChannelLine
import top.yogiczy.mytv.tv.ui.utils.Configs
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MpvVideoPlayer(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val decoderMode: Configs.MpvDecoderMode,
) : VideoPlayer(coroutineScope), MPVLib.EventObserver, MPVLib.LogObserver {
    @Volatile
    private var initialized = false

    @Volatile
    private var released = false

    @Volatile
    private var failureReported = false

    private var pendingLine: ChannelLine? = null
    private var surfaceView: SurfaceView? = null
    @Volatile
    private var currentSurface: Surface? = null
    @Volatile
    private var surfaceAttached = false
    private var updateJob: Job? = null
    private var volume = 1f
    private val loadLock = Any()
    private var latestRequestedLine: ChannelLine? = null
    private var loadTaskScheduled = false

    private val surfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            Log.i(TAG, "surfaceCreated valid=${holder.surface.isValid}")
            currentSurface = holder.surface
            surfaceAttached = false
            enqueue { attachSurfaceIfReady(holder.surface) }
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            currentSurface = holder.surface
            surfaceAttached = false
            enqueue {
                if (currentSurface === holder.surface && initialized) {
                    MPVLib.setPropertyString("android-surface-size", "${width}x$height")
                    attachSurfaceIfReady(holder.surface)
                }
            }
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            Log.i(TAG, "surfaceDestroyed")
            currentSurface = null
            surfaceAttached = false
            enqueue {
                if (initialized && currentSurface == null) {
                    MPVLib.command(arrayOf("stop"))
                    MPVLib.detachSurface()
                }
            }
        }
    }

    override fun initialize() {
        super.initialize()

        enqueue {
            try {
                if (!MPVLib.ensureLoaded()) {
                    reportNativeFailure(
                        code = "MPV_NATIVE_LIBRARY_MISSING",
                        cause = MPVLib.loadErrorMessage(),
                    )
                } else {
                    Log.i(TAG, "initialize decoder=${decoderMode.label} hwdec=${decoderMode.hwdec}")
                    MPVLib.addObserver(this)
                    MPVLib.addLogObserver(this)
                    MPVLib.create(context.applicationContext)
                    setOption("config", "no")
                    setOption("load-scripts", "no")
                    setOption("ytdl", "no")
                    setOption("idle", "yes")
                    setOption("profile", "fast")
                    setOption("force-window", "no")
                    setOption("vo", "gpu")
                    setOption("gpu-context", "android")
                    setOption("opengl-es", "yes")
                    setOption("hwdec", decoderMode.hwdec)
                    setOption("ao", "audiotrack,opensles")
                    setOption("cache", "yes")
                    setOption("cache-secs", "5")
                    setOption("demuxer", "lavf")
                    setOption("demuxer-lavf-format", "mpegts")
                    setOption("demuxer-lavf-probesize", "2097152")
                    setOption("demuxer-lavf-o", "seekable=0,timeout=5000000,reconnect=1")
                    setOption("demuxer-max-bytes", "32MiB")
                    setOption("force-seekable", "no")
                    setOption("stream-buffer-size", "8MiB")
                    setOption("network-timeout", (Configs.videoPlayerLoadTimeout / 1000).toString())
                    MPVLib.init()
                    MPVLib.observeProperty("duration", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE)
                    MPVLib.observeProperty("pause", MPVLib.MpvFormat.MPV_FORMAT_FLAG)
                    MPVLib.observeProperty("video-params/w", MPVLib.MpvFormat.MPV_FORMAT_INT64)
                    MPVLib.observeProperty("video-params/h", MPVLib.MpvFormat.MPV_FORMAT_INT64)
                    initialized = true
                    currentSurface?.takeIf { it.isValid }?.let { attachSurfaceIfReady(it) }
                }
            } catch (t: Throwable) {
                reportNativeFailure("MPV_INITIALIZE_FAILED", t.message ?: t.javaClass.simpleName)
            }
        }
    }

    override fun prepare(line: ChannelLine) {
        Log.i(TAG, "prepare url=${line.playableUrl}")
        triggerPrepared()

        scheduleLoad(line)
    }

    override fun play() {
        enqueue { if (initialized) MPVLib.setPropertyBoolean("pause", false) }
    }

    override fun pause() {
        enqueue { if (initialized) MPVLib.setPropertyBoolean("pause", true) }
    }

    override fun seekTo(position: Long) {
        enqueue {
            if (initialized) {
                MPVLib.command(arrayOf("seek", (position / 1000.0).toString(), "absolute"))
            }
        }
    }

    override fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        enqueue {
            if (initialized) MPVLib.setPropertyDouble("volume", (this.volume * 100).toDouble())
        }
    }

    override fun getVolume(): Float {
        return volume
    }

    override fun stop() {
        enqueue { if (initialized) MPVLib.command(arrayOf("stop")) }
        updateJob?.cancel()
        super.stop()
    }

    override fun selectVideoTrack(track: Metadata.Video?) {}

    override fun selectAudioTrack(track: Metadata.Audio?) {}

    override fun selectSubtitleTrack(track: Metadata.Subtitle?) {}

    override fun setVideoSurfaceView(surfaceView: SurfaceView) {
        if (this.surfaceView === surfaceView) {
            val surface = surfaceView.holder.surface
            if (surface.isValid) {
                currentSurface = surface
                enqueue { attachSurfaceIfReady(surface) }
            }
            return
        }

        this.surfaceView?.holder?.removeCallback(surfaceCallback)
        this.surfaceView = surfaceView
        surfaceView.holder.addCallback(surfaceCallback)
        val surface = surfaceView.holder.surface
        if (surface.isValid) {
            currentSurface = surface
            surfaceAttached = false
            enqueue { attachSurfaceIfReady(surface) }
        }
    }

    override fun setVideoTextureView(textureView: TextureView) {
        triggerError(PlaybackException("MPV_TEXTURE_VIEW_UNSUPPORTED", 20003))
    }

    override fun release() {
        updateJob?.cancel()
        surfaceView?.holder?.removeCallback(surfaceCallback)
        surfaceView = null
        currentSurface = null
        surfaceAttached = false
        released = true
        synchronized(loadLock) {
            latestRequestedLine = null
            loadTaskScheduled = false
        }

        mpvExecutor.execute {
            val shouldCleanup = initialized || failureReported
            initialized = false
            if (shouldCleanup) {
                runCatching { MPVLib.removeObserver(this) }
                runCatching { MPVLib.removeLogObserver(this) }
                runCatching { MPVLib.command(arrayOf("stop")) }
                runCatching { MPVLib.detachSurface() }
                runCatching { MPVLib.destroy() }
            }
        }

        super.release()
    }

    override fun eventProperty(property: String) {}

    override fun eventProperty(property: String, value: Long) {
        runOnMain {
            when (property) {
                "video-params/w" -> updateResolution(width = value.toInt(), height = null)
                "video-params/h" -> updateResolution(width = null, height = value.toInt())
            }
        }
    }

    override fun eventProperty(property: String, value: Boolean) {
        if (property == "pause") runOnMain { triggerIsPlayingChanged(!value) }
    }

    override fun eventProperty(property: String, value: String) {}

    override fun eventProperty(property: String, value: Double) {
        if (property == "duration") runOnMain { triggerDuration((value * 1000).toLong()) }
    }

    override fun event(eventId: Int) {
        Log.i(TAG, "event=$eventId")
        runOnMain {
            when (eventId) {
                MPVLib.MpvEvent.MPV_EVENT_FILE_LOADED,
                MPVLib.MpvEvent.MPV_EVENT_PLAYBACK_RESTART -> {
                    triggerReady()
                    triggerBuffering(false)
                    startPositionUpdates()
                }

                MPVLib.MpvEvent.MPV_EVENT_END_FILE -> triggerIsPlayingChanged(false)
            }
        }
    }

    override fun logMessage(prefix: String, level: Int, text: String) {
        Log.d(TAG, "[$prefix][$level] $text")
    }

    private fun loadLine(line: ChannelLine) {
        val playableUrl = line.playableUrl.toMpvPlayableUrl()
        Log.i(TAG, "loadfile url=$playableUrl raw=${line.playableUrl}")
        setOption("user-agent", line.httpUserAgent ?: Configs.videoPlayerUserAgent)
        setOption("http-header-fields", "Connection: close")
        MPVLib.setPropertyBoolean("pause", false)
        MPVLib.command(
            arrayOf(
                "loadfile",
                playableUrl,
                "replace",
            )
        )
        runOnMain { triggerBuffering(true) }
    }

    private fun scheduleLoad(line: ChannelLine) {
        val shouldSchedule = synchronized(loadLock) {
            latestRequestedLine = line
            if (loadTaskScheduled) {
                false
            } else {
                loadTaskScheduled = true
                true
            }
        }
        if (!shouldSchedule) return

        enqueue {
            while (!released) {
                val nextLine = synchronized(loadLock) {
                    latestRequestedLine.also { latestRequestedLine = null }
                        ?: run {
                            loadTaskScheduled = false
                            null
                        }
                } ?: break

                if (initialized && surfaceAttached && currentSurface?.isValid == true) {
                    loadLine(nextLine)
                } else {
                    Log.i(
                        TAG,
                        "defer load initialized=$initialized surfaceValid=${currentSurface?.isValid} surfaceAttached=$surfaceAttached"
                    )
                    pendingLine = nextLine
                }
            }
        }
    }

    private fun attachSurfaceIfReady(surface: Surface) {
        if (!initialized || released || !surface.isValid || currentSurface !== surface) {
            Log.i(
                TAG,
                "skip attach initialized=$initialized released=$released " +
                    "surfaceValid=${surface.isValid} isCurrent=${currentSurface === surface}"
            )
            return
        }

        Log.i(TAG, "attachSurface")
        MPVLib.attachSurface(surface)
        surfaceAttached = true
        MPVLib.setOptionString("force-window", "yes")
        MPVLib.setPropertyString("vo", "gpu")
        MPVLib.setPropertyBoolean("pause", false)
        pendingLine?.let { loadLine(it) }
        pendingLine = null
    }

    private fun startPositionUpdates() {
        updateJob?.cancel()
        updateJob = coroutineScope.launch {
            while (isActive && !released) {
                enqueue {
                    if (initialized) {
                        val position = MPVLib.getPropertyDouble("time-pos")
                            ?.let { (it * 1000).toLong() }
                        val isPlaying = !(MPVLib.getPropertyBoolean("pause") ?: false)
                        runOnMain {
                            position?.let { triggerCurrentPosition(it) }
                            triggerIsPlayingChanged(isPlaying)
                        }
                    }
                }
                delay(1000)
            }
        }
    }

    private fun runOnMain(block: () -> Unit) {
        coroutineScope.launch { if (!released) block() }
    }

    private fun reportNativeFailure(code: String, cause: String?) {
        if (failureReported || released) return

        failureReported = true
        initialized = false
        surfaceAttached = false
        pendingLine = null
        updateJob?.cancel()
        synchronized(loadLock) {
            latestRequestedLine = null
            loadTaskScheduled = false
        }

        Log.e(TAG, "$code${cause?.let { ": $it" } ?: ""}")
        runOnMain {
            triggerError(PlaybackException(code, 20005))
        }
    }

    private fun setOption(name: String, value: String) {
        val result = MPVLib.setOptionString(name, value)
        if (result < 0) {
            Log.w(TAG, "setOption failed name=$name value=$value result=$result")
        }
    }

    private fun String.toMpvPlayableUrl(): String {
        return if (startsWith("http://") || startsWith("https://")) {
            "ffmpeg://$this"
        } else {
            this
        }
    }

    private fun enqueue(block: () -> Unit) {
        if (released || failureReported) return
        mpvExecutor.execute {
            if (!released && !failureReported) {
                try {
                    block()
                } catch (t: Throwable) {
                    reportNativeFailure(
                        code = "MPV_COMMAND_FAILED",
                        cause = t.message ?: t.javaClass.simpleName,
                    )
                }
            }
        }
    }

    private var videoWidth = 0
    private var videoHeight = 0

    private fun updateResolution(width: Int?, height: Int?) {
        width?.let { videoWidth = it }
        height?.let { videoHeight = it }

        if (videoWidth > 0 && videoHeight > 0) {
            metadata = metadata.copy(
                video = Metadata.Video(
                    width = videoWidth,
                    height = videoHeight,
                    decoder = "mpv",
                )
            )
            triggerResolution(videoWidth, videoHeight)
            triggerMetadata(metadata)
        }
    }

    companion object {
        private const val TAG = "XiaojunMpv"

        // MPVLib is process-global. One queue preserves command order across player recreation.
        private val mpvExecutor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "mpv-command").apply { isDaemon = true }
        }
    }
}
