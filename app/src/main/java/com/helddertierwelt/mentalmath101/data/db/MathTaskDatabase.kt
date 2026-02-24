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

package com.helddertierwelt.mentalmath101.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.helddertierwelt.mentalmath101.data.dao.AdditionTaskDao
import com.helddertierwelt.mentalmath101.data.dao.DivisionTaskDao
import com.helddertierwelt.mentalmath101.data.dao.MultiplicationTaskDao
import com.helddertierwelt.mentalmath101.data.dao.SubtractionTaskDao
import com.helddertierwelt.mentalmath101.data.entity.AdditionTask
import com.helddertierwelt.mentalmath101.data.entity.DivisionTask
import com.helddertierwelt.mentalmath101.data.entity.MultiplicationTask
import com.helddertierwelt.mentalmath101.data.entity.SubtractionTask

@Database(
    entities = [
        AdditionTask::class,
        SubtractionTask::class,
        MultiplicationTask::class,
        DivisionTask::class
    ],
    version = 101
)
abstract class MathTaskDatabase : RoomDatabase() {
    abstract fun additionTaskDao(): AdditionTaskDao
    abstract fun subtractionTaskDao(): SubtractionTaskDao
    abstract fun multiplicationTaskDao(): MultiplicationTaskDao
    abstract fun divisionTaskDao(): DivisionTaskDao
}