package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.IPTVDatabase
import com.example.data.model.Channel
import com.example.data.model.EpgProgram
import com.example.data.model.Playlist
import com.example.data.model.WatchHistory
import com.example.data.preference.PreferencesManager
import com.example.data.repository.IPTVRepository
import com.example.data.sync.EpgSyncWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class IPTVViewModel(application: Application) : AndroidViewModel(application) {
    private val database = IPTVDatabase.getDatabase(application)
    private val repository = IPTVRepository(database.iptvDao(), application)
    val preferences = PreferencesManager(application)

    // --- Core Data Flows ---
    val playlists: StateFlow<List<Playlist>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteChannels: StateFlow<List<Channel>> = repository.favoriteChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = repository.allCategories
        .map { list -> listOf("All") + list.sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    val watchHistory: StateFlow<List<WatchHistory>> = repository.watchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- State & Filtering ---
    private val _selectedPlaylistId = MutableStateFlow<Long?>(null)
    val selectedPlaylistId = _selectedPlaylistId.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Combined Live Channels Stream based on Selected Playlist, Category, and Search Query
    val filteredChannels: StateFlow<List<Channel>> = combine(
        repository.allChannels,
        _selectedPlaylistId,
        _selectedCategory,
        _searchQuery
    ) { channels, playlistId, category, query ->
        var list = channels
        if (playlistId != null) {
            list = list.filter { it.playlistId == playlistId }
        }
        if (category != "All") {
            list = list.filter { it.category == category }
        }
        if (query.isNotEmpty()) {
            list = list.filter { it.name.contains(query, ignoreCase = true) }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Currently Playing Channel ---
    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel = _currentChannel.asStateFlow()

    private val _currentProgram = MutableStateFlow<EpgProgram?>(null)
    val currentProgram = _currentProgram.asStateFlow()

    // --- Preferences & Theme States ---
    private val _themeMode = MutableStateFlow(preferences.themeMode)
    val themeMode = _themeMode.asStateFlow()

    private val _themeColor = MutableStateFlow(preferences.themeColor)
    val themeColor = _themeColor.asStateFlow()

    private val _bufferingOption = MutableStateFlow(preferences.bufferingOption)
    val bufferingOption = _bufferingOption.asStateFlow()

    private val _parentalEnabled = MutableStateFlow(preferences.parentalEnabled)
    val parentalEnabled = _parentalEnabled.asStateFlow()

    private val _reminders = MutableStateFlow(preferences.reminders)
    val reminders = _reminders.asStateFlow()

    // --- Operations States (Loading, success, error) ---
    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    init {
        // Automatically set the first playlist if available
        viewModelScope.launch {
            playlists.collectLatest { list ->
                if (list.isNotEmpty() && _selectedPlaylistId.value == null) {
                    _selectedPlaylistId.value = list.first().id
                }
            }
        }
    }

    // --- Channel Filtering API ---
    fun selectPlaylist(id: Long?) {
        _selectedPlaylistId.value = id
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Playlist Actions ---
    fun importM3U(name: String, url: String, isLocal: Boolean = false, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isImporting.value = true
            _errorMessage.value = null
            val success = repository.importM3UPlaylist(name, url, isLocal)
            _isImporting.value = false
            if (success) {
                onSuccess()
            } else {
                _errorMessage.value = "Failed to parse M3U. Ensure the file or URL is valid."
            }
        }
    }

    fun importXtream(name: String, server: String, user: String, pass: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isImporting.value = true
            _errorMessage.value = null
            val success = repository.importXtreamPlaylist(name, server, user, pass)
            _isImporting.value = false
            if (success) {
                onSuccess()
            } else {
                _errorMessage.value = "Failed to connect to Xtream Server. Verify your credentials."
            }
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
            if (_selectedPlaylistId.value == playlist.id) {
                _selectedPlaylistId.value = playlists.value.firstOrNull { it.id != playlist.id }?.id
            }
        }
    }

    fun refreshPlaylist(playlistId: Long) {
        viewModelScope.launch {
            _isImporting.value = true
            val success = repository.refreshPlaylist(playlistId)
            _isImporting.value = false
            if (!success) {
                _errorMessage.value = "Failed to refresh playlist."
            }
        }
    }

    // --- Video Player Action ---
    fun playChannel(channel: Channel?) {
        _currentChannel.value = channel
        if (channel == null) {
            _currentProgram.value = null
            return
        }
        viewModelScope.launch {
            repository.addToWatchHistory(channel)
            // Fetch EPG details
            if (channel.epgId != null) {
                _currentProgram.value = repository.getCurrentProgramSync(channel.epgId)
            } else {
                _currentProgram.value = null
            }
        }
    }

    fun saveWatchProgress(channel: Channel, progress: Long, duration: Long) {
        viewModelScope.launch {
            repository.addToWatchHistory(channel, progress, duration)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun toggleFavorite(channelId: Long, isFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(channelId, isFav)
        }
    }

    // --- EPG Actions ---
    fun importEpg(url: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isImporting.value = true
            val success = repository.importEpg(url)
            _isImporting.value = false
            if (success) {
                preferences.epgUrl = url
                EpgSyncWorker.schedule(getApplication())
                onSuccess()
            } else {
                _errorMessage.value = "Failed to load EPG Guide."
            }
        }
    }

    fun getUpcomingPrograms(epgId: String): Flow<List<EpgProgram>> {
        return repository.getUpcomingPrograms(epgId)
    }

    fun toggleReminder(program: EpgProgram): Boolean {
        val key = "${program.channelEpgId}_${program.startTime}"
        val isAdded = preferences.toggleReminder(key)
        _reminders.value = preferences.reminders
        return isAdded
    }

    fun isReminderSet(program: EpgProgram): Boolean {
        val key = "${program.channelEpgId}_${program.startTime}"
        return preferences.isReminderSet(key)
    }

    // --- Settings Updates ---
    fun setThemeMode(mode: String) {
        preferences.themeMode = mode
        _themeMode.value = mode
    }

    fun setThemeColor(color: String) {
        preferences.themeColor = color
        _themeColor.value = color
    }

    fun setBufferingOption(option: String) {
        preferences.bufferingOption = option
        _bufferingOption.value = option
    }

    fun setParentalEnabled(enabled: Boolean, pin: String) {
        preferences.parentalEnabled = enabled
        preferences.parentalPin = pin
        _parentalEnabled.value = enabled
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun createBackup(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val jsonStr = repository.exportBackup()
            onResult(jsonStr)
        }
    }

    fun restoreBackup(jsonStr: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isImporting.value = true
            val success = repository.restoreBackup(jsonStr)
            _isImporting.value = false
            onResult(success)
        }
    }
}
