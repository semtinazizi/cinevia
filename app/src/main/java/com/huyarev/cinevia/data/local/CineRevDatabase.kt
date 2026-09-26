package com.huyarev.cinevia.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.huyarev.cinevia.data.local.dao.DiaryDao
import com.huyarev.cinevia.data.local.dao.MovieDao
import com.huyarev.cinevia.data.local.entity.DiaryEntity
import com.huyarev.cinevia.data.local.entity.MovieEntity

@Database(entities = [MovieEntity::class, DiaryEntity::class], version = 2)
abstract class CineRevDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun diaryDao(): DiaryDao

    companion object {
        @Volatile
        private var INSTANCE: CineRevDatabase? = null

        fun getDatabase(context: Context): CineRevDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CineRevDatabase::class.java,
                    "CineRev_database"
                )
                .fallbackToDestructiveMigration() // Şema değiştiğinde eski verileri silip yenisini kurar (Geliştirme aşamasında kolaylık sağlar)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
