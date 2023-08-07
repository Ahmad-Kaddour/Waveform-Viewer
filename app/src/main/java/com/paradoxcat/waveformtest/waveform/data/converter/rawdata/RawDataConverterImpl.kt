package com.paradoxcat.waveformtest.waveform.data.converter.rawdata

import java.nio.ByteBuffer
import javax.inject.Inject

class RawDataConverterImpl @Inject constructor() : RawDataConverter {
    override fun transformRawData(buffer: ByteBuffer): IntArray {
        val nSamples = buffer.limit() / 2 // assuming 16-bit PCM mono
        val waveForm = IntArray(nSamples)
        for (i in 1 until buffer.limit() step 2) {
            waveForm[i / 2] = (buffer[i].toInt() shl 8) or buffer[i - 1].toInt()
        }
        return waveForm
    }
}