package com.iptv.player.data.repo

import com.iptv.player.data.api.ApiClient
import com.iptv.player.data.api.NasBrowse
import com.iptv.player.data.api.NasStreamInfo
import com.iptv.player.data.api.bodyOrThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NasRepo {

    suspend fun browse(sourceId: Int, dir: String?): Result<NasBrowse> = withContext(Dispatchers.IO) {
        runCatching { ApiClient.service.nasBrowse(sourceId, dir).bodyOrThrow() }
    }

    suspend fun streamInfo(sourceId: Int, path: String): Result<NasStreamInfo> = withContext(Dispatchers.IO) {
        runCatching { ApiClient.service.nasStreamInfo(sourceId, path).bodyOrThrow() }
    }

    fun fileUrl(sourceId: Int, path: String): String? =
        ApiClient.absolute("/api/nas/$sourceId/file")?.let { base -> "$base?path=${java.net.URLEncoder.encode(path, "UTF-8")}" }

    fun playlistUrl(sourceId: Int, path: String): String? =
        ApiClient.absolute("/api/nas/$sourceId/playlist.m3u8")?.let { base -> "$base?path=${java.net.URLEncoder.encode(path, "UTF-8")}" }

    fun vodUrl(sourceId: Int, path: String, height: Int): String? =
        ApiClient.absolute("/api/nas/$sourceId/vod.m3u8")?.let { base ->
            "$base?path=${java.net.URLEncoder.encode(path, "UTF-8")}&h=$height"
        }
}
