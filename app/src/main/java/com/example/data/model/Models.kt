package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: String, // "M3U" or "XTREAM"
    val url: String, // URL or local file path
    val username: String? = null,
    val password: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val playlistId: Long,
    val name: String,
    val url: String,
    val logo: String?,
    val category: String,
    val streamType: String = "LIVE", // "LIVE", "MOVIE", "SERIES"
    val isFavorite: Boolean = false,
    val epgId: String? = null
)

@Entity(tableName = "watch_history")
data class WatchHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val channelId: Long,
    val channelName: String,
    val channelUrl: String,
    val channelLogo: String?,
    val category: String,
    val streamType: String = "LIVE",
    val watchedAt: Long = System.currentTimeMillis(),
    val playbackPosition: Long = 0L,
    val duration: Long = 0L
)

@Entity(tableName = "epg_programs")
data class EpgProgram(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val channelEpgId: String,
    val title: String,
    val description: String?,
    val startTime: Long, // Unix timestamp in seconds
    val endTime: Long // Unix timestamp in seconds
)
