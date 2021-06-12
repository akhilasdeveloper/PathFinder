package com.akhilasdeveloper.pathfinder.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.akhilasdeveloper.pathfinder.models.Node
import com.akhilasdeveloper.pathfinder.views.Keys.BLOCK
import com.akhilasdeveloper.pathfinder.views.Keys.EMPTY
import com.akhilasdeveloper.pathfinder.views.Keys.END
import com.akhilasdeveloper.pathfinder.views.Keys.START


class SpanGrid(context: Context?) : View(context) {

    private val paint = Paint()
    var mListener: OnNodeSelectListener? = null
    private val colors = intArrayOf(Color.DKGRAY, Color.RED, Color.BLUE, Color.YELLOW, Color.rgb(255,170,0))
    private var scale = 30
    private var marginV = 1
    var widthS = 0
    var heightS = 0

    var data:MutableSet<Node> = mutableSetOf()

    fun init(){
        setSize()
    }

    fun setDatas(data:MutableSet<Node>){
            synchronized(this.data) {
                val threadSet: MutableSet<Node> = mutableSetOf()
                threadSet.addAll(data)
                this.data = threadSet
            }
            postInvalidate()

    }

    private fun setSize(){
        widthS = (width - marginV) / (scale + marginV)
        heightS = (height - marginV) / (scale + marginV)
        postInvalidate()
    }

    fun setScale(scale: Int) {
        this.scale = scale
        setSize()
        postInvalidate()
    }

    fun setMargin(margin: Int) {
        this.marginV = margin
        setSize()
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

    fun addNode(px: Node){
        removeStartNode(px)
        removeEndNode(px)
        data.add(px.apply { type = BLOCK })
        postInvalidate()
    }

    fun addStartNode(px: Node){
        deleteNode(px)
        removeEndNode(px)
        data.filter { it.type == START }.forEach { data.remove(it) }
        data.add(px.apply { type = START })
        postInvalidate()
    }

    fun addEndNode(px: Node){
        deleteNode(px)
        removeStartNode(px)
        data.filter { it.type == END }.forEach { data.remove(it) }
        data.add(px.apply { type = END })
        postInvalidate()
    }

    fun deleteNode(px: Node){
        data.remove( px.apply { type = BLOCK } )
        postInvalidate()
    }

    fun removeStartNode(px: Node){
        data.remove( px.apply { type = START } )
        postInvalidate()
    }

    fun removeEndNode(px: Node){
        data.remove( px.apply { type = END } )
        postInvalidate()
    }

    private fun getPixelDetails(x: Int, y: Int): Node {
        val sx = (x / (scale + marginV))
        val sy = (y / (scale + marginV))
        return Node(
            x = sx,
            y = sy
        )
    }

    private fun getViewFactor(c: Float) = (c * (scale + marginV)) + if(marginV == 1) 1 else (marginV / 2)

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {
            drawColor(Color.WHITE)
            paint.color = Color.LTGRAY
            paint.strokeWidth = marginV.toFloat()
            var xx = 0f
            var yy = 0f
            for (x in 0..widthS) {
                for (y in 0..heightS) {
                    drawLine(xx, yy, xx + width - 1, yy, paint)
                    yy += marginV + scale.toFloat()
                }
                yy = 0f
                drawLine(xx, yy, xx, yy + height - 1, paint)
                xx += marginV + scale.toFloat()
            }

            synchronized(data){
                for ( it in data){
                    if (it.type != EMPTY) {
                        paint.color = colors[it.type]
                        val xs = getViewFactor(it.x.toFloat())
                        val ys = getViewFactor(it.y.toFloat())
                        drawRect(xs, ys, xs + scale, ys + scale, paint)
                        Log.d("SpanGrid : forEach", "$xs:$ys:${xs + scale}:${ys + scale}")
                        paint.color = Color.BLACK
                        paint.textSize = 25f
                        drawText("${it.distance} ${it.previousNode?.x}:${it.previousNode?.y}",xs , ys + (scale/2),paint)
                        drawText("cur ${it.x}:${it.y}",xs , ys + (scale/2) + 25f,paint)
                    }
                }
            }

        }
    }

    fun setNodeSelectListener(eventListener: OnNodeSelectListener) {
        mListener = eventListener
    }
}