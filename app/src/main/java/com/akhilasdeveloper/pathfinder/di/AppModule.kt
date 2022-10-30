package com.akhilasdeveloper.pathfinder.di

import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.GenerateMaze
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideGenerateMaze(): GenerateMaze = GenerateMaze()

    @Singleton
    @Provides
    fun provideFindPath(): FindPath = FindPath()

}
