/*
Mental Math - Android app for practicing mental arithmetic
Copyright (C) 2025 HeldDerTierwelt

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

package com.helddertierwelt.mentalmath.data.db

import android.content.Context
import androidx.room.Room
import com.helddertierwelt.mentalmath.data.dao.AdditionTaskDao
import com.helddertierwelt.mentalmath.data.dao.DivisionTaskDao
import com.helddertierwelt.mentalmath.data.dao.MultiplicationTaskDao
import com.helddertierwelt.mentalmath.data.dao.SubtractionTaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object MathTaskDatabaseModule {

    @Provides
    fun provideMathTaskDatabase(
        @ApplicationContext context: Context
    ): MathTaskDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            MathTaskDatabase::class.java,
            "math_tasks.db"
        ).createFromAsset("math_tasks.db")
            .build()
    }

    @Provides
    fun provideAdditionTaskDao(database: MathTaskDatabase): AdditionTaskDao {
        return database.additionTaskDao()
    }

    @Provides
    fun provideSubtractionTaskDao(database: MathTaskDatabase): SubtractionTaskDao {
        return database.subtractionTaskDao()
    }

    @Provides
    fun provideMultiplicationTaskDao(database: MathTaskDatabase): MultiplicationTaskDao {
        return database.multiplicationTaskDao()
    }

    @Provides
    fun provideDivisionTaskDao(database: MathTaskDatabase): DivisionTaskDao {
        return database.divisionTaskDao()
    }
}