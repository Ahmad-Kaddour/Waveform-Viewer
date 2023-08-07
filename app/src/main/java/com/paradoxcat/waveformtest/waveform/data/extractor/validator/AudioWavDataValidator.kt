package com.paradoxcat.waveformtest.waveform.data.extractor.validator

import android.media.AudioFormat
import android.media.MediaExtractor
import android.media.MediaFormat
import javax.inject.Inject

/**
 * Audio data validator that checks if the file is single track, PCM, 44100HZ, 1-channel, and 16-bit format in the buffer.
 */
class AudioWavDataValidator @Inject constructor(): MediaDataValidator {

    override suspend fun isValid(mediaExtractor: MediaExtractor): Boolean {
        val mime = mediaExtractor.getTrackFormat(0)
        if (mime.containsKey(MediaFormat.KEY_PCM_ENCODING) && mime.getInteger(MediaFormat.KEY_PCM_ENCODING) != AudioFormat.ENCODING_PCM_16BIT) {
            throw IllegalArgumentException(
                "Expected AudioFormat ${AudioFormat.ENCODING_PCM_16BIT}, got AudioFormat ${
                    mime.getInteger(MediaFormat.KEY_PCM_ENCODING)
                }"
            )
        }
        if (mime.containsKey(MediaFormat.KEY_CHANNEL_COUNT) && mime.getInteger(MediaFormat.KEY_CHANNEL_COUNT) != 1) {
            throw IllegalArgumentException(
                "Expected 1 channel, got ${
                    mime.getInteger(
                        MediaFormat.KEY_CHANNEL_COUNT
                    )
                }"
            )
        }
        if (mime.containsKey(MediaFormat.KEY_SAMPLE_RATE) && mime.getInteger(MediaFormat.KEY_SAMPLE_RATE) != 44100) {
            throw IllegalArgumentException(
                "Expected 44100 sample rate, got ${
                    mime.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                }"
            )
        }

        if (mediaExtractor.trackCount < 1) {
            throw IllegalArgumentException("No media tracks found, aborting initialization")
        }
        return true
    }
}