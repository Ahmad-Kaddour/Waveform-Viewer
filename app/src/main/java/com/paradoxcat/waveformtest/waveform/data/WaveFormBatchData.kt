package com.paradoxcat.waveformtest.waveform.data

/**
 * Data class to store precalculated values needed when drawing the waveform.
 */
data class WaveFormBatchData(
    val minAmplitude: Int,
    val maxAmplitude: Int,
    val minFirstIndex: Int,
    val minLastIndex: Int,
    val maxFirstIndex: Int,
    val maxLastIndex: Int,
)
