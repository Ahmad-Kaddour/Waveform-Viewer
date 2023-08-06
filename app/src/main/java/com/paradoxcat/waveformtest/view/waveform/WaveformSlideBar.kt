package com.paradoxcat.waveformtest.view.waveform

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.nio.ByteBuffer
import kotlin.math.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * All functionality assumes that provided data has only 1 channel, 44100 Hz sample rate, 16-bits per sample, and is
 * already without WAV header.
 */
class WaveformSlideBar(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val linePaint = Paint()
    private val progressPaint = Paint()
    private val progressIndicatorPaint = Paint()
    private val timeBarPaint = Paint()
    private val textRect = Rect()

    private lateinit var waveForm: IntArray
    private lateinit var waveFormBatched: List<WaveFormBatchData>
    private lateinit var timeFrames: List<Long>

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
    get() {
        return if (indicatorTouchX != 0f) indicatorTouchX
        else LEFT_RIGHT_PADDING + (progress * (width - LEFT_RIGHT_PADDING * 2) / 100).toFloat()
    }

    // Calculating Y coordinate for the progress indicator.
    private val progressY: Float
    get() = height.toFloat() - INDICATOR_CIRCLE_RADIOS

    init {
        linePaint.color = Color.rgb(200, 200, 200)
        linePaint.strokeWidth = 2f
        progressPaint.color = Color.rgb(255, 255, 255)
        progressPaint.strokeWidth = 2f
        progressIndicatorPaint.color = Color.rgb(255, 0, 0)
        progressIndicatorPaint.strokeWidth = 4f
        timeBarPaint.color = Color.rgb(255, 255, 255)
        timeBarPaint.strokeWidth = 2f
        timeBarPaint.textSize = 20f
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

            if (::timeFrames.isInitialized) {
                drawTimeBar(canvas)
            }

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

    private fun drawTimeBar(canvas: Canvas){
        // distance in pixels between time frames.
        val framesDistance =
            if (timeFrames.size <= 1) 0f
            else (width - LEFT_RIGHT_PADDING * 2) / (timeFrames.size - 1)

        timeBarPaint.strokeWidth = 1f
        timeFrames.forEachIndexed{ i, time ->
            // drawing time frame anchor
            val x = LEFT_RIGHT_PADDING + framesDistance * i
            canvas.drawLine(x, 0f, x, 20f, timeBarPaint)

            // drawing the time frame text
            val duration = time.toDuration(DurationUnit.MILLISECONDS)
            val timeString =
                duration.toComponents { minutes, seconds, _ ->
                    String.format("%02d:%02d", minutes, seconds) // formatting time frame text in mm:ss format
                }

            timeBarPaint.getTextBounds(timeString, 0, timeString.length, textRect)
            val textX = x - textRect.width() / 2.toFloat() // centering the text by the anchor.
            canvas.drawText(timeString, textX, 50f, timeBarPaint)
        }

        // drawing the time frames bar horizontal line
        timeBarPaint.strokeWidth = 2f
        canvas.drawLine(0f, 0f, width.toFloat(), 0f, timeBarPaint)
    }


    private var indicatorTouchX = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN ->
                // Check if the touch event is within the indicator bounds
                if (isWithinIndicatorBounds(event.x, event.y)) {
                    // Store the initial touch position
                    indicatorTouchX = event.x
                    return true
                }
            MotionEvent.ACTION_MOVE -> {
                // Update the last touch position
                indicatorTouchX = event.x
                // Preventing indicator from going out of the view bounds
                indicatorTouchX = max(LEFT_RIGHT_PADDING, indicatorTouchX)
                indicatorTouchX = min(width - LEFT_RIGHT_PADDING, indicatorTouchX)
                // Invalidate the view to redraw the line
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                // Updating the progress value
                updateIndicatorProgress()
                // Reset the last touch position
                indicatorTouchX = 0f
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
     * Updating the current progress value and notifying observers with the new value.
     */
    private fun updateIndicatorProgress(){
        progress = ((indicatorTouchX - LEFT_RIGHT_PADDING) / (width - LEFT_RIGHT_PADDING * 2) * 100).toDouble()
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

    /**
     * Set the duration of the audio, to draw the time frames bar.
     * @param duration -- The duration of the audio in milliseconds..
     */
    fun setDuration(duration: Long){
        timeFrames = convertDurationToTimeFrames(duration, 5)
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

        /** Converting the duration of the audio into time frames, with minimum interval of 1 second.
         * @param duration -- Duration of the audio in milliseconds.
         * @param count -- The count of the time frames to be split into.
         *                 If the duration is not enough to create time windows of the specified count,
         *                 the returned list will contain less elements than count.
         * @return List contains the time frames in milliseconds.
         * @throws IllegalArgumentException if the duration is negative, or the count is less than 2.
         **/
        fun convertDurationToTimeFrames(duration: Long, count: Int): List<Long> {
            if (duration < 0) {
                throw IllegalArgumentException("duration must be positive")
            }
            if (count < 1) {
                throw IllegalArgumentException("count must be at least 2")
            }
            val frames = mutableListOf<Long>()
            val step = duration / (count - 1).toDouble()
            var currentTime = step

            frames.add(0)
            while (currentTime < duration && step >= 1000){
                frames.add(floor(currentTime).toLong())
                currentTime += step
            }
            if (duration >= 1000) {
                frames.add(duration)
            }

            return frames
        }
    }
}