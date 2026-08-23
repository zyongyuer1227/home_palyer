package com.iptv.player.ui.home

import com.iptv.player.data.api.Channel
import com.iptv.player.data.api.ChannelPage
import com.iptv.player.data.api.GroupItem
import com.iptv.player.data.api.Source
import com.iptv.player.data.api.ApiClient
import com.iptv.player.data.api.bodyOrThrow
import com.iptv.player.data.repo.ChannelRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ChannelFilter {
    data object All : ChannelFilter
    data object Favorite : ChannelFilter
    data class BySource(val source: Source) : ChannelFilter
    data class ByProgramGroup(val group: GroupItem) : ChannelFilter
}

data class HomeUiState(
    val sources: List<Source> = emptyList(),
    val programGroups: List<GroupItem> = emptyList(),
    val filter: ChannelFilter = ChannelFilter.All,
    val query: String = "",
    val channels: List<Channel> = emptyList(),
    val total: Int = 0,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val grid: Boolean = false,
)

@OptIn(FlowPreview::class)
class HomeController(private val scope: CoroutineScope) {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var queryJob: Job? = null
    private val queryFlow = MutableStateFlow("")

    init {
        loadSources()
        loadProgramGroups()
        queryJob = scope.launch {
            queryFlow
                .debounce(400)
                .distinctUntilChanged()
                .collect { q ->
                    _state.update { it.copy(query = q) }
                    reload()
                }
        }
    }

    fun loadSources() {
        scope.launch {
            runCatching { ChannelRepo.sources().getOrThrow() }
                .onSuccess { srcs ->
                    _state.update { it.copy(sources = srcs) }
                }
        }
    }

    fun loadProgramGroups() {
        scope.launch {
            runCatching { ApiClient.service.channelGroups().bodyOrThrow() }
                .onSuccess { groups ->
                    _state.update { it.copy(programGroups = groups.filter { g -> g.name.isNotBlank() }) }
                }
        }
    }

    fun selectFilter(filter: ChannelFilter) {
        _state.update { it.copy(filter = filter) }
        reload()
    }

    fun setQuery(q: String) {
        queryFlow.value = q
    }

    fun toggleView() {
        _state.update { it.copy(grid = !it.grid) }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loading || s.loadingMore) return
        if (s.channels.size >= s.total) return
        if (s.total == 0 && s.channels.isEmpty()) return
        loadJob?.cancel()
        loadJob = scope.launch {
            _state.update { it.copy(loadingMore = true) }
            val page = fetch(offset = s.channels.size)
            page?.let { p ->
                _state.update { st ->
                    st.copy(
                        channels = st.channels + p.channels,
                        total = p.total,
                        loadingMore = false,
                    )
                }
            } ?: _state.update { it.copy(loadingMore = false) }
        }
    }

    fun reload() {
        loadJob?.cancel()
        loadJob = scope.launch {
            _state.update { it.copy(loading = true, error = null, channels = emptyList(), total = 0) }
            val page = fetch(offset = 0)
            if (page != null) {
                _state.update {
                    it.copy(loading = false, channels = page.channels, total = page.total)
                }
            } else {
                _state.update { it.copy(loading = false, error = "加载失败：无法连接服务器") }
            }
        }
    }

    private suspend fun fetch(offset: Int): ChannelPage? {
        val s = _state.value
        val q = s.query.trim().takeIf { it.isNotEmpty() }
        val favorite = s.filter is ChannelFilter.Favorite
        val sourceId = (s.filter as? ChannelFilter.BySource)?.source?.id
        val grp = (s.filter as? ChannelFilter.ByProgramGroup)?.group?.name
        return runCatching {
            ChannelRepo.channels(
                grp = grp,
                q = q,
                favorite = favorite,
                sourceId = sourceId,
                limit = 200,
                offset = offset,
            ).getOrThrow()
        }.getOrNull()
    }
}
