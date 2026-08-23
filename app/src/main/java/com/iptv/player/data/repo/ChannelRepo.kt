package com.iptv.player.data.repo

import com.iptv.player.data.api.ApiClient
import com.iptv.player.data.api.Channel
import com.iptv.player.data.api.ChannelPage
import com.iptv.player.data.api.Epg
import com.iptv.player.data.api.Source
import com.iptv.player.data.api.StreamInfo
import com.iptv.player.data.api.bodyOrThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ChannelRepo {

    suspend fun sources(): Result<List<Source>> = withContext(Dispatchers.IO) {
        runCatching { ApiClient.service.sources().bodyOrThrow() }
    }

    suspend fun channels(
        grp: String? = null,
        q: String? = null,
        favorite: Boolean = false,
        sourceId: Int? = null,
        limit: Int = 200,
        offset: Int = 0,
    ): Result<ChannelPage> = withContext(Dispatchers.IO) {
        runCatching {
            ApiClient.service.channels(
                grp = grp,
                q = q?.takeIf { it.isNotBlank() },
                favorite = if (favorite) 1 else null,
                sourceId = sourceId,
                limit = limit,
                offset = offset,
            ).bodyOrThrow()
        }
    }

    suspend fun streamInfo(id: Int): Result<StreamInfo> = withContext(Dispatchers.IO) {
        runCatching { ApiClient.service.streamInfo(id).bodyOrThrow() }
    }

    suspend fun epg(channelId: Int): Result<Epg> = withContext(Dispatchers.IO) {
        runCatching { ApiClient.service.epg(channelId).bodyOrThrow() }
    }
}
