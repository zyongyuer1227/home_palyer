package com.iptv.player.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.iptv.player.data.api.ApiClient
import com.iptv.player.data.api.Channel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelCard(
    channel: Channel,
    favorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEpg: () -> Unit,
    onLongPress: () -> Unit = onToggleFavorite,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.combinedClickable(onClick = onPlay, onLongClick = onLongPress),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center,
            ) {
                val logo = ApiClient.resolve(channel.logo)
                if (logo != null) {
                    AsyncImage(
                        model = logo,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        channel.name.take(2),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                channel.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 10.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    channel.grp?.takeIf { it.isNotBlank() } ?: "直播",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                SmallActionButton(
                    icon = if (favorite) Icons.Filled.Star else Icons.Outlined.Star,
                    tint = if (favorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onToggleFavorite,
                    contentDescription = "收藏",
                )
                SmallActionButton(
                    icon = Icons.Filled.Sensors,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onEpg,
                    contentDescription = "节目单",
                )
            }
        }
    }
}

@Composable
private fun SmallActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
    contentDescription: String,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(34.dp).clip(CircleShape),
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelRow(
    channel: Channel,
    favorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEpg: () -> Unit,
    onLongPress: () -> Unit = onToggleFavorite,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onPlay, onLongClick = onLongPress)
            .padding(horizontal = 0.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val logo = ApiClient.resolve(channel.logo)
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
            contentAlignment = Alignment.Center,
        ) {
            if (logo != null) {
                AsyncImage(
                    model = logo,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    channel.name.take(1),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
        ) {
            Text(
                channel.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            channel.grp?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
            modifier = Modifier.size(34.dp),
        ) {
            IconButton(onClick = onPlay) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "播放",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(34.dp)) {
            Icon(
                if (favorite) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = "收藏",
                tint = if (favorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onEpg, modifier = Modifier.size(34.dp)) {
            Icon(
                Icons.Filled.Sensors,
                contentDescription = "节目单",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun ChannelPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 1.dp)) {
            content()
        }
    }
}

@Composable
fun ChannelListView(
    channels: List<Channel>,
    favoriteIds: Set<Int>,
    onPlay: (Channel) -> Unit,
    modifier: Modifier = Modifier,
    onToggleFavorite: ((Channel) -> Unit)? = null,
    onEpg: ((Channel) -> Unit)? = null,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(channels, key = { it.id }) { ch ->
            ChannelPanel {
                ChannelRow(
                    channel = ch,
                    favorite = ch.id in favoriteIds,
                    onPlay = { onPlay(ch) },
                    onToggleFavorite = { onToggleFavorite?.invoke(ch) },
                    onEpg = { onEpg?.invoke(ch) },
                )
            }
        }
    }
}
