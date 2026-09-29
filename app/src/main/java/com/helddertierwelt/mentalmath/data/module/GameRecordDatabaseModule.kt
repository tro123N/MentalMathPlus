/*
Mental Math - Android app for practicing mental arithmetic
Copyright (C) 2025-2026 HeldDerTierwelt

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see https://www.gnu.org/licenses/gpl-3.0.md.
*/

package com.helddertierwelt.mentalmath.data.module

import android.content.Context
import androidx.room.Room
import com.helddertierwelt.mentalmath.data.dao.GameRecordDao
import com.helddertierwelt.mentalmath.data.db.GameRecordDatabase
import com.helddertierwelt.mentalmath.data.module.GameRecordMigrations.MIGRATION_1_2
import com.helddertierwelt.mentalmath.data.module.GameRecordMigrations.MIGRATION_2_3
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
        ).addMigrations(
            MIGRATION_1_2,
            MIGRATION_2_3
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