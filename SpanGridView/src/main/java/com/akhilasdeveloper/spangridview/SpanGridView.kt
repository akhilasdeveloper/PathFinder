package com.akhilasdeveloper.spangridview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.FloatPropertyCompat
import com.akhilasdeveloper.spangridview.algorithms.QuadTree
import com.akhilasdeveloper.spangridview.algorithms.models.Node
import com.akhilasdeveloper.spangridview.algorithms.models.PointNode
import com.akhilasdeveloper.spangridview.algorithms.models.RectangleCentered
import com.akhilasdeveloper.spangridview.models.Point
import com.akhilasdeveloper.spangridview.models.PointF
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs


class SpanGridView(
    context: Context,
    attributeSet: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : View(context, attributeSet, defStyleAttr, defStyleRes) {

    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : this(
        context,
        attributeSet,
        defStyleAttr,
        0
    ) {
        init(attributeSet)
    }

    constructor(context: Context, attributeSet: AttributeSet) : this(context, attributeSet, 0, 0) {
        init(attributeSet)
    }

    constructor(context: Context) : this(context, null, 0, 0) {
        init(null)
    }

    val MODE_VIEW: Int = -2
    val MODE_DRAW: Int = -3

    private val MIN_DISTANCE_MOVED = 50
    private val FRICTION = 1.1f

    private var maxTranslationX = 0f
    private var maxTranslationY = 0f

    private val scaleLimitStart = 20f
    private val scaleLimitEnd = 150f

    private var touchCount = 0
    private var fact = 0f

    private val paint = Paint().apply {
        isAntiAlias = true

    }
    private var mListener: OnGridSelectListener? = null
    private var points = ConcurrentHashMap<Point, Node>()
    private var historyQuad = createQuadTree()

    private fun createQuadTree() = QuadTree(RectangleCentered(0f, 0f, Int.MAX_VALUE.toFloat(), Int.MAX_VALUE.toFloat()), 4)

    var gridColor = ResourcesCompat.getColor(resources, R.color.grid_color, null)
    var lineColor = ResourcesCompat.getColor(resources, R.color.line_color, null)
    var scaleEnabled = true
    var drawEnabled = true
        set(value) {
            field = value
            determineViewMode()
        }
    var spanEnabled = true
    var lineEnabled = true
        set(value) {
            field = value
            this.setGridSize()
        }
    var mode: Int = MODE_DRAW
        set(value) {
            field = value
            mListener?.onModeChange(mode)
        }

    var brushSize = 1
        set(value) {
            val data = value.coerceIn(1, 3)
            field = data
        }

    private var resolution: Float = 0f
        set(value) {
            val data = value.coerceIn(scaleLimitStart, scaleLimitEnd)
            field = data
            this.setGridSize()
        }

    var scale: Float = 0f
        set(value) {
            val data = value.coerceIn(0f, 1f)
            field = data
            setScaleToResolution(data)
        }
        get() = resolution / (scaleLimitEnd - scaleLimitEnd)


    var lineWidth: Float = 1f
        set(value) {
            field = value
            _lineWidth = value
            this.setGridSize()
        }

    private var _textSize: Float = 12f
    private var _lineWidth: Float = 1f
    private var _strokeWidth: Float = 5f

    private var xOff = 0f
    private var yOff = 0f

    var startPoint: Point = Point(0, 0)
        get() = getPixelDetails(PointF(0f, 0f))
        private set

    var gridWidth: Float = 0f
        set(value) {
            field = value
            fact = gridWidth / width
        }

    var gridHeight: Float = 0f
        private set

    private var xFlingValue: Float = 0f
        set(value) {
            field = value
            xOff = (value * fact)

            setGridSize()
        }

    private var yFlingValue: Float = 0f
        set(value) {
            field = value
            yOff = (value * fact)

            setGridSize()
        }


    private val xFling = object : FloatPropertyCompat<SpanGridView>("xFling") {
        override fun getValue(`object`: SpanGridView?): Float {
            return `object`?.xFlingValue ?: 0f
        }

        override fun setValue(`object`: SpanGridView?, value: Float) {
            `object`?.xFlingValue = value
        }

    }

    private val yFling = object : FloatPropertyCompat<SpanGridView>("yFling") {
        override fun getValue(`object`: SpanGridView?): Float {
            return `object`?.yFlingValue ?: 0f
        }

        override fun setValue(`object`: SpanGridView?, value: Float) {
            `object`?.yFlingValue = value
        }

    }

    private val flingX = FlingAnimation(this@SpanGridView, xFling).setFriction(FRICTION)
    private val flingY = FlingAnimation(this@SpanGridView, yFling).setFriction(FRICTION)

    private fun init(set: AttributeSet?) {
        set?.let { attrSet ->

            val ta = context.obtainStyledAttributes(attrSet, R.styleable.SpanGridView)

            lineColor = ta.getColor(
                R.styleable.SpanGridView_lineColor,
                ResourcesCompat.getColor(resources, R.color.line_color, null)
            )
            gridColor = ta.getColor(
                R.styleable.SpanGridView_gridColor,
                ResourcesCompat.getColor(resources, R.color.grid_color, null)
            )
            scale = ta.getFloat(R.styleable.SpanGridView_scale, 1f)
            lineWidth = ta.getFloat(R.styleable.SpanGridView_lineWidth, 1f)
            scaleEnabled = ta.getBoolean(R.styleable.SpanGridView_enableScale, true)
            spanEnabled = ta.getBoolean(R.styleable.SpanGridView_enableSpan, true)
            lineEnabled = ta.getBoolean(R.styleable.SpanGridView_enableLine, true)
            brushSize = ta.getInteger(R.styleable.SpanGridView_brushSize, 1)

            ta.recycle()

        }
    }

    private fun setScaleToResolution(scale: Float) {
        resolution = (scaleLimitEnd - scaleLimitStart) * scale
    }

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {

            if (mode == MODE_VIEW && spanEnabled) {

                xFling.setValue(this@SpanGridView, (xOff - distanceX * fact) / fact)
                yFling.setValue(this@SpanGridView, (yOff - distanceY * fact) / fact)

            }
            return true
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {

            if (mode == MODE_VIEW) {

                val distanceInX: Float = abs(e2.rawX - e1.rawX)
                val distanceInY: Float = abs(e2.rawY - e1.rawY)


                if (distanceInX > MIN_DISTANCE_MOVED) {
                    flingX.setStartVelocity(velocityX)
                        .start()
                }
                if (distanceInY > MIN_DISTANCE_MOVED) {
                    flingY.setStartVelocity(velocityY)
                        .start()
                }
            }

            return true
        }
    }


    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {

            if (mode == MODE_VIEW && scaleEnabled) {

                val scale = resolution * detector.scaleFactor
                if (scale in scaleLimitStart..scaleLimitEnd) {

                    _textSize *= detector.scaleFactor
                    _lineWidth *= detector.scaleFactor
                    _strokeWidth *= detector.scaleFactor

                    val factS =
                        (scale - resolution) / ((resolution + _lineWidth) * (scale + _lineWidth))

                    xFling.setValue(this@SpanGridView, (xOff - detector.focusX * factS) / fact)
                    yFling.setValue(this@SpanGridView, (yOff - detector.focusY * factS) / fact)

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

    fun clearData(){
        points.clear()
        historyQuad = createQuadTree()
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
        mode = if (touchCount > 1 || !drawEnabled)
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
            }
        }
        return true
    }

    private fun drawCenterSquare(xc: Int, yc: Int, r: Int) {
        val rr = r - 1
        val x1 = xc - rr
        val y1 = yc - rr
        val x2 = xc + rr
        val y2 = yc + rr

        for (x in x1..x2)
            for (y in y1..y2)
                mListener?.onDraw(Point(x, y))
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
        drawColor(gridColor)

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

            /*if (resolution >= 50f) {
                val text = px.text
                paint.textSize = _textSize
                paint.color = Color.WHITE
                paint.textAlign = Paint.Align.CENTER

                val xPos: Float = px.x1 + ((px.x2 - px.x1) / 2)
                val yPos = px.y1 + (((px.y2 - px.y1) / 2 - (paint.descent() + paint.ascent()) / 2))

                drawText(text, xPos, yPos, paint)
            }*/

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
        fun onDraw(px: Point)
        fun onModeChange(mode: Int)
    }
}


