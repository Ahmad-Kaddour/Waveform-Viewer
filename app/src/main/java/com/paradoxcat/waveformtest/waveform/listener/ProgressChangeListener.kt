package com.paradoxcat.waveformtest.waveform.listener

/**
 * A functional interface for listening to progress changes.
 */
fun interface ProgressChangeListener{
    /**
     * Called when the progress is changed.
     *
     * @param progress The new progress value.
     */
    fun onProgressChanged(progress: Double)
}