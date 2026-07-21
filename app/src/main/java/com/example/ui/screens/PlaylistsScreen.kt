package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.Playlist
import com.example.ui.viewmodel.IPTVViewModel

@Composable
fun PlaylistsScreen(
    viewModel: IPTVViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playlists by viewModel.playlists.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var importType by remember { mutableStateOf("M3U") } // "M3U" or "XTREAM"

    // Inputs for Add M3U Dialog
    var m3uName by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }

    // Inputs for Xtream Codes Dialog
    var xtreamName by remember { mutableStateOf("") }
    var xtreamServer by remember { mutableStateOf("") }
    var xtreamUser by remember { mutableStateOf("") }
    var xtreamPass by remember { mutableStateOf("") }

    // Selected local file URI state
    var selectedLocalFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedLocalFileName by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedLocalFileUri = uri
            selectedLocalFileName = uri.lastPathSegment ?: "selected_playlist.m3u"
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (playlists.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.PlaylistAdd,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Playlists Found",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Import a legally obtained M3U playlist file, web URL, or Xtream Codes stream endpoint to begin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("add_playlist_empty_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Playlist")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Managed Playlists (${playlists.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.testTag("add_playlist_top_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                }

                items(playlists) { playlist ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Type: ${playlist.type}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Endpoint: ${playlist.url}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = {
                                        viewModel.refreshPlaylist(playlist.id)
                                        Toast.makeText(context, "Refreshing ${playlist.name}...", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("refresh_${playlist.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh Feed",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deletePlaylist(playlist) },
                                    modifier = Modifier.testTag("delete_${playlist.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Feed",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Processing Overlay Loader
        if (isImporting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Importing streams and mapping groupings. Please wait...",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }

    // Add Playlist Main Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Import New Playlist") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = importType == "M3U",
                            onClick = { importType = "M3U" },
                            label = { Text("M3U / M3U8 URL") },
                            modifier = Modifier.weight(1f).testTag("chip_m3u")
                        )
                        FilterChip(
                            selected = importType == "XTREAM",
                            onClick = { importType = "XTREAM" },
                            label = { Text("Xtream API Login") },
                            modifier = Modifier.weight(1f).testTag("chip_xtream")
                        )
                    }

                    if (importType == "M3U") {
                        OutlinedTextField(
                            value = m3uName,
                            onValueChange = { m3uName = it },
                            label = { Text("Playlist Label Name") },
                            placeholder = { Text("e.g. Home Cable") },
                            modifier = Modifier.fillMaxWidth().testTag("m3u_name_field"),
                            singleLine = true
                        )

                        Text(
                            text = "Import source option:",
                            style = MaterialTheme.typography.labelSmall
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { filePickerLauncher.launch("*/*") },
                                modifier = Modifier.weight(1f).testTag("pick_file_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (selectedLocalFileUri == null) "Select File" else "File Chosen")
                            }

                            if (selectedLocalFileUri != null) {
                                Button(
                                    onClick = {
                                        selectedLocalFileUri = null
                                        selectedLocalFileName = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(0.4f)
                                ) {
                                    Text("Clear")
                                }
                            }
                        }

                        if (selectedLocalFileUri != null) {
                            Text(
                                text = "Picked: $selectedLocalFileName",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            OutlinedTextField(
                                value = m3uUrl,
                                onValueChange = { m3uUrl = it },
                                label = { Text("M3U/M3U8 Web URL") },
                                placeholder = { Text("https://example.com/playlist.m3u") },
                                modifier = Modifier.fillMaxWidth().testTag("m3u_url_field"),
                                singleLine = true
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = xtreamName,
                            onValueChange = { xtreamName = it },
                            label = { Text("Provider Label Name") },
                            placeholder = { Text("e.g. IPTV Service") },
                            modifier = Modifier.fillMaxWidth().testTag("xtream_name_field"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = xtreamServer,
                            onValueChange = { xtreamServer = it },
                            label = { Text("Server URL Endpoints") },
                            placeholder = { Text("http://domain.com:8080") },
                            modifier = Modifier.fillMaxWidth().testTag("xtream_server_field"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = xtreamUser,
                            onValueChange = { xtreamUser = it },
                            label = { Text("Xtream Account Username") },
                            modifier = Modifier.fillMaxWidth().testTag("xtream_user_field"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = xtreamPass,
                            onValueChange = { xtreamPass = it },
                            label = { Text("Xtream Account Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("xtream_pass_field"),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importType == "M3U") {
                            val localUri = selectedLocalFileUri
                            if (m3uName.isNotEmpty()) {
                                if (localUri != null) {
                                    viewModel.importM3U(m3uName, localUri.toString(), isLocal = true) {
                                        Toast.makeText(context, "M3U File Imported", Toast.LENGTH_SHORT).show()
                                        showAddDialog = false
                                        m3uName = ""
                                        selectedLocalFileUri = null
                                    }
                                } else if (m3uUrl.isNotEmpty()) {
                                    viewModel.importM3U(m3uName, m3uUrl, isLocal = false) {
                                        Toast.makeText(context, "M3U Playlist URL Imported", Toast.LENGTH_SHORT).show()
                                        showAddDialog = false
                                        m3uName = ""
                                        m3uUrl = ""
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (xtreamName.isNotEmpty() && xtreamServer.isNotEmpty() && xtreamUser.isNotEmpty() && xtreamPass.isNotEmpty()) {
                                viewModel.importXtream(xtreamName, xtreamServer, xtreamUser, xtreamPass) {
                                    Toast.makeText(context, "Xtream Account Connected!", Toast.LENGTH_SHORT).show()
                                    showAddDialog = false
                                    xtreamName = ""
                                    xtreamServer = ""
                                    xtreamUser = ""
                                    xtreamPass = ""
                                }
                            } else {
                                Toast.makeText(context, "Please complete all fields", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.testTag("add_confirm_btn")
                ) {
                    Text("Add Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
