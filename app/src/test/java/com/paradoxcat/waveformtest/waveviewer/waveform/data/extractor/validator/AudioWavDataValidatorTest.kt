package com.paradoxcat.waveformtest.waveviewer.waveform.data.extractor.validator

import android.media.AudioFormat
import android.media.MediaExtractor
import android.media.MediaFormat
import com.paradoxcat.waveformtest.waveform.data.extractor.validator.AudioWavDataValidator
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class AudioWavDataValidatorTest {
    private lateinit var audioWavDataValidator: AudioWavDataValidator

    private val mediaExtractor = mock<MediaExtractor>()
    private val mime = mock<MediaFormat>()

    @Before
    fun init() {
        audioWavDataValidator = AudioWavDataValidator()
    }

    @Test
    fun testValidation_valid(){
        runBlocking {
            whenever(mediaExtractor.getTrackFormat(0)).thenReturn(mime)
            whenever(mediaExtractor.trackCount).thenReturn(1)
            whenever(mime.containsKey(MediaFormat.KEY_PCM_ENCODING)).thenReturn(true)
            whenever(mime.containsKey(MediaFormat.KEY_CHANNEL_COUNT)).thenReturn(true)
            whenever(mime.containsKey(MediaFormat.KEY_SAMPLE_RATE)).thenReturn(true)
            whenever(mime.getInteger(MediaFormat.KEY_PCM_ENCODING)).thenReturn(AudioFormat.ENCODING_PCM_16BIT)
            whenever(mime.getInteger(MediaFormat.KEY_CHANNEL_COUNT)).thenReturn(1)
            whenever(mime.getInteger(MediaFormat.KEY_SAMPLE_RATE)).thenReturn(44100)
            val result = audioWavDataValidator.isValid(mediaExtractor)
            Assert.assertTrue(result)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testValidation_invalid_notPCM(){
        runBlocking {
            whenever(mediaExtractor.getTrackFormat(0)).thenReturn(mime)
            whenever(mime.containsKey(MediaFormat.KEY_PCM_ENCODING)).thenReturn(false)
            val result = audioWavDataValidator.isValid(mediaExtractor)
            Assert.assertTrue(result)
        }
    }


    @Test(expected = IllegalArgumentException::class)
    fun testValidation_invalid_channelCount(){
        runBlocking {
            whenever(mediaExtractor.getTrackFormat(0)).thenReturn(mime)
            whenever(mime.containsKey(MediaFormat.KEY_PCM_ENCODING)).thenReturn(true)
            whenever(mime.containsKey(MediaFormat.KEY_CHANNEL_COUNT)).thenReturn(true)
            whenever(mime.getInteger(MediaFormat.KEY_PCM_ENCODING)).thenReturn(AudioFormat.ENCODING_PCM_16BIT)
            whenever(mime.getInteger(MediaFormat.KEY_CHANNEL_COUNT)).thenReturn(2)
            val result = audioWavDataValidator.isValid(mediaExtractor)
            Assert.assertTrue(result)
        }
    }
}