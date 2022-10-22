package com.akhilasdeveloper.pathfinder.algorithms.pathfinding

import android.widget.Toast
import com.akhilasdeveloper.pathfinder.MainActivity
import com.akhilasdeveloper.pathfinder.models.nodes
import com.akhilasdeveloper.pathfinder.views.Keys
import com.akhilasdeveloper.spangridview.models.Point
import kotlinx.coroutines.*
import kotlin.math.sqrt


internal fun MainActivity.findPathAStar() {

    CoroutineScope(Dispatchers.Default).launch {

        createBorder()

        val startP = startPont!!
        val endP = endPont!!

        val openSet = mutableSetOf<Point>()
        val closedSet = mutableSetOf<Point>()

        openSet.add(startP)
        var currentPoint: Point = startP

        while (openSet.isNotEmpty()) {

            for (set in openSet) {
                if (getData(set).f < getData(currentPoint).f) {
                    currentPoint = set
                }
            }

            if (currentPoint == endP) {
                Toast.makeText(this@findPathAStar, "Done", Toast.LENGTH_SHORT).show()
                var n: Point = currentPoint
                while (n != startP) {
                    delay(sleepValPath)
                    val nodeN = gridHash[n]
                    if (nodeN?.type != Keys.START && nodeN?.type != Keys.END) {
                        setBit(n, Keys.PATH)
                    }
                    n = gridHash[n]?.previous!!
                }
                break
            }

            openSet.remove(currentPoint)
            closedSet.add(currentPoint)

            val neighbours: Array<Point> = getNeighbours(currentPoint, closedSet)

            for (nei in neighbours) {
                val tempG = gridHash[currentPoint]!!.g + 1

                if (openSet.contains(nei)){
                    if (tempG < gridHash[nei]!!.g)
                        gridHash[nei]!!.g = tempG
                }else{
                    gridHash[nei]!!.g = tempG
                    openSet.add(nei)
                }

                gridHash[nei]!!.h = heuristic(nei, endP)
                gridHash[nei]!!.f = gridHash[nei]!!.g + gridHash[nei]!!.h
                gridHash[nei]!!.previous = currentPoint

                val nodeN = gridHash[currentPoint]
                if (nodeN?.type != Keys.START) {
                    setBit(
                        currentPoint,
                        Keys.VISITED
                    )
                }
            }
        }

    }
}

private fun heuristic(nei: Point, endP: Point): Int {
    var xx = endP.x - nei.x
    var yy = endP.y - nei.y
    xx *= xx
    yy *= yy

    return sqrt(xx.toFloat() - yy).toInt()
}

/**
 * Function to find neighbours (top, left, bottom, right) of the short distance node
 */

private fun MainActivity.getNeighbours(
    shortPoint: Point,
    closedSet: MutableSet<Point>
): Array<Point> {

    val xx = shortPoint.x
    val yy = shortPoint.y

    val n = mutableListOf<Point>()
    val points =
        arrayOf(
            Point(xx - 1, yy),
            Point(xx, yy - 1),
            Point(xx, yy + 1),
            Point(xx + 1, yy)
        )

    points.forEach { p ->
        val data = getData(p)
        if (data.type == Keys.END ||
            data.type == Keys.AIR ||
            data.type == Keys.GRANITE ||
            data.type == Keys.GRASS ||
            data.type == Keys.SAND ||
            data.type == Keys.SNOW ||
            data.type == Keys.STONE ||
            data.type == Keys.WATER ||
            data.type == Keys.WATER_DEEP
        ) {
            if (!closedSet.contains(p))
                n.add(p)
        }
    }
    return n.toTypedArray()
}

