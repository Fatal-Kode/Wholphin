package com.github.damontecres.wholphin.ui.playback.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.Chapter
import com.github.damontecres.wholphin.data.model.PlaylistItem
import com.github.damontecres.wholphin.ui.AppColors
import com.github.damontecres.wholphin.ui.LocalImageUrlService
import com.github.damontecres.wholphin.ui.components.TimeDisplay
import com.github.damontecres.wholphin.ui.isNotNullOrBlank
import com.github.damontecres.wholphin.ui.playback.AnalyticsState
import com.github.damontecres.wholphin.ui.playback.ControllerViewState
import com.github.damontecres.wholphin.ui.playback.CurrentPlayback
import com.github.damontecres.wholphin.ui.playback.PlaybackDialogType
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.MediaSegmentDto
import org.jellyfin.sdk.model.api.PlayMethod
import org.jellyfin.sdk.model.api.TrickplayInfoDto
import kotlin.time.Duration

/**
 * The overlay during playback showing controls, seek preview image, debug info, etc
 */
@Composable
fun PlaybackOverlay(
    item: BaseItem?,
    chapters: List<Chapter>,
    player: Player,
    controllerViewState: ControllerViewState,
    showPlay: Boolean,
    showClock: Boolean,
    previousEnabled: Boolean,
    nextEnabled: Boolean,
    seekEnabled: Boolean,
    seekBack: Duration,
    skipBackOnResume: Duration?,
    seekForward: Duration,
    onPlaybackActionClick: (PlaybackAction) -> Unit,
    onClickPlaybackDialogType: (PlaybackDialogType) -> Unit,
    onSeekBarChange: (Long) -> Unit,
    showDebugInfo: Boolean,
    currentPlayback: CurrentPlayback?,
    currentSegment: MediaSegmentDto?,
    analyticsState: AnalyticsState,
    queue: List<PlaylistItem>,
    modifier: Modifier = Modifier,
    trickplayInfo: TrickplayInfoDto? = null,
    trickplayUrlFor: (Int) -> String? = { null },
    onClickPlaylist: (BaseItem) -> Unit = {},
    seekBarInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val seekBarFocused by seekBarInteractionSource.collectIsFocusedAsState()
    // Will be used for preview/trick play images
    var seekProgressMs by remember(seekBarFocused) { mutableLongStateOf(player.currentPosition) }
    var seekProgressPercent = (seekProgressMs.toDouble() / player.duration).toFloat()

    val density = LocalDensity.current

    val titleHeight =
        remember(item?.title, density) {
            if (item?.title.isNotNullOrBlank()) with(density) { titleTextSize.toDp() } else 0.dp
        }
    val subtitleHeight =
        remember(item?.subtitleLong, density) {
            if (item?.subtitleLong.isNotNullOrBlank()) with(density) { subtitleTextSize.toDp() } else 0.dp
        }

    // This will be calculated after composition
    var controllerHeight by remember { mutableStateOf(0.dp) }
    var state by remember(controllerViewState.controlsVisible) {
        mutableStateOf(if (controllerViewState.controlsVisible) OverlayViewState.CONTROLLER else OverlayViewState.HIDDEN)
    }
    val onChangeState = { newState: OverlayViewState ->
        state = newState
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = controllerViewState.controlsVisible && !showDebugInfo,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.matchParentSize(),
        ) {
            // Background scrim for OSD readability
            val scrimBrush =
                remember {
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.80f),
                            ),
                    )
                }
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(scrimBrush),
            )
        }

        AnimatedContent(
            targetState = state,
            label = "controls transition",
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    // Moving down, so move content up
                    (slideInVertically { it / 2 } + fadeIn()).togetherWith(slideOutVertically { -it / 2 } + fadeOut())
                } else {
                    // Moving up
                    (slideInVertically { -it / 2 } + fadeIn()).togetherWith(slideOutVertically { it / 2 } + fadeOut())
                }
            },
        ) { targetState ->
            when (targetState) {
                OverlayViewState.HIDDEN -> {
                    // Necessary so the bounds for animation are full width
                    Box(Modifier.fillMaxWidth())
                }

                OverlayViewState.CONTROLLER -> {
                    if (seekBarFocused) {
                        LaunchedEffect(Unit) {
                            seekProgressPercent =
                                (player.currentPosition.toFloat() / player.duration)
                        }
                    }
                    val nextState =
                        remember(chapters, nextEnabled) {
                            if (chapters.isNotEmpty()) {
                                OverlayViewState.CHAPTERS
                            } else if (nextEnabled) {
                                OverlayViewState.QUEUE
                            } else {
                                null
                            }
                        }
                    PlaybackController(
                        item = item,
                        statusBadges = { currentPlayback?.let { PlaybackModeBadges(it, player) } },
                        nextState = nextState,
                        player = player,
                        controllerViewState = controllerViewState,
                        showPlay = showPlay,
                        previousEnabled = previousEnabled,
                        nextEnabled = nextEnabled,
                        seekEnabled = seekEnabled,
                        seekBack = seekBack,
                        skipBackOnResume = skipBackOnResume,
                        seekForward = seekForward,
                        onPlaybackActionClick = onPlaybackActionClick,
                        onClickPlaybackDialogType = onClickPlaybackDialogType,
                        onSeekBarChange = {
                            onSeekBarChange(it)
                            seekProgressMs = it
                        },
                        currentSegment = currentSegment,
                        onChangeState = onChangeState,
                        modifier =
                            Modifier
                                .padding(bottom = 8.dp)
                                .onGloballyPositioned {
                                    controllerHeight = with(density) { it.size.height.toDp() }
                                },
                        seekBarInteractionSource = seekBarInteractionSource,
                    )
                }

                OverlayViewState.CHAPTERS -> {
                    if (chapters.isNotEmpty()) {
                        ChapterRowOverlay(
                            player = player,
                            controllerViewState = controllerViewState,
                            chapters = chapters,
                            hasNext = nextEnabled,
                            onChangeState = onChangeState,
                            modifier =
                                Modifier
                                    .padding(horizontal = 8.dp)
                                    .fillMaxWidth(),
                        )
                    }
                }

                OverlayViewState.QUEUE -> {
                    if (nextEnabled) {
                        QueueRowOverlay(
                            queue = queue,
                            controllerViewState = controllerViewState,
                            nextState =
                                remember(chapters) {
                                    if (chapters.isNotEmpty()) {
                                        OverlayViewState.CHAPTERS
                                    } else {
                                        OverlayViewState.CONTROLLER
                                    }
                                },
                            onChangeState = onChangeState,
                            onClickPlaylist = onClickPlaylist,
                            modifier =
                                Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                        )
                    }
                }
            }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            // Trickplay
            AnimatedVisibility(
                visible = controllerViewState.controlsVisible && seekProgressPercent >= 0 && seekBarFocused,
                enter =
                    expandVertically(
                        spring(
                            stiffness = Spring.StiffnessMedium,
                            visibilityThreshold = IntSize.VisibilityThreshold,
                        ),
                    ) + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(.95f),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                            Modifier
                                .align(Alignment.BottomStart)
                                .offsetByPercent(
                                    xPercentage = seekProgressPercent.coerceIn(0f, 1f),
                                ).padding(bottom = controllerHeight - titleHeight - subtitleHeight),
                    ) {
                        TrickplayPreview(
                            seekProgressMs = seekProgressMs,
                            trickplayInfo = trickplayInfo,
                            trickplayUrlFor = trickplayUrlFor,
                        )
                    }
                }
            }
        }

        // Top
        val logoImageUrl = LocalImageUrlService.current.rememberImageUrl(item, ImageType.LOGO)
        AnimatedVisibility(
            visible = !showDebugInfo && logoImageUrl.isNotNullOrBlank() && controllerViewState.controlsVisible,
            enter = slideIn { IntOffset(x = -it.width / 2, y = -it.height / 2) } + fadeIn(),
            exit = slideOut { IntOffset(x = -it.width / 2, y = -it.height / 2) } + fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.TopStart),
        ) {
            AsyncImage(
                model = logoImageUrl,
                contentDescription = "Logo",
                alignment = Alignment.TopStart,
                modifier =
                    Modifier
                        .size(width = 240.dp, height = 120.dp)
                        .padding(16.dp),
            )
        }
        AnimatedVisibility(
            visible = controllerViewState.controlsVisible && !showDebugInfo && showClock,
            enter = slideIn { IntOffset(x = it.width / 2, y = -it.height / 2) } + fadeIn(),
            exit = slideOut { IntOffset(x = it.width / 2, y = -it.height / 2) } + fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.TopEnd),
        ) {
            Column(horizontalAlignment = Alignment.End) {
                if (!showDebugInfo && showClock) {
                    Box { TimeDisplay() }
                }
            }
        }

        // Debug overlay
        AnimatedVisibility(
            visible = showDebugInfo && controllerViewState.controlsVisible,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.TopStart),
        ) {
            val configuration = LocalConfiguration.current
            val height =
                remember(
                    configuration,
                    controllerHeight,
                ) { configuration.screenHeightDp.dp - controllerHeight }
            PlaybackDebugOverlay(
                analyticsState = analyticsState,
                currentPlayback = currentPlayback,
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .heightIn(max = height)
                        .padding(bottom = 16.dp)
                        .background(AppColors.TransparentBlack50)
                        .padding(8.dp)
                        .onFocusChanged {
                            if (it.hasFocus) {
                                // If Debug overlay gains focus, do not hide the controls
                                controllerViewState.pulseControls(Long.MAX_VALUE)
                            } else {
                                controllerViewState.pulseControls()
                            }
                        },
            )
        }
    }
}

/**
 * The view state of the overlay
 */
enum class OverlayViewState {
    HIDDEN,
    CONTROLLER,
    CHAPTERS,
    QUEUE,
}


@Composable
private fun PlaybackModeBadges(playback: CurrentPlayback, player: Player) {
    val view = androidx.compose.ui.platform.LocalView.current
    val rates by androidx.compose.runtime.produceState("— fps → — Hz", player, playback.sourceId, view) {
        val format = java.text.DecimalFormat("0.###")
        while (true) {
            val fps = (player as? androidx.media3.exoplayer.ExoPlayer)?.videoFormat?.frameRate
                ?.takeIf { it > 0f } ?: player.currentTracks.groups.firstNotNullOfOrNull { group ->
                    if (group.type != androidx.media3.common.C.TRACK_TYPE_VIDEO) null else
                        (0 until group.length).firstNotNullOfOrNull { index ->
                            group.getTrackFormat(index).frameRate.takeIf { group.isTrackSelected(index) && it > 0f }
                        }
                } ?: playback.mediaSourceInfo.mediaStreams.orEmpty().firstOrNull {
                    playback.playMethod == PlayMethod.DIRECT_PLAY && it.type == org.jellyfin.sdk.model.api.MediaStreamType.VIDEO
                }?.let { it.averageFrameRate ?: it.realFrameRate }?.takeIf { it > 0f }
            val hz = view.display?.mode?.refreshRate
            value = "${fps?.let { format.format(it) } ?: "—"} fps → ${hz?.let { format.format(it) } ?: "—"} Hz"
            kotlinx.coroutines.delay(1000)
        }
    }
    val info = playback.transcodeInfo
    val directPlay = playback.playMethod == PlayMethod.DIRECT_PLAY
    val directStream = playback.playMethod == PlayMethod.DIRECT_STREAM
    val video = when {
        directPlay -> R.string.osd_mode_direct_play
        info?.isVideoDirect == true -> R.string.osd_mode_copy
        info?.isVideoDirect == false -> R.string.osd_mode_transcode
        directStream -> R.string.osd_mode_remux
        else -> R.string.osd_mode_pending
    }
    val audio = when {
        directPlay -> R.string.osd_mode_direct_play
        info?.isAudioDirect == true -> R.string.osd_mode_copy
        info?.isAudioDirect == false -> R.string.osd_mode_transcode
        else -> R.string.osd_mode_pending
    }

    Column(
        modifier = Modifier.padding(end = 6.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        val shape = RoundedCornerShape(6.dp)
        val badge = Modifier
            .background(AppColors.TransparentBlack50, shape)
            .padding(horizontal = 4.dp)
        Text(rates, modifier = badge, style = MaterialTheme.typography.labelSmall, color = Color.White)
        Text(
            text = stringResource(R.string.osd_video_mode, stringResource(video)),
            modifier = badge,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
        Text(
            text = stringResource(R.string.osd_audio_mode, stringResource(audio)),
            modifier = badge,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}
