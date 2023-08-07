package com.paradoxcat.waveformtest.waveform.data.converter.rawdata

import java.nio.ByteBuffer

/** Interface for transforming raw audio data into amplitude values **/
interface RawDataConverter {
    /** Transform raw audio into drawable array of integers.
     * @param buffer The byte buffer that contains the samples.
     * @return List of integers represents the extracted samples amplitudes.
     **/
    fun transformRawData(buffer: ByteBuffer): IntArray
}