package com.iptv.player.ui.fav

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.iptv.player.data.api.GroupItemsBody
import com.iptv.player.data.api.GroupItem
import com.iptv.player.data.api.GroupNameBody
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
    var creatingGroup by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<GroupItem?>(null) }
    var deletingGroup by remember { mutableStateOf<GroupItem?>(null) }
    var confirmClearFavorites by remember { mutableStateOf(false) }
    var confirmClearRecent by remember { mutableStateOf(false) }

    fun reloadGroups() {
        scope.launch {
            groupsError = null
            try {
                groups = ApiClient.service.groups().bodyOrThrow()
            } catch (e: Exception) {
                groupsError = "加载分组失败：${e.message}"
            }
        }
    }

    LaunchedEffect(tab) {
        if (tab == 1 && groups == null) reloadGroups()
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
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${favorites.size} 个收藏频道",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { confirmClearFavorites = true }) {
                                Text("清空收藏")
                            }
                        }
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
            }
            1 -> {
                when {
                    groups == null && groupsError == null -> LoadingBox("加载分组...", Modifier.weight(1f))
                    groupsError != null -> com.iptv.player.ui.common.ErrorBox(
                        message = groupsError!!,
                        onRetry = { reloadGroups() },
                        modifier = Modifier.weight(1f),
                    )
                    else -> LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            Button(
                                onClick = { creatingGroup = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp).size(18.dp))
                                Text("新增分组")
                            }
                        }
                        if (groups.isNullOrEmpty()) {
                            item { EmptyBox("暂无自定义分组", Modifier.fillMaxWidth()) }
                        } else {
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
                                        IconButton(onClick = { editingGroup = g }) {
                                            Icon(
                                                Icons.Filled.Edit,
                                                contentDescription = "编辑分组频道",
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                        IconButton(onClick = { deletingGroup = g }) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "删除分组",
                                                tint = MaterialTheme.colorScheme.error,
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
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${recentItems.size} 条最近播放",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { confirmClearRecent = true }) {
                                    Text("清空最近")
                                }
                            }
                        }
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

    if (creatingGroup) {
        CreateGroupDialog(
            onDismiss = { creatingGroup = false },
            onCreate = { name ->
                scope.launch {
                    val response = runCatching { ApiClient.service.createGroup(GroupNameBody(name)) }.getOrNull()
                    if (response?.isSuccessful == true) {
                        creatingGroup = false
                        toast("分组已创建")
                        reloadGroups()
                    } else {
                        toast("创建分组失败")
                    }
                }
            },
        )
    }

    editingGroup?.let { group ->
        GroupEditorDialog(
            group = group,
            onDismiss = { editingGroup = null },
            onSaved = {
                editingGroup = null
                reloadGroups()
                toast("分组已保存")
            },
            onError = { toast(it) },
        )
    }

    deletingGroup?.let { group ->
        ConfirmDialog(
            title = "删除分组",
            text = "确定删除“${group.name}”吗？分组内频道不会被删除。",
            confirmText = "删除",
            onDismiss = { deletingGroup = null },
            onConfirm = {
                val id = group.id
                deletingGroup = null
                if (id == null) {
                    toast("删除分组失败")
                    return@ConfirmDialog
                }
                scope.launch {
                    val ok = runCatching { ApiClient.service.deleteGroup(id) }.getOrNull()?.isSuccessful == true
                    if (ok) {
                        toast("分组已删除")
                        reloadGroups()
                    } else {
                        toast("删除分组失败")
                    }
                }
            },
        )
    }

    if (confirmClearFavorites) {
        ConfirmDialog(
            title = "清空收藏",
            text = "确定取消所有收藏频道吗？",
            confirmText = "清空",
            onDismiss = { confirmClearFavorites = false },
            onConfirm = {
                confirmClearFavorites = false
                scope.launch {
                    if (FavRepo.clearAll()) toast("收藏已清空") else toast("清空收藏失败")
                }
            },
        )
    }

    if (confirmClearRecent) {
        ConfirmDialog(
            title = "清空最近播放",
            text = "确定清空最近播放记录吗？",
            confirmText = "清空",
            onDismiss = { confirmClearRecent = false },
            onConfirm = {
                confirmClearRecent = false
                RecentRepo.clear()
                toast("最近播放已清空")
            },
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增分组") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("分组名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onCreate(name.trim()) },
            ) { Text("创建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun GroupEditorDialog(
    group: GroupItem,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onError: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var channels by remember(group.id) { mutableStateOf<List<Channel>?>(null) }
    var selectedIds by remember(group.id) { mutableStateOf(group.items.map { it.id }.toSet()) }
    var query by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(group.id) {
        channels = runCatching {
            ApiClient.service.channels(limit = 5000, offset = 0).bodyOrThrow().channels
        }.getOrElse {
            onError("加载频道失败")
            emptyList()
        }
    }

    val filtered = channels.orEmpty().filter { ch ->
        query.isBlank() ||
            ch.name.contains(query, ignoreCase = true) ||
            ch.grp.orEmpty().contains(query, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "编辑：${group.name}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("搜索频道") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (channels == null) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                    }
                } else {
                    Text(
                        "已选择 ${selectedIds.size} 个频道",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyColumn {
                        items(filtered, key = { it.id }) { ch ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedIds = if (ch.id in selectedIds) selectedIds - ch.id else selectedIds + ch.id
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = ch.id in selectedIds,
                                    onCheckedChange = { checked ->
                                        selectedIds = if (checked) selectedIds + ch.id else selectedIds - ch.id
                                    },
                                )
                                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                    Text(ch.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        ch.grp?.takeIf { it.isNotBlank() } ?: "直播",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && channels != null && group.id != null,
                onClick = {
                    val id = group.id ?: return@TextButton
                    saving = true
                    scope.launch {
                        val ok = runCatching {
                            ApiClient.service.updateGroupItems(id, GroupItemsBody(selectedIds.toList()))
                        }.getOrNull()?.isSuccessful == true
                        saving = false
                        if (ok) onSaved() else onError("保存分组失败")
                    }
                },
            ) { Text(if (saving) "保存中" else "保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
