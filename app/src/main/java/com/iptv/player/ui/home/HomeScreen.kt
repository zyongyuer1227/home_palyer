package com.iptv.player.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iptv.player.data.api.Channel
import com.iptv.player.data.repo.FavRepo
import com.iptv.player.ui.common.EmptyBox
import com.iptv.player.ui.common.ErrorBox
import com.iptv.player.ui.common.LoadingBox
import com.iptv.player.ui.common.rememberToastMessage
import com.iptv.player.ui.epg.EpgDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onPlay: (Channel) -> Unit,
    onOpenGroup: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val toast = rememberToastMessage()
    val controller = remember { HomeController(scope) }
    val state by controller.state.collectAsState()
    val favIds by FavRepo.favoriteIds.collectAsState()

    var epgChannel by remember { mutableStateOf<Channel?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var sourcesExpanded by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    val nearEnd by remember {
        derivedStateOf {
            if (state.grid) {
                val info = gridState.layoutInfo
                val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                last >= info.totalItemsCount - 6
            } else {
                val info = listState.layoutInfo
                val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                last >= info.totalItemsCount - 6
            }
        }
    }
    LaunchedEffect(nearEnd) {
        if (nearEnd) controller.loadMore()
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        FilterChipsRow(
            state = state,
            onSelectFilter = { controller.selectFilter(it) },
            sourcesExpanded = sourcesExpanded,
            onToggleSources = { sourcesExpanded = !sourcesExpanded },
        )

        HeaderRow(
            total = state.total,
            query = state.query,
            grid = state.grid,
            onSearch = { showSearch = true },
            onClearSearch = { controller.setQuery("") },
            onToggleView = { controller.toggleView() },
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.loading -> LoadingBox("加载频道...", Modifier.fillMaxSize())
                state.error != null -> ErrorBox(
                    message = state.error!!,
                    onRetry = { controller.reload() },
                    modifier = Modifier.fillMaxSize(),
                )
                state.channels.isEmpty() && state.query.isNotEmpty() -> EmptyBox("没有找到匹配的频道", Modifier.fillMaxSize())
                state.channels.isEmpty() -> EmptyBox("暂无频道", Modifier.fillMaxSize())
                else -> PullToRefreshBox(
                    isRefreshing = false,
                    onRefresh = { controller.reload() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (state.grid) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            state = gridState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(state.channels.size, key = { state.channels[it].id }) { i ->
                                val ch = state.channels[i]
                                ChannelCard(
                                    channel = ch,
                                    favorite = ch.id in favIds,
                                    onPlay = { onPlay(ch) },
                                    onToggleFavorite = {
                                        scope.launch {
                                            if (FavRepo.toggle(ch)) {
                                                if (state.filter is ChannelFilter.Favorite) controller.reload()
                                            } else {
                                                toast("收藏操作失败")
                                            }
                                        }
                                    },
                                    onEpg = { epgChannel = ch },
                                    onLongPress = {
                                        scope.launch {
                                            if (FavRepo.toggle(ch)) {
                                                if (state.filter is ChannelFilter.Favorite) controller.reload()
                                            } else {
                                                toast("收藏操作失败")
                                            }
                                        }
                                    },
                                )
                            }
                            if (state.loadingMore) {
                                item {
                                    Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
                                    }
                                }
                            }
                        }
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(state.channels.size, key = { state.channels[it].id }) { i ->
                                val ch = state.channels[i]
                                ChannelPanel {
                                    ChannelRow(
                                        channel = ch,
                                        favorite = ch.id in favIds,
                                        onPlay = { onPlay(ch) },
                                        onToggleFavorite = {
                                            scope.launch {
                                                if (FavRepo.toggle(ch)) {
                                                    if (state.filter is ChannelFilter.Favorite) controller.reload()
                                                } else {
                                                    toast("收藏操作失败")
                                                }
                                            }
                                        },
                                        onEpg = { epgChannel = ch },
                                        onLongPress = {
                                            scope.launch {
                                                if (FavRepo.toggle(ch)) {
                                                    if (state.filter is ChannelFilter.Favorite) controller.reload()
                                                } else {
                                                    toast("收藏操作失败")
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                            if (state.loadingMore) {
                                item {
                                    Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
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

    if (showSearch) {
        SearchDialog(
            query = state.query,
            onQueryChange = { controller.setQuery(it) },
            onDismiss = { showSearch = false },
        )
    }

    onOpenGroup.hashCode()
}

@Composable
private fun SearchDialog(query: String, onQueryChange: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("搜索频道") },
        text = {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("输入频道名称") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "清空")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )
}

@Composable
private fun FilterChipsRow(
    state: HomeUiState,
    onSelectFilter: (ChannelFilter) -> Unit,
    sourcesExpanded: Boolean,
    onToggleSources: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item {
            FilterChip(
                selected = state.filter is ChannelFilter.All || state.filter is ChannelFilter.BySource,
                onClick = onToggleSources,
                label = { Text("直播源") },
                colors = filterChipColors(),
                trailingIcon = {
                    Icon(
                        if (sourcesExpanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (sourcesExpanded) "收起直播源" else "展开直播源",
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
        }
        if (sourcesExpanded) {
            item {
                FilterChip(
                    selected = state.filter is ChannelFilter.All,
                    onClick = { onSelectFilter(ChannelFilter.All) },
                    label = { Text("全部直播源") },
                    colors = filterChipColors(),
                )
            }
            items(state.sources.size, key = { state.sources[it].id }) { i ->
                val src = state.sources[i]
                FilterChip(
                    selected = (state.filter as? ChannelFilter.BySource)?.source?.id == src.id,
                    onClick = { onSelectFilter(ChannelFilter.BySource(src)) },
                    label = { Text(src.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = filterChipColors(),
                )
            }
        }
        item {
            FilterChip(
                selected = state.filter is ChannelFilter.Favorite,
                onClick = { onSelectFilter(ChannelFilter.Favorite) },
                label = { Text("收藏") },
                colors = filterChipColors(),
            )
        }
        items(state.customGroups.size, key = { state.customGroups[it].id ?: state.customGroups[it].name }) { i ->
            val group = state.customGroups[i]
            FilterChip(
                selected = (state.filter as? ChannelFilter.ByCustomGroup)?.group?.id == group.id,
                onClick = { onSelectFilter(ChannelFilter.ByCustomGroup(group)) },
                label = {
                    Text(
                        "${group.name}（${group.itemCount}）",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = filterChipColors(),
            )
        }
    }
}

@Composable
private fun filterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    labelColor = MaterialTheme.colorScheme.onSurface,
    selectedContainerColor = MaterialTheme.colorScheme.surface,
    selectedLabelColor = MaterialTheme.colorScheme.primary,
    selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
)

@Composable
private fun HeaderRow(
    total: Int,
    query: String,
    grid: Boolean,
    onSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onToggleView: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "正在播 · $total 个频道",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (query.isNotEmpty()) {
            TextButton(onClick = onClearSearch) {
                Text(
                    "清除搜索",
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        IconButton(onClick = onSearch, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Filled.Search,
                contentDescription = "搜索频道",
                tint = if (query.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onToggleView, modifier = Modifier.size(32.dp)) {
            Icon(
                if (grid) Icons.AutoMirrored.Filled.List else Icons.Filled.GridView,
                contentDescription = "切换视图",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
