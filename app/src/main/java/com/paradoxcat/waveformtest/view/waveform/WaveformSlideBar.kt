package com.paradoxcat.waveformtest.view.waveform

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.nio.ByteBuffer
import kotlin.math.*

/**
 * All functionality assumes that provided data has only 1 channel, 44100 Hz sample rate, 16-bits per sample, and is
 * already without WAV header.
 */
class WaveformSlideBar(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val linePaint = Paint()
    private val progressPaint = Paint()
    private val progressIndicatorPaint = Paint()

    private lateinit var waveForm: IntArray
    private lateinit var waveFormBatched: List<WaveFormBatchData>

    var progressChangeListener: ProgressChangeListener? = null

    var progress = 0.0
    set(value) {
        field = if (value > 100.0) 100.0
                else if (value < 0.0) 0.0
                else value
        invalidate()
    }

    // Calculating X coordinate for the progress indicator.
    private val progressX: Float
    get() = LEFT_RIGHT_PADDING + (progress * (width - LEFT_RIGHT_PADDING * 2) / 100).toFloat()

    // Calculating Y coordinate for the progress indicator.
    private val progressY: Float
    get() = height.toFloat() - INDICATOR_CIRCLE_RADIOS

    init {
        linePaint.color = Color.rgb(200, 200, 200)
        progressPaint.color = Color.rgb(255, 255, 255)
        progressIndicatorPaint.color = Color.rgb(255, 0, 0)
        linePaint.strokeWidth = 2f
        progressPaint.strokeWidth = 2f
        progressIndicatorPaint.strokeWidth = 4f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (::waveForm.isInitialized) {
            splitWaveForm()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (::waveForm.isInitialized) {
            if (!::waveFormBatched.isInitialized) {
                splitWaveForm()
            }

            drawWaveForm(canvas)
            drawProgressIndicator(canvas)
        }
    }

    private fun drawWaveForm(canvas: Canvas){
        val sampleDistance =
            (width - LEFT_RIGHT_PADDING * 2) / (waveForm.size - 1) // distance between centers of 2 samples
        val maxAmplitude =
            height / 2.0f - TOP_BOTTOM_PADDING // max amount of px from middle to the edge minus pad
        val amplitudeScaleFactor =
            INV_MAX_VALUE * maxAmplitude // multiply by this to get number of px from middle

        var prevX = 0f
        var prevLastY = 0f

        waveFormBatched.forEachIndexed { i, batchData ->

            val x = LEFT_RIGHT_PADDING + i * max(1f, sampleDistance) // minimum distance between centers should be 1 pixel.
            val minY = height / 2.0f - batchData.minAmplitude * amplitudeScaleFactor
            val maxY = height / 2.0f - batchData.maxAmplitude * amplitudeScaleFactor
            val firstY =
                if (batchData.minFirstIndex < batchData.maxFirstIndex) minY
                else maxY
            val lastY =
                if (batchData.minLastIndex > batchData.maxLastIndex) minY
                else maxY

            val paint = if (x <= progressX) progressPaint else linePaint
            // drawing vertical line between the minimum and maximum amplitudes in the same pixels column.
            canvas.drawLine(x, minY, x, maxY, paint)

            // drawing a line between the last sample in the previous column and the first sample in the current column.
            if (i > 0){
                canvas.drawLine(prevX, prevLastY, x, firstY, paint)
            }

            prevX = x
            prevLastY = lastY
        }
    }

    private fun drawProgressIndicator(canvas: Canvas){
        canvas.drawLine(progressX, 0f, progressX, progressY, progressIndicatorPaint)
        canvas.drawCircle(progressX, progressY, INDICATOR_CIRCLE_RADIOS , progressIndicatorPaint)
    }


    private var lastTouchX = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN ->
                // Check if the touch event is within the indicator bounds
                if (isWithinIndicatorBounds(event.x, event.y)) {
                    // Store the initial touch position
                    lastTouchX = event.x
                    return true
                }
            MotionEvent.ACTION_MOVE -> {
                // Calculate the distance moved horizontally
                val distanceX: Float = event.x - lastTouchX
                // Update the indicator position
                updateIndicatorPosition(distanceX)
                // Update the last touch position
                lastTouchX = event.x
                // Invalidate the view to redraw the line
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                // Reset the last touch position
                lastTouchX = 0f
                return true
            }
        }
        return super.onTouchEvent(event)
    }


    /**
     * Checking if the touch coordinates fall within the bounds of the progress indicator.
     * @param touchX -- X position of the touch coordinates in pixels.
     * @param touchY -- Y position of the touch coordinates in pixels.
     */
    private fun isWithinIndicatorBounds(touchX: Float, touchY: Float): Boolean {
        val centerX = progressX
        val centerY = progressY
        val dx = (touchX - centerX).pow(2)
        val dy = (touchY - centerY).pow(2)
        val isInsideTheHandle = dx + dy < INDICATOR_CIRCLE_RADIOS.pow(2)
        val isOnTheLine = touchX <= centerX + INDICATOR_CIRCLE_RADIOS && touchX >= centerX - INDICATOR_CIRCLE_RADIOS
        return isInsideTheHandle || isOnTheLine
    }

    /**
     * Updating the position of the progress indicator by the specified distance.
     * @param distance -- The distance the pointer should move.
     *                    If the distance is negative the indicator will move left otherwise it will move right.
     */
    private fun updateIndicatorPosition(distance: Float){
        var newProgressX = progressX + distance
        newProgressX = max(LEFT_RIGHT_PADDING, newProgressX)
        newProgressX = min(LEFT_RIGHT_PADDING + (width - LEFT_RIGHT_PADDING * 2), newProgressX)
        progress = ((newProgressX - LEFT_RIGHT_PADDING) / (width - LEFT_RIGHT_PADDING * 2) * 100).toDouble()
        progressChangeListener?.onProgressChanged(progress)
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
            .map {  batch ->
                val min = batch.min()
                val max = batch.max()
                WaveFormBatchData(
                    minAmplitude = min,
                    maxAmplitude = max,
                    minFirstIndex = batch.indexOf(min),
                    minLastIndex = batch.lastIndexOf(min),
                    maxFirstIndex = batch.indexOf(max),
                    maxLastIndex = batch.lastIndexOf(max)
                )
        }
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

    companion object {
        const val LEFT_RIGHT_PADDING = 50.0f
        const val TOP_BOTTOM_PADDING = 50.0f
        const val INDICATOR_CIRCLE_RADIOS = 20.0f
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
}