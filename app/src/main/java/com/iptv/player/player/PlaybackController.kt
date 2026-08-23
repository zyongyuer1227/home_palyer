package com.iptv.player.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.iptv.player.data.api.Channel
import com.iptv.player.data.api.StreamInfo
import com.iptv.player.data.local.FileLogger
import com.iptv.player.data.repo.ChannelRepo
import com.iptv.player.data.repo.NasRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayCandidate(
    val url: String,
    val label: String,
    val reconnectable: Boolean = false,
)

data class NasPlayRequest(
    val sourceId: Int,
    val sourceName: String,
    val path: String,
    val name: String,
)

object PlaybackController {

    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val errors = _errors.asSharedFlow()

    private val _playingChannel = MutableStateFlow<Channel?>(null)
    val playingChannel: StateFlow<Channel?> = _playingChannel.asStateFlow()

    private val _nasRequest = MutableStateFlow<NasPlayRequest?>(null)
    val nasRequest: StateFlow<NasPlayRequest?> = _nasRequest.asStateFlow()

    private val _canTryNext = MutableStateFlow(false)
    val canTryNext: StateFlow<Boolean> = _canTryNext.asStateFlow()

    private var playJob: Job? = null
    private var watchdog: Job? = null
    private var retryJob: Job? = null
    private var sequenceToken = 0L

    private var candidates: List<PlayCandidate> = emptyList()
    private var candidateIndex = 0
    private var reconnectAttempts = 0
    private var reachedReady = false
    private var unsupportedAudioReported = false

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val player: ExoPlayer by lazy {
        val dataSourceFactory = DefaultDataSource.Factory(appContext)
        val sourceFactory = DefaultMediaSourceFactory(appContext).setDataSourceFactory(dataSourceFactory)
        val renderersFactory = DefaultRenderersFactory(appContext)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        ExoPlayer.Builder(appContext)
            .setRenderersFactory(renderersFactory)
            .setMediaSourceFactory(sourceFactory)
            .build()
            .also { exo ->
                exo.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            reachedReady = true
                            watchdog?.cancel()
                            candidates.getOrNull(candidateIndex)?.let { _status.value = it.label }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        onStreamFailed()
                    }

                    override fun onTracksChanged(tracks: Tracks) {
                        reportUnsupportedAudioIfNeeded(tracks)
                    }
                })
            }
    }

    private fun buildLiveCandidates(channel: Channel, info: StreamInfo): List<PlayCandidate> {
        val id = channel.id
        val proxyHls = "/api/stream/$id/playlist.m3u8"
        val ts = "/api/ts/$id/ts"
        val fmp4 = "/api/ts/$id/fmp4"
        val relay = "/api/relay/$id/playlist.m3u8"
        val manifest = "/api/stream/$id/manifest.m3u8"
        val source = channel.url

        fun abs(path: String): String = ApiUrl.absolute(path)
        fun cand(url: String, label: String, reconnectable: Boolean = false) =
            PlayCandidate(url, label, reconnectable)

        val list = mutableListOf<PlayCandidate>()
        when (info.type.lowercase()) {
            "hls" -> {
                if (info.forceProxy) {
                    list += cand(abs(ts), "兼容模式", reconnectable = true)
                    list += cand(abs(fmp4), "兼容模式 fMP4", reconnectable = true)
                    list += cand(abs(relay), "服务器中继")
                    list += cand(abs(proxyHls), "服务器代理")
                } else {
                    list += cand(abs(manifest), "直连")
                    list += cand(abs(proxyHls), "服务器代理")
                }
            }
            "mp4" -> {
                list += cand(source, "直连")
                list += cand(abs(proxyHls), "服务器代理")
            }
            "mpegts" -> {
                list += cand(abs(ts), "TS流", reconnectable = true)
                list += cand(abs(proxyHls), "服务器代理")
            }
            "flv" -> {
                list += cand(abs(proxyHls), "服务器代理")
            }
            "rtmp", "rtsp", "udp" -> {
                list += cand(abs(relay), "服务器中继")
                list += cand(abs(proxyHls), "服务器代理")
            }
            else -> {
                list += cand(abs(proxyHls), "服务器代理")
            }
        }
        list += cand(abs(fmp4), "fMP4", reconnectable = true)
        return list.distinctBy { it.url }
    }

    private fun buildNasCandidates(
        req: NasPlayRequest,
        quality: NasQuality,
        tryDirect: Boolean,
        audioCodec: String?,
    ): List<PlayCandidate> {
        val path = req.path
        val ext = path.substringAfterLast('.', "").lowercase()
        val allowedAudio = listOf("aac", "mp3", "opus", "vorbis", "flac")
        val directCapable = ext in listOf("mp4", "m4v", "mov") && audioCodec != null && audioCodec.lowercase() in allowedAudio

        val direct = NasRepo.fileUrl(req.sourceId, path)
        val proxy = NasRepo.playlistUrl(req.sourceId, path)
        val fallbackVod = NasRepo.vodUrl(req.sourceId, path, 1080)

        return when {
            quality != NasQuality.ORIGINAL -> {
                val h = quality.h ?: 1080
                val vod = NasRepo.vodUrl(req.sourceId, path, h)
                listOfNotNull(vod?.let { PlayCandidate(it, "转码中 (${quality.label})") })
            }
            tryDirect || directCapable -> {
                val c = mutableListOf<PlayCandidate>()
                direct?.let { c += PlayCandidate(it, "原画直连") }
                proxy?.let { c += PlayCandidate(it, "服务器代理") }
                fallbackVod?.let { c += PlayCandidate(it, "兼容转码") }
                c
            }
            else -> {
                listOfNotNull(
                    fallbackVod?.let { PlayCandidate(it, "兼容转码") },
                    proxy?.let { PlayCandidate(it, "服务器代理") },
                )
            }
        }
    }

    fun playLive(channel: Channel) {
        sequenceToken++
        cancelPending()
        _playingChannel.value = channel
        _nasRequest.value = null
        playJob = scope.launch {
            _status.value = "正在探测流类型..."
            FileLogger.i("Player", "playLive channel=${channel.id} ${channel.name}")
            val info = runCatching { ChannelRepo.streamInfo(channel.id).getOrThrow() }
                .getOrNull() ?: StreamInfo(channel.id, "unknown", false)
            candidates = buildLiveCandidates(channel, info)
            FileLogger.i("Player", "streaminfo type=${info.type} forceProxy=${info.forceProxy} candidates=${candidates.map { it.url + "(" + it.label + ")" }}")
            if (candidates.isEmpty()) {
                _status.value = "播放失败：无可用方式"
                _errors.tryEmit("播放失败：无可用方式")
                return@launch
            }
            startCandidate(0)
        }
    }

    fun playNas(req: NasPlayRequest, quality: NasQuality, tryDirect: Boolean, audioCodec: String?) {
        sequenceToken++
        cancelPending()
        _playingChannel.value = null
        _nasRequest.value = req
        playJob = scope.launch {
            _status.value = "准备中..."
            FileLogger.i("Player", "playNas source=${req.sourceId} path=${req.path} quality=${quality.label} tryDirect=$tryDirect audioCodec=$audioCodec")
            candidates = buildNasCandidates(req, quality, tryDirect, audioCodec)
            FileLogger.i("Player", "nas candidates=${candidates.map { it.url + "(" + it.label + ")" }}")
            if (candidates.isEmpty()) {
                _status.value = "播放失败：无可用方式"
                _errors.tryEmit("播放失败：无可用方式")
                return@launch
            }
            startCandidate(0)
        }
    }

    fun stop() {
        sequenceToken++
        cancelPending()
        player.stop()
        player.clearMediaItems()
        _status.value = ""
        _playingChannel.value = null
        _nasRequest.value = null
        _canTryNext.value = false
    }

    fun togglePlay() {
        if (player.isPlaying) player.pause() else if (player.playbackState != Player.STATE_IDLE) player.play()
    }

    private fun cancelPending() {
        playJob?.cancel()
        watchdog?.cancel()
        retryJob?.cancel()
    }

    private fun startCandidate(index: Int) {
        candidateIndex = index
        reconnectAttempts = 0
        reachedReady = false
        _canTryNext.value = candidates.size > index + 1
        reloadCurrent()
    }

    private fun reloadCurrent() {
        val cand = candidates.getOrNull(candidateIndex) ?: return
        FileLogger.d("Player", "load candidate[$candidateIndex] ${cand.url} (${cand.label})")
        unsupportedAudioReported = false
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(cand.url))
        player.prepare()
        player.play()
        startWatchdog()
    }

    private fun startWatchdog() {
        watchdog?.cancel()
        val token = sequenceToken
        watchdog = scope.launch {
            delay(20_000)
            if (sequenceToken != token) return@launch
            if (!reachedReady) onStreamFailed()
        }
    }

    private fun onStreamFailed() {
        watchdog?.cancel()
        val cand = candidates.getOrNull(candidateIndex) ?: return
        FileLogger.w("Player", "stream failed at candidate[$candidateIndex] ${cand.label}")
        if (cand.reconnectable && reachedReady && reconnectAttempts < 3) {
            reconnectAttempts++
            _status.value = "连接中断，正在重连 ($reconnectAttempts/3)..."
            FileLogger.w("Player", "reconnecting attempt $reconnectAttempts/3")
            scheduleReload(1500)
        } else {
            advanceToNext()
        }
    }

    private fun scheduleReload(delayMs: Long) {
        retryJob?.cancel()
        val token = sequenceToken
        retryJob = scope.launch {
            delay(delayMs)
            if (sequenceToken != token) return@launch
            reloadCurrent()
        }
    }

    private fun advanceToNext() {
        reconnectAttempts = 0
        reachedReady = false
        candidateIndex++
        _canTryNext.value = candidates.size > candidateIndex + 1
        if (candidateIndex >= candidates.size) {
            _canTryNext.value = false
            _status.value = "播放失败：所有方式均不可用"
            FileLogger.e("Player", "all candidates exhausted, playback failed")
            _errors.tryEmit("播放失败：所有方式均不可用")
            return
        }
        FileLogger.w("Player", "falling through to candidate[$candidateIndex]")
        reloadCurrent()
    }

    /** Request a reload of current stream (used for NAS quality switch). */
    fun reloadCurrentWith(quality: NasQuality, tryDirect: Boolean, audioCodec: String?) {
        val req = _nasRequest.value ?: return
        playNas(req, quality, tryDirect, audioCodec)
    }

    fun retryCurrent() {
        if (candidates.isEmpty()) return
        sequenceToken++
        cancelPending()
        reconnectAttempts = 0
        reachedReady = false
        _status.value = "正在重试..."
        FileLogger.i("Player", "retry candidate[$candidateIndex]")
        reloadCurrent()
    }

    fun tryNextCandidate(): Boolean {
        if (candidateIndex + 1 >= candidates.size) return false
        sequenceToken++
        cancelPending()
        FileLogger.i("Player", "user requested next candidate")
        advanceToNext()
        return true
    }

    private fun reportUnsupportedAudioIfNeeded(tracks: Tracks) {
        if (unsupportedAudioReported) return
        val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        if (audioGroups.isEmpty()) return
        val hasSupportedAudio = audioGroups.any { group ->
            (0 until group.length).any { index -> group.isTrackSupported(index) }
        }
        if (hasSupportedAudio) return

        unsupportedAudioReported = true
        val codec = audioGroups
            .asSequence()
            .flatMap { group -> (0 until group.length).asSequence().map { group.getTrackFormat(it) } }
            .mapNotNull { it.sampleMimeType ?: it.codecs }
            .firstOrNull()
            ?: "未知音频编码"
        val message = "当前资源的音频编码设备不支持（$codec），需要服务端转码为 AAC"
        _status.value = "音频编码不支持"
        FileLogger.w("Player", message)
        if (candidateIndex + 1 < candidates.size) {
            FileLogger.w("Player", "unsupported audio, falling through to next candidate")
            advanceToNext()
        } else {
            _errors.tryEmit(message)
        }
    }

    private object ApiUrl {
        fun absolute(path: String): String =
            com.iptv.player.data.api.ApiClient.absolute(path) ?: "http://localhost$path"
    }
}
