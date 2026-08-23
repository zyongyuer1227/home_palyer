package com.iptv.player.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Health(@SerialName("ok") val ok: Boolean = false)

@Serializable
data class Source(
    val id: Int,
    val name: String,
    val url: String? = null,
    val type: String = "",
    @SerialName("channel_count") val channelCount: Int = 0,
    val protocol: String? = null,
    val host: String? = null,
    val port: JsonElement? = null,
    @SerialName("mount_path") val mountPath: String? = null,
) {
    val isNas: Boolean get() = !protocol.isNullOrEmpty()
}

@Serializable
data class Channel(
    val id: Int,
    @SerialName("source_id") val sourceId: Int,
    val name: String,
    val url: String,
    val logo: String? = null,
    val grp: String? = null,
    @SerialName("tvg_id") val tvgId: String? = null,
    @SerialName("tvg_name") val tvgName: String? = null,
    val enabled: Int = 1,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("favorited_at") val favoritedAt: String? = null,
)

@Serializable
data class ChannelPage(
    val total: Int = 0,
    val channels: List<Channel> = emptyList(),
)

@Serializable
data class StreamInfo(
    val id: Int,
    val type: String = "unknown",
    @SerialName("forceProxy") val forceProxy: Boolean = false,
)

@Serializable
data class NasDir(
    val name: String,
    val path: String,
)

@Serializable
data class NasVideo(
    val name: String,
    val path: String,
    val size: Long = 0,
)

@Serializable
data class NasBrowse(
    val dir: String = "",
    val dirs: List<NasDir> = emptyList(),
    val videos: List<NasVideo> = emptyList(),
)

@Serializable
data class NasStreamInfo(
    @SerialName("audioCodec") val audioCodec: String? = null,
    @SerialName("videoCodec") val videoCodec: String? = null,
)

@Serializable
data class EpgProgram(
    @SerialName("tvgId") val tvgId: String? = null,
    val title: String,
    val start: Long = 0,
    val stop: Long = 0,
    val desc: String? = null,
    @SerialName("isNow") val isNow: Boolean = false,
)

@Serializable
data class Epg(
    @SerialName("channelId") val channelId: Int,
    @SerialName("tvgId") val tvgId: String? = null,
    val now: Long = 0,
    val programs: List<EpgProgram> = emptyList(),
)

@Serializable
data class Group(
    val id: Int,
    val name: String,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("item_count") val itemCount: Int = 0,
    val items: List<Channel> = emptyList(),
)

@Serializable
data class GroupItem(
    val id: Int? = null,
    val name: String = "",
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("item_count") val itemCount: Int = 0,
    val items: List<Channel> = emptyList(),
)
