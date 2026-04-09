package com.helddertierwelt.mentalmath

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.helddertierwelt.mentalmath.data.entity.GameRecord
import com.helddertierwelt.mentalmath.util.SettingsLinkUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class SettingsLinkUtilTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.helddertierwelt.mentalmath", appContext.packageName)
    }

    @Test
    fun testGameStringSerializerAndDeserializer() {
        val original = GameRecord(
            id = 42,
            isModeEnabled = false,
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
            totalScore = 5622.10f,
            seed = 1234567890123456789L,
            solvedProblems = listOf()
        )
        val serialized = SettingsLinkUtil.serialize(original)
        val deserialized = SettingsLinkUtil.deserialize(serialized)

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

        assertEquals(0, deserialized.id)
        assertEquals(0, deserialized.totalAnswers)
        assertEquals(0, deserialized.correctAnswers)
        assertEquals(0L, deserialized.activeTime)
        assertEquals(0L, deserialized.totalTime)
        assertEquals(0f, deserialized.activeScore, 0.0f)
        assertTrue(deserialized.solvedProblems.isEmpty())
    }
}