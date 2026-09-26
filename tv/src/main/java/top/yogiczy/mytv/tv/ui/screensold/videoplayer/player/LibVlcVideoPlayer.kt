package top.yogiczy.mytv.tv.ui.screensold.videoplayer.player

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.SurfaceView
import android.view.TextureView
import kotlinx.coroutines.CoroutineScope
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import top.yogiczy.mytv.core.data.entities.channel.ChannelLine
import top.yogiczy.mytv.tv.ui.utils.Configs
import kotlin.math.roundToInt

class LibVlcVideoPlayer(
    context: Context,
    coroutineScope: CoroutineScope,
) : VideoPlayer(coroutineScope) {

    private val libVlc = LibVLC(
        context.applicationContext,
        arrayListOf(
            "--network-caching=1200",
            "--live-caching=1200",
            "--http-reconnect",
            "--drop-late-frames",
            "--skip-frames",
        ),
    ).apply {
        setUserAgent(Configs.videoPlayerUserAgent, Configs.videoPlayerUserAgent)
    }
    private val mediaPlayer = MediaPlayer(libVlc)

    private var surfaceView: SurfaceView? = null
    private var textureView: TextureView? = null
    private var currentLine = ChannelLine()
    private var released = false

    private val useHardwareDecoder = !isEmulator()

    private val eventListener = MediaPlayer.EventListener { event ->
        if (released) return@EventListener

        when (event.type) {
            MediaPlayer.Event.Opening -> {
                triggerBuffering(true)
            }

            MediaPlayer.Event.Buffering -> {
                triggerBuffering(event.buffering < 100f)
            }

            MediaPlayer.Event.Playing -> {
                triggerBuffering(false)
                triggerReady()
                triggerIsPlayingChanged(true)
                triggerDuration(mediaPlayer.length.coerceAtLeast(0L))
                refreshMetadata()
            }

            MediaPlayer.Event.Paused,
            MediaPlayer.Event.Stopped,
            MediaPlayer.Event.EndReached -> {
                triggerIsPlayingChanged(false)
            }

            MediaPlayer.Event.EncounteredError -> {
                triggerBuffering(false)
                triggerError(PlaybackException("VLC_ERROR_PLAYBACK_FAILED", 30001))
            }

            MediaPlayer.Event.TimeChanged -> {
                triggerCurrentPosition(event.timeChanged.coerceAtLeast(0L))
            }

            MediaPlayer.Event.LengthChanged -> {
                triggerDuration(event.lengthChanged.coerceAtLeast(0L))
            }

            MediaPlayer.Event.Vout,
            MediaPlayer.Event.ESAdded,
            MediaPlayer.Event.ESDeleted,
            MediaPlayer.Event.ESSelected -> refreshMetadata()
        }
    }

    override fun initialize() {
        super.initialize()
        mediaPlayer.setEventListener(eventListener)
        attachVideoOutput()
    }

    override fun prepare(line: ChannelLine) {
        if (released || line.playableUrl.isBlank()) return

        currentLine = line
        mediaPlayer.stop()

        val media = Media(libVlc, Uri.parse(line.playableUrl)).apply {
            setHWDecoderEnabled(useHardwareDecoder, false)
            setDefaultMediaPlayerOptions()
            addOption(":network-caching=1200")
            addOption(":live-caching=1200")
            addOption(":http-reconnect")
            addOption(":input-repeat=-1")
            line.httpUserAgent?.let { addOption(":http-user-agent=$it") }
        }

        mediaPlayer.media = media
        media.release()
        Log.i(TAG, "prepare: hardwareDecoder=$useHardwareDecoder, url=${line.playableUrl}")
        triggerPrepared()
        mediaPlayer.play()
    }

    override fun play() {
        if (!released && mediaPlayer.hasMedia()) mediaPlayer.play()
    }

    override fun pause() {
        if (!released && mediaPlayer.isPlaying) mediaPlayer.pause()
    }

    override fun seekTo(position: Long) {
        if (!released && mediaPlayer.isSeekable) mediaPlayer.time = position
    }

    override fun setVolume(volume: Float) {
        if (!released) mediaPlayer.volume = (volume.coerceIn(0f, 1f) * 100).roundToInt()
    }

    override fun getVolume(): Float {
        return if (released) 0f else (mediaPlayer.volume.coerceAtLeast(0) / 100f)
    }

    override fun stop() {
        if (!released) mediaPlayer.stop()
        super.stop()
    }

    override fun selectVideoTrack(track: Metadata.Video?) {
        if (released) return
        if (track?.index == null) mediaPlayer.setVideoTrackEnabled(false)
        else {
            mediaPlayer.setVideoTrackEnabled(true)
            mediaPlayer.setVideoTrack(track.index)
        }
        refreshMetadata()
    }

    override fun selectAudioTrack(track: Metadata.Audio?) {
        if (released) return
        mediaPlayer.setAudioTrack(track?.index ?: -1)
        refreshMetadata()
    }

    override fun selectSubtitleTrack(track: Metadata.Subtitle?) {
        if (released) return
        mediaPlayer.setSpuTrack(track?.index ?: -1)
        refreshMetadata()
    }

    override fun setVideoSurfaceView(surfaceView: SurfaceView) {
        if (released || (this.surfaceView === surfaceView && mediaPlayer.vlcVout.areViewsAttached())) return
        this.surfaceView = surfaceView
        textureView = null
        attachVideoOutput()
    }

    override fun setVideoTextureView(textureView: TextureView) {
        if (released || (this.textureView === textureView && mediaPlayer.vlcVout.areViewsAttached())) return
        this.textureView = textureView
        surfaceView = null
        attachVideoOutput()
    }

    private fun attachVideoOutput() {
        if (released || (surfaceView == null && textureView == null)) return

        val vout = mediaPlayer.vlcVout
        if (vout.areViewsAttached()) vout.detachViews()
        surfaceView?.let(vout::setVideoView)
        textureView?.let(vout::setVideoView)
        vout.attachViews { _, width, height, visibleWidth, visibleHeight, sarNum, sarDen ->
            val videoWidth = width.takeIf { it > 0 } ?: visibleWidth
            val videoHeight = height.takeIf { it > 0 } ?: visibleHeight
            if (videoWidth <= 0 || videoHeight <= 0) return@attachViews

            val visibleVideoWidth = visibleWidth.takeIf { it > 0 } ?: videoWidth
            val visibleVideoHeight = visibleHeight.takeIf { it > 0 } ?: videoHeight
            // IPTV 4K 流偶尔携带错误的 SAR。将 SAR 乘入布局宽度会把画面放大并裁掉边缘，
            // 界面只使用可见像素尺寸计算比例，交给 LibVLC 在 Surface 内保持原始比例。
            triggerResolution(
                visibleVideoWidth.coerceAtLeast(1),
                visibleVideoHeight.coerceAtLeast(1),
            )
        }
    }

    private fun refreshMetadata() {
        if (released) return

        runCatching {
            val media = mediaPlayer.media ?: return
            try {
                val selectedVideo = mediaPlayer.videoTrack
                val selectedAudio = mediaPlayer.audioTrack
                val selectedSubtitle = mediaPlayer.spuTrack
                val videoTracks = mutableListOf<Metadata.Video>()
                val audioTracks = mutableListOf<Metadata.Audio>()
                val subtitleTracks = mutableListOf<Metadata.Subtitle>()

                repeat(media.trackCount) { index ->
                    when (val track = media.getTrack(index)) {
                        is IMedia.VideoTrack -> videoTracks += Metadata.Video(
                            index = track.id,
                            isSelected = track.id == selectedVideo,
                            width = track.width,
                            height = track.height,
                            frameRate = if (track.frameRateDen > 0) {
                                track.frameRateNum.toFloat() / track.frameRateDen
                            } else null,
                            bitrate = track.bitrate,
                            mimeType = track.codec,
                            decoder = "LibVLC ${LibVLC.version()}",
                        )

                        is IMedia.AudioTrack -> audioTracks += Metadata.Audio(
                            index = track.id,
                            isSelected = track.id == selectedAudio,
                            channels = track.channels,
                            sampleRate = track.rate,
                            bitrate = track.bitrate,
                            mimeType = track.codec,
                            language = track.language,
                            decoder = "LibVLC ${LibVLC.version()}",
                        )

                        is IMedia.SubtitleTrack -> subtitleTracks += Metadata.Subtitle(
                            index = track.id,
                            isSelected = track.id == selectedSubtitle,
                            bitrate = track.bitrate,
                            mimeType = track.codec,
                            language = track.language,
                        )
                    }
                }

                metadata = Metadata(
                    video = videoTracks.firstOrNull { it.isSelected == true },
                    audio = audioTracks.firstOrNull { it.isSelected == true },
                    subtitle = subtitleTracks.firstOrNull { it.isSelected == true },
                    videoTracks = videoTracks,
                    audioTracks = audioTracks,
                    subtitleTracks = subtitleTracks,
                )
                triggerMetadata(metadata)
            } finally {
                media.release()
            }
        }
    }

    override fun release() {
        if (released) return
        released = true
        mediaPlayer.setEventListener(null)
        if (mediaPlayer.vlcVout.areViewsAttached()) mediaPlayer.vlcVout.detachViews()
        mediaPlayer.stop()
        mediaPlayer.release()
        libVlc.release()
        surfaceView = null
        textureView = null
        super.release()
    }

    private fun isEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.contains("emulator") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu") ||
            Build.MODEL.contains("Emulator")
    }

    private companion object {
        const val TAG = "LibVlcVideoPlayer"
    }
}
