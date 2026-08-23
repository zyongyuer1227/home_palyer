package com.iptv.player.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.iptv.player.IptvApplication
import com.iptv.player.data.api.ApiClient
import com.iptv.player.data.api.Channel
import com.iptv.player.data.api.GroupItem
import com.iptv.player.data.api.ServerConfig
import com.iptv.player.data.api.bodyOrThrow
import com.iptv.player.data.local.FileLogger
import com.iptv.player.data.repo.FavRepo
import com.iptv.player.data.repo.ServerRepo
import com.iptv.player.player.PlaybackController
import com.iptv.player.ui.common.EmptyBox
import com.iptv.player.ui.common.ErrorBox
import com.iptv.player.ui.common.LoadingBox
import com.iptv.player.ui.common.PlayPayload
import com.iptv.player.ui.common.rememberToastMessage
import com.iptv.player.ui.fav.FavoritesScreen
import com.iptv.player.ui.home.ChannelListView
import com.iptv.player.ui.home.HomeScreen
import com.iptv.player.ui.nas.NasScreen
import com.iptv.player.ui.player.PlayerScreen
import com.iptv.player.ui.server.ServerScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private const val ROUTE_MAIN = "main"
private const val ROUTE_SERVER = "server"
private const val ROUTE_PLAYER = "player"
private const val ROUTE_GROUP = "group"

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as IptvApplication
    val serverRepo = app.serverRepo
    val state by serverRepo.state.collectAsState()

    LaunchedEffect(Unit) {
        PlaybackController.errors.collect { msg ->
            FileLogger.e("App", "playback error: $msg")
        }
    }
    LaunchedEffect(Unit) {
        runCatching { FavRepo.refresh() }
    }
    LaunchedEffect(state.active?.id) {
        if (state.active == null && navController.currentDestination?.route != ROUTE_SERVER) {
            navController.navigate(ROUTE_SERVER) { launchSingleTop = true }
        } else {
            state.active?.let { active ->
                FileLogger.i("App", "active server: ${active.name} @ ${active.address}")
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = ROUTE_MAIN,
    ) {
        composable(ROUTE_SERVER) {
            ServerScreen(onDone = { navController.popBackStack() })
        }
        composable(ROUTE_MAIN) {
            MainScaffold(
                serverRepo = serverRepo,
                onOpenServer = { navController.navigate(ROUTE_SERVER) { launchSingleTop = true } },
                onOpenPlayer = { payload -> navController.navigate("$ROUTE_PLAYER/${java.net.URLEncoder.encode(payload, "UTF-8")}") },
                onOpenGroup = { id -> navController.navigate("$ROUTE_GROUP/$id") },
            )
        }
        composable("$ROUTE_PLAYER/{payload}") { entry ->
            val payload = entry.arguments?.getString("payload")
            PlayerScreen(payloadJson = payload, onClose = { navController.popBackStack() })
        }
        composable("$ROUTE_GROUP/{id}") { entry ->
            val id = entry.arguments?.getString("id")?.toIntOrNull() ?: return@composable
            GroupScreen(groupId = id, onPlay = { ch ->
                navController.navigate("$ROUTE_PLAYER/${java.net.URLEncoder.encode(PlayPayload.Live(ch).toJson(), "UTF-8")}")
            }, onBack = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    serverRepo: ServerRepo,
    onOpenServer: () -> Unit,
    onOpenPlayer: (String) -> Unit,
    onOpenGroup: (Int) -> Unit,
) {
    var tabIndex by rememberSaveable { mutableIntStateOf(MainTab.LIVE.ordinal) }
    val tab = MainTab.entries.getOrElse(tabIndex) { MainTab.LIVE }
    val state by serverRepo.state.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    ServerSwitcher(
                        onSelect = { id -> scope.launch { serverRepo.select(id) } },
                    )
                    IconButton(onClick = onOpenServer) {
                        Icon(Icons.Filled.Settings, contentDescription = "服务器设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 3.dp,
            ) {
                MainTab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tabIndex = t.ordinal },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        when (tab) {
            MainTab.LIVE -> HomeScreen(
                modifier = Modifier.padding(innerPadding),
                onPlay = { ch -> onOpenPlayer(PlayPayload.Live(ch).toJson()) },
            )
            MainTab.NAS -> NasScreen(
                modifier = Modifier.padding(innerPadding),
                onPlay = { nas -> onOpenPlayer(nas.toJson()) },
            )
            MainTab.MINE -> FavoritesScreen(
                modifier = Modifier.padding(innerPadding),
                onPlay = { ch -> onOpenPlayer(PlayPayload.Live(ch).toJson()) },
                onOpenRecent = onOpenPlayer,
                onOpenGroup = onOpenGroup,
            )
        }
    }
}

private enum class MainTab(val label: String, val icon: ImageVector) {
    LIVE("直播", Icons.Filled.LiveTv),
    NAS("点播", Icons.Filled.VideoLibrary),
    MINE("我的", Icons.Filled.Favorite),
}

@Composable
private fun ServerSwitcher(onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val state by (LocalContext.current.applicationContext as IptvApplication).serverRepo.state.collectAsState()
    val activeIndex = state.servers.indexOfFirst { it.id == state.active?.id }.takeIf { it >= 0 }
    val activeLabel = state.active?.let { serverDisplayName(it, activeIndex) } ?: "未配置"

    Box {
        TextButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier.widthIn(max = 128.dp),
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .padding(end = 7.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (state.active == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary),
                )
                Text(
                    activeLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = "切换服务器",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        ) {
            state.servers.forEachIndexed { index, s ->
                DropdownMenuItem(
                    text = {
                        Text(
                            serverDisplayName(s, index),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onClick = {
                        expanded = false
                        if (s.id != state.activeId) onSelect(s.id)
                    },
                )
            }
        }
    }
}

private fun serverDisplayName(server: ServerConfig, index: Int?): String {
    val cleanName = server.name.trim()
    val cleanAddress = server.address.trim()
    val looksLikeAddress = cleanName.isBlank() ||
        cleanName.equals(cleanAddress, ignoreCase = true) ||
        cleanName.startsWith("http://", ignoreCase = true) ||
        cleanName.startsWith("https://", ignoreCase = true) ||
        cleanName.matches(Regex("""\d{1,3}(\.\d{1,3}){3}(:\d+)?/?"""))

    if (!looksLikeAddress) return cleanName
    return index?.let { "服务器 ${it + 1}" } ?: "当前服务器"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(groupId: Int, onPlay: (Channel) -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val toast = rememberToastMessage()
    var data by remember { mutableStateOf<GroupItem?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(groupId) {
        loading = true
        val r = runCatching {
            val list = ApiClient.service.groups().bodyOrThrow()
            list.firstOrNull { it.id == groupId }
        }
        loading = false
        r.onSuccess { data = it }.onFailure { error = "加载分组失败：${it.message}" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(data?.name ?: "分组") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        val favIds by FavRepo.favoriteIds.collectAsState()
        when {
            loading -> LoadingBox(modifier = Modifier.padding(padding))
            error != null -> ErrorBox(message = error!!, modifier = Modifier.padding(padding))
            data?.items.isNullOrEmpty() -> EmptyBox("分组内暂无频道", Modifier.padding(padding))
            else -> ChannelListView(
                channels = data!!.items,
                onPlay = onPlay,
                favoriteIds = favIds,
                onToggleFavorite = { ch -> scope.launch { if (!FavRepo.toggle(ch)) toast("收藏操作失败") } },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

fun PlayPayload.toJson(): String = Json.encodeToString(PlayPayload.serializer(), this)
