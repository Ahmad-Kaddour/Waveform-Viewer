package com.paradoxcat.waveformtest.di

import com.paradoxcat.waveformtest.waveform.data.converter.duration.DurationToTimeFrameConverter
import com.paradoxcat.waveformtest.waveform.data.converter.duration.DurationToTimeFrameConverterImpl
import com.paradoxcat.waveformtest.waveform.data.converter.rawdata.RawDataConverter
import com.paradoxcat.waveformtest.waveform.data.converter.rawdata.RawDataConverterImpl
import com.paradoxcat.waveformtest.waveform.data.extractor.RawWaveDataExtractor
import com.paradoxcat.waveformtest.waveform.data.extractor.WaveDataFromMediaExtractor
import com.paradoxcat.waveformtest.waveform.data.extractor.validator.AudioWavDataValidator
import com.paradoxcat.waveformtest.waveform.data.extractor.validator.MediaDataValidator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface AppModule {
    @Binds
    fun bindsDurationToTimeFrameConverter(converter: DurationToTimeFrameConverterImpl): DurationToTimeFrameConverter

    @Binds
    fun bindsRawDataConverter(converter: RawDataConverterImpl): RawDataConverter

    @Binds
    fun bindsMediaDataValidator(converter: AudioWavDataValidator): MediaDataValidator

    @Binds
    fun bindsRawWaveDataExtractor(converter: WaveDataFromMediaExtractor): RawWaveDataExtractor
}