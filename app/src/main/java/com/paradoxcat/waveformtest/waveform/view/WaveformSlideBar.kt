package com.paradoxcat.waveformtest.waveform.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.paradoxcat.waveformtest.waveform.data.WaveFormBatchData
import com.paradoxcat.waveformtest.waveform.listener.ProgressChangeListener
import com.paradoxcat.waveformtest.waveviewer.R
import kotlin.math.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration


/**
 * All functionality assumes that provided data has only 1 channel, 44100 Hz sample rate, 16-bits per sample, and is
 * already without WAV header.
 */
class WaveformSlideBar(context: Context, attrs: AttributeSet) : View(context, attrs) {
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
        else horizontalPadding + (progress * (width - horizontalPadding * 2) / 100).toFloat()
    }

    // Calculating Y coordinate for the progress indicator.
    private val progressY: Float
    get() = height.toFloat() - indicatorHandleRadius

    // Drawing attributes
    private var showProgress: Boolean = false
    private var horizontalPadding: Float = 50f
    private var verticalPadding: Float = 50f
    private var indicatorHandleRadius: Float = 20f

    private val waveformPaint = Paint()
    private val waveformProgressPaint = Paint()
    private val progressIndicatorPaint = Paint()
    private val timeBarPaint = Paint()
    private val textRect = Rect()

    init {
        initDrawingParameters(attrs)
    }

    // Setting up drawing attributes
    private fun initDrawingParameters(attrs: AttributeSet){
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.WaveformSlideBar)
        val indicatorLineWidth = typedArray.getFloat(R.styleable.WaveformSlideBar_indicatorLineWidth, 4f)
        val indicatorColor = typedArray.getColor(R.styleable.WaveformSlideBar_indicatorColor, Color.RED)
        val waveformColor = typedArray.getColor(R.styleable.WaveformSlideBar_waveformColor, Color.DKGRAY)
        val waveformProgressColor = typedArray.getColor(R.styleable.WaveformSlideBar_waveformProgressColor, Color.GRAY)
        val timeBarColor = typedArray.getColor(R.styleable.WaveformSlideBar_timeBarColor, Color.GRAY)

        waveformPaint.color = waveformColor
        waveformPaint.strokeWidth = 2f
        waveformProgressPaint.color = waveformProgressColor
        waveformProgressPaint.strokeWidth = 2f
        progressIndicatorPaint.color = indicatorColor
        progressIndicatorPaint.strokeWidth = indicatorLineWidth
        timeBarPaint.color = timeBarColor
        timeBarPaint.strokeWidth = 2f
        timeBarPaint.textSize = 20f

        showProgress = typedArray.getBoolean(R.styleable.WaveformSlideBar_showProgress, false)
        horizontalPadding = typedArray.getFloat(R.styleable.WaveformSlideBar_waveformHorizontalPadding, 50f)
        verticalPadding = typedArray.getFloat(R.styleable.WaveformSlideBar_waveformVerticalPadding, 50f)
        indicatorHandleRadius = typedArray.getFloat(R.styleable.WaveformSlideBar_indicatorHandleRadius, 20f)

        typedArray.recycle()
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

            if (showProgress) {
                drawProgressIndicator(canvas)
            }
        }
    }

    private fun drawWaveForm(canvas: Canvas){
        val sampleDistance =
            (width - horizontalPadding * 2) / (waveForm.size - 1) // distance between centers of 2 samples
        val maxAmplitude =
            height / 2.0f - verticalPadding // max amount of px from middle to the edge minus pad
        val amplitudeScaleFactor =
            INV_MAX_VALUE * maxAmplitude // multiply by this to get number of px from middle

        var prevX = 0f
        var prevLastY = 0f

        waveFormBatched.forEachIndexed { i, batchData ->

            val x = horizontalPadding + i * max(1f, sampleDistance) // minimum distance between centers should be 1 pixel.
            val minY = height / 2.0f - batchData.minAmplitude * amplitudeScaleFactor
            val maxY = height / 2.0f - batchData.maxAmplitude * amplitudeScaleFactor
            val firstY =
                if (batchData.minFirstIndex < batchData.maxFirstIndex) minY
                else maxY
            val lastY =
                if (batchData.minLastIndex > batchData.maxLastIndex) minY
                else maxY

            val paint = if (x <= progressX && showProgress) waveformProgressPaint else waveformPaint
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
        canvas.drawCircle(progressX, progressY, indicatorHandleRadius , progressIndicatorPaint)
    }

    private fun drawTimeBar(canvas: Canvas){
        // distance in pixels between time frames.
        val framesDistance =
            if (timeFrames.size <= 1) 0f
            else (width - horizontalPadding * 2) / (timeFrames.size - 1)

        timeBarPaint.strokeWidth = 1f
        timeFrames.forEachIndexed{ i, time ->
            // drawing time frame anchor
            val x = horizontalPadding + framesDistance * i
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
                indicatorTouchX = max(horizontalPadding, indicatorTouchX)
                indicatorTouchX = min(width - horizontalPadding, indicatorTouchX)
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
        val isInsideTheHandle = dx + dy < indicatorHandleRadius.pow(2)
        val isOnTheLine = touchX <= centerX + indicatorHandleRadius && touchX >= centerX - indicatorHandleRadius
        return isInsideTheHandle || isOnTheLine
    }

    /**
     * Updating the current progress value and notifying observers with the new value.
     */
    private fun updateIndicatorProgress(){
        progress = ((indicatorTouchX - horizontalPadding) / (width - horizontalPadding * 2) * 100).toDouble()
        progressChangeListener?.onProgressChanged(progress)
    }

    /**
     * Split waveform into batches,
     * each batch contains samples that would have been drown over the same column of pixels,
     * because there may not be enough horizontal pixels to cover all the samples.
     */
    private fun splitWaveForm() {
        if (width == 0) return
        val pixelsCount = (width - horizontalPadding * 2)  // available amount of pixels in a raw
        val samplesPerPixel = waveForm.size / pixelsCount  // to determine how many samples will have the same X value.
        val batchSize = max(1, ceil(samplesPerPixel).toInt()) // to handle the case if samples are less than the pixels.
        val batches = mutableListOf<WaveFormBatchData>()

        // Splitting data
        var index = 0
        while (index < waveForm.size){
            var minInBatch = Int.MAX_VALUE
            var maxInBatch = Int.MIN_VALUE
            var minFirstIndex = -1
            var minLastIndex = -1
            var maxFirstIndex = -1
            var maxLastIndex = -1

            for (j in 1 .. batchSize){
                val sample = waveForm[index]
                if (sample <= minInBatch){
                    if (sample != minInBatch) minFirstIndex = index
                    minLastIndex = index
                    minInBatch = sample
                }
                if (sample >= maxInBatch){
                    maxLastIndex = index
                    if (sample != maxInBatch) maxFirstIndex = index
                    maxInBatch = sample
                }
                if (++index == waveForm.size) break
            }

            val batchData = WaveFormBatchData(
                                minInBatch,
                                maxInBatch,
                                minFirstIndex,
                                minLastIndex,
                                maxFirstIndex,
                                maxLastIndex
                            )
            batches.add(batchData)
        }

        waveFormBatched = batches
    }

    /**
     * Set audio data and draw it.
     * @param waveForm -- the amplitudes of the waveform samples.
     */
    fun setData(waveForm: IntArray) {
        this.waveForm = waveForm
        progress = 0.0
        splitWaveForm()
        invalidate()
    }

    /**
     * Set the time frames of the audio, to draw the time frames bar.
     * @param timeFrames -- The time frames of the audio in milliseconds..
     */
    fun setTimeFrames(timeFrames: List<Long>){
        this.timeFrames = timeFrames
        invalidate()
    }

    companion object {
        private val MAX_VALUE = 2.0f.pow(16.0f) - 1 // max 16-bit value
        val INV_MAX_VALUE = 1.0f / MAX_VALUE // multiply with this to get % of max value
    }
}