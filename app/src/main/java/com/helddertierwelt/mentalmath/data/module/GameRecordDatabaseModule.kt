package com.helddertierwelt.mentalmath.data.module

import android.content.Context
import androidx.room.Room
import com.helddertierwelt.mentalmath.data.dao.GameRecordDao
import com.helddertierwelt.mentalmath.data.db.GameRecordDatabase
import com.helddertierwelt.mentalmath.data.repository.GameRecordRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GameRecordDatabaseModule {

    @Provides
    @Singleton
    fun provideGameRecordDatabase(
        @ApplicationContext context: Context
    ): GameRecordDatabase {
        return Room.databaseBuilder(
            context,
            GameRecordDatabase::class.java,
            "game_record.db"
        ).build()
    }

    @Provides
    fun provideGameRecordDao(database: GameRecordDatabase): GameRecordDao {
        return database.gameRecordDao()
    }

    @Provides
    @Singleton
    fun provideGameRecordRepository(gameRecordDao: GameRecordDao): GameRecordRepository {
        return GameRecordRepository(gameRecordDao)
    }
}