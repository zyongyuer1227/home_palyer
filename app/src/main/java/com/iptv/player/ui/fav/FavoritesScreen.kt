package com.iptv.player.ui.fav

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iptv.player.data.api.ApiClient
import com.iptv.player.data.api.Channel
import com.iptv.player.data.api.GroupItem
import com.iptv.player.data.api.bodyOrThrow
import com.iptv.player.data.repo.FavRepo
import com.iptv.player.data.repo.RecentRepo
import com.iptv.player.ui.common.EmptyBox
import com.iptv.player.ui.common.LoadingBox
import com.iptv.player.ui.common.rememberToastMessage
import com.iptv.player.ui.home.ChannelListView
import com.iptv.player.ui.epg.EpgDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    onPlay: (Channel) -> Unit,
    onOpenRecent: (String) -> Unit,
    onOpenGroup: (Int) -> Unit = {},
) {
    var tab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val toast = rememberToastMessage()
    val favorites by FavRepo.favorites.collectAsState()
    val favIds by FavRepo.favoriteIds.collectAsState()
    val recentItems by RecentRepo.items.collectAsState()

    var groups by remember { mutableStateOf<List<GroupItem>?>(null) }
    var groupsError by remember { mutableStateOf<String?>(null) }
    var epgChannel by remember { mutableStateOf<Channel?>(null) }

    LaunchedEffect(tab) {
        if (tab == 1 && groups == null) {
            try {
                groups = ApiClient.service.groups().bodyOrThrow()
            } catch (e: Exception) {
                groupsError = "加载分组失败：${e.message}"
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TabRow(
            selectedTabIndex = tab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { positions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(positions[tab]),
                    color = MaterialTheme.colorScheme.primary,
                )
            },
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("收藏") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("分组") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("最近") })
        }

        when (tab) {
            0 -> {
                if (favorites.isEmpty()) {
                    EmptyBox("暂无收藏，长按频道可添加收藏", Modifier.weight(1f))
                } else {
                    ChannelListView(
                        channels = favorites,
                        favoriteIds = favIds,
                        onPlay = onPlay,
                        onToggleFavorite = { ch -> scope.launch { if (!FavRepo.toggle(ch)) toast("收藏操作失败") } },
                        onEpg = { epgChannel = it },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            1 -> {
                when {
                    groups == null && groupsError == null -> LoadingBox("加载分组...", Modifier.weight(1f))
                    groupsError != null -> com.iptv.player.ui.common.ErrorBox(
                        message = groupsError!!,
                        modifier = Modifier.weight(1f),
                    )
                    groups.isNullOrEmpty() -> EmptyBox("暂无自定义分组", Modifier.weight(1f))
                    else -> LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            ) {
                                groups!!.forEachIndexed { index, g ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenGroup(g.id ?: return@clickable) }
                                            .padding(horizontal = 14.dp, vertical = 13.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.padding(end = 12.dp),
                                        ) {
                                            Icon(
                                                Icons.Filled.Folder,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(9.dp),
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                g.name,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                "${g.itemCount} 个频道",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    if (index != groups!!.lastIndex) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                if (recentItems.isEmpty()) {
                    EmptyBox("暂无最近播放", Modifier.weight(1f))
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            ) {
                                recentItems.forEachIndexed { index, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenRecent(item.payloadJson) }
                                            .padding(horizontal = 14.dp, vertical = 13.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.padding(end = 12.dp),
                                        ) {
                                            Icon(
                                                if (item.id.startsWith("live:")) Icons.Filled.PlayCircle else Icons.Filled.History,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(9.dp),
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                item.title,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                item.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                    if (index != recentItems.lastIndex) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    epgChannel?.let { EpgDialog(channel = it, onDismiss = { epgChannel = null }) }
}
