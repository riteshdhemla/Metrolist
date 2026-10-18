/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.R
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Where a mood row gets its tracks from. */
sealed interface CalmSource {
    /** Exact videos, always shown first. */
    data class Videos(val ids: List<String>) : CalmSource

    /** Top songs and videos from a YouTube channel or YouTube Music artist page. */
    data class Channel(val browseId: String) : CalmSource

    /** Playlist id without the "VL" prefix. */
    data class Playlist(val id: String) : CalmSource

    data class VideoSearch(val query: String) : CalmSource
}

@Immutable
enum class CalmMood(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val gradientStart: Long,
    val gradientEnd: Long,
    /** Banner taps start a radio from one of these; falls back to a random row item. */
    val seedVideoIds: List<String>,
    val sources: List<CalmSource>,
) {
    DEEP_FOCUS(
        titleRes = R.string.calm_mood_deep_focus,
        subtitleRes = R.string.calm_mood_deep_focus_desc,
        gradientStart = 0xFF27324D,
        gradientEnd = 0xFF4A5D8A,
        seedVideoIds = listOf("oPVte6aMprI"),
        sources =
            listOf(
                // Quiet Quest "Deep Focus", Claude FM
                CalmSource.Videos(listOf("oPVte6aMprI", "tRsQsTMvPNg")),
                // Quiet Quest: Newest Study Music & Deep Focus
                CalmSource.Playlist("PLNIOIzEHtNJbXCOTAlbxazG1MwruX0Rg5"),
                // Healing Raga Studio: Focused Mind
                CalmSource.Playlist("PLkwjRVBr39MQe_uE8Z6NJ49uJqIsC5VdH"),
                // Lofi Girl
                CalmSource.Channel("UCSJ4gkVC6NrvII8umztf0Ow"),
            ),
    ),
    FLOW_STATE(
        titleRes = R.string.calm_mood_flow_state,
        subtitleRes = R.string.calm_mood_flow_state_desc,
        gradientStart = 0xFF234A40,
        gradientEnd = 0xFF5B8C7B,
        seedVideoIds = listOf("D5NHvEVAbWY"),
        sources =
            listOf(
                CalmSource.Videos(listOf("D5NHvEVAbWY")),
                // Healing Raga Studio
                CalmSource.Channel("UCHKhmZDdac42bnBNCye9xqA"),
                // Healing Raga Studio: Deep Healing
                CalmSource.Playlist("PLkwjRVBr39MRmMu9NvS3lAK1bj-Da1Dw_"),
                CalmSource.VideoSearch("cortisol reset bansuri 432Hz"),
            ),
    ),
    BOLLYWOOD_INSTRUMENTAL(
        titleRes = R.string.calm_mood_bollywood,
        subtitleRes = R.string.calm_mood_bollywood_desc,
        gradientStart = 0xFF6B3F31,
        gradientEnd = 0xFFB07A5A,
        seedVideoIds = emptyList(),
        sources =
            listOf(
                CalmSource.VideoSearch("bollywood songs on flute instrumental relaxing"),
                CalmSource.VideoSearch("evergreen bollywood flute instrumental"),
            ),
    ),
    DEVOTIONAL(
        titleRes = R.string.calm_mood_devotional,
        subtitleRes = R.string.calm_mood_devotional_desc,
        gradientStart = 0xFF7A4A12,
        gradientEnd = 0xFFC08A3E,
        seedVideoIds = emptyList(),
        sources =
            listOf(
                // Religious India
                CalmSource.Channel("UC_NGoezF2f_GK3wU2CaquHg"),
                // Sounds of Isha
                CalmSource.Channel("UCRE_MtWjp36g2PTP0g6wBPQ"),
                CalmSource.VideoSearch("krishna flute bhajan peaceful"),
            ),
    ),
    AFFIRMATIONS(
        titleRes = R.string.calm_mood_affirmations,
        subtitleRes = R.string.calm_mood_affirmations_desc,
        gradientStart = 0xFF4A3D66,
        gradientEnd = 0xFF8A76A8,
        seedVideoIds = emptyList(),
        sources =
            listOf(
                CalmSource.VideoSearch("positive affirmations indian flute music"),
                CalmSource.VideoSearch("positive energy flute meditation"),
            ),
    ),
    HOPEFUL_ANTHEMS(
        titleRes = R.string.calm_mood_hopeful_anthems,
        subtitleRes = R.string.calm_mood_hopeful_anthems_desc,
        gradientStart = 0xFF1F4B6E,
        gradientEnd = 0xFFD08A5C,
        seedVideoIds = emptyList(),
        sources =
            listOf(
                // Coldplay
                CalmSource.Channel("UCDPM_n1atn2ijUwHd0NNRQw"),
            ),
    ),
}

@HiltViewModel
class CalmHomeViewModel @Inject constructor() : ViewModel() {
    private val _tracks = MutableStateFlow<Map<CalmMood, List<SongItem>>>(emptyMap())
    val tracks: StateFlow<Map<CalmMood, List<SongItem>>> = _tracks.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        load(onDone = { _isRefreshing.value = false })
    }

    private fun load(onDone: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            coroutineScope {
                CalmMood.entries.map { mood ->
                    async {
                        val items = fetchTracks(mood)
                        if (items.isNotEmpty()) {
                            _tracks.update { it + (mood to items) }
                        }
                    }
                }.awaitAll()
            }
            onDone()
        }
    }

    private suspend fun fetchTracks(mood: CalmMood): List<SongItem> {
        val perSource =
            coroutineScope {
                mood.sources.map { source -> async { fetch(source) } }.awaitAll()
            }
        val pinned = mood.sources.zip(perSource).filter { it.first is CalmSource.Videos }.flatMap { it.second }
        val discovered = interleave(perSource.filterIndexed { i, _ -> mood.sources[i] !is CalmSource.Videos })
        return (pinned + discovered)
            .distinctBy { it.id }
            .take(TRACKS_PER_MOOD)
    }

    private suspend fun fetch(source: CalmSource): List<SongItem> {
        val result =
            when (source) {
                is CalmSource.Videos -> {
                    YouTube.queue(videoIds = source.ids)
                }

                is CalmSource.Channel -> {
                    YouTube.artist(source.browseId).map { page ->
                        page.sections.flatMap { it.items }.filterIsInstance<SongItem>()
                    }
                }

                is CalmSource.Playlist -> {
                    YouTube.playlist(source.id).map { it.songs }
                }

                is CalmSource.VideoSearch -> {
                    YouTube.search(source.query, YouTube.SearchFilter.FILTER_VIDEO).map { page ->
                        page.items.filterIsInstance<SongItem>()
                    }
                }
            }
        return result
            .onFailure(::reportException)
            .getOrNull()
            .orEmpty()
            .take(TRACKS_PER_SOURCE)
    }

    // Round-robin across sources so each row mixes every flavour of its mood.
    private fun <T> interleave(lists: List<List<T>>): List<T> {
        val maxSize = lists.maxOfOrNull { it.size } ?: 0
        return (0 until maxSize).flatMap { i -> lists.mapNotNull { it.getOrNull(i) } }
    }

    private companion object {
        const val TRACKS_PER_SOURCE = 6
        const val TRACKS_PER_MOOD = 12
    }
}
