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
import com.akhilasdeveloper.pathfinder.algorithms.quadtree.QuadTree
import com.akhilasdeveloper.pathfinder.algorithms.quadtree.RectangleCentered
import com.akhilasdeveloper.pathfinder.models.Point
import com.akhilasdeveloper.pathfinder.models.PointF
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs


class SpanGrid(context: Context) : View(context) {

    private val MODE_VIEW: Int = -2
    private val MODE_DRAW: Int = -3

    private val paint = Paint()
    private var canvasBuffer: Canvas? = null
    private var bitmapBuffer: Bitmap? = null

    var xOff = 0f
        private set

    var yOff = 0f
        private set

    var startPoint: Point = Point(0, 0)
        get() = getPixelDetails(PointF(0f, 0f))
        private set

    private var touchCount = 0
    var scaleEnabled = true
    var spanEnabled = true
    var lineEnabled = true
        set(value) {
            field = value
            this.setGridSize()
        }

    private val history = ConcurrentHashMap<Point, Int>()
    private val historyStroke = ConcurrentHashMap<Point, Int>()
    private val defaultCellColor = ResourcesCompat.getColor(context.resources, R.color.empty, null)
    private val historyQuad = QuadTree(RectangleCentered(0f, 0f, Int.MAX_VALUE.toFloat() , Int.MAX_VALUE.toFloat() ), 4)
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
            field = value
            this.setGridSize()
        }

    var lineWidth: Float = 1f
        set(value) {
            field = value
            _lineWidth = value
            this.setGridSize()
        }

    var strokeWidth: Float = 1f
        set(value) {
            field = value
            _strokeWidth = value
            this.setGridSize()
        }

    private var _lineWidth: Float = 1f
    private var _strokeWidth: Float = 5f


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

                val fact = gridWidth / width

                xOff += -distanceX * fact
                yOff += -distanceY * fact

                setGridSize()
            }
            return true
        }
    }

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {

            if (mode == MODE_VIEW && scaleEnabled) {

                val scale = resolution * detector.scaleFactor
                if (scale in 10f..100f) {

                    _lineWidth *= detector.scaleFactor
                    _strokeWidth *= detector.scaleFactor

                    val fact = (scale - resolution) / ((resolution + _lineWidth) * (scale + _lineWidth))

                    xOff -= detector.focusX * fact
                    yOff -= detector.focusY * fact

                    resolution = scale
                }

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
        val fact = resolution + _lineWidth
        val sx = (px.x / fact) - xOff
        val sy = (px.y / fact) - yOff
        return Point(sx.toInt(), sy.toInt())
    }

    private fun determineViewMode() {
        mode = if (touchCount > 1)
            MODE_VIEW
        else
            MODE_DRAW
    }

    private fun setTouchCount(count: Int) {
        if (count == 0)
            touchCount = 0
        else
            if (count > touchCount)
                touchCount = count
        determineViewMode()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        mGestureDetector.onTouchEvent(event)
        mScaleDetector.onTouchEvent(event)
        this.performClick()

        setTouchCount(event.pointerCount)

        isTouched = true

        val pxF = PointF(event.x, event.y)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {

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
                if (mode == MODE_DRAW) {
                    val px = getPixelDetails(pxF)
                    mListener?.onEventDown(px)
                }
                isTouched = false
                lineStartPx = null
                setTouchCount(0)
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

    private fun drawRect(x1: Float, y1: Float, x2: Float, y2: Float, color: Int) {
        paint.color = color
        paint.style = Paint.Style.FILL
        canvasBuffer?.drawRect(x1, y1, x2, y2, paint)
    }

    private fun drawLinePx(x1: Float, y1: Float, x2: Float, y2: Float) {
        paint.color = lineColor
        paint.strokeWidth = _lineWidth
        paint.style = Paint.Style.STROKE
        canvasBuffer?.drawLine(x1, y1, x2, y2, paint)
    }

    private fun drawRectStroke(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        color: Int,
        strokeColor: Int
    ) {

        if (color != strokeColor) {

            paint.style = Paint.Style.STROKE

            paint.strokeWidth = _strokeWidth
            paint.color = strokeColor
            var addFact = _strokeWidth / 2
            canvasBuffer?.drawRect(x1 + addFact, y1 + addFact, x2 - addFact, y2 - addFact, paint)

            paint.color = lineColor
            paint.strokeWidth = _lineWidth
            addFact = _strokeWidth + (_lineWidth/2)
            canvasBuffer?.drawRect(x1 + addFact, y1 + addFact, x2 - addFact, y2 - addFact, paint)

            addFact = _strokeWidth + _lineWidth
            paint.color = color
            paint.style = Paint.Style.FILL
            canvasBuffer?.drawRect(x1 + addFact, y1 + addFact, x2 - addFact, y2 - addFact, paint)

        } else {
            drawRect(x1, y1, x2, y2, color)
        }


    }

    fun plotPoint(px: Point, color: Int, strokeColor: Int) {
        history[px] = color
        historyQuad.insert(px)
        Timber.d("QuadTree Output history : ${history.size}")
        historyStroke[px] = strokeColor
        setRectRestore(px, color)
    }

    private fun setRectRestore(px: Point, color: Int) {
        val pxx = PointF(px.x + xOff, px.y + yOff)
        if (pxx.x < gridWidth && pxx.y < gridHeight && pxx.x + resolution >= 0 && pxx.y + resolution >= 0) {
            val xs = getViewFactor(pxx.x) + (_lineWidth / 2)
            val ys = getViewFactor(pxx.y) + (_lineWidth / 2)

            val stroke = historyStroke[px]
            if (stroke == null)
                drawRect(xs, ys, xs + resolution, ys + resolution, color)
            else
                drawRectStroke(xs, ys, xs + resolution, ys + resolution, color, stroke)
        }
    }

    private fun setLineRestore(px1: Point, px2: Point) {

        val xs1 = getViewFactor(px1.x + xOff)
        val ys1 = getViewFactor(px1.y + yOff)

        val xs2 = getViewFactor(px2.x + xOff)
        val ys2 = getViewFactor(px2.y + yOff)

        drawLinePx(xs1, ys1, xs2, ys2)
    }

    private fun drawLines() {

        for (xx in -1..gridWidth.toInt() + 1) {
            val px1 = Point(xx - xOff.toInt(), -1 - yOff.toInt())
            val px2 = Point(xx - xOff.toInt(), gridHeight.toInt() - yOff.toInt() + 2)

            setLineRestore(px1, px2)
        }

        for (yy in -1..gridHeight.toInt() + 1) {
            val px1 = Point(-1 - xOff.toInt(), yy - yOff.toInt())
            val px2 = Point(gridWidth.toInt() - xOff.toInt() + 2, yy - yOff.toInt())

            setLineRestore(px1, px2)
        }
    }

    fun removeRect(px: Point) {
        history.remove(px)
        historyStroke.remove(px)
        historyQuad.remove(px)
        setRectRestore(px, defaultCellColor)
    }

    private fun setGridSize() {
        clearCanvas()
        gridWidth = ((width - _lineWidth) / (resolution + _lineWidth))
        gridHeight = ((height - _lineWidth) / (resolution + _lineWidth))

        if (lineEnabled)
            drawLines()
        restore()
        postInvalidate()
    }

    private fun restore() {
        val w = (gridWidth/2) + 2
        val h = (gridHeight/2) + 2
        val x = w - xOff - 1
        val y = h - yOff - 1
        val points = historyQuad.pull(
            RectangleCentered(
                x = x,
                y = y,
                w = w,
                h = h
            )
        )

        points.forEach { point ->
            history[point]?.let {
                setRectRestore(point, it)
            }
        }

        /*history.forEach() {
            setRectRestore(it.key, it.value)
        }*/
    }

    private fun getViewFactor(c: Float) = (c * (resolution + _lineWidth))

    private fun clearCanvas() {
        canvasBuffer?.drawColor(defaultCellColor)
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