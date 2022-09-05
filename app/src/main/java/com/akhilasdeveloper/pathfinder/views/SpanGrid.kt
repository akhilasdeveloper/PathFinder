package com.akhilasdeveloper.pathfinder.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.akhilasdeveloper.pathfinder.R
import com.akhilasdeveloper.pathfinder.models.Point
import com.akhilasdeveloper.pathfinder.models.PointF
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs


class SpanGrid(context: Context) : View(context) {

    private val MODE_VIEW: Int = -2
    private val MODE_DRAW: Int = -3

    private val paint = Paint()
    private var canvasBuffer: Canvas? = null
    private var bitmapBuffer: Bitmap? = null
    var scale = 0f
    var xOff = 0f
        private set
    var yOff = 0f
        private set

    var scaleEnabled = true
    var spanEnabled = true
    var lineEnabled = true
        set(value) {
            field = value
            this.setGridSize()
        }

    private val history = ConcurrentHashMap<Point, Int>()
    private val defaultCellColor = ResourcesCompat.getColor(context.resources, R.color.empty, null)
    private val lineColor = ResourcesCompat.getColor(context.resources, R.color.gray_600, null)
    private var lineStartPx: Point? = null

    var mode: Int = MODE_DRAW
        set(value) {
            field = value
            mListener?.onModeChange(mode)
        }

    var isTouched = false
        private set

    var resolution: Float = 0f
        set(value) {
            if (scale == 0f)
                scale = value
            field = value
            this.setGridSize()
        }

    var lineWidth: Float = 1f
        set(value) {
            field = value
            this.setGridSize()
        }

    var gridWidth: Float = 0f
        private set

    var gridHeight: Float = 0f
        private set

    private var mListener: OnGridSelectListener? = null

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {

            if (mode == MODE_VIEW && spanEnabled) {

                xOff += -(distanceX * gridWidth / width)
                yOff += -(distanceY * gridHeight / height)

                setGridSize()
            }
            return true
        }
    }

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {

            if (mode == MODE_VIEW && scaleEnabled) {

                scale = 10f.coerceAtLeast((scale * detector.scaleFactor).coerceAtMost(100f))

                val fact = (scale - resolution) / ((resolution + lineWidth) * (scale + lineWidth))

                xOff -= detector.focusX * fact
                yOff -= detector.focusY * fact

                resolution = scale
            }

            return true
        }
    }

    private val mScaleDetector = ScaleGestureDetector(context, scaleListener)
    private val mGestureDetector = GestureDetector(context, mGestureListener)

    fun init() {
        bitmapBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        canvasBuffer = Canvas(bitmapBuffer!!)

        setGridSize()
    }


    fun play() {
        invalidate()
    }

    /***
     * Change the canvas coordinates to Grid Coordinates
     */
    private fun getPixelDetails(px: PointF): Point {
        val sx = (px.x / (resolution + lineWidth)) - xOff
        val sy = (px.y / (resolution + lineWidth)) - yOff
        return Point(sx.toInt(), sy.toInt())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        mGestureDetector.onTouchEvent(event)
        mScaleDetector.onTouchEvent(event)
        this.performClick()

        mode = if (event.pointerCount > 1)
            MODE_VIEW
        else
            MODE_DRAW

        isTouched = true

        val pxF = PointF(event.x, event.y)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (mode == MODE_DRAW) {
                    val px = getPixelDetails(pxF)
                    mListener?.onEventDown(px)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == MODE_DRAW) {
                    val px = getPixelDetails(pxF)
                    lineStartPx?.let { sPx ->
                        drawLine(sPx.x, sPx.y, px.x, px.y)
                    }
                    lineStartPx = px
                }
            }
            MotionEvent.ACTION_UP -> {
                isTouched = false
                lineStartPx = null
                mListener?.onEventUp()
            }
        }
        return true
    }

    private fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int) {
        var x = x1
        var y = y1
        val w = x2 - x
        val h = y2 - y
        var dx1 = 0
        var dy1 = 0
        var dx2 = 0
        var dy2 = 0
        if (w < 0) dx1 = -1 else if (w > 0) dx1 = 1
        if (h < 0) dy1 = -1 else if (h > 0) dy1 = 1
        if (w < 0) dx2 = -1 else if (w > 0) dx2 = 1
        var longest = abs(w)
        var shortest = abs(h)
        if (longest <= shortest) {
            longest = abs(h)
            shortest = abs(w)
            if (h < 0) dy2 = -1 else if (h > 0) dy2 = 1
            dx2 = 0
        }
        var numerator = longest shr 1
        for (i in 0..longest) {

            mListener?.onEventMove(Point(x, y))

            numerator += shortest
            if (numerator >= longest) {
                numerator -= longest
                x += dx1
                y += dy1
            } else {
                x += dx2
                y += dy2
            }
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun drawRect(x1: Float, y1: Float, x2: Float, y2: Float, type: Int) {
        paint.color = type
        canvasBuffer?.drawRect(x1, y1, x2, y2, paint)
    }

    fun plotPoint(px: Point, color: Int) {
        history[px] = color
        setRectRestore(px, color)
    }

    private fun setRectRestore(px: Point, color: Int) {
        val pxx = PointF(px.x + xOff, px.y + yOff)
        if (pxx.x < gridWidth && pxx.y < gridHeight && pxx.x + resolution >= 0 && pxx.y + resolution >= 0) {
            val xs = getViewFactor(pxx.x)
            val ys = getViewFactor(pxx.y)
            drawRect(xs, ys, xs + resolution, ys + resolution, color)
        }
    }

    private fun populateAll() {
        for (xx in -1..gridWidth.toInt() + 1) {
            for (yy in -1..gridHeight.toInt() + 1) {
                val px = Point(xx - xOff.toInt(), yy - yOff.toInt())
                setRectRestore(px, history[px] ?: defaultCellColor)
            }
        }
    }

    fun removeRect(px: Point) {
        history.remove(px)
        setRectRestore(px, defaultCellColor)
    }

    private fun setGridSize() {
        clearCanvas()
        gridWidth = ((width - lineWidth) / (resolution + lineWidth))
        gridHeight = ((height - lineWidth) / (resolution + lineWidth))
        if (lineEnabled)
            populateAll()
        else
            restore()
        postInvalidate()
    }

    private fun restore() {
        history.forEach {
            setRectRestore(it.key, it.value)
        }
    }

    private fun getViewFactor(c: Float) = (c * (resolution + lineWidth)) + (lineWidth / 2)

    private fun clearCanvas() {
        canvasBuffer?.drawColor(if (lineEnabled) lineColor else defaultCellColor)
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            bitmapBuffer?.let {
                canvas.drawBitmap(it, 0f, 0f, paint)
            }
        }
    }


    fun setGridSelectListener(eventListener: OnGridSelectListener) {
        mListener = eventListener
    }

    interface OnGridSelectListener {
        fun onEventMove(px: Point)
        fun onEventUp()
        fun onEventDown(px: Point)
        fun onModeChange(mode: Int)
    }
}