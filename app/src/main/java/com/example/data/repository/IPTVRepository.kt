package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.database.IPTVDao
import com.example.data.database.IPTVDatabase
import com.example.data.model.Channel
import com.example.data.model.EpgProgram
import com.example.data.model.Playlist
import com.example.data.model.WatchHistory
import com.example.data.parser.EpgParser
import com.example.data.parser.PlaylistParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class IPTVRepository(
    private val dao: IPTVDao,
    private val context: Context
) {
    val allPlaylists: Flow<List<Playlist>> = dao.getAllPlaylists()
    val allChannels: Flow<List<Channel>> = dao.getAllChannels()
    val favoriteChannels: Flow<List<Channel>> = dao.getFavoriteChannels()
    val allCategories: Flow<List<String>> = dao.getAllCategories()
    val watchHistory: Flow<List<WatchHistory>> = dao.getWatchHistory()

    suspend fun getPlaylistById(id: Long): Playlist? = dao.getPlaylistById(id)

    fun getChannelsByPlaylist(playlistId: Long): Flow<List<Channel>> =
        dao.getChannelsByPlaylist(playlistId)

    fun getChannelsByCategory(category: String): Flow<List<Channel>> =
        dao.getChannelsByCategory(category)

    fun searchChannels(query: String): Flow<List<Channel>> =
        dao.searchChannels(query)

    suspend fun getChannelById(id: Long): Channel? =
        dao.getChannelById(id)

    // --- Playlist Import & Update ---
    suspend fun importM3UPlaylist(name: String, url: String, isLocalFile: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        try {
            val playlist = Playlist(name = name, type = "M3U", url = url)
            val playlistId = dao.insertPlaylist(playlist)

            val m3uContent = if (isLocalFile) {
                // Read from local uri/path string or asset
                context.contentResolver.openInputStream(android.net.Uri.parse(url))?.use { input ->
                    input.bufferedReader().use { it.readText() }
                } ?: ""
            } else {
                PlaylistParser.downloadUrl(url)
            }

            if (m3uContent.isNotEmpty()) {
                val channels = PlaylistParser.parseM3U(m3uContent, playlistId)
                if (channels.isNotEmpty()) {
                    dao.insertChannels(channels)
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            Log.e("IPTVRepository", "Failed to import M3U playlist", e)
        }
        false
    }

    suspend fun importXtreamPlaylist(name: String, baseUrl: String, user: String, pass: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val playlist = Playlist(
                name = name,
                type = "XTREAM",
                url = baseUrl,
                username = user,
                password = pass
            )
            val playlistId = dao.insertPlaylist(playlist)

            val channels = PlaylistParser.fetchXtreamChannels(baseUrl, user, pass, playlistId)
            if (channels.isNotEmpty()) {
                dao.insertChannels(channels)
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e("IPTVRepository", "Failed to import Xtream playlist", e)
        }
        false
    }

    suspend fun deletePlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        dao.deleteChannelsByPlaylist(playlist.id)
        dao.deletePlaylist(playlist)
    }

    suspend fun refreshPlaylist(playlistId: Long): Boolean = withContext(Dispatchers.IO) {
        val playlist = dao.getPlaylistById(playlistId) ?: return@withContext false
        try {
            dao.deleteChannelsByPlaylist(playlist.id)
            val channels = if (playlist.type == "XTREAM") {
                PlaylistParser.fetchXtreamChannels(
                    playlist.url,
                    playlist.username ?: "",
                    playlist.password ?: "",
                    playlist.id
                )
            } else {
                val content = if (playlist.url.startsWith("content://") || playlist.url.startsWith("file://")) {
                    context.contentResolver.openInputStream(android.net.Uri.parse(playlist.url))?.use { input ->
                        input.bufferedReader().use { it.readText() }
                    } ?: ""
                } else {
                    PlaylistParser.downloadUrl(playlist.url)
                }
                PlaylistParser.parseM3U(content, playlist.id)
            }

            if (channels.isNotEmpty()) {
                dao.insertChannels(channels)
                dao.updatePlaylist(playlist.copy(lastUpdated = System.currentTimeMillis()))
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e("IPTVRepository", "Error refreshing playlist ${playlist.name}", e)
        }
        false
    }

    // --- Favorites ---
    suspend fun toggleFavorite(channelId: Long, isFavorite: Boolean) {
        dao.updateFavoriteStatus(channelId, isFavorite)
    }

    // --- Watch History ---
    suspend fun addToWatchHistory(channel: Channel, playbackPos: Long = 0L, duration: Long = 0L) {
        val entry = WatchHistory(
            channelId = channel.id,
            channelName = channel.name,
            channelUrl = channel.url,
            channelLogo = channel.logo,
            category = channel.category,
            streamType = channel.streamType,
            watchedAt = System.currentTimeMillis(),
            playbackPosition = playbackPos,
            duration = duration
        )
        dao.insertWatchHistory(entry)
    }

    suspend fun clearHistory() {
        dao.clearWatchHistory()
    }

    // --- XMLTV EPG Support ---
    suspend fun importEpg(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val xmlContent = PlaylistParser.downloadUrl(url)
            if (xmlContent.isNotEmpty()) {
                val programs = EpgParser.parseEpg(xmlContent)
                if (programs.isNotEmpty()) {
                    dao.insertEpgPrograms(programs)
                    // Clear older than 1 day
                    val cutoff = (System.currentTimeMillis() / 1000L) - 86400L
                    dao.clearOldEpg(cutoff)
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            Log.e("IPTVRepository", "Failed to import EPG", e)
        }
        false
    }

    fun getUpcomingPrograms(epgId: String): Flow<List<EpgProgram>> {
        val now = System.currentTimeMillis() / 1000L
        return dao.getUpcomingPrograms(epgId, now)
    }

    suspend fun getCurrentProgramSync(epgId: String): EpgProgram? {
        val now = System.currentTimeMillis() / 1000L
        return dao.getCurrentProgramSync(epgId, now)
    }

    // --- Backup & Restore ---
    suspend fun exportBackup(): String = withContext(Dispatchers.IO) {
        val json = JSONObject()
        try {
            // Get playlists directly as list instead of flow if needed, but since we have flow, let's read the current list
            // For backup, we can load synchronized from DB
            // Let's query playlists and favorites
            val playlistsArray = JSONArray()
            val playlistsList = mutableListOf<Playlist>()
            // We can query the database directly
            val db = IPTVDatabase.getDatabase(context)
            val playlists = db.query("SELECT * FROM playlists", null)
            if (playlists.moveToFirst()) {
                do {
                    val pObj = JSONObject()
                    pObj.put("name", playlists.getString(playlists.getColumnIndexOrThrow("name")))
                    pObj.put("type", playlists.getString(playlists.getColumnIndexOrThrow("type")))
                    pObj.put("url", playlists.getString(playlists.getColumnIndexOrThrow("url")))
                    pObj.put("username", playlists.getString(playlists.getColumnIndexOrThrow("username")))
                    pObj.put("password", playlists.getString(playlists.getColumnIndexOrThrow("password")))
                    playlistsArray.put(pObj)
                } while (playlists.moveToNext())
            }
            playlists.close()
            json.put("playlists", playlistsArray)

            // Export favorites URLs
            val favoritesArray = JSONArray()
            val favorites = db.query("SELECT url FROM channels WHERE isFavorite = 1", null)
            if (favorites.moveToFirst()) {
                do {
                    favoritesArray.put(favorites.getString(0))
                } while (favorites.moveToNext())
            }
            favorites.close()
            json.put("favorites", favoritesArray)
        } catch (e: Exception) {
            Log.e("IPTVRepository", "Error exporting backup", e)
        }
        json.toString()
    }

    suspend fun restoreBackup(backupJson: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject(backupJson)
            val playlistsArray = json.optJSONArray("playlists") ?: return@withContext false
            val favoritesArray = json.optJSONArray("favorites")

            for (i in 0 until playlistsArray.length()) {
                val pObj = playlistsArray.getJSONObject(i)
                val name = pObj.getString("name")
                val type = pObj.getString("type")
                val url = pObj.getString("url")
                val username = pObj.optString("username")
                val password = pObj.optString("password")

                if (type == "XTREAM") {
                    importXtreamPlaylist(name, url, username, password)
                } else {
                    importM3UPlaylist(name, url)
                }
            }

            // Restore favorites by matching URLs
            if (favoritesArray != null && favoritesArray.length() > 0) {
                for (i in 0 until favoritesArray.length()) {
                    val favUrl = favoritesArray.getString(i)
                    dao.setFavoriteByUrl(favUrl)
                }
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e("IPTVRepository", "Error restoring backup", e)
        }
        false
    }
}
