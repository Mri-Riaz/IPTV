package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.IPTVViewModel

@Composable
fun SettingsScreen(
    viewModel: IPTVViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val currentThemeMode by viewModel.themeMode.collectAsState()
    val currentThemeColor by viewModel.themeColor.collectAsState()
    val currentBufferingOption by viewModel.bufferingOption.collectAsState()
    val isParentalEnabled by viewModel.parentalEnabled.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    var showBackupDialog by remember { mutableStateOf(false) }
    var backupText by remember { mutableStateOf("") }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreInput by remember { mutableStateOf("") }

    var showEpgDialog by remember { mutableStateOf(false) }
    var epgUrlInput by remember { mutableStateOf("") }

    var selectedPlaylistType by remember { mutableStateOf("M3U") }
    var playlistLabel by remember { mutableStateOf("") }
    var m3uUrlStr by remember { mutableStateOf("") }

    var xtreamServerUrl by remember { mutableStateOf("") }
    var xtreamUsername by remember { mutableStateOf("") }
    var xtreamPassword by remember { mutableStateOf("") }

    var pickedFileUri by remember { mutableStateOf<Uri?>(null) }
    var pickedFileName by remember { mutableStateOf("") }

    val settingsFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pickedFileUri = uri
            pickedFileName = uri.lastPathSegment ?: "selected_playlist.m3u"
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        Text(
            text = "App Customization",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Theme Mode Selector
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text(text = "Aesthetic Theme Mode", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("DARK", "LIGHT", "SYSTEM").forEach { mode ->
                        FilterChip(
                            selected = currentThemeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = { Text(mode) },
                            modifier = Modifier.weight(1f).testTag("theme_mode_$mode")
                        )
                    }
                }
            }
        }

        // Color Accent Theme Selector
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text(text = "Color Accent Theme", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val colors = listOf(
                        "COSMIC_BLUE" to "Cosmic Blue",
                        "MYSTIC_PURPLE" to "Purple",
                        "SUNSET_ORANGE" to "Orange",
                        "MINT_GREEN" to "Mint"
                    )
                    colors.forEach { (key, label) ->
                        ElevatedButton(
                            onClick = { viewModel.setThemeColor(key) },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = if (currentThemeColor == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                contentColor = if (currentThemeColor == key) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f).testTag("theme_color_$key"),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(text = label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        Text(
            text = "Streaming Settings",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Buffering Options Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text(
                    text = "Player Buffering Strategy",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Configure ExoPlayer latency options depending on your internet stability.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val options = listOf(
                        "LOW" to "Low Latency",
                        "NORMAL" to "Standard",
                        "HIGH" to "Stable Buffer"
                    )
                    options.forEach { (key, label) ->
                        FilterChip(
                            selected = currentBufferingOption == key,
                            onClick = { viewModel.setBufferingOption(key) },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f).testTag("buffer_chip_$key")
                        )
                    }
                }
            }
        }

        // XMLTV EPG Support Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEpgDialog = true }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "XMLTV EPG Guide Feed", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Import XMLTV guide URLs to populate program times slots dynamically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(imageVector = Icons.Default.CloudDownload, contentDescription = "Import EPG")
            }
        }

        Text(
            text = "Playlists & IPTV Sources",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Dynamic Playlist Setup Form Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add New IPTV Dynamic Source",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedPlaylistType == "M3U",
                        onClick = { selectedPlaylistType = "M3U" },
                        label = { Text("M3U / M3U8") },
                        modifier = Modifier.weight(1f).testTag("settings_chip_m3u")
                    )
                    FilterChip(
                        selected = selectedPlaylistType == "XTREAM",
                        onClick = { selectedPlaylistType = "XTREAM" },
                        label = { Text("Xtream API") },
                        modifier = Modifier.weight(1f).testTag("settings_chip_xtream")
                    )
                }

                if (selectedPlaylistType == "M3U") {
                    OutlinedTextField(
                        value = playlistLabel,
                        onValueChange = { playlistLabel = it },
                        label = { Text("Playlist Name") },
                        placeholder = { Text("e.g. My Premium Playlist") },
                        modifier = Modifier.fillMaxWidth().testTag("settings_playlist_name_field"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "File Upload or Web URL Endpoint:",
                        style = MaterialTheme.typography.labelMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { settingsFilePicker.launch("*/*") },
                            modifier = Modifier.weight(1f).testTag("settings_pick_file_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (pickedFileUri == null) "Choose M3U File" else "File Chosen")
                        }

                        if (pickedFileUri != null) {
                            IconButton(
                                onClick = {
                                    pickedFileUri = null
                                    pickedFileName = ""
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear selected file",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    if (pickedFileUri != null) {
                        Text(
                            text = "Picked file: $pickedFileName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    } else {
                        OutlinedTextField(
                            value = m3uUrlStr,
                            onValueChange = { m3uUrlStr = it },
                            label = { Text("M3U Playlist URL") },
                            placeholder = { Text("https://example.com/playlist.m3u") },
                            modifier = Modifier.fillMaxWidth().testTag("settings_playlist_url_field"),
                            singleLine = true,
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Link, contentDescription = null)
                            }
                        )
                    }

                } else {
                    OutlinedTextField(
                        value = playlistLabel,
                        onValueChange = { playlistLabel = it },
                        label = { Text("Provider Name") },
                        placeholder = { Text("e.g. Xtream Service") },
                        modifier = Modifier.fillMaxWidth().testTag("settings_xtream_name_field"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = xtreamServerUrl,
                        onValueChange = { xtreamServerUrl = it },
                        label = { Text("Server URL") },
                        placeholder = { Text("http://example.com:8080") },
                        modifier = Modifier.fillMaxWidth().testTag("settings_xtream_server_field"),
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Link, contentDescription = null)
                        }
                    )

                    OutlinedTextField(
                        value = xtreamUsername,
                        onValueChange = { xtreamUsername = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth().testTag("settings_xtream_user_field"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = xtreamPassword,
                        onValueChange = { xtreamPassword = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("settings_xtream_pass_field"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (playlistLabel.trim().isEmpty()) {
                            Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (selectedPlaylistType == "M3U") {
                            val localUri = pickedFileUri
                            if (localUri != null) {
                                viewModel.importM3U(playlistLabel, localUri.toString(), isLocal = true) {
                                    Toast.makeText(context, "M3U File dynamic playlist added successfully!", Toast.LENGTH_SHORT).show()
                                    playlistLabel = ""
                                    pickedFileUri = null
                                    pickedFileName = ""
                                }
                            } else if (m3uUrlStr.trim().isNotEmpty()) {
                                viewModel.importM3U(playlistLabel, m3uUrlStr, isLocal = false) {
                                    Toast.makeText(context, "M3U Web URL dynamic playlist added successfully!", Toast.LENGTH_SHORT).show()
                                    playlistLabel = ""
                                    m3uUrlStr = ""
                                }
                            } else {
                                Toast.makeText(context, "Please choose a file or input a web URL", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (xtreamServerUrl.isNotEmpty() && xtreamUsername.isNotEmpty() && xtreamPassword.isNotEmpty()) {
                                viewModel.importXtream(playlistLabel, xtreamServerUrl, xtreamUsername, xtreamPassword) {
                                    Toast.makeText(context, "Xtream playlist added successfully!", Toast.LENGTH_SHORT).show()
                                    playlistLabel = ""
                                    xtreamServerUrl = ""
                                    xtreamUsername = ""
                                    xtreamPassword = ""
                                }
                            } else {
                                Toast.makeText(context, "Please complete all fields", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("settings_add_playlist_submit_btn")
                ) {
                    Icon(imageVector = Icons.Default.PlaylistAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import & Map Playlist")
                }
            }
        }

        // List of Active Playlists in settings
        if (playlists.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Active Playlists",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    playlists.forEach { pl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = pl.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${pl.type} | ${pl.url}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        viewModel.refreshPlaylist(pl.id)
                                        Toast.makeText(context, "Refreshing ${pl.name}...", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(36.dp).testTag("settings_refresh_${pl.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh Feed",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deletePlaylist(pl) },
                                    modifier = Modifier.size(36.dp).testTag("settings_delete_${pl.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Feed",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "Security & Privacy",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Parental Lock Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPinDialog = true }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Parental Controls PIN", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = if (isParentalEnabled) "Parental lock is ACTIVE. Streams require PIN." else "Parental lock is DISABLED.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isParentalEnabled,
                    onCheckedChange = { showPinDialog = true },
                    modifier = Modifier.testTag("parental_switch")
                )
            }
        }

        Text(
            text = "Maintenance",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Backup & Restore Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    viewModel.createBackup { json ->
                        backupText = json
                        showBackupDialog = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.weight(1f).testTag("backup_button")
            ) {
                Icon(imageVector = Icons.Default.Backup, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Backup")
            }

            Button(
                onClick = { showRestoreDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                modifier = Modifier.weight(1f).testTag("restore_button")
            ) {
                Icon(imageVector = Icons.Default.Restore, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import Backup")
            }
        }

        // Wipe History Button
        OutlinedButton(
            onClick = {
                viewModel.clearHistory()
                Toast.makeText(context, "Watch History Wiped successfully", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().testTag("wipe_history_button")
        ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Clear Recently Watched Streams")
        }
    }

    // --- DIALOGS ---

    // XMLTV EPG Import Dialog
    if (showEpgDialog) {
        AlertDialog(
            onDismissRequest = { showEpgDialog = false },
            title = { Text("Import XMLTV EPG Feed") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter the URL of your legally obtained XMLTV EPG provider (.xml or .xml.gz).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = epgUrlInput,
                        onValueChange = { epgUrlInput = it },
                        label = { Text("XMLTV URL") },
                        placeholder = { Text("http://example.com/guide.xml") },
                        modifier = Modifier.fillMaxWidth().testTag("epg_url_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (epgUrlInput.isNotEmpty()) {
                            viewModel.importEpg(epgUrlInput) {
                                Toast.makeText(context, "EPG Guide Loaded Successfully", Toast.LENGTH_SHORT).show()
                            }
                            showEpgDialog = false
                        }
                    },
                    modifier = Modifier.testTag("epg_confirm_button")
                ) {
                    Text("Load Guide")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEpgDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Parental PIN Control Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                pinInput = ""
                pinError = false
            },
            title = { Text(if (isParentalEnabled) "Disable Parental Locks" else "Setup / Enable Parental Locks") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (isParentalEnabled) "Enter your current PIN to unlock stream controls." else "Set a 4-digit numeric PIN to protect channels and categories.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4) pinInput = it },
                        label = { Text("PIN Lock") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("parental_pin_input"),
                        singleLine = true
                    )
                    if (pinError) {
                        Text(
                            text = "Incorrect PIN code. Try again.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length == 4) {
                            if (isParentalEnabled) {
                                if (viewModel.preferences.verifyPin(pinInput)) {
                                    viewModel.setParentalEnabled(false, "")
                                    showPinDialog = false
                                    pinInput = ""
                                    pinError = false
                                } else {
                                    pinError = true
                                }
                            } else {
                                viewModel.setParentalEnabled(true, pinInput)
                                showPinDialog = false
                                pinInput = ""
                                pinError = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("parental_confirm_button")
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinDialog = false
                    pinInput = ""
                    pinError = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export Backup Dialog
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Exported Backup Data") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Copy this text block to preserve your saved playlists and favorites configuration.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = backupText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("novastream_iptv_backup", backupText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                        showBackupDialog = false
                    },
                    modifier = Modifier.testTag("copy_backup_button")
                ) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Import Backup Dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore From Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Paste a valid NovaStream backup JSON block below to restore your playlists.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = restoreInput,
                        onValueChange = { restoreInput = it },
                        placeholder = { Text("{ \"playlists\": [...] }") },
                        modifier = Modifier.fillMaxWidth().height(150.dp).testTag("restore_input"),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreInput.isNotEmpty()) {
                            viewModel.restoreBackup(restoreInput) { success ->
                                if (success) {
                                    Toast.makeText(context, "Configuration restored successfully", Toast.LENGTH_SHORT).show()
                                    showRestoreDialog = false
                                } else {
                                    Toast.makeText(context, "Failed to parse backup text.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("restore_confirm_button")
                ) {
                    Text("Restore Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Processing Overlay Loader
    if (isImporting) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Importing streams and mapping groupings. Please wait...",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}
}
