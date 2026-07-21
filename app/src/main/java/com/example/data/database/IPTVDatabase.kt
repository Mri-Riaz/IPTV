package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Channel
import com.example.data.model.EpgProgram
import com.example.data.model.Playlist
import com.example.data.model.WatchHistory

@Database(
    entities = [Playlist::class, Channel::class, WatchHistory::class, EpgProgram::class],
    version = 1,
    exportSchema = false
)
abstract class IPTVDatabase : RoomDatabase() {
    abstract fun iptvDao(): IPTVDao

    companion object {
        @Volatile
        private var INSTANCE: IPTVDatabase? = null

        fun getDatabase(context: Context): IPTVDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IPTVDatabase::class.java,
                    "novastream_iptv_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
