package com.akhilasdeveloper.pathfinder.algorithms.pathfinding

import android.view.Gravity
import android.widget.Toast
import com.akhilasdeveloper.pathfinder.MainActivity
import com.akhilasdeveloper.pathfinder.models.Point
import com.akhilasdeveloper.pathfinder.models.Square
import com.akhilasdeveloper.pathfinder.views.Keys
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main


internal fun MainActivity.findPathDijkstra() {

    CoroutineScope(Dispatchers.Default).launch {

        data?.let { data ->

            /** check if there is any start node or end node*/
            if (invalidateData()) {

                val gHeight = spanGrid.heightS
                val gWidth = spanGrid.widthS
                val startNode = spanGrid.startPont
                val endNode = spanGrid.endPont
                val startP = startNode!!.x + startNode.y * gWidth
                val endP = endNode!!.x + endNode.y * gWidth

                /** set start node distance as 0 and push to heap*/
                data[startP].distance = 0
                heapMin.push(startP, data)

                while (true) {

                    /** animation delay*/
                    delay(sleepVal)

                    /**
                     * fetch short node from heap
                     * if the heap returns null (heap is empty) the exit the loop. All nodes are visited and destination is not reachable
                     */
                    val shortNode: Int = heapMin.pull(data) ?: break
                    data[shortNode].isVisited = true

                    /**
                     * if short node == end node, then the destination is reached.
                     * Now the shortest path will be drawn by traversing backward using previous value of the node
                     * else set node as visited
                     */
                    if (shortNode == endP) {
                        var n: Int = shortNode
                        while (n != startP) {
                            delay(sleepVal)
                            if (data[n].type != Keys.START && data[n].type != Keys.END)
                                spanGrid.setRect(Point(n % gWidth, n / gWidth), Keys.PATH)
                            n = data[n].previous!!
                        }
                        break
                    } else {
                        if (data[shortNode].distance == Int.MAX_VALUE) break
                        if (data[shortNode].type != Keys.START)
                            spanGrid.setRect(
                                Point(shortNode % gWidth, shortNode / gWidth),
                                Keys.VISITED
                            )
                    }

                    /**
                     * Fetch all neighbours(top, left, bottom, right) of short node
                     */
                    val neighbours: Array<Int> =
                        getNeighbours(gHeight, gWidth, shortNode, data)

                    /**
                     * checking all the neighbours and comparing the distance with short distance + 1
                     * if the distance is greater, then assign short distance + 1 to neighbours
                     */
                    neighbours.forEach {
                        val dis = data[shortNode].distance + 1
                        if (dis < data[it].distance) {
                            data[it].distance = dis
                            spanGrid.drawSquares[it].distance = data[it].distance
                            heapMin.push(it, data)
                        }
                        data[it].previous = shortNode
                    }
                }
            }else{
                withContext(Main) {
                    Toast.makeText(applicationContext,"Please add Start node and End node.", Toast.LENGTH_SHORT).apply {
                        setGravity(Gravity.CENTER, 0, 0)
                        show()
                    }
                }
            }
        }
    }
}

/**
 * Function to find neighbours (top, left, bottom, right) of the short distance node
 */

private fun getNeighbours(
    gHeight: Int,
    gWidth: Int,
    shortNode: Int,
    grids: List<Square>
): Array<Int> {

    val xx = shortNode % gWidth
    val yy = shortNode / gWidth

    val n = mutableListOf<Int>()
    val points =
        arrayOf(
            Point(xx - 1, yy),
            Point(xx, yy - 1),
            Point(xx, yy + 1),
            Point(xx + 1, yy)
        )

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
