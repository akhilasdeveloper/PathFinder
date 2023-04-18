package com.akhilasdeveloper.pathfinder.algorithms.pathfinding

import com.akhilasdeveloper.spangridview.models.Point
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.math.floor
import kotlin.random.Random

class GenerateTerrain {
    private var mListener: OnTerrainGenerateListener? = null
    var sleepVal = 0L

    fun generateTerrain(startPoint: Point, gridHeight: Int, gridWidth: Int) {
        CoroutineScope(Dispatchers.Default).launch {
            var gHeight = gridHeight
            var gWidth = gridWidth

            gHeight = if (gHeight % 2 == 0) gHeight else gHeight - 1
            gWidth = if (gWidth % 2 == 0) gWidth else gWidth - 1

            generateBorder(startPoint, gWidth, gHeight)
            terrain(
                startPoint.x + 1,
                startPoint.y + 1,
                gWidth + startPoint.x - 1,
                gHeight + startPoint.y - 1
            )
        }
    }

    private fun generateBorder(startPoint: Point, gWidth: Int, gHeight: Int) {

        val xEnd = gWidth + startPoint.x
        val yEnd = gHeight + startPoint.y

        for (i in startPoint.x until xEnd) {
            mListener?.addData(Point(i, startPoint.y), FindPath.WALL_NODE)
            mListener?.addData(Point(i, yEnd), FindPath.WALL_NODE)
        }
        for (j in startPoint.y until yEnd + 1) {
            mListener?.addData(Point(startPoint.x, j), FindPath.WALL_NODE)
            mListener?.addData(Point(xEnd, j), FindPath.WALL_NODE)
        }
    }

    private fun terrain(x1: Int, y1: Int, x2: Int, y2: Int) {
        for(x in x1..x2){
            for (y in y1..y2){
                val noiseValue = OpenSimplex2S.noise3_ImproveXY(0,x * .05 , y * .05, 0.0).map(-1.0,1.0, -10.0, -1.0).toInt()
                mListener?.addData(Point(x, y), noiseValue)
            }
        }
    }

    fun Number.map(fromLow: Double, fromHigh: Double, toLow: Double, toHigh: Double): Double {
        return (this.toDouble() - fromLow) * (toHigh - toLow) / (fromHigh - fromLow) + toLow
    }

    fun setTerrainGenerateListener(eventListener: OnTerrainGenerateListener) {
        mListener = eventListener
    }

    interface OnTerrainGenerateListener {
        fun addData(px: Point, weight: Int)
    }
}