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

package com.helddertierwelt.mentalmath.data.module

import android.content.Context
import androidx.room.Room
import com.helddertierwelt.mentalmath.data.dao.AdditionProblemDao
import com.helddertierwelt.mentalmath.data.dao.DivisionProblemDao
import com.helddertierwelt.mentalmath.data.dao.MultiplicationProblemDao
import com.helddertierwelt.mentalmath.data.dao.SubtractionProblemDao
import com.helddertierwelt.mentalmath.data.db.MathProblemDatabase
import com.helddertierwelt.mentalmath.data.repository.MathProblemRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MathProblemDatabaseModule {

    @Provides
    fun provideMathProblemDatabase(
        @ApplicationContext context: Context
    ): MathProblemDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            MathProblemDatabase::class.java,
            "math_problems.db"
        ).createFromAsset("math_problems.db")
            .build()
    }

    @Provides
    fun provideAdditionProblemDao(database: MathProblemDatabase): AdditionProblemDao {
        return database.additionProblemDao()
    }

    @Provides
    fun provideSubtractionProblemDao(database: MathProblemDatabase): SubtractionProblemDao {
        return database.subtractionProblemDao()
    }

    @Provides
    fun provideMultiplicationProblemDao(database: MathProblemDatabase): MultiplicationProblemDao {
        return database.multiplicationProblemDao()
    }

    @Provides
    fun provideDivisionProblemDao(database: MathProblemDatabase): DivisionProblemDao {
        return database.divisionProblemDao()
    }

    @Provides
    @Singleton
    fun provideMathProblemRepository(
        additionProblemDao: AdditionProblemDao,
        subtractionProblemDao: SubtractionProblemDao,
        multiplicationProblemDao: MultiplicationProblemDao,
        divisionProblemDao: DivisionProblemDao
    ): MathProblemRepository {
        return MathProblemRepository(
            additionProblemDao,
            subtractionProblemDao,
            multiplicationProblemDao,
            divisionProblemDao
        )
    }
}