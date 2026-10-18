/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.viewmodels.CalmHomeViewModel
import com.metrolist.music.viewmodels.CalmMood
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalmHomeScreen(viewModel: CalmHomeViewModel = hiltViewModel()) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullToRefreshState()

    val hour = remember { LocalTime.now().hour }
    val featuredMood = remember(hour) { moodForHour(hour) }

    // Starting from a single video gives an endless radio of similar tracks.
    val playRadio: (SongItem) -> Unit = { song ->
        if (!isListenTogetherGuest) {
            playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = song.id), song.toMediaMetadata()))
        }
    }

    val playMood: (CalmMood) -> Unit = { mood ->
        val moodTracks = tracks[mood].orEmpty()
        val seed = moodTracks.firstOrNull { it.id in mood.seedVideoIds } ?: moodTracks.randomOrNull()
        if (seed != null) playRadio(seed)
    }

    val onTrackClick: (SongItem) -> Unit = { song ->
        if (song.id == mediaMetadata?.id) playerConnection.togglePlayPause() else playRadio(song)
    }

    val onTrackLongClick: (SongItem) -> Unit = { song ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        menuState.show {
            YouTubeSongMenu(song = song, onDismiss = menuState::dismiss)
        }
    }

    PullToRefreshBox(
        state = pullRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        indicator = {
            Indicator(
                isRefreshing = isRefreshing,
                state = pullRefreshState,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
            )
        },
    ) {
        LazyColumn(
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            item(key = "sky") {
                SkyHeader(hour = hour)
            }

            item(key = "featured") {
                FeaturedMoodCard(
                    mood = featuredMood,
                    cover = tracks[featuredMood]?.firstOrNull(),
                    onPlay = { playMood(featuredMood) },
                )
            }

            item(key = "featured_row") {
                TrackCarousel(
                    mood = featuredMood,
                    tracks = tracks[featuredMood],
                    activeId = mediaMetadata?.id,
                    isPlaying = isPlaying,
                    onClick = onTrackClick,
                    onLongClick = onTrackLongClick,
                )
            }

            CalmMood.entries.filter { it != featuredMood }.forEach { mood ->
                item(key = "header_${mood.name}") {
                    MoodHeader(mood = mood, onPlay = { playMood(mood) })
                }
                item(key = "row_${mood.name}") {
                    TrackCarousel(
                        mood = mood,
                        tracks = tracks[mood],
                        activeId = mediaMetadata?.id,
                        isPlaying = isPlaying,
                        onClick = onTrackClick,
                        onLongClick = onTrackLongClick,
                    )
                }
            }

            item(key = "footer") {
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

private fun moodForHour(hour: Int): CalmMood =
    when (hour) {
        in 5..8 -> CalmMood.DEVOTIONAL
        in 9..12 -> CalmMood.DEEP_FOCUS
        in 13..16 -> CalmMood.FLOW_STATE
        in 17..20 -> CalmMood.BOLLYWOOD_INSTRUMENTAL
        else -> CalmMood.AFFIRMATIONS
    }

private data class SkyPalette(
    val top: Color,
    val middle: Color,
    val horizon: Color,
    val farRidge: Color,
    val nearRidge: Color,
    val glow: Color,
    val showStars: Boolean,
)

@Composable
private fun skyPalette(): SkyPalette {
    val background = MaterialTheme.colorScheme.background
    return if (background.luminance() < 0.5f) {
        SkyPalette(
            top = background,
            middle = Color(0xFF17285A),
            horizon = Color(0xFF3A4F8C),
            farRidge = Color(0xFF1A2A55),
            nearRidge = Color(0xFF0F1B3B),
            glow = Color(0x33C9D6FF),
            showStars = true,
        )
    } else {
        SkyPalette(
            top = background,
            middle = Color(0xFFD5E0F6),
            horizon = Color(0xFFF7DCCB),
            farRidge = Color(0xFFB2C1E2),
            nearRidge = Color(0xFF8FA3D0),
            glow = Color(0x55FFFFFF),
            showStars = false,
        )
    }
}

// Calm-style hero: a quiet sky over layered ridges, with the greeting and a breathing exercise.
@Composable
private fun SkyHeader(hour: Int) {
    val sky = skyPalette()
    val background = MaterialTheme.colorScheme.background
    val greetingRes =
        when (hour) {
            in 5..11 -> R.string.calm_greeting_morning
            in 12..16 -> R.string.calm_greeting_afternoon
            else -> R.string.calm_greeting_evening
        }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .animateContentSize(),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                Brush.verticalGradient(
                    0f to sky.top,
                    0.45f to sky.middle,
                    0.78f to sky.horizon,
                    1f to sky.horizon,
                ),
            )
            val glowCenter = Offset(w * 0.78f, h * 0.30f)
            drawCircle(
                brush = Brush.radialGradient(listOf(sky.glow, Color.Transparent), center = glowCenter, radius = w * 0.35f),
                radius = w * 0.35f,
                center = glowCenter,
            )
            if (sky.showStars) {
                STARS.forEach { (x, y, r) ->
                    drawCircle(Color.White.copy(alpha = 0.55f), radius = r * density, center = Offset(w * x, h * y))
                }
            }
            val far =
                Path().apply {
                    moveTo(0f, h * 0.80f)
                    cubicTo(w * 0.18f, h * 0.66f, w * 0.30f, h * 0.70f, w * 0.42f, h * 0.76f)
                    cubicTo(w * 0.55f, h * 0.62f, w * 0.72f, h * 0.60f, w * 0.86f, h * 0.72f)
                    cubicTo(w * 0.93f, h * 0.70f, w * 0.97f, h * 0.71f, w, h * 0.74f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
            drawPath(far, sky.farRidge)
            val near =
                Path().apply {
                    moveTo(0f, h * 0.88f)
                    cubicTo(w * 0.20f, h * 0.80f, w * 0.38f, h * 0.92f, w * 0.58f, h * 0.84f)
                    cubicTo(w * 0.74f, h * 0.78f, w * 0.88f, h * 0.86f, w, h * 0.82f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
            drawPath(near, sky.nearRidge)
            drawRect(Brush.verticalGradient(0.85f to Color.Transparent, 1f to background))
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 96.dp),
        ) {
            Text(
                text = stringResource(greetingRes),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.calm_greeting_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(20.dp))
            BreathingExercise()
        }
    }
}

// (x, y, radius in dp) as fractions of the header; fixed so the sky never flickers.
private val STARS =
    listOf(
        Triple(0.08f, 0.10f, 1.2f), Triple(0.22f, 0.28f, 0.8f), Triple(0.35f, 0.08f, 1.0f),
        Triple(0.52f, 0.20f, 0.7f), Triple(0.63f, 0.06f, 1.1f), Triple(0.91f, 0.14f, 0.9f),
        Triple(0.15f, 0.48f, 0.7f), Triple(0.46f, 0.40f, 0.9f), Triple(0.70f, 0.46f, 0.6f),
        Triple(0.96f, 0.38f, 0.8f), Triple(0.28f, 0.58f, 0.6f), Triple(0.84f, 0.56f, 0.7f),
    )

private const val BREATH_MILLIS = 4000
private const val BREATH_CYCLES = 6

// One minute of 4s-in / 4s-out breathing; runs only after a tap so it costs nothing at rest.
@Composable
private fun BreathingExercise() {
    var active by remember { mutableStateOf(false) }
    var inhaling by remember { mutableStateOf(true) }
    val scale = remember { Animatable(0.55f) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(active) {
        if (!active) {
            scale.snapTo(0.55f)
            return@LaunchedEffect
        }
        repeat(BREATH_CYCLES) {
            inhaling = true
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            scale.animateTo(1f, tween(BREATH_MILLIS, easing = FastOutSlowInEasing))
            inhaling = false
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            scale.animateTo(0.55f, tween(BREATH_MILLIS, easing = FastOutSlowInEasing))
        }
        active = false
    }

    val onBackground = MaterialTheme.colorScheme.onBackground
    if (!active) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .clip(CircleShape)
                    .background(onBackground.copy(alpha = 0.10f))
                    .border(BorderStroke(1.dp, onBackground.copy(alpha = 0.18f)), CircleShape)
                    .clickable { active = true }
                    .padding(start = 8.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.calm_take_a_breath),
                style = MaterialTheme.typography.labelLarge,
                color = onBackground,
            )
        }
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clickable { active = false },
        ) {
            Box(
                modifier =
                    Modifier
                        .size(170.dp)
                        .scale(scale.value)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                ),
                            ),
                        ).border(BorderStroke(1.dp, onBackground.copy(alpha = 0.25f)), CircleShape),
            )
            Text(
                text = stringResource(if (inhaling) R.string.calm_breathe_in else R.string.calm_breathe_out),
                style = MaterialTheme.typography.titleMedium,
                color = onBackground,
            )
        }
    }
}

@Composable
private fun FeaturedMoodCard(
    mood: CalmMood,
    cover: SongItem?,
    onPlay: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Color(mood.gradientStart), Color(mood.gradientEnd))))
                .clickable(onClick = onPlay),
    ) {
        // Blurred so the video's own title art becomes a soft backdrop behind ours (API 31+).
        if (cover != null) {
            CoverImage(song = cover, widthPx = 1080, modifier = Modifier.matchParentSize().blur(18.dp))
        }
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color(mood.gradientStart).copy(alpha = 0.25f),
                            0.5f to Color(mood.gradientStart).copy(alpha = 0.45f),
                            1f to Color.Black.copy(alpha = 0.80f),
                        ),
                    ),
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(20.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.calm_for_this_moment).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.6.sp,
                    color = Color.White.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(mood.titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                )
                Text(
                    text = stringResource(mood.subtitleRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
            Spacer(Modifier.width(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(start = 14.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = null,
                    tint = Color(mood.gradientStart),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.calm_play),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(mood.gradientStart),
                )
            }
        }
    }
}

@Composable
private fun MoodHeader(
    mood: CalmMood,
    onPlay: () -> Unit,
) {
    val title = stringResource(mood.titleRes)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 28.dp, bottom = 4.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(mood.subtitleRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(onClick = onPlay),
        ) {
            Icon(
                painter = painterResource(R.drawable.play),
                contentDescription = stringResource(R.string.calm_play_mood, title),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun TrackCarousel(
    mood: CalmMood,
    tracks: List<SongItem>?,
    activeId: String?,
    isPlaying: Boolean,
    onClick: (SongItem) -> Unit,
    onLongClick: (SongItem) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        userScrollEnabled = tracks != null,
    ) {
        if (tracks == null) {
            items(3) { PlaceholderCard(mood) }
        } else {
            items(tracks, key = { "${mood.name}_${it.id}" }) { song ->
                TrackCard(
                    song = song,
                    mood = mood,
                    isActive = song.id == activeId,
                    isPlaying = isPlaying,
                    onClick = { onClick(song) },
                    onLongClick = { onLongClick(song) },
                )
            }
        }
    }
}

private val CARD_WIDTH = 200.dp
private val CARD_SHAPE = RoundedCornerShape(16.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackCard(
    song: SongItem,
    mood: CalmMood,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .width(CARD_WIDTH)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(CARD_SHAPE)
                    .background(Brush.linearGradient(listOf(Color(mood.gradientStart), Color(mood.gradientEnd))))
                    .then(
                        if (isActive) Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), CARD_SHAPE) else Modifier,
                    ),
        ) {
            CoverImage(song = song, widthPx = 560, modifier = Modifier.matchParentSize())
            if (isActive) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                ) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = song.artists.joinToString { it.name },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PlaceholderCard(mood: CalmMood) {
    Column(modifier = Modifier.width(CARD_WIDTH)) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(CARD_SHAPE)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(mood.gradientStart).copy(alpha = 0.5f), Color(mood.gradientEnd).copy(alpha = 0.3f)),
                        ),
                    ),
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier =
                Modifier
                    .width(140.dp)
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
    }
}

// Videos get YouTube's letterbox-free 16:9 frames; album art is resized from its own URL.
@Composable
private fun CoverImage(
    song: SongItem,
    widthPx: Int,
    modifier: Modifier = Modifier,
) {
    val thumbnail = song.thumbnail
    val isVideoFrame = thumbnail.contains("ytimg.com")
    val url = if (isVideoFrame) "https://i.ytimg.com/vi/${song.id}/hq720.jpg" else thumbnail.resize(widthPx, widthPx)
    val fallback = rememberAsyncImagePainter(if (isVideoFrame) "https://i.ytimg.com/vi/${song.id}/mqdefault.jpg" else thumbnail)
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        error = fallback,
        modifier = modifier,
    )
}
