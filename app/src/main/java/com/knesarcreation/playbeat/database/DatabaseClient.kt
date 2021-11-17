package com.knesarcreation.playbeat.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


@Database(
    entities = [AllSongsModel::class, QueueListModel::class, PlaylistModel::class, AlbumModel::class, ArtistsModel::class],
    version = 2,
    exportSchema = false
)
abstract class DatabaseClient : RoomDatabase() {
    abstract fun allSongsDao(): AllSongsDao
    abstract fun queueListDao(): QueueListDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao

    companion object {

        private val migration_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE allSongsModel ADD COLUMN folderId VARCHAR NOT NULL DEFAULT('')")
                database.execSQL("ALTER TABLE allSongsModel ADD COLUMN folderName VARCHAR Not null default('')")
                database.execSQL("ALTER TABLE allSongsModel ADD COLUMN noOfSongs INTEGER NOT NULL DEFAULT(0)")
            }
        }

        /* private val migration_2_3 = object : Migration(2, 3) {
             override fun migrate(database: SupportSQLiteDatabase) {
                 database.execSQL("ALTER TABLE allSongsModel ADD COLUMN folderId VARCHAR NOT NULL DEFAULT('')")
             }
         }*/

        private var mInstance: DatabaseClient? = null

        @Synchronized
        fun getInstance(mContext: Context): DatabaseClient? {
            if (mInstance == null) {
                mInstance = Room.databaseBuilder(
                    mContext.applicationContext,
                    DatabaseClient::class.java,
                    "PlayBeatDatabase"
                ).addMigrations(migration_1_2)
                    .build()
            }
            return mInstance
        }
    }
}