package com.knesarcreation.playbeat.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(
    entities = [AllSongsModel::class, QueueListModel::class, PlaylistModel::class, AlbumModel::class, ArtistsModel::class],
    version = 1,
    exportSchema = false
)
abstract class DatabaseClient : RoomDatabase() {
    abstract fun allSongsDao(): AllSongsDao
    abstract fun queueListDao(): QueueListDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao

    companion object {
        private var mInstance: DatabaseClient? = null

        @Synchronized
        fun getInstance(mContext: Context): DatabaseClient? {
            if (mInstance == null) {
                mInstance = Room.databaseBuilder(
                    mContext.applicationContext,
                    DatabaseClient::class.java,
                    "PlayBeatDatabase"
                )
                    .fallbackToDestructiveMigration().build()
            }
            return mInstance
        }
    }
}