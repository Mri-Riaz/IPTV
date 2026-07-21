package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Channel
import com.example.data.model.EpgProgram
import com.example.ui.components.VideoPlayer
import com.example.ui.viewmodel.IPTVViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: IPTVViewModel,
    isInPipMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playlists by viewModel.playlists.collectAsState()
    val channels by viewModel.filteredChannels.collectAsState()
    val favoriteChannels by viewModel.favoriteChannels.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val watchHistory by viewModel.watchHistory.collectAsState()

    val selectedPlaylistId by viewModel.selectedPlaylistId.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val bufferingOption by viewModel.bufferingOption.collectAsState()
    val isParentalEnabled by viewModel.parentalEnabled.collectAsState()

    var showPlaylistMenu by remember { mutableStateOf(false) }

    // Parental PIN Prompt logic
    var pendingChannelToPlay by remember { mutableStateOf<Channel?>(null) }
    var showParentalPrompt by remember { mutableStateOf(false) }
    var parentalInputPin by remember { mutableStateOf("") }
    var parentalPinError by remember { mutableStateOf(false) }

    // EPG Details Prompt logic
    var selectedEpgProgramForDetail by remember { mutableStateOf<EpgProgram?>(null) }

    fun checkAndPlayChannel(channel: Channel) {
        if (isParentalEnabled) {
            pendingChannelToPlay = channel
            showParentalPrompt = true
        } else {
            viewModel.playChannel(channel)
        }
    }

    if (isInPipMode && currentChannel != null) {
        VideoPlayer(
            channel = currentChannel!!,
            bufferingOption = bufferingOption,
            onProgressUpdate = { progress, duration ->
                viewModel.saveWatchProgress(currentChannel!!, progress, duration)
            },
            onClosePlayer = { viewModel.playChannel(null) },
            isInPipMode = true,
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val columnsCount = when {
            screenWidth < 600.dp -> 2
            screenWidth < 900.dp -> 4
            else -> 6
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Header Search & Playlist Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Playlist Dropdown Trigger
                Box(modifier = Modifier.wrapContentSize()) {
                    val activePlaylistName = playlists.firstOrNull { it.id == selectedPlaylistId }?.name ?: "No Feed"
                    Button(
                        onClick = { showPlaylistMenu = true },
                        modifier = Modifier.testTag("playlist_selector_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Icon(imageVector = Icons.Default.PlaylistPlay, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = activePlaylistName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 120.dp)
                        )
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = showPlaylistMenu,
                        onDismissRequest = { showPlaylistMenu = false }
                    ) {
                        playlists.forEach { playlist ->
                            DropdownMenuItem(
                                text = { Text(playlist.name) },
                                onClick = {
                                    viewModel.selectPlaylist(playlist.id)
                                    showPlaylistMenu = false
                                }
                            )
                        }
                    }
                }

                // Smart Search TextField
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search streams...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_search_input"),
                    singleLine = true,
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.setSearchQuery(searchQuery) })
                )
            }

            // Categories list (Horizontal scroll chips)
            if (categories.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { viewModel.selectCategory(cat) },
                            label = { Text(cat) },
                            modifier = Modifier.testTag("category_chip_$cat")
                        )
                    }
                }
            }

            // Main Content Area: Playback vs Grid
            if (currentChannel != null) {
                // Built-in Player container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    VideoPlayer(
                        channel = currentChannel!!,
                        bufferingOption = bufferingOption,
                        onProgressUpdate = { progress, duration ->
                            viewModel.saveWatchProgress(currentChannel!!, progress, duration)
                        },
                        onClosePlayer = { viewModel.playChannel(null) }
                    )

                    // Quick launch external player option
                    IconButton(
                        onClick = {
                            val channelUrl = currentChannel!!.url
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(channelUrl), "video/*")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                val chooser = Intent.createChooser(intent, "Launch Live stream")
                                context.startActivity(chooser)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No external players available.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 48.dp, end = 16.dp)
                            .testTag("external_player_fab"),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.onTertiary)
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "Play External Player")
                    }
                }

                // Horizontal EPG Timeline View for the currently active channel
                EpgTimelineView(
                    epgId = currentChannel!!.epgId ?: "",
                    viewModel = viewModel,
                    onProgramClick = { program ->
                        selectedEpgProgramForDetail = program
                    }
                )
            }

            // Continue Watching section
            if (watchHistory.isNotEmpty()) {
                Text(
                    text = "Continue Watching",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(watchHistory) { historyItem ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .width(180.dp)
                                .clickable {
                                    val originalChannel = channels.firstOrNull { it.id == historyItem.channelId }
                                        ?: Channel(
                                            id = historyItem.channelId,
                                            playlistId = 0L,
                                            name = historyItem.channelName,
                                            url = historyItem.channelUrl,
                                            logo = historyItem.channelLogo,
                                            category = historyItem.category,
                                            streamType = historyItem.streamType
                                        )
                                    checkAndPlayChannel(originalChannel)
                                }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(70.dp)
                                        .background(Color.Black.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small)
                                ) {
                                    if (historyItem.channelLogo != null) {
                                        AsyncImage(
                                            model = historyItem.channelLogo,
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Tv,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .align(Alignment.Center)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = historyItem.channelName,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Linear Progress bar representing completion percentage
                                if (historyItem.duration > 0) {
                                    val percent = historyItem.playbackPosition.toFloat() / historyItem.duration.toFloat()
                                    LinearProgressIndicator(
                                        progress = { percent.coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
                                            .height(3.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Grid Layout of Channels
            if (channels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "No channels matched filters", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnsCount),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(channels, key = { it.id }) { channel ->
                        val isFavorite = favoriteChannels.any { it.id == channel.id }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { checkAndPlayChannel(channel) }
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                ) {
                                    // TV Logo Box
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                shape = MaterialTheme.shapes.small
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (channel.logo != null) {
                                            AsyncImage(
                                                model = channel.logo,
                                                contentDescription = channel.name,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Tv,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Name & Category
                                    Text(
                                        text = channel.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = channel.category,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Favorite Icon overlay (top right corner)
                                IconButton(
                                    onClick = { viewModel.toggleFavorite(channel.id, !isFavorite) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "Favorite",
                                        tint = if (isFavorite) Color(0xFFFFD700) else Color.LightGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Parental Control PIN Verification Dialog
    if (showParentalPrompt) {
        AlertDialog(
            onDismissRequest = {
                showParentalPrompt = false
                parentalInputPin = ""
                parentalPinError = false
                pendingChannelToPlay = null
            },
            title = { Text("Parental Lock PIN Required") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This stream category is protected by parental controls. Enter your 4-digit PIN to play.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = parentalInputPin,
                        onValueChange = { if (it.length <= 4) parentalInputPin = it },
                        label = { Text("Enter PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("parental_prompt_input"),
                        singleLine = true
                    )
                    if (parentalPinError) {
                        Text(
                            text = "Incorrect PIN code. Access Denied.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (viewModel.preferences.verifyPin(parentalInputPin)) {
                            pendingChannelToPlay?.let {
                                viewModel.playChannel(it)
                            }
                            showParentalPrompt = false
                            parentalInputPin = ""
                            parentalPinError = false
                            pendingChannelToPlay = null
                        } else {
                            parentalPinError = true
                        }
                    },
                    modifier = Modifier.testTag("parental_prompt_confirm")
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showParentalPrompt = false
                    parentalInputPin = ""
                    parentalPinError = false
                    pendingChannelToPlay = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // EPG Program Detail Dialog
    if (selectedEpgProgramForDetail != null) {
        val program = selectedEpgProgramForDetail!!
        val sdfDate = remember { SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()) }
        val sdfTime = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
        val dateStr = remember(program.startTime) { sdfDate.format(Date(program.startTime * 1000L)) }
        val startStr = remember(program.startTime) { sdfTime.format(Date(program.startTime * 1000L)) }
        val endStr = remember(program.endTime) { sdfTime.format(Date(program.endTime * 1000L)) }
        val durationMin = (program.endTime - program.startTime) / 60

        AlertDialog(
            onDismissRequest = { selectedEpgProgramForDetail = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = program.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Date & Time Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "$startStr - $endStr ($durationMin min)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Description text
                    Column {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = program.description ?: "No description available for this show.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                val context = LocalContext.current
                val reminders by viewModel.reminders.collectAsState()
                val isSetState = remember(program, reminders) {
                    reminders.contains("${program.channelEpgId}_${program.startTime}")
                }
                val buttonText = if (isSetState) "Remove Reminder" else "Set Reminder"
                val icon = if (isSetState) Icons.Default.NotificationsActive else Icons.Default.Notifications

                Button(
                    onClick = {
                        val isAdded = viewModel.toggleReminder(program)
                        Toast.makeText(
                            context,
                            if (isAdded) "Reminder set for: ${program.title}" else "Reminder removed",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.testTag("epg_detail_dialog_reminder")
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(buttonText)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedEpgProgramForDetail = null },
                    modifier = Modifier.testTag("epg_detail_dialog_close")
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun EpgTimelineView(
    epgId: String,
    viewModel: IPTVViewModel,
    onProgramClick: (EpgProgram) -> Unit,
    modifier: Modifier = Modifier
) {
    val upcomingPrograms by remember(epgId) {
        if (epgId.isNotEmpty()) {
            viewModel.getUpcomingPrograms(epgId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    val now = remember { System.currentTimeMillis() / 1000L }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "EPG TV Guide",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (upcomingPrograms.isNotEmpty()) {
                val liveProgram = upcomingPrograms.firstOrNull { now in it.startTime..it.endTime }
                if (liveProgram != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = CircleShape
                    ) {
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        if (upcomingPrograms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "No EPG data available for this channel.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            val reminders by viewModel.reminders.collectAsState()
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("epg_horizontal_timeline"),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(upcomingPrograms) { program ->
                    val isLive = now in program.startTime..program.endTime
                    val hasReminder = remember(program, reminders) {
                        reminders.contains("${program.channelEpgId}_${program.startTime}")
                    }
                    EpgProgramCard(
                        program = program,
                        isLive = isLive,
                        hasReminder = hasReminder,
                        now = now,
                        onClick = { onProgramClick(program) }
                    )
                }
            }
        }
    }
}

@Composable
fun EpgProgramCard(
    program: EpgProgram,
    isLive: Boolean,
    hasReminder: Boolean,
    now: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val durationMin = (program.endTime - program.startTime) / 60
    val sdf = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val startTimeStr = remember(program.startTime) { sdf.format(Date(program.startTime * 1000L)) }
    val endTimeStr = remember(program.endTime) { sdf.format(Date(program.endTime * 1000L)) }

    Card(
        modifier = modifier
            .width(200.dp)
            .height(115.dp)
            .clickable(onClick = onClick)
            .testTag("epg_card_${program.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isLive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        border = if (isLive) {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = program.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isLive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (hasReminder) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Reminder Set",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(start = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$startTimeStr - $endTimeStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isLive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isLive) {
                val progress = remember(program.startTime, program.endTime) {
                    val total = program.endTime - program.startTime
                    if (total > 0) {
                        val elapsed = now - program.startTime
                        elapsed.toFloat() / total.toFloat()
                    } else {
                        0f
                    }
                }
                Column {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "LIVE (${(progress * 100).toInt()}% done)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Text(
                    text = "$durationMin min",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
