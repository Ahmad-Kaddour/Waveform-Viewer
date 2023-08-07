package com.paradoxcat.waveformtest.ui.main

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.paradoxcat.waveformtest.waveform.data.converter.duration.DurationToTimeFrameConverter
import com.paradoxcat.waveformtest.waveform.data.converter.rawdata.RawDataConverter
import com.paradoxcat.waveformtest.waveform.data.extractor.RawWaveDataExtractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val waveFormDataExtractor: RawWaveDataExtractor,
    private val rawDataConverter: RawDataConverter,
    private val durationToTimeFrameConverter: DurationToTimeFrameConverter
) : AndroidViewModel(application) {
    val uriLiveData = MutableLiveData<Uri>()

    private val _waveFormData = MutableLiveData<IntArray>()
    val waveFormData: LiveData<IntArray> = _waveFormData

    private val _timeFramesData = MutableLiveData<List<Long>>()
    val timeFramesData: LiveData<List<Long>> = _timeFramesData

    fun extractDataFromUri(uri: Uri) {
        uriLiveData.value = uri
        viewModelScope.launch {
            try {
                val rawData = waveFormDataExtractor.extractData(uri)
                val samples = rawDataConverter.transformRawData(rawData)
                _waveFormData.value = samples
            } catch (e: Exception) {
                e.printStackTrace()
                if (e is CancellationException) throw CancellationException()
            }
        }
    }

    // duration in milliseconds
    fun convertDurationToTimeFrames(duration: Long) {
        val timeFrames = durationToTimeFrameConverter.convert(duration, 5)
        _timeFramesData.value = timeFrames
    }
}