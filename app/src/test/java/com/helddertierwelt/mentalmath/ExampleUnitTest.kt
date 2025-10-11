package com.helddertierwelt.mentalmath

import com.helddertierwelt.mentalmath.data.entity.GameRecord
import com.helddertierwelt.mentalmath.util.GameStringSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testGameStringSerializer_roundTrip() {
        val original = GameRecord(
            id = 42,
            isModeEnabled = true,
            modeLimit = 5,
            additionRangeStart = 2,
            additionRangeEnd = 7,
            subtractionRangeStart = 1,
            subtractionRangeEnd = 8,
            multiplicationRangeStart = 3,
            multiplicationRangeEnd = 6,
            divisionRangeStart = 4,
            divisionRangeEnd = 9,
            totalAnswers = 123,
            correctAnswers = 111,
            activeTime = 5555L,
            totalTime = 9999L,
            activeScore = 12.34f,
            totalScore = 56.78f,
            seed = 1234567890123456789L,
            taskResults = listOf() // wird nicht serialisiert
        )
        val serialized = GameStringSerializer.serialize(original)
        val deserialized = GameStringSerializer.deserialize(serialized)

        // Vergleiche nur die serialisierten Felder
        assertEquals(original.isModeEnabled, deserialized.isModeEnabled)
        assertEquals(original.modeLimit, deserialized.modeLimit)
        assertEquals(original.additionRangeStart, deserialized.additionRangeStart)
        assertEquals(original.additionRangeEnd, deserialized.additionRangeEnd)
        assertEquals(original.subtractionRangeStart, deserialized.subtractionRangeStart)
        assertEquals(original.subtractionRangeEnd, deserialized.subtractionRangeEnd)
        assertEquals(original.multiplicationRangeStart, deserialized.multiplicationRangeStart)
        assertEquals(original.multiplicationRangeEnd, deserialized.multiplicationRangeEnd)
        assertEquals(original.divisionRangeStart, deserialized.divisionRangeStart)
        assertEquals(original.divisionRangeEnd, deserialized.divisionRangeEnd)
        assertEquals(original.totalScore, deserialized.totalScore)
        assertEquals(original.seed, deserialized.seed)

        // Nicht serialisierte Felder sollten Defaultwerte haben
        assertEquals(0, deserialized.id)
        assertEquals(0, deserialized.totalAnswers)
        assertEquals(0, deserialized.correctAnswers)
        assertEquals(0L, deserialized.activeTime)
        assertEquals(0L, deserialized.totalTime)
        assertEquals(0f, deserialized.activeScore, 0.0f)
        assertTrue(deserialized.taskResults.isEmpty())
    }
}