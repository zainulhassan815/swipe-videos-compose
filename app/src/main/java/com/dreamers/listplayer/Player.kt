package com.dreamers.listplayer

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import dev.chrisbanes.snapper.SnapOffsets
import dev.chrisbanes.snapper.rememberSnapperFlingBehavior
import kotlin.math.abs
import kotlin.random.Random

data class Video(
    val url: String,
    val isFavorite: Boolean = false,
)

val SampleVideos = listOf(
    "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
    "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/VolkswagenGTIReview.mp4",
    "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
    "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
    "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
    "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
    "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
    "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
    "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
    "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
    "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
    "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
    "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4"
).map { Video(url = it, isFavorite = Random.nextBoolean()) }


private fun determineCurrentlyPlayingItem(listState: LazyListState): Int? {
    val layoutInfo = listState.layoutInfo
    val midPoint = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    return layoutInfo.visibleItemsInfo
        .sortedBy { abs((it.offset + it.size / 2) - midPoint) }
        .map { it.index }
        .firstOrNull()
}

@OptIn(ExperimentalSnapperApi::class)
@Composable
fun Player(
    videos: List<Video> = SampleVideos,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {
    val context = LocalContext.current
    val mediaItems = videos.map { MediaItem.fromUri(it.url) }
    val player = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            addMediaItems(mediaItems)
            prepare()
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            seekTo(0, 100L)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.playWhenReady = false
                Lifecycle.Event.ON_RESUME -> player.playWhenReady = true
                Lifecycle.Event.ON_DESTROY -> {
                    player.stop()
                    player.release()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val listState = rememberLazyListState()
    val currentItem = determineCurrentlyPlayingItem(listState)

    LaunchedEffect(currentItem) {
        if (currentItem != null && player.currentMediaItemIndex != currentItem) {
            player.seekTo(currentItem, 100L)
        }
        Log.v("Player",
            "Current Item: $currentItem, playerCurrentItem: ${player.currentMediaItemIndex}")
    }

    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
            state = listState,
            flingBehavior = rememberSnapperFlingBehavior(
                lazyListState = listState,
                snapOffsetForItem = SnapOffsets.Center,
            )
        ) {
            items(
                items = videos,
                key = { it.url }
            ) {
                Video(
                    modifier = Modifier.fillParentMaxSize(),
                    player = player,
                    video = it,
                    index = videos.indexOf(it)
                )
            }
        }
    }
}

data class PlayerState(
    val loading: Boolean,
    val playing: Boolean,
)

@Composable
fun rememberPlaybackState(player: ExoPlayer, mediaItemIndex: Int): PlayerState {

    var state by remember {
        mutableStateOf(PlayerState(loading = true, playing = false))
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (player.currentMediaItemIndex == mediaItemIndex) {
                    state = state.copy(playing = isPlaying)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (player.currentMediaItemIndex == mediaItemIndex) {
                    val loaded = playbackState == Player.STATE_READY
                    state = state.copy(loading = !loaded)
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    return state
}

@SuppressLint("InflateParams")
@Composable
private fun Video(
    modifier: Modifier = Modifier,
    player: ExoPlayer,
    video: Video,
    index: Int,
    onBackClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
) {
    val state = rememberPlaybackState(player, index)
    val context = LocalContext.current
    val playerView = remember {
        LayoutInflater.from(context).inflate(R.layout.video_player_view, null, false) as StyledPlayerView
    }

    DisposableEffect(true) {
        onDispose {
            playerView.player = null
            Log.v("PlayerView", "Removing Player For $index")
        }
    }

    Surface(
        modifier = modifier,
        contentColor = Color.White,
        color = Color.Black,
    ) {
        Box {
            if (!state.loading) {
                AndroidView(
                    modifier = Modifier.matchParentSize(),
                    factory = {
                        playerView.apply {
                            this.player = player
                            Log.v("PlayerView", "Adding Player For $index")
                        }
                    }
                )
            }

            IconButton(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                onClick = onBackClick
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back_24px),
                    contentDescription = stringResource(R.string.cd_navigate_back),
                )
            }

            if (state.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                IconButton(
                    modifier = Modifier.align(Alignment.Center),
                    onClick = {
                        player.run {
                            if (isPlaying) pause() else play()
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            if (state.playing) R.drawable.pause_24px else R.drawable.play_arrow_24px
                        ),
                        contentDescription = stringResource(
                            if (state.playing) R.string.cd_play else R.string.cd_pause
                        )
                    )
                }
            }

            VideoActions(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                isFavorite = video.isFavorite,
                onFavoriteClick = onFavoriteClick,
                onShareClick = onShareClick,
                onDownloadClick = onDownloadClick,
            )
        }
    }
}

@Composable
private fun VideoActions(
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onFavoriteClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    onDownloadClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        onFavoriteClick?.let {
            IconButton(onClick = it) {
                Icon(
                    painter = painterResource(
                        if (isFavorite) R.drawable.favorite_filled_24px else R.drawable.favorite_24px
                    ),
                    contentDescription = stringResource(R.string.cd_add_to_favorite),
                    tint = if (isFavorite) Color.Red else LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                )
            }
        }

        onShareClick?.let {
            IconButton(onClick = it) {
                Icon(
                    painter = painterResource(R.drawable.share_24px),
                    contentDescription = stringResource(R.string.cd_share),
                )
            }
        }

        onDownloadClick?.let {
            IconButton(onClick = it) {
                Icon(
                    painter = painterResource(R.drawable.download_24px),
                    contentDescription = stringResource(R.string.cd_download),
                )
            }
        }
    }
}