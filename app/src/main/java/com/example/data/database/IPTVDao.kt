package com.example.data.database

import androidx.room.*
import com.example.data.model.Channel
import com.example.data.model.EpgProgram
import com.example.data.model.Playlist
import com.example.data.model.WatchHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface IPTVDao {

    // --- Playlists ---
    @Query("SELECT * FROM playlists ORDER BY lastUpdated DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): Playlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    // --- Channels ---
    @Query("SELECT * FROM channels")
    fun getAllChannels(): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId")
    fun getChannelsByPlaylist(playlistId: Long): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId")
    suspend fun getChannelsByPlaylistSync(playlistId: Long): List<Channel>

    @Query("SELECT * FROM channels WHERE isFavorite = 1")
    fun getFavoriteChannels(): Flow<List<Channel>>

    @Query("SELECT DISTINCT category FROM channels")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT * FROM channels WHERE category = :category")
    fun getChannelsByCategory(category: String): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE name LIKE '%' || :query || '%'")
    fun searchChannels(query: String): Flow<List<Channel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<Channel>)

    @Query("UPDATE channels SET isFavorite = :isFav WHERE id = :channelId")
    suspend fun updateFavoriteStatus(channelId: Long, isFav: Boolean)

    @Query("UPDATE channels SET isFavorite = 1 WHERE url = :url")
    suspend fun setFavoriteByUrl(url: String)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteChannelsByPlaylist(playlistId: Long)

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun getChannelById(id: Long): Channel?

    // --- Watch History ---
    @Query("SELECT * FROM watch_history ORDER BY watchedAt DESC LIMIT 30")
    fun getWatchHistory(): Flow<List<WatchHistory>>

    @Query("SELECT * FROM watch_history WHERE channelId = :channelId LIMIT 1")
    suspend fun getWatchHistoryByChannel(channelId: Long): WatchHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchHistory(history: WatchHistory)

    @Query("DELETE FROM watch_history WHERE id = :id")
    suspend fun deleteWatchHistoryById(id: Long)

    @Query("DELETE FROM watch_history")
    suspend fun clearWatchHistory()

    // --- EPG Programs ---
    @Query("SELECT * FROM epg_programs WHERE channelEpgId = :epgId AND endTime > :now ORDER BY startTime ASC")
    fun getUpcomingPrograms(epgId: String, now: Long): Flow<List<EpgProgram>>

    @Query("SELECT * FROM epg_programs WHERE channelEpgId = :epgId AND startTime <= :now AND endTime >= :now LIMIT 1")
    suspend fun getCurrentProgramSync(epgId: String, now: Long): EpgProgram?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEpgPrograms(programs: List<EpgProgram>)

    @Query("DELETE FROM epg_programs WHERE endTime < :cutoff")
    suspend fun clearOldEpg(cutoff: Long)
}
