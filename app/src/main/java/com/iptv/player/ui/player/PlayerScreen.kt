package com.iptv.player.ui.player

import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.iptv.player.data.api.Channel
import com.iptv.player.data.api.NasVideo
import com.iptv.player.data.repo.ChannelRepo
import com.iptv.player.data.repo.FavRepo
import com.iptv.player.data.repo.NasRepo
import com.iptv.player.data.repo.RecentRepo
import com.iptv.player.player.NasQuality
import com.iptv.player.player.PlaybackController
import com.iptv.player.ui.common.PlayPayload
import com.iptv.player.ui.common.rememberToastMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private val TopScrim = Brush.verticalGradient(
    0f to Color.Black.copy(alpha = 0.85f),
    1f to Color.Transparent,
)

private val BottomScrim = Brush.verticalGradient(
    0f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.9f),
)

@Composable
fun PlayerScreen(payloadJson: String?, onClose: () -> Unit) {
    val context = LocalContext.current
    val toast = rememberToastMessage()

    val payload = remember(payloadJson) {
        runCatching {
            Json.decodeFromString(PlayPayload.serializer(), payloadJson.orEmpty())
        }.getOrNull()
    }

    var quality by remember { mutableStateOf(NasQuality.ORIGINAL) }
    var tryDirect by remember { mutableStateOf(false) }
    var audioCodec by remember { mutableStateOf<String?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var qualityMenu by remember { mutableStateOf(false) }
    var isLandscapeFullscreen by remember { mutableStateOf(false) }
    var gestureHint by remember { mutableStateOf<String?>(null) }
    var isMuted by remember { mutableStateOf(false) }
    var volumeBeforeMute by remember { mutableStateOf(PlaybackController.player.volume.coerceAtLeast(0.6f)) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var livePlaylist by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var currentNas by remember { mutableStateOf<PlayPayload.Nas?>(null) }
    var episodeList by remember { mutableStateOf<List<NasVideo>>(emptyList()) }
    var episodeDialogVisible by remember { mutableStateOf(false) }
    var autoAdvanceLocked by remember { mutableStateOf(false) }

    val status by PlaybackController.status.collectAsState()
    val playingChannel by PlaybackController.playingChannel.collectAsState()
    val canTryNext by PlaybackController.canTryNext.collectAsState()
    val favIds by FavRepo.favoriteIds.collectAsState()

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    val scope = rememberCoroutineScope()
    val closePlayer = {
        PlaybackController.stop()
        onClose()
    }
    fun playNasEpisode(nas: PlayPayload.Nas) {
        playbackError = null
        controlsVisible = true
        currentNas = nas
        RecentRepo.record(nas)
        scope.launch {
            val nextQuality = NasQuality.fromLabel(nas.quality)
            quality = nextQuality
            tryDirect = nas.tryDirect
            val nextAudioCodec = if (nextQuality == NasQuality.ORIGINAL) {
                runCatching { NasRepo.streamInfo(nas.sourceId, nas.path).getOrThrow().audioCodec }.getOrNull()
            } else {
                null
            }
            audioCodec = nextAudioCodec
            PlaybackController.playNas(
                com.iptv.player.player.NasPlayRequest(nas.sourceId, nas.sourceName, nas.path, nas.name),
                nextQuality,
                nas.tryDirect,
                nextAudioCodec,
            )
        }
    }

    DisposableEffect(isLandscapeFullscreen) {
        val activity = context as? android.app.Activity
        val previous = activity?.requestedOrientation
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (isLandscapeFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            window?.decorView?.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            activity?.requestedOrientation = previous ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Start playback
    LaunchedEffect(payload) {
        when (val p = payload) {
            is PlayPayload.Live -> {
                PlaybackController.playLive(p.channel)
                RecentRepo.record(p)
                livePlaylist = ChannelRepo.channels(sourceId = p.channel.sourceId, limit = 1000)
                    .getOrNull()
                    ?.channels
                    ?.takeIf { it.isNotEmpty() }
                    ?: listOf(p.channel)
            }
            is PlayPayload.Nas -> {
                livePlaylist = emptyList()
                playNasEpisode(p)
            }
            null -> toast("无法解析播放请求")
        }
    }

    LaunchedEffect(currentNas?.sourceId, currentNas?.path) {
        val nas = currentNas ?: run {
            episodeList = emptyList()
            return@LaunchedEffect
        }
        val parentPath = parentDir(nas.path)
        episodeList = NasRepo.browse(nas.sourceId, parentPath)
            .getOrNull()
            ?.videos
            ?.takeIf { it.isNotEmpty() }
            ?: listOf(NasVideo(nas.name, nas.path))
    }

    // Position / state polling
    LaunchedEffect(Unit) {
        while (true) {
            val player = PlaybackController.player
            position = player.currentPosition
            duration = if (player.duration > 0) player.duration else 0L
            playbackState = player.playbackState
            delay(500)
        }
    }

    LaunchedEffect(Unit) {
        PlaybackController.errors.collect { msg ->
            playbackError = msg
            controlsVisible = true
        }
    }

    // Auto-hide controls
    LaunchedEffect(controlsVisible, playbackState) {
        if (controlsVisible && playbackState == Player.STATE_READY) {
            delay(3000)
            controlsVisible = false
        }
    }

    BackHandler {
        if (isLandscapeFullscreen) {
            isLandscapeFullscreen = false
            controlsVisible = true
        } else {
            closePlayer()
        }
    }

    if (payload == null) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("无效的播放请求", color = Color.White)
        }
        return
    }

    val isPlaying = PlaybackController.player.isPlaying
    val currentLiveChannel = (payload as? PlayPayload.Live)?.let { playingChannel ?: it.channel }
    val currentLiveIndex = currentLiveChannel?.let { ch -> livePlaylist.indexOfFirst { it.id == ch.id } } ?: -1
    val canPreviousLive = currentLiveIndex > 0
    val canNextLive = currentLiveIndex >= 0 && currentLiveIndex < livePlaylist.lastIndex
    val currentNasIndex = currentNas?.let { nas -> episodeList.indexOfFirst { it.path == nas.path } } ?: -1
    val canNextNas = currentNasIndex >= 0 && currentNasIndex < episodeList.lastIndex
    fun playLiveAt(index: Int) {
        val channel = livePlaylist.getOrNull(index) ?: return
        playbackError = null
        controlsVisible = true
        RecentRepo.record(PlayPayload.Live(channel))
        PlaybackController.playLive(channel)
    }
    fun playNasAt(index: Int) {
        val base = currentNas ?: return
        val episode = episodeList.getOrNull(index) ?: return
        episodeDialogVisible = false
        playNasEpisode(
            base.copy(
                path = episode.path,
                name = episode.name,
                quality = quality.label,
                tryDirect = tryDirect,
            )
        )
    }

    LaunchedEffect(playbackState, currentNasIndex, episodeList) {
        if (playbackState != Player.STATE_ENDED) {
            autoAdvanceLocked = false
        } else if (!autoAdvanceLocked && canNextNas) {
            autoAdvanceLocked = true
            playNasAt(currentNasIndex + 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures { controlsVisible = !controlsVisible }
            }
            .then(
                if (isLandscapeFullscreen) {
                    Modifier.pointerInput(Unit) {
                        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                        var startBrightness = currentBrightness(context)
                        var startVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                        var adjustBrightness = true
                        var accumulatedDrag = 0f

                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                adjustBrightness = offset.x < size.width / 2f
                                startBrightness = currentBrightness(context)
                                startVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                                accumulatedDrag = 0f
                                controlsVisible = true
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                accumulatedDrag += dragAmount
                                val delta = -accumulatedDrag / size.height
                                if (adjustBrightness) {
                                    val value = (startBrightness + delta).coerceIn(0.05f, 1f)
                                    setWindowBrightness(context, value)
                                    gestureHint = "亮度 ${(value * 100).toInt()}%"
                                } else {
                                    val target = (startVolume + delta * maxVolume).toInt().coerceIn(0, maxVolume)
                                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                                    gestureHint = "音量 ${(target * 100 / maxVolume)}%"
                                }
                            },
                            onDragEnd = {
                                scope.launch {
                                    delay(700)
                                    gestureHint = null
                                }
                            },
                            onDragCancel = { gestureHint = null },
                        )
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    player = PlaybackController.player
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // 缓冲指示
        if (playbackState == Player.STATE_BUFFERING) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(52.dp),
                color = Color.White,
                strokeWidth = 3.5.dp,
            )
        } else if (!isPlaying && playbackState == Player.STATE_READY && controlsVisible) {
            // 暂停态：居中大播放键
            IconButton(
                onClick = { PlaybackController.togglePlay() },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .background(Color.Black.copy(alpha = 0.55f)),
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "播放",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp),
                )
            }
        }

        if (controlsVisible) {
            // 顶部
            Box(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().background(TopScrim)) {
                TopBar(
                    title = when (payload) {
                        is PlayPayload.Live -> currentLiveChannel?.name ?: payload.channel.name
                        is PlayPayload.Nas -> currentNas?.name ?: payload.name
                    },
                    subtitle = when (payload) {
                        is PlayPayload.Live -> listOfNotNull(
                            playingChannel?.grp?.takeIf { it.isNotBlank() },
                        ).joinToString(" · ").ifBlank { "直播" }
                        is PlayPayload.Nas -> "${currentNas?.sourceName ?: payload.sourceName} · NAS"
                    },
                    isFavorite = currentLiveChannel?.id?.let { it in favIds } == true,
                    onFavorite = {
                        currentLiveChannel?.let { ch ->
                            scope.launch { if (!FavRepo.toggle(ch)) toast("收藏操作失败") }
                        }
                    },
                    onClose = closePlayer,
                )
            }

            // 底部
            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(BottomScrim)) {
                BottomBar(
                    status = status,
                    position = position,
                    duration = duration,
                    isPlaying = isPlaying,
                    onTogglePlay = { PlaybackController.togglePlay() },
                    isMuted = isMuted,
                    onToggleMute = {
                        val player = PlaybackController.player
                        if (isMuted) {
                            player.volume = volumeBeforeMute.coerceIn(0.05f, 1f)
                            isMuted = false
                        } else {
                            volumeBeforeMute = player.volume.takeIf { it > 0f } ?: volumeBeforeMute
                            player.volume = 0f
                            isMuted = true
                        }
                        controlsVisible = true
                    },
                    isLandscapeFullscreen = isLandscapeFullscreen,
                    onToggleFullscreen = {
                        isLandscapeFullscreen = !isLandscapeFullscreen
                        controlsVisible = true
                    },
                    isLive = payload is PlayPayload.Live,
                    canPreviousLive = canPreviousLive,
                    canNextLive = canNextLive,
                    onPreviousLive = { playLiveAt(currentLiveIndex - 1) },
                    onNextLive = { playLiveAt(currentLiveIndex + 1) },
                    isNas = payload is PlayPayload.Nas,
                    hasEpisodes = episodeList.size > 1,
                    canNextNas = canNextNas,
                    onShowEpisodes = { episodeDialogVisible = true },
                    onNextNas = { playNasAt(currentNasIndex + 1) },
                    quality = quality,
                    tryDirect = tryDirect,
                    qualityMenu = qualityMenu,
                    onQualityMenuToggle = { qualityMenu = !qualityMenu },
                    onSelectQuality = { q ->
                        qualityMenu = false
                        if (q != quality) {
                            quality = q
                            val nas = currentNas
                            if (nas != null) {
                                if (q == NasQuality.ORIGINAL && audioCodec == null) {
                                    scope.launch {
                                        audioCodec = runCatching {
                                            NasRepo.streamInfo(nas.sourceId, nas.path).getOrThrow().audioCodec
                                        }.getOrNull()
                                    }
                                }
                                PlaybackController.reloadCurrentWith(q, tryDirect, audioCodec)
                            }
                        }
                    },
                    onTryDirectChange = { value ->
                        tryDirect = value
                        if (currentNas != null) {
                            PlaybackController.reloadCurrentWith(quality, value, audioCodec)
                        }
                    },
                    onSeek = { target -> PlaybackController.player.seekTo(target) },
                )
            }
        }

        gestureHint?.let { hint ->
            Text(
                hint,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }

    playbackError?.let { msg ->
        AlertDialog(
            onDismissRequest = { playbackError = null },
            title = { Text("播放失败") },
            text = {
                Text(
                    msg.ifBlank { "当前资源暂时不可播放，可以重试或切换线路。" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    playbackError = null
                    PlaybackController.retryCurrent()
                }) {
                    Text("重试")
                }
            },
            dismissButton = {
                Row {
                    if (canTryNext) {
                        TextButton(onClick = {
                            playbackError = null
                            PlaybackController.tryNextCandidate()
                        }) {
                            Text("换线路")
                        }
                    }
                    TextButton(onClick = {
                        playbackError = null
                        closePlayer()
                    }) {
                        Text("返回列表")
                    }
                }
            },
        )
    }

    if (episodeDialogVisible && currentNas != null) {
        EpisodeDialog(
            episodes = episodeList,
            currentPath = currentNas!!.path,
            onSelect = { index -> playNasAt(index) },
            onDismiss = { episodeDialogVisible = false },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopBar(
    title: String,
    subtitle: String,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = "关闭", tint = Color.White)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(),
            )
            Text(
                subtitle,
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onFavorite) {
            Icon(
                if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = "收藏",
                tint = if (isFavorite) Color(0xFFFFC107) else Color.White,
            )
        }
    }
}

@Composable
private fun BottomBar(
    status: String,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    isLandscapeFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    isLive: Boolean,
    canPreviousLive: Boolean,
    canNextLive: Boolean,
    onPreviousLive: () -> Unit,
    onNextLive: () -> Unit,
    isNas: Boolean,
    hasEpisodes: Boolean,
    canNextNas: Boolean,
    onShowEpisodes: () -> Unit,
    onNextNas: () -> Unit,
    quality: NasQuality,
    tryDirect: Boolean,
    qualityMenu: Boolean,
    onQualityMenuToggle: () -> Unit,
    onSelectQuality: (NasQuality) -> Unit,
    onTryDirectChange: (Boolean) -> Unit,
    onSeek: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        var dragPosition by remember { mutableLongStateOf(-1L) }
        val shownPosition = if (dragPosition >= 0) dragPosition else position
        val max = duration.toFloat().coerceAtLeast(1f)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (duration > 0) formatDuration(shownPosition) else "直播",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                if (duration > 0) formatDuration(duration) else status,
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 10.dp),
            )
        }
        if (duration > 0) {
            Slider(
                value = shownPosition.toFloat().coerceIn(0f, max),
                onValueChange = { v -> dragPosition = v.toLong() },
                onValueChangeFinished = {
                    if (dragPosition >= 0) onSeek(dragPosition.coerceAtMost(duration))
                    dragPosition = -1L
                },
                valueRange = 0f..max,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                ),
                modifier = Modifier.fillMaxWidth().height(32.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.28f)),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onTogglePlay) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = Color.White,
                )
            }
            IconButton(onClick = onToggleMute) {
                Icon(
                    if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isMuted) "取消静音" else "静音",
                    tint = Color.White,
                )
            }
            IconButton(onClick = onToggleFullscreen) {
                Icon(
                    if (isLandscapeFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                    contentDescription = if (isLandscapeFullscreen) "退出全屏" else "全屏",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
            if (isLive) {
                IconButton(onClick = onPreviousLive, enabled = canPreviousLive) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "上一台",
                        tint = if (canPreviousLive) Color.White else Color.White.copy(alpha = 0.35f),
                    )
                }
                IconButton(onClick = onNextLive, enabled = canNextLive) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "下一台",
                        tint = if (canNextLive) Color.White else Color.White.copy(alpha = 0.35f),
                    )
                }
            }
            if (isNas) {
                IconButton(onClick = onShowEpisodes, enabled = hasEpisodes) {
                    Icon(
                        Icons.AutoMirrored.Filled.FormatListBulleted,
                        contentDescription = "剧集清单",
                        tint = if (hasEpisodes) Color.White else Color.White.copy(alpha = 0.35f),
                    )
                }
                IconButton(onClick = onNextNas, enabled = canNextNas) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "下一集",
                        tint = if (canNextNas) Color.White else Color.White.copy(alpha = 0.35f),
                    )
                }
                Box(modifier = Modifier.weight(1f))
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onQualityMenuToggle() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            quality.label,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                        )
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            contentDescription = "画质",
                            tint = Color.White,
                        )
                    }
                    DropdownMenu(expanded = qualityMenu, onDismissRequest = { onQualityMenuToggle() }) {
                        NasQuality.entries.forEach { q ->
                            DropdownMenuItem(
                                text = { Text(q.label) },
                                onClick = { onSelectQuality(q) },
                            )
                        }
                        if (quality == NasQuality.ORIGINAL) {
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("尝试直连", modifier = Modifier.weight(1f))
                                Switch(checked = tryDirect, onCheckedChange = onTryDirectChange)
                            }
                        }
                    }
                }
            } else {
                Text(
                    status,
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
            }
        }
        if (status.isNotBlank() && isNas) {
            Text(
                status,
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp, top = 2.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EpisodeDialog(
    episodes: List<NasVideo>,
    currentPath: String,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("剧集清单") },
        text = {
            LazyColumn {
                items(episodes.size) { index ->
                    val episode = episodes[index]
                    val selected = episode.path == currentPath
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent,
                            )
                            .clickable { onSelect(index) }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(
                                episode.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.basicMarquee(),
                            )
                            if (selected) {
                                Text(
                                    "正在播放",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "--:--"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%02d:%02d", m, s)
}

private fun parentDir(path: String): String? {
    val clean = path.trimEnd('/')
    val index = clean.lastIndexOf('/')
    return when {
        index < 0 -> null
        index == 0 -> "/"
        else -> clean.substring(0, index)
    }
}

private fun currentBrightness(context: android.content.Context): Float {
    val activity = context as? android.app.Activity
    val current = activity?.window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    return if (current >= 0f) current else 0.5f
}

private fun setWindowBrightness(context: android.content.Context, value: Float) {
    val activity = context as? android.app.Activity ?: return
    val params = activity.window.attributes
    params.screenBrightness = value.coerceIn(0.05f, 1f)
    activity.window.attributes = params
}
