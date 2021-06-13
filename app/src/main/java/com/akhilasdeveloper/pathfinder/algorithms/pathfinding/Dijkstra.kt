package com.akhilasdeveloper.pathfinder.algorithms.pathfinding

import android.util.Log
import com.akhilasdeveloper.pathfinder.MainActivity
import com.akhilasdeveloper.pathfinder.models.Point
import com.akhilasdeveloper.pathfinder.models.Square
import com.akhilasdeveloper.pathfinder.views.Keys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private var short = 0

internal fun MainActivity.findPathDijkstra() {

    CoroutineScope(Dispatchers.Default).launch {
        data?.let { data ->

            val gHeight = spanGrid.heightS
            val gWidth = spanGrid.widthS
            val startNode = spanGrid.startPont
            val endNode = spanGrid.endPont
            val startP = startNode!!.x + startNode.y * gWidth
            val endP = endNode!!.x + endNode.y * gWidth

            if (!invalidateData()) this.cancel()

            val grid = data.toTypedArray()

            grid[startP].distance = 0

            while (grid.isNotEmpty()) {

                val shortNode: Int = shortGridNode(grid)
                grid[shortNode].isVisited = true

                Log.d("findPath : neighbours", "$shortNode")

                if (shortNode == endP) {
                    var n: Int = shortNode
                    while (n != startP) {
                        if (grid[n].type != Keys.START && grid[n].type != Keys.END)
                            spanGrid.setRect(Point(n % gWidth, n / gWidth), Keys.PATH)
                        n = grid[n].previous!!
                    }
                    break
                } else {
                    if (grid[shortNode].distance == Int.MAX_VALUE) break
                    if (grid[shortNode].type != Keys.START)
                        spanGrid.setRect(
                            Point(shortNode % gWidth, shortNode / gWidth),
                            Keys.VISITED
                        )
                }

                val neighbours: Array<Int> =
                    getNeighbours(gHeight, gWidth, shortNode, grid)

                neighbours.forEach {
                    val dis = grid[shortNode].distance + 1
                    if (dis < grid[it].distance) {
                        grid[it].distance = dis
                        spanGrid.drawSquares[it].distance = grid[it].distance
                    }
                    grid[it].previous = shortNode
                }
            }
        }
    }
}

private fun getNeighbours(
    gHeight: Int,
    gWidth: Int,
    shortNode: Int,
    grids: Array<Square>
): Array<Int> {

    val xx = shortNode % gWidth
    val yy = shortNode / gWidth


    val n = mutableListOf<Int>()
    val points =
        arrayOf(
            Point(xx, yy - 1),
            Point(xx + 1, yy),
            Point(xx, yy + 1),
            Point(xx - 1, yy))

    points.forEach { p ->
        if (p.x in 0 until gWidth && p.y in 0 until gHeight) {
            val pi = p.x + p.y * gWidth
            grids[pi].let {
                if (it.type == Keys.EMPTY || it.type == Keys.END) {
                    n.add(pi)
                }
            }
        }
    }
    return n.toTypedArray()
}

private fun shortGridNode(grids: Array<Square>): Int {
    var index: Int? = null

    for ((i, j) in grids.withIndex()) {
        if (!j.isVisited) {
            if (index == null) {
                index = i
            }
            if (grids[index].distance > j.distance)
                index = i
            if (j.distance <= short) {
                short = j.distance
                return i
            }
        }
    }
    short = grids[index!!].distance
    return index
}