package com.iptv.player.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import com.iptv.player.data.api.ServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "iptv_servers")

data class ServersState(
    val servers: List<ServerConfig> = emptyList(),
    val activeId: String? = null,
) {
    val active: ServerConfig? get() = servers.firstOrNull { it.id == activeId } ?: servers.firstOrNull()
}

class ServerStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val serversKey = stringPreferencesKey("servers")
    private val activeKey = stringPreferencesKey("active_id")

    val state: Flow<ServersState> = context.dataStore.data.map { prefs ->
        val raw = prefs[serversKey] ?: ""
        val servers = try {
            json.decodeFromString(ListSerializer(ServerConfig.serializer()), raw)
        } catch (e: Exception) {
            emptyList()
        }
        ServersState(servers, prefs[activeKey])
    }

    suspend fun save(state: ServersState) {
        context.dataStore.edit { prefs ->
            prefs[serversKey] = json.encodeToString(ListSerializer(ServerConfig.serializer()), state.servers)
            state.activeId?.let { prefs[activeKey] = it } ?: prefs.remove(activeKey)
        }
    }
}
