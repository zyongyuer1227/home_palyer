package com.iptv.player.data.repo

import com.iptv.player.data.api.ApiClient
import com.iptv.player.data.api.Channel
import com.iptv.player.data.api.bodyOrThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FavRepo {

    private val _favorites = MutableStateFlow<List<Channel>>(emptyList())
    val favorites: StateFlow<List<Channel>> = _favorites.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<Int>>(emptySet())
    val favoriteIds: StateFlow<Set<Int>> = _favoriteIds.asStateFlow()

    suspend fun refresh() = runCatching {
        val list = ApiClient.service.favorites().bodyOrThrow()
        _favorites.value = list
        _favoriteIds.value = list.map { it.id }.toSet()
    }

    suspend fun toggle(channel: Channel): Boolean {
        val current = _favoriteIds.value.contains(channel.id)
        val result = runCatching {
            if (current) {
                ApiClient.service.removeFavorite(channel.id)
                true
            } else {
                ApiClient.service.addFavorite(com.iptv.player.data.api.FavoriteBody(channel.id))
                true
            }
        }
        if (result.isSuccess) refresh()
        return result.isSuccess
    }
}
