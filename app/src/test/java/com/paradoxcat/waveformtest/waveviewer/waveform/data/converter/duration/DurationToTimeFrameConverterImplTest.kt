package com.paradoxcat.waveformtest.waveviewer.waveform.data.converter.duration


import com.paradoxcat.waveformtest.waveform.data.converter.duration.DurationToTimeFrameConverterImpl
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class DurationToTimeFrameConverterImplTest {
    private lateinit var durationToTimeFrameConverterImpl: DurationToTimeFrameConverterImpl

    @Before
    fun init() {
        durationToTimeFrameConverterImpl = DurationToTimeFrameConverterImpl()
    }

    @Test
    fun testConvertingDuration() {
        val duration = 1000 * 1000L
        val result = durationToTimeFrameConverterImpl.convert(duration, 5)
        Assert.assertEquals(5, result.size)
        Assert.assertEquals(0, result[0])
        Assert.assertEquals(duration, result[4])
    }

    @Test(expected = IllegalArgumentException::class)
    fun testConvertingDuration_negativeDuration() {
        val result = durationToTimeFrameConverterImpl.convert(-1000, 5)
        Assert.assertEquals(5, result.size)
    }

    @Test
    fun testConvertingDuration_zeroDuration() {
        val result = durationToTimeFrameConverterImpl.convert(0, 5)
        Assert.assertEquals(1, result.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testConvertingDuration_zeroCount() {
        val result = durationToTimeFrameConverterImpl.convert(1000, 0)
        Assert.assertEquals(0, result.size)
    }
}