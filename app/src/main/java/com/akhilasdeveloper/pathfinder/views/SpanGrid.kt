package com.akhilasdeveloper.pathfinder.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.akhilasdeveloper.pathfinder.R
import com.akhilasdeveloper.pathfinder.algorithms.quadtree.Node
import com.akhilasdeveloper.pathfinder.algorithms.quadtree.QuadTree
import com.akhilasdeveloper.pathfinder.algorithms.quadtree.RectangleCentered
import com.akhilasdeveloper.pathfinder.models.Point
import com.akhilasdeveloper.pathfinder.models.PointF
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs


class SpanGrid(context: Context) : View(context) {

    private val MODE_VIEW: Int = -2
    private val MODE_DRAW: Int = -3

    private val paint = Paint()

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

    private var points = listOf<Node>()
    private val history = ConcurrentHashMap<Point, Int>()
    private val historyStroke = ConcurrentHashMap<Point, Int>()
    private val defaultCellColor = ResourcesCompat.getColor(context.resources, R.color.empty, null)
    private val historyQuad =
        QuadTree(RectangleCentered(0f, 0f, Int.MAX_VALUE.toFloat(), Int.MAX_VALUE.toFloat()), 4)
    private val lineColor = ResourcesCompat.getColor(context.resources, R.color.gray_600, null)

    var mode: Int = MODE_DRAW
        set(value) {
            field = value
            mListener?.onModeChange(mode)
        }

    var isTouched = false
        private set

    var brushSize = 0

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
                if (scale in 20f..150f) {

                    _lineWidth *= detector.scaleFactor
                    _strokeWidth *= detector.scaleFactor

                    val fact =
                        (scale - resolution) / ((resolution + _lineWidth) * (scale + _lineWidth))

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
                    drawCenterSquare(px.x, px.y, brushSize)
                }
            }
            MotionEvent.ACTION_UP -> {
                if (mode == MODE_DRAW) {
                    val px = getPixelDetails(pxF)
                    drawCenterSquare(px.x, px.y, brushSize)
                }
                isTouched = false
                setTouchCount(0)
                mListener?.onEventUp()
            }
        }
        return true
    }

    private fun drawCenterSquare(xc: Int, yc: Int, r: Int) {
        val x1 = xc - r
        val y1 = yc - r
        val x2 = xc + r
        val y2 = yc + r

        for (x in x1..x2)
            for (y in y1..y2)
                mListener?.onEventMove(Point(x, y))
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }


    fun plotPoint(px: Point, color: Int, strokeColor: Int) {
        history[px] = color
        historyQuad.insert(px)
        historyStroke[px] = strokeColor
        restore()
    }


    fun removeRect(px: Point) {
        history.remove(px)
        historyStroke.remove(px)
        historyQuad.remove(px)
        restore()
    }

    private fun setGridSize() {
        gridWidth = ((width - _lineWidth) / (resolution + _lineWidth))
        gridHeight = ((height - _lineWidth) / (resolution + _lineWidth))

        restore()
    }

    private fun restore() {
        val w = (gridWidth / 2) + 2
        val h = (gridHeight / 2) + 2
        val x = w - xOff - 1
        val y = h - yOff - 1
        points = historyQuad.pull(
            RectangleCentered(
                x = x,
                y = y,
                w = w,
                h = h
            )
        ).map {

            val xs = getViewFactor(it.x + xOff) + (_lineWidth / 2)
            val ys = getViewFactor(it.y + yOff) + (_lineWidth / 2)
            val fill = history[it] ?: 0
            val stroke = historyStroke[it] ?: fill

            Node(
                x1 = xs,
                y1 = ys,
                x2 = xs + resolution,
                y2 = ys + resolution,
                fill = fill,
                stroke = stroke
            )
        }
        invalidate()
    }

    private fun getViewFactor(c: Float) = (c * (resolution + _lineWidth))

    private fun Canvas.drawLines(x1: Float, y1: Float, x2: Float, y2: Float) {
        val xs1 = getViewFactor(x1)
        val ys1 = getViewFactor(y1)

        val xs2 = getViewFactor(x2)
        val ys2 = getViewFactor(y2)

        paint.color = lineColor
        paint.strokeWidth = _lineWidth
        paint.style = Paint.Style.STROKE
        this.drawLine(xs1, ys1, xs2, ys2, paint)
    }

    private fun Canvas.drawGridLines() {
        drawColor(defaultCellColor)

        val factX = -xOff.toInt() + xOff
        val factY = -yOff.toInt() + yOff
        if (lineEnabled) {
            for (xx in -1..gridWidth.toInt() + 1) {

                drawLines(
                    xx + factX,
                    -1 + factY,
                    xx + factX,
                    gridHeight.toInt() + factY + 2
                )

            }

            for (yy in -1..gridHeight.toInt() + 1) {

                drawLines(
                    -1 + factX,
                    yy + factY,
                    gridWidth.toInt() + factX + 2,
                    yy + factY
                )

            }
        }
    }

    private fun Canvas.drawGridPoints() {
        points.forEach { px ->

                if (px.stroke == px.fill) {
                    paint.color = px.fill
                    paint.style = Paint.Style.FILL
                    this.drawRect(px.x1, px.y1, px.x2, px.y2, paint)
                } else {

                    paint.style = Paint.Style.STROKE

                    paint.strokeWidth = _strokeWidth
                    paint.color = px.stroke
                    var addFact = _strokeWidth / 2
                    this.drawRect(
                        px.x1 + addFact,
                        px.y1 + addFact,
                        px.x2 - addFact,
                        px.y2 - addFact,
                        paint
                    )

                    paint.color = lineColor
                    paint.strokeWidth = _lineWidth
                    addFact = _strokeWidth + (_lineWidth / 2)
                    this.drawRect(
                        px.x1 + addFact,
                        px.y1 + addFact,
                        px.x2 - addFact,
                        px.y2 - addFact,
                        paint
                    )

                    addFact = _strokeWidth + _lineWidth
                    paint.color = px.fill
                    paint.style = Paint.Style.FILL
                    this.drawRect(
                        px.x1 + addFact,
                        px.y1 + addFact,
                        px.x2 - addFact,
                        px.y2 - addFact,
                        paint
                    )
                }

        }
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            drawGridLines()
            drawGridPoints()
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


