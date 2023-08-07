package com.paradoxcat.waveformtest.waveform.data.extractor

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioFormat
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import com.paradoxcat.waveformtest.ui.main.MainActivity
import com.paradoxcat.waveformtest.waveform.data.extractor.validator.MediaDataValidator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import javax.inject.Inject

/**
 * RawWaveDataExtractor implementation based on MediaExtractor class to extract the data.
 * @see MediaExtractor
 */
class WaveDataFromMediaExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaDataValidator: MediaDataValidator
) : RawWaveDataExtractor {

    override suspend fun extractData(uri: Uri): ByteBuffer {
        return withContext(Dispatchers.IO) {
            val assetFileDescriptor = context.contentResolver.openAssetFileDescriptor(uri, "r")
            if (assetFileDescriptor == null) throw IllegalStateException("uri passed is not valid")

            // allocate a buffer
            val fileSize = getFileSize(assetFileDescriptor)
            val rawAudioBuffer = ByteBuffer.allocate(fileSize.toInt())

            // extract raw audio samples from file into the buffer
            val mediaExtractor = MediaExtractor()
            mediaExtractor.setDataSource(assetFileDescriptor)
            assetFileDescriptor.close()

            // Assuming single track, PCM, 1-channel, and 16-bit format in the buffer.
            mediaDataValidator.isValid(mediaExtractor)

            // dump raw data into a buffer
            mediaExtractor.selectTrack(0)
            var bufferSize = 0
            var samplesRead = mediaExtractor.readSampleData(rawAudioBuffer, 0)
            while (samplesRead > 0) {
                bufferSize += samplesRead
                mediaExtractor.advance()
                samplesRead = mediaExtractor.readSampleData(rawAudioBuffer, bufferSize)
            }
            mediaExtractor.release()

            return@withContext rawAudioBuffer
        }
    }

    private fun getFileSize(assetFileDescriptor: AssetFileDescriptor): Long {
        var fileSize = assetFileDescriptor.length // in bytes
        if (fileSize == AssetFileDescriptor.UNKNOWN_LENGTH) {
            fileSize =
                30 * 1024 * 1024 // 30 MB would accommodate ~6 minutes of 44.1 KHz, 16-bit uncompressed audio
        } else if (fileSize > Int.MAX_VALUE) {
            fileSize = Int.MAX_VALUE.toLong()
        }
        return fileSize
    }
}