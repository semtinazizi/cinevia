package com.huyarev.cinevia.di

import com.huyarev.cinevia.network.RetrofitInstance
import com.huyarev.cinevia.network.TmdbApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideTmdbApi(): TmdbApi {
        return RetrofitInstance.api
    }
}
