package com.iptv.player.ui.nas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iptv.player.data.api.ApiClient
import com.iptv.player.data.api.NasDir
import com.iptv.player.data.api.NasVideo
import com.iptv.player.data.api.Source
import com.iptv.player.data.api.bodyOrThrow
import com.iptv.player.data.repo.NasRepo
import com.iptv.player.player.NasQuality
import com.iptv.player.ui.common.EmptyBox
import com.iptv.player.ui.common.ErrorBox
import com.iptv.player.ui.common.LoadingBox
import com.iptv.player.ui.common.PlayPayload
import com.iptv.player.ui.common.formatSize

@Composable
fun NasScreen(modifier: Modifier = Modifier, onPlay: (PlayPayload.Nas) -> Unit) {
    var sources by remember { mutableStateOf<List<Source>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Source?>(null) }

    LaunchedEffect(Unit) {
        val r = runCatching { ApiClient.service.sources().bodyOrThrow() }
        r.onSuccess { list -> sources = list.filter { it.isNas } }
            .onFailure { error = "加载源失败：${it.message}" }
    }

    when {
        sources == null && error == null -> LoadingBox("加载源...", modifier)
        error != null -> ErrorBox(error!!, onRetry = { error = null; sources = null }, modifier = modifier)
        sources.isNullOrEmpty() -> EmptyBox("没有可用的 NAS 网盘源", modifier)
        selected == null -> SourcePicker(sources!!, onSelect = { selected = it }, modifier = modifier)
        else -> NasBrowser(
            source = selected!!,
            onBackToSources = { selected = null },
            onPlay = onPlay,
            modifier = modifier,
        )
    }
}

@Composable
private fun SourcePicker(sources: List<Source>, onSelect: (Source) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        item {
            Text(
                "选择网盘源",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                sources.forEachIndexed { index, src ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(src) }
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
                            Text(src.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${src.channelCount} 个文件",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (index != sources.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun NasBrowser(
    source: Source,
    onBackToSources: () -> Unit,
    onPlay: (PlayPayload.Nas) -> Unit,
    modifier: Modifier = Modifier,
) {
    var stack by remember { mutableStateOf<List<NasDir>>(emptyList()) }
    var dirs by remember { mutableStateOf<List<NasDir>>(emptyList()) }
    var videos by remember { mutableStateOf<List<NasVideo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingVideo by remember { mutableStateOf<NasVideo?>(null) }

    val currentPath = stack.lastOrNull()?.path
    fun goBack() {
        if (stack.isNotEmpty()) {
            stack = stack.dropLast(1)
        } else {
            onBackToSources()
        }
    }

    BackHandler { goBack() }

    LaunchedEffect(currentPath) {
        loading = true
        error = null
        val r = runCatching { NasRepo.browse(source.id, currentPath).getOrThrow() }
        loading = false
        r.onSuccess { b ->
            dirs = b.dirs
            videos = b.videos
        }.onFailure { error = "打开目录失败：${it.message}" }
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { goBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            BreadcrumbItem("根目录", onClick = { stack = emptyList() }, last = stack.isEmpty())
            stack.forEachIndexed { index, dir ->
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                BreadcrumbItem(dir.name, onClick = { stack = stack.take(index + 1) }, last = index == stack.lastIndex)
            }
        }

        when {
            loading -> LoadingBox("加载目录...", Modifier.weight(1f))
            error != null -> ErrorBox(message = error!!, modifier = Modifier.weight(1f))
            dirs.isEmpty() && videos.isEmpty() -> EmptyBox("空目录", Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        dirs.forEachIndexed { index, dir ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { stack = stack + dir }
                                    .padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    dir.name,
                                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (index != dirs.lastIndex || videos.isNotEmpty()) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        videos.forEachIndexed { index, video ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { pendingVideo = video }
                                    .padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                    Text(
                                        video.name,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        formatSize(video.size),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (index != videos.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }

    pendingVideo?.let { video ->
        QualityDialog(
            fileName = video.name,
            onConfirm = { quality, tryDirect ->
                pendingVideo = null
                onPlay(
                    PlayPayload.Nas(
                        sourceId = source.id,
                        sourceName = source.name,
                        path = video.path,
                        name = video.name,
                        quality = quality.label,
                        tryDirect = tryDirect,
                    )
                )
            },
            onDismiss = { pendingVideo = null },
        )
    }
}

@Composable
private fun BreadcrumbItem(name: String, onClick: () -> Unit, last: Boolean) {
    Text(
        name,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (last) FontWeight.SemiBold else FontWeight.Normal,
        color = if (last) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 2.dp),
    )
}

@Composable
private fun QualityDialog(
    fileName: String,
    onConfirm: (NasQuality, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var quality by remember { mutableStateOf(NasQuality.ORIGINAL) }
    var tryDirect by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(fileName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                NasQuality.entries.forEach { q ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { quality = q },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = quality == q, onClick = { quality = q })
                        Text(q.label, modifier = Modifier.padding(start = 8.dp))
                        if (q == NasQuality.ORIGINAL) {
                            Text(
                                "（不转码）",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (quality == NasQuality.ORIGINAL) {
                    androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("尝试直连", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "AC3/DTS 等设备可硬解的音频将尝试直接播放，失败自动回退代理",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = tryDirect, onCheckedChange = { tryDirect = it })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(quality, tryDirect) }) { Text("播放") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
