package com.paradoxcat.waveformtest.waveform.data.converter.duration

/** Interface for converting audio file duration into several time frames**/
interface DurationToTimeFrameConverter {
    /** Converting the duration of the audio into time frames.
     * @param duration -- Duration of the audio in milliseconds.
     * @param count -- The count of the time frames to be split into.
     * @return List contains the time frames in milliseconds.
     **/
    fun convert(duration: Long, count: Int): List<Long>
}