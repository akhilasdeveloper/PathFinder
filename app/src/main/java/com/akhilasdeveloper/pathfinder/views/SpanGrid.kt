package com.akhilasdeveloper.pathfinder.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.akhilasdeveloper.pathfinder.R
import com.akhilasdeveloper.pathfinder.models.Line
import com.akhilasdeveloper.pathfinder.models.Point
import com.akhilasdeveloper.pathfinder.models.Square
import com.akhilasdeveloper.pathfinder.views.Keys.EMPTY
import com.akhilasdeveloper.pathfinder.views.Keys.END
import com.akhilasdeveloper.pathfinder.views.Keys.START
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


class SpanGrid(context: Context) : View(context) {

    private val paint = Paint()
    var mListener: OnNodeSelectListener? = null
    private val colors = intArrayOf(ResourcesCompat.getColor(context.resources, R.color.block, null),
        ResourcesCompat.getColor(context.resources, R.color.start,null),
        ResourcesCompat.getColor(context.resources, R.color.end,null),
        ResourcesCompat.getColor(context.resources, R.color.path,null),
        ResourcesCompat.getColor(context.resources, R.color.visited,null),
        ResourcesCompat.getColor(context.resources, R.color.empty,null))

    private var scale = 10
    private var marginV = 1
    var widthS = 0
    var heightS = 0
    var startPont:Point? = null
    var endPont:Point? = null
    var drawSquares = mutableListOf<Square>()
    var canvasB:Canvas? = null
    var bitmap:Bitmap? = null

    fun init(){
        bitmap =  Bitmap.createBitmap(width,height,Bitmap.Config.RGB_565);
        canvasB = Canvas(bitmap!!)
        setSize()
        postInvalidate()
    }

    private fun setSize(){
        clearCanvas()
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
        paint.color = ResourcesCompat.getColor(context.resources, R.color.line,null)
        paint.strokeWidth = marginV.toFloat()
        var xx = 0f
        var yy = 0f
        for (x in 0..widthS) {
            for (y in 0..heightS) {
                canvasB?.drawLine(xx,yy,xx + width - 1,yy, paint)
                yy += marginV + scale.toFloat()
            }
            yy = 0f
            canvasB?.drawLine(xx, yy, xx, yy + height - 1, paint)
            xx += marginV + scale.toFloat()
        }

    }

    fun setScale(scale: Int) {
        val backup = drawSquares.toTypedArray()
        val widths = widthS
        this@SpanGrid.scale = scale
        setSize()
        restoreData(backup, widths)
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
                val pp = drawSquares[p]
                drawRect(pp.x1,pp.y1, pp.x2, pp.y2, i.type)
            }
        }
    }

    private fun drawRect(x1:Float, y1:Float, x2:Float, y2:Float, type: Int){
        paint.color = colors[type]
        canvasB?.drawRect(x1,y1,x2,y2, paint)
    }

    private fun clearCanvas(){
        canvasB?.drawColor(ResourcesCompat.getColor(context.resources, R.color.empty,null))
    }

    fun setMargin(margin: Int) {
        val backup = drawSquares.toTypedArray()
        val widths = widthS
        this@SpanGrid.marginV = margin
        setSize()
        restoreData(backup, widths)
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
            val pp = drawSquares[px.x + px.y * widthS]
            drawRect(pp.x1,pp.y1, pp.x2, pp.y2, type)
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
                    val pp = drawSquares[it.x + it.y * widthS]
                    drawRect(pp.x1,pp.y1, pp.x2, pp.y2, EMPTY)
                }
            }
            startPont = px
            drawSquares[px.x + px.y * widthS].type = START
            val pp = drawSquares[px.x + px.y * widthS]
            drawRect(pp.x1,pp.y1, pp.x2, pp.y2, START)
            postInvalidate()
        }
    }

    fun setEnd(px: Point){
        if (px.x < widthS && px.y < heightS) {
            endPont?.let {
                if (it != px) {
                    drawSquares[it.x + it.y * widthS].type = EMPTY
                    val pp = drawSquares[it.x + it.y * widthS]
                    drawRect(pp.x1,pp.y1, pp.x2, pp.y2, EMPTY)
                    endPont = null
                }
            }
            endPont = px
            drawSquares[px.x + px.y * widthS].type = END
            val pp = drawSquares[px.x + px.y * widthS]
            drawRect(pp.x1,pp.y1, pp.x2, pp.y2, END)
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
        bitmap?.let {
            canvas?.drawBitmap(it, 0f, 0f, paint)
        }
    }

    fun setNodeSelectListener(eventListener: OnNodeSelectListener) {
        mListener = eventListener
    }
}