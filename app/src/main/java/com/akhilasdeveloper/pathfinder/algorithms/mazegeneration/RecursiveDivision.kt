package com.akhilasdeveloper.pathfinder.algorithms.mazegeneration

import android.util.Log
import com.akhilasdeveloper.pathfinder.MainActivity
import com.akhilasdeveloper.pathfinder.models.Point
import com.akhilasdeveloper.pathfinder.views.Keys
import kotlinx.coroutines.*
import kotlin.math.floor
import kotlin.random.Random

private var gaps = mutableListOf<Point>()

internal fun MainActivity.generateMaze() {
    CoroutineScope(Dispatchers.Default).launch {
        dataGrid?.let { data ->

            var gHeight = spanGrid.heightS
            var gWidth = spanGrid.widthS

            gHeight = if (gHeight % 2 == 0) gHeight - 1 else gHeight
            gWidth = if (gWidth % 2 == 0) gWidth - 1 else gWidth

            generateBorder(gWidth, gHeight)
            recursiveMaze(1, 1, gWidth - 1, gHeight - 1)
        }
    }
}

private fun MainActivity.generateBorder(gWidth: Int, gHeight: Int) {

    for (i in 0 until gWidth) {
        spanGrid.setRect(Point(i, 0), Keys.BLOCK)
        spanGrid.setRect(Point(i, gHeight - 1), Keys.BLOCK)
    }
    for (j in 0 until gHeight) {
        spanGrid.setRect(Point(0, j), Keys.BLOCK)
        spanGrid.setRect(Point(gWidth - 1, j), Keys.BLOCK)
    }
}

private fun MainActivity.recursiveMaze(x1: Int, y1: Int, x2: Int, y2: Int) {

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

        while (gaps.contains(Point(x1 - 1, cutHor)) || gaps.contains(Point(x2 + 1, cutHor))) {
            if (cols.size < 3)
                return
            randCol = floor(Math.random() * cols.size).toInt()
            cutHor = cols[randCol]
        }

        drawLineHor(x1, x2, cutHor)

        val gapHor = rows[randRow]
        gaps.add(Point(gapHor, cutHor))
        spanGrid.setRect(Point(gapHor, cutHor), Keys.EMPTY)

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

        while (gaps.contains(Point(cutVer, y1 - 1)) || gaps.contains(Point(cutVer, y2 + 1))) {
            if (rows.size < 3)
                return
            randRow = floor(Math.random() * rows.size).toInt()
            cutVer = rows[randRow]
        }

        drawLineVer(y1, y2, cutVer)

        val gapVer = cols[randCol]
        gaps.add(Point(cutVer, gapVer))
        spanGrid.setRect(Point(cutVer, gapVer), Keys.EMPTY)

        recursiveMaze(x1, y1, cutVer - 1, y2)
        recursiveMaze(cutVer + 1, y1, x2, y2)
    }
}

private fun MainActivity.drawLineHor(x1: Int, x2: Int, y: Int) {
    for (i in x1..x2) {
        runBlocking {
            delay(sleepVal)
        }
        Log.d("Output Horizontal : ", "$i , $y")
        spanGrid.setRect(Point(i, y), Keys.BLOCK)
    }
}

private fun MainActivity.drawLineVer(y1: Int, y2: Int, x: Int) {
    for (i in y1..y2) {
        runBlocking {
            delay(sleepVal)
        }
        Log.d("Output Vertical : ", "$x , $i")
        spanGrid.setRect(Point(x, i), Keys.BLOCK)
    }
}