package com.akhilasdeveloper.pathfinder.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.*
import androidx.core.content.res.ResourcesCompat
import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.FloatPropertyCompat
import com.akhilasdeveloper.pathfinder.R
import com.akhilasdeveloper.pathfinder.algorithms.quadtree.Node
import com.akhilasdeveloper.pathfinder.algorithms.quadtree.PointNode
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

    private val MIN_DISTANCE_MOVED = 50
    private val MIN_TRANSLATION = 0f
    private val FRICTION = 1.1f

    private var maxTranslationX = 0f
    private var maxTranslationY = 0f

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

    private var points = ConcurrentHashMap<Point, Node>()
    private val defaultCellColor = ResourcesCompat.getColor(context.resources, R.color.empty, null)
    private val historyQuad =
        QuadTree(RectangleCentered(0f, 0f, Int.MAX_VALUE.toFloat(), Int.MAX_VALUE.toFloat()), 4)
    private val lineColor = ResourcesCompat.getColor(context.resources, R.color.gray_600, null)

    var mode: Int = MODE_DRAW
        set(value) {
            field = value
            mListener?.onModeChange(mode)
        }

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


    private var xFlingValue:Float = 0f
        set(value) {
            field = value
            val fact = gridWidth / width

            xOff =  (value * fact)

            setGridSize()
        }

    private var yFlingValue:Float = 0f
        set(value) {
            field = value
            val fact = gridWidth / width

            yOff =  (value * fact)

            setGridSize()

        }

    val xFling = object : FloatPropertyCompat<SpanGrid>("xFling") {
        override fun getValue(`object`: SpanGrid?): Float {
            return `object`?.xFlingValue ?: 0f
        }

        override fun setValue(`object`: SpanGrid?, value: Float) {
            `object`?.xFlingValue = value
        }

    }

    val yFling = object : FloatPropertyCompat<SpanGrid>("yFling") {
        override fun getValue(`object`: SpanGrid?): Float {
            return `object`?.yFlingValue ?: 0f
        }

        override fun setValue(`object`: SpanGrid?, value: Float) {
            `object`?.yFlingValue = value
        }

    }

    val flingX = FlingAnimation(this@SpanGrid, xFling)
    val flingY = FlingAnimation(this@SpanGrid, yFling)

    private var vTracker: VelocityTracker? = null

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {

            if (mode == MODE_VIEW && spanEnabled) {

                val fact = gridWidth / width

                xFling.setValue(this@SpanGrid , (xOff - distanceX * fact)/fact)
                yFling.setValue(this@SpanGrid , (yOff - distanceY * fact)/fact)

                setGridSize()
            }
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent?,
            velocityX: Float,
            velocityY: Float
        ): Boolean {

            //downEvent : when user puts his finger down on the view
            //moveEvent : when user lifts his finger at the end of the movement
            val distanceInX: Float = abs((e2?.rawX) ?: 0f - (e1?.rawX ?: 0f))
            val distanceInY: Float = abs((e2?.rawY) ?: 0f - (e1?.rawY ?: 0f))

            Timber.d("distanceInX : $distanceInX\ndistanceInY : $distanceInY")

            if (distanceInX > MIN_DISTANCE_MOVED) {
                //Fling Right/Left
                flingX.setStartVelocity(velocityX)
                    .setFriction(FRICTION)
                    .start()
            }
            if (distanceInY > MIN_DISTANCE_MOVED) {
                //Fling Down/Up
                flingY.setStartVelocity(velocityY)
                    .setFriction(FRICTION)
                    .start()
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

                    xFling.setValue(this@SpanGrid , (xOff - detector.focusX * fact)/(gridWidth / width))
                    yFling.setValue(this@SpanGrid , (yOff - detector.focusY * fact)/(gridWidth / width))

                    resolution = scale

                }

            }

            return true
        }

    }

    private val mScaleDetector = ScaleGestureDetector(context, scaleListener)
    private val mGestureDetector = GestureDetector(context, mGestureListener)

    fun init() {
        maxTranslationX = width.toFloat() / 2
        maxTranslationY = height.toFloat() / 2

        setGridSize()
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

        val pxF = PointF(event.x, event.y)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                flingX.cancel()
                flingY.cancel()

                if (vTracker == null) {
                    vTracker = VelocityTracker.obtain();
                } else {
                    vTracker?.clear();
                }
                vTracker?.addMovement(event);
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
        val pointNode = PointNode(x = px.x, y = px.y, fill = color, stroke = strokeColor)
        historyQuad.insert(pointNode)
        if (isPointInCurrentScreen(px)) {
            addToPoints(pointNode)
            invalidate()
        }
    }

    private fun isPointInCurrentScreen(point: Point): Boolean {

        val w = (gridWidth / 2) + 2
        val h = (gridHeight / 2) + 2
        val x = w - xOff - 1
        val y = h - yOff - 1

        return point.x >= x - w &&
                point.x <= x + w &&
                point.y >= y - h &&
                point.y <= y + h
    }

    fun removeRect(px: Point) {
        historyQuad.remove(px)
        points.remove(px)
        invalidate()
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
        points.clear()
        historyQuad.pull(
            RectangleCentered(
                x = x,
                y = y,
                w = w,
                h = h
            )
        ).forEach {
            addToPoints(it)
        }
        invalidate()
    }

    private fun addToPoints(px: PointNode) {
        val xs = getViewFactor(px.x + xOff) + (_lineWidth / 2)
        val ys = getViewFactor(px.y + yOff) + (_lineWidth / 2)
        val fill = px.fill
        val stroke = px.stroke

        points[Point(x = px.x, y = px.y)] =
            Node(
                x = px.x,
                y = px.y,
                x1 = xs,
                y1 = ys,
                x2 = xs + resolution,
                y2 = ys + resolution,
                fill = fill,
                stroke = stroke
            )

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
        points.forEach { data ->
            val px = data.value
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


