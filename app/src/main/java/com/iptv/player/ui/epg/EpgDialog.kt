package com.iptv.player.ui.epg

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iptv.player.data.api.Channel
import com.iptv.player.data.api.Epg
import com.iptv.player.data.api.EpgProgram
import com.iptv.player.data.repo.ChannelRepo
import com.iptv.player.ui.common.formatDateTime
import com.iptv.player.ui.common.LoadingBox
import com.iptv.player.ui.common.formatTime

@Composable
fun EpgDialog(channel: Channel, onDismiss: () -> Unit) {
    var epg by remember { mutableStateOf<Epg?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(channel.id) {
        epg = null
        error = null
        val r = runCatching { ChannelRepo.epg(channel.id).getOrThrow() }
        r.onSuccess { epg = it }.onFailure { error = "获取节目单失败：${it.message}" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(channel.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            when {
                epg == null && error == null -> LoadingBox("加载节目单...", Modifier)
                error != null -> Text(error ?: "")
                else -> {
                    val e = epg!!
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "今天 ${formatDateTime(e.now)} 的节目",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        if (e.programs.isEmpty()) {
                            Text("暂无节目信息", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            LazyColumn(modifier = Modifier.height(320.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(e.programs, key = { "${it.start}-${it.title}" }) { p ->
                                    EpgRow(p)
                                }
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

@Composable
private fun EpgRow(p: EpgProgram) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (p.isNow) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "${formatTime(p.start)}-${formatTime(p.stop)}",
            style = MaterialTheme.typography.bodySmall,
            color = if (p.isNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                if (p.isNow) {
                    Text(
                        "直播中",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
                Text(
                    p.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (p.isNow) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            p.desc?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
