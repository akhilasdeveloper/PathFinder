package com.akhilasdeveloper.pathfinder.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.akhilasdeveloper.pathfinder.models.Line
import com.akhilasdeveloper.pathfinder.models.Point
import com.akhilasdeveloper.pathfinder.models.Square
import com.akhilasdeveloper.pathfinder.views.Keys.EMPTY
import com.akhilasdeveloper.pathfinder.views.Keys.END
import com.akhilasdeveloper.pathfinder.views.Keys.START


class SpanGrid(context: Context?) : View(context) {

    private val paint = Paint()
    var mListener: OnNodeSelectListener? = null
    private val colors = intArrayOf(Color.DKGRAY, Color.RED, Color.BLUE, Color.YELLOW, Color.rgb(255,170,0),Color.WHITE)
    private var scale = 30
    private var marginV = 1
    var widthS = 0
    var heightS = 0
    var startPont:Point? = null
    var endPont:Point? = null
    var drawLines = mutableListOf<Line>()
    var drawSquares = mutableListOf<Square>()

    fun init(){
        setSize()
        postInvalidate()
    }

    private fun setSize(){
        widthS = (width - marginV) / (scale + marginV)
        heightS = (height - marginV) / (scale + marginV)
        initSquares()
        initLines()
    }

    private fun initSquares() {
        drawSquares.clear()
        for (y in 0 until heightS)
            for (x in 0 until widthS){
                val xs = getViewFactor(x.toFloat())
                val ys = getViewFactor(y.toFloat())
                drawSquares.add(Square(xs, ys, xs + scale, ys + scale))
            }
    }

    private fun initLines(){
        drawLines.clear()
        var xx = 0f
        var yy = 0f
        for (x in 0..widthS) {
            for (y in 0..heightS) {
                drawLines.add(Line(xx, yy, xx + width - 1, yy))
                yy += marginV + scale.toFloat()
            }
            yy = 0f
            drawLines.add(Line(xx, yy, xx, yy + height - 1))
            xx += marginV + scale.toFloat()
        }
    }

    fun setScale(scale: Int) {
        val backup = drawSquares.toTypedArray()
        val widths = widthS
        this.scale = scale
        setSize()
        restoreData(backup,widths)
        postInvalidate()
    }

    private fun restoreData(backup: Array<Square>, widths: Int) {
        startPont = null
        endPont = null
        for((j, i) in backup.withIndex()){
            val xx = j%widths
            val yy = j/widths
            val p = xx + yy * widthS
            if (p < drawSquares.size) {
                drawSquares[p].type = i.type
                if (i.type == START) startPont = Point(xx,yy)
                if (i.type == END) endPont = Point(xx,yy)
            }
        }
    }

    fun setMargin(margin: Int) {
        val backup = drawSquares.toTypedArray()
        val widths = widthS
        this.marginV = margin
        setSize()
        restoreData(backup,widths)
        postInvalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
            }
            MotionEvent.ACTION_MOVE -> {
                val px = getPixelDetails(event.x.toInt(), event.y.toInt())
                mListener?.onEvent(px)
            }
            MotionEvent.ACTION_UP -> {
            }
        }
        return true
    }

    fun setRect(px: Point, type: Int){

        if (px.x < widthS && px.y < heightS && px.x >= 0 && px.y >= 0) {

            drawSquares[px.x + px.y * widthS].type = type
            postInvalidate()

            if (px == startPont)
                startPont = null
            if (px == endPont)
                endPont = null
        }
    }

    fun setStart(px: Point){
        if (px.x < widthS && px.y < heightS) {
            startPont?.let {
                if (it != px) {
                    drawSquares[it.x + it.y * widthS].type = EMPTY
                    startPont = null
                }
            }
            startPont = px
            drawSquares[px.x + px.y * widthS].type = START
            postInvalidate()
        }
    }

    fun setEnd(px: Point){
        if (px.x < widthS && px.y < heightS) {
            endPont?.let {
                if (it != px) {
                    drawSquares[it.x + it.y * widthS].type = EMPTY
                    endPont = null
                }
            }
            endPont = px
            drawSquares[px.x + px.y * widthS].type = END
            postInvalidate()
        }
    }

    private fun getPixelDetails(x: Int, y: Int): Point {
        val sx = (x / (scale + marginV))
        val sy = (y / (scale + marginV))
        return Point(sx,sy)
    }

    private fun getViewFactor(c: Float) = (c * (scale + marginV)) + if(marginV == 1) 1 else (marginV / 2)

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            drawColor(Color.WHITE)
            paint.color = Color.LTGRAY
            paint.strokeWidth = marginV.toFloat()

            for (line in drawLines)
                drawLine(line.x1,line.y1,line.x2,line.y2, paint)

            for (p in drawSquares) {
                paint.color = colors[p.type]
                drawRect(p.x1, p.y1, p.x2, p.y2, paint)
                /*paint.color = Color.BLACK
                paint.textSize = 35f
                drawText("${p.distance} ",p.x1 , p.y1 + (scale/2),paint)*/
            }

        }
    }

    fun setNodeSelectListener(eventListener: OnNodeSelectListener) {
        mListener = eventListener
    }
}