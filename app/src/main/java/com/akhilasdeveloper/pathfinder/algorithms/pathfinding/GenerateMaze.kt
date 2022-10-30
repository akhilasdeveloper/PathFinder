package com.akhilasdeveloper.pathfinder.algorithms.pathfinding

import com.akhilasdeveloper.spangridview.models.Point
import kotlinx.coroutines.*
import kotlin.math.floor
import kotlin.random.Random

class GenerateMaze {
    private var gaps = mutableListOf<Point>()
    private var mListener: OnMazeGenerateListener? = null
    var sleepVal = 0L

    fun generateRecursiveMaze(startPoint: Point, gridHeight: Int, gridWidth: Int) {
        CoroutineScope(Dispatchers.Default).launch {
            var gHeight = gridHeight
            var gWidth = gridWidth

            gHeight = if (gHeight % 2 == 0) gHeight else gHeight - 1
            gWidth = if (gWidth % 2 == 0) gWidth else gWidth - 1

            generateBorder(startPoint, gWidth, gHeight)
            recursiveMaze(
                startPoint.x + 1,
                startPoint.y + 1,
                gWidth + startPoint.x - 1,
                gHeight + startPoint.y - 1
            )
            gaps.clear()
        }
    }

    private fun generateBorder(startPoint: Point, gWidth: Int, gHeight: Int) {

        val xEnd = gWidth + startPoint.x
        val yEnd = gHeight + startPoint.y

        for (i in startPoint.x until xEnd) {
            mListener?.addData(Point(i, startPoint.y))
            mListener?.addData(Point(i, yEnd))
        }
        for (j in startPoint.y until yEnd + 1) {
            mListener?.addData(Point(startPoint.x, j))
            mListener?.addData(Point(xEnd, j))
        }
    }

    private fun recursiveMaze(x1: Int, y1: Int, x2: Int, y2: Int) {

        if (x2 - x1 < 2 || y2 - y1 < 2)
            return

        val isHorizontal =
            if (x2 - x1 < y2 - y1) true else if (x2 - x1 > y2 - y1) false else Random.nextBoolean()

        val rows = mutableListOf<Int>()
        val cols = mutableListOf<Int>()

        if (isHorizontal) {

            for (i in x1 until x2 + 1 step 2)
                rows.add(i)

            for (i in y1 + 1 until y2 step 2)
                cols.add(i)

            val randRow = floor(Math.random() * rows.size).toInt()
            var randCol = floor(Math.random() * cols.size).toInt()

            var cutHor = cols[randCol]

            while (gaps.contains(Point(x1 - 1, cutHor)) || gaps.contains(
                    Point(
                        x2 + 1,
                        cutHor
                    )
                )
            ) {
                if (cols.size < 3)
                    return
                randCol = floor(Math.random() * cols.size).toInt()
                cutHor = cols[randCol]
            }

            drawLineHor(x1, x2, cutHor)

            val gapHor = rows[randRow]
            gaps.add(Point(gapHor, cutHor))
            mListener?.removeData(Point(gapHor, cutHor))

            recursiveMaze(x1, y1, x2, cutHor - 1)
            recursiveMaze(x1, cutHor + 1, x2, y2)
        } else {

            for (i in x1 + 1 until x2 step 2)
                rows.add(i)

            for (i in y1 until y2 + 1 step 2)
                cols.add(i)

            var randRow = floor(Math.random() * rows.size).toInt()
            val randCol = floor(Math.random() * cols.size).toInt()

            var cutVer = rows[randRow]

            while (gaps.contains(
                    Point(
                        cutVer,
                        y1 - 1
                    )
                ) || gaps.contains(Point(cutVer, y2 + 1))
            ) {
                if (rows.size < 3)
                    return
                randRow = floor(Math.random() * rows.size).toInt()
                cutVer = rows[randRow]
            }

            drawLineVer(y1, y2, cutVer)

            val gapVer = cols[randCol]
            gaps.add(Point(cutVer, gapVer))
            mListener?.removeData(Point(cutVer, gapVer))

            recursiveMaze(x1, y1, cutVer - 1, y2)
            recursiveMaze(cutVer + 1, y1, x2, y2)
        }
    }

    private fun drawLineHor(x1: Int, x2: Int, y: Int) {
        for (i in x1..x2) {
            runBlocking {
                delay(sleepVal)
            }
            mListener?.addData(Point(i, y))
        }
    }

    private fun drawLineVer(y1: Int, y2: Int, x: Int) {
        for (i in y1..y2) {
            runBlocking {
                delay(sleepVal)
            }
            mListener?.addData(Point(x, i))
        }
    }

    fun setMazeGenerateListener(eventListener: OnMazeGenerateListener) {
        mListener = eventListener
    }

    interface OnMazeGenerateListener {
        fun addData(px: Point)
        fun removeData(px: Point)
    }
}