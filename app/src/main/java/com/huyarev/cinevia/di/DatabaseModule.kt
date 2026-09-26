package com.huyarev.cinevia.di

import android.content.Context
import com.huyarev.cinevia.data.local.CineRevDatabase
import com.huyarev.cinevia.data.local.dao.DiaryDao
import com.huyarev.cinevia.data.local.dao.MovieDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): CineRevDatabase {
        return CineRevDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideMovieDao(database: CineRevDatabase): MovieDao {
        return database.movieDao()
    }

    @Provides
    @Singleton
    fun provideDiaryDao(database: CineRevDatabase): DiaryDao {
        return database.diaryDao()
    }
}
