package com.example.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.Channel
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    channel: Channel,
    bufferingOption: String,
    onProgressUpdate: (Long, Long) -> Unit,
    onClosePlayer: () -> Unit,
    modifier: Modifier = Modifier,
    isInPipMode: Boolean = false
) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    // If in PiP mode, ensure controls are never shown
    val actualShowControls = showControls && !isInPipMode

    // Recreate/Re-configure ExoPlayer when stream URL or buffering option changes
    DisposableEffect(channel.url, bufferingOption) {
        // Setup LoadControl for Buffer Management
        val loadControlBuilder = DefaultLoadControl.Builder()
        val minBufferMs = when (bufferingOption) {
            "LOW" -> 15000
            "HIGH" -> 80000
            else -> 40000 // NORMAL
        }
        val maxBufferMs = when (bufferingOption) {
            "LOW" -> 30000
            "HIGH" -> 150000
            else -> 80000 // NORMAL
        }
        val bufferForPlaybackMs = when (bufferingOption) {
            "LOW" -> 1000
            "HIGH" -> 4000
            else -> 2000 // NORMAL
        }
        val bufferForPlaybackAfterRebufferMs = when (bufferingOption) {
            "LOW" -> 2000
            "HIGH" -> 8000
            else -> 4000 // NORMAL
        }

        loadControlBuilder.setBufferDurationsMs(
            minBufferMs,
            maxBufferMs,
            bufferForPlaybackMs,
            bufferForPlaybackAfterRebufferMs
        )

        val player = ExoPlayer.Builder(context)
            .setLoadControl(loadControlBuilder.build())
            .build()
            .apply {
                playWhenReady = true
                val mediaItem = MediaItem.fromUri(channel.url)
                setMediaItem(mediaItem)
                prepare()
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        isLoading = playbackState == Player.STATE_BUFFERING
                        if (playbackState == Player.STATE_READY) {
                            isLoading = false
                        }
                    }

                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                    }
                })
            }

        exoPlayer = player

        onDispose {
            player.release()
            exoPlayer = null
        }
    }

    // Auto-hide controls after 4 seconds
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }

    // Periodic progress updates (for Continue Watching feature)
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            exoPlayer?.let {
                val current = it.currentPosition
                val duration = it.duration
                if (duration > 0) {
                    onProgressUpdate(current, duration)
                }
            }
            delay(5000)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = !isInPipMode) { showControls = !showControls }
    ) {
        // Native ExoPlayer View container
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false // Use custom Compose controls
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                view.player = exoPlayer
                view.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading Indicator (Buffering)
        if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.Center)
            )
        }

        // Custom Overlay UI Controls
        if (actualShowControls) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp)
            ) {
                // Top Panel: Title and Back button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClosePlayer) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Close Player",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = channel.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = "Category: ${channel.category}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.LightGray
                            )
                        }
                    }

                    // Quick Buffer Indicator
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = "Buffer: $bufferingOption",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Middle Panel: Play / Pause, Skip, Resize Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Toggle Aspect Ratio
                    IconButton(
                        onClick = {
                            resizeMode = when (resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Toggle Resize",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Play/Pause Main Button
                    IconButton(
                        onClick = {
                            exoPlayer?.let {
                                if (it.isPlaying) it.pause() else it.play()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    // Forward 10s (For DVR / Video-On-Demand / Catchup)
                    IconButton(
                        onClick = {
                            exoPlayer?.let {
                                it.seekTo(it.currentPosition + 10000)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10 Seconds",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bottom Panel: Timeline Tracker / Live indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.Red, shape = MaterialTheme.shapes.extraSmall)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE STREAM",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }

                        // Aspect Mode Text Display
                        val aspectLabel = when (resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> "FIT"
                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> "STRETCH"
                            else -> "ZOOM"
                        }
                        Text(
                            text = "Aspect: $aspectLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }
    }
}
