package com.paradoxcat.waveformtest.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.nio.ByteBuffer
import kotlin.math.*

/**
 * All functionality assumes that provided data has only 1 channel, 44100 Hz sample rate, 16-bits per sample, and is
 * already without WAV header.
 */
class WaveformSlideBar(context: Context, attrs: AttributeSet) : View(context, attrs) {

    companion object {
        const val LEFT_RIGHT_PADDING = 50.0f
        const val TOP_BOTTOM_PADDING = 50.0f
        private val MAX_VALUE = 2.0f.pow(16.0f) - 1 // max 16-bit value
        val INV_MAX_VALUE = 1.0f / MAX_VALUE // multiply with this to get % of max value

        /** Transform raw audio into drawable array of integers */
        fun transformRawData(buffer: ByteBuffer): IntArray {
            val nSamples = buffer.limit() / 2 // assuming 16-bit PCM mono
            val waveForm = IntArray(nSamples)
            for (i in 1 until buffer.limit() step 2) {
                waveForm[i / 2] = (buffer[i].toInt() shl 8) or buffer[i - 1].toInt()
            }
            return waveForm
        }
    }

    private val linePaint = Paint()
    private lateinit var waveForm: IntArray
    private lateinit var waveFormBatched: List<List<Int>>

    init {
        linePaint.color = Color.rgb(0, 0, 255)
        linePaint.strokeWidth = 2f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (::waveForm.isInitialized) {
            splitWaveForm()
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (::waveForm.isInitialized) {
            if (!::waveFormBatched.isInitialized) {
                splitWaveForm()
            }

            val sampleDistance =
                (width - LEFT_RIGHT_PADDING * 2) / (waveForm.size - 1) // distance between centers of 2 samples
            val maxAmplitude =
                height / 2.0f - TOP_BOTTOM_PADDING // max amount of px from middle to the edge minus pad
            val amplitudeScaleFactor =
                INV_MAX_VALUE * maxAmplitude // multiply by this to get number of px from middle

            var prevX = 0f
            var prevLastY = 0f

            waveFormBatched.forEachIndexed { i, batch ->
                val maxSample = batch.maxOrNull()
                val minSample = batch.minOrNull()

                if (maxSample != null && minSample != null) {
                    val x = LEFT_RIGHT_PADDING + i * max(1f, sampleDistance) // minimum distance between centers should be 1 pixel.
                    val minY = height / 2.0f - minSample * amplitudeScaleFactor
                    val maxY = height / 2.0f - maxSample * amplitudeScaleFactor
                    val firstY =
                        if (batch.indexOf(minSample) < batch.indexOf(maxSample)) minY
                        else maxY
                    val lastY =
                        if (batch.lastIndexOf(minSample) > batch.lastIndexOf(maxSample)) minY
                        else maxY

                    // drawing vertical line between the minimum and maximum amplitudes in the same pixels column.
                    canvas?.drawLine(x, minY, x, maxY, linePaint)

                    // drawing a line between the last sample in the previous column and the first sample in the current column.
                    if (i > 0){
                        canvas?.drawLine(prevX, prevLastY, x, firstY, linePaint)
                    }

                    prevX = x
                    prevLastY = lastY
                }
            }
        }
    }


    /**
     * Split waveform into batches,
     * each batch contains samples that would have been drown over the same column of pixels,
     * because there may not be enough horizontal pixels to cover all the samples.
     */
    private fun splitWaveForm() {
        val pixelsCount = (width - LEFT_RIGHT_PADDING * 2)  // available amount of pixels in a raw
        val samplesPerPixel = waveForm.size / pixelsCount  // to determine how many samples will have the same X value.
        val batchSize = max(1, ceil(samplesPerPixel).toInt()) // to handle the case if samples are less than the pixels.
        waveFormBatched = waveForm.toList().chunked(batchSize)
    }

    /**
     * Set raw audio data and draw it.
     * @param buffer -- raw audio buffer must be 16-bit samples packed together (mono, 16-bit PCM). Sample rate does
     *                  not matter, since we are not rendering any time-related information yet.
     */
    fun setData(buffer: ByteBuffer) {
        waveForm = transformRawData(buffer)
        invalidate()
    }
}
