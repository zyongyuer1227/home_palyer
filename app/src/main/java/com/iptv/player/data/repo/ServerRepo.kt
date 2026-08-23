package com.iptv.player.data.repo

import com.iptv.player.data.api.ApiClient
import com.iptv.player.data.api.ServerConfig
import com.iptv.player.data.local.FileLogger
import com.iptv.player.data.local.ServerStore
import com.iptv.player.data.local.ServersState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ServerRepo(private val store: ServerStore) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _state = MutableStateFlow(ServersState())
    val state: StateFlow<ServersState> = _state.asStateFlow()

    init {
        scope.launch {
            store.state.collect { s ->
                _state.value = s
                s.active?.let { server ->
                    ApiClient.applyBase(server)
                }
            }
        }
    }

    suspend fun add(address: String, name: String? = null): ServerConfig {
        val normalized = ApiClient.normalizeAddress(address)?.toString() ?: address.trim()
        val label = name?.takeIf { it.isNotBlank() } ?: normalized
        val server = ServerConfig(id = UUID.randomUUID().toString(), name = label, address = normalized)
        val updated = _state.value.let {
            ServersState(
                servers = it.servers + server,
                activeId = if (it.servers.isEmpty()) server.id else it.activeId ?: server.id,
            )
        }
        store.save(updated)
        _state.value = updated
        FileLogger.i("Server", "added server id=${server.id} address=$normalized")
        return server
    }

    suspend fun remove(id: String) {
        val cur = _state.value
        val servers = cur.servers.filterNot { it.id == id }
        val activeId = if (cur.activeId == id) servers.firstOrNull()?.id else cur.activeId
        val updated = ServersState(servers, activeId)
        store.save(updated)
        _state.value = updated
        FileLogger.i("Server", "removed server id=$id")
    }

    suspend fun select(id: String) {
        val cur = _state.value
        if (cur.servers.none { it.id == id }) return
        val updated = cur.copy(activeId = id)
        store.save(updated)
        _state.value = updated
        FileLogger.i("Server", "select server id=$id")
    }

    suspend fun rename(id: String, name: String) {
        val cur = _state.value
        val servers = cur.servers.map { if (it.id == id) it.copy(name = name) else it }
        val updated = cur.copy(servers = servers)
        store.save(updated)
        _state.value = updated
    }
}
