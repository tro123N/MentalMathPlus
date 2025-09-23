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
import java.io.File
import java.io.FileOutputStream

class MathTaskDatabaseImporter(private val context: Context) {

    fun importDb(): MathTaskDatabase {

        val dbFileName = "math_tasks.db"
        val dbPath = context.filesDir.absolutePath + "/$dbFileName"
        val dbFile = File(dbPath)

        if (!dbFile.exists()) {
            context.assets.open(dbFileName).use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
        }

        return Room.databaseBuilder(
            context.applicationContext,
            MathTaskDatabase::class.java,
            dbFile.absolutePath
        ).createFromFile(dbFile)
            .build()
    }
}