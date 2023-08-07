package com.paradoxcat.waveformtest.waveform.data.extractor

import android.net.Uri
import java.nio.ByteBuffer

/**
 * Interface to extract raw samples data from file uri.
 */
interface RawWaveDataExtractor {
    suspend fun extractData(uri: Uri): ByteBuffer
}