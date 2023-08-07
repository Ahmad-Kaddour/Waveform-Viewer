package com.paradoxcat.waveformtest.waveform.data.converter.duration

import javax.inject.Inject
import kotlin.math.floor

class DurationToTimeFrameConverterImpl @Inject constructor(): DurationToTimeFrameConverter {

    /** Converting the duration of the audio into time frames, with minimum interval of 1 second.
     * If the duration is not enough to create time windows of the specified count,
     * the returned list will contain less elements than count.
     * @throws IllegalArgumentException if the duration is negative, or the count is less than 2.
     **/
    override fun convert(duration: Long, count: Int): List<Long> {
        if (duration < 0) {
            throw IllegalArgumentException("duration must be positive")
        }
        if (count < 1) {
            throw IllegalArgumentException("count must be at least 2")
        }
        val frames = mutableListOf<Long>()
        val step = duration / (count - 1).toDouble()
        var currentTime = step

        frames.add(0)
        while (currentTime < duration && step >= 1000){
            frames.add(floor(currentTime).toLong())
            currentTime += step
        }
        if (duration >= 1000) {
            frames.add(duration)
        }

        return frames
    }
}