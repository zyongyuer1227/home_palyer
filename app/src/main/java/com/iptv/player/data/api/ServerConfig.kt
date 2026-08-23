package com.iptv.player.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class ServerConfig(
    val id: String,
    val name: String,
    val address: String,
)
