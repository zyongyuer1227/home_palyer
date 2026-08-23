package com.iptv.player.data.repo

import com.iptv.player.data.api.ApiClient
import com.iptv.player.data.api.Channel
import com.iptv.player.data.api.FavoriteBody
import com.iptv.player.data.api.bodyOrThrow
import com.iptv.player.data.local.FileLogger
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
        val previousFavorites = _favorites.value
        val previousIds = _favoriteIds.value

        if (current) {
            _favoriteIds.value = previousIds - channel.id
            _favorites.value = previousFavorites.filterNot { it.id == channel.id }
        } else {
            _favoriteIds.value = previousIds + channel.id
            _favorites.value = (listOf(channel) + previousFavorites.filterNot { it.id == channel.id })
        }

        val result = runCatching {
            if (current) {
                val response = ApiClient.service.removeFavorite(channel.id)
                if (!response.isSuccessful) error("HTTP ${response.code()}")
            } else {
                ApiClient.service.addFavorite(FavoriteBody(channel.id)).bodyOrThrow()
            }
        }
        if (result.isSuccess) {
            refresh()
        } else {
            _favorites.value = previousFavorites
            _favoriteIds.value = previousIds
            FileLogger.w("FavRepo", "toggle favorite failed channel=${channel.id}: ${result.exceptionOrNull()?.message}")
        }
        return result.isSuccess
    }
}
