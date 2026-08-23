package com.iptv.player.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.iptv.player.ui.common.PlayPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.recentStore: DataStore<Preferences> by preferencesDataStore(name = "iptv_recent")

@Serializable
data class RecentItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val payloadJson: String,
    val updatedAt: Long,
)

object RecentRepo {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }
    private val recentKey = stringPreferencesKey("items")
    private val _items = MutableStateFlow<List<RecentItem>>(emptyList())

    private lateinit var context: Context

    val items: StateFlow<List<RecentItem>> = _items.asStateFlow()

    fun init(appContext: Context) {
        context = appContext.applicationContext
        scope.launch {
            context.recentStore.data
                .map { prefs -> decode(prefs[recentKey].orEmpty()) }
                .collect { _items.value = it }
        }
    }

    fun record(payload: PlayPayload) {
        if (!::context.isInitialized) return
        scope.launch {
            val item = when (payload) {
                is PlayPayload.Live -> RecentItem(
                    id = "live:${payload.channel.id}",
                    title = payload.channel.name,
                    subtitle = payload.channel.grp?.takeIf { it.isNotBlank() } ?: "直播",
                    payloadJson = json.encodeToString(PlayPayload.serializer(), payload),
                    updatedAt = System.currentTimeMillis(),
                )
                is PlayPayload.Nas -> RecentItem(
                    id = "nas:${payload.sourceId}:${payload.path}",
                    title = payload.name,
                    subtitle = payload.sourceName,
                    payloadJson = json.encodeToString(PlayPayload.serializer(), payload),
                    updatedAt = System.currentTimeMillis(),
                )
            }
            val next = (listOf(item) + _items.value.filterNot { it.id == item.id })
                .take(10)
            save(next)
        }
    }

    private suspend fun save(items: List<RecentItem>) {
        context.recentStore.edit { prefs ->
            prefs[recentKey] = json.encodeToString(ListSerializer(RecentItem.serializer()), items)
        }
    }

    private fun decode(raw: String): List<RecentItem> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(RecentItem.serializer()), raw)
        }.getOrDefault(emptyList())
    }
}
