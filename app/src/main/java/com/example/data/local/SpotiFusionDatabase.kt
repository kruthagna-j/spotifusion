package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    LikedTrackEntity::class,
    PlaylistEntity::class,
    PlaylistTrackEntity::class,
    HistoryEntity::class
  ],
  version = 1,
  exportSchema = false
)
abstract class SpotiFusionDatabase : RoomDatabase() {
  abstract fun musicDao(): MusicDao

  companion object {
    @Volatile
    private var INSTANCE: SpotiFusionDatabase? = null

    fun getDatabase(context: Context): SpotiFusionDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          SpotiFusionDatabase::class.java,
          "spotifusion_database"
        ).fallbackToDestructiveMigration().build()
        INSTANCE = instance
        instance
      }
    }
  }
}
