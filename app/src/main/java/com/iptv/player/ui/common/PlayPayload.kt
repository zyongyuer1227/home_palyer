package com.iptv.player.ui.common

import com.iptv.player.data.api.Channel
import kotlinx.serialization.Serializable

@Serializable
sealed interface PlayPayload {
    @Serializable
    data class Live(val channel: Channel) : PlayPayload

    @Serializable
    data class Nas(
        val sourceId: Int,
        val sourceName: String,
        val path: String,
        val name: String,
        val quality: String = "原画",
        val tryDirect: Boolean = true,
    ) : PlayPayload
}
