package com.akhilasdeveloper.pathfinder.algorithms.pathfinding

import com.akhilasdeveloper.pathfinder.MainActivity
import com.akhilasdeveloper.pathfinder.models.nodes
import com.akhilasdeveloper.pathfinder.views.Keys
import com.akhilasdeveloper.spangridview.models.Point
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main


internal fun MainActivity.findPathDijkstra() {

    CoroutineScope(Dispatchers.Default).launch {

        reset()

        val startP = startPont!!
        val endP = endPont!!

        /** set start node distance as 0 and push to heap*/
        gridHash[startP]?.distance = 0
        heapMin.push(startP, gridHash)

        while (true) {

            /** animation delay*/
            delay(sleepValPath)

            /**
             * fetch short node from heap
             * if the heap returns null (heap is empty) the exit the loop. All nodes are visited and destination is not reachable
             */
            val node = heapMin.pull(gridHash)
            if (node == null){
                withContext(Main){
                    setPathMessage("Dijkstra")
                }
                break
            }
            val shortNode: Point = node

            /**
             * if short node == end node, then the destination is reached.
             * Now the shortest path will be drawn by traversing backward using previous value of the node
             * else set node as visited
             */
            if (shortNode == endP) {
                var n: Point = shortNode
                while (n != startP) {
                    delay(sleepVal)
                    val nodeN = gridHash[n]
                    if (nodeN?.type != Keys.START && nodeN?.type != Keys.END) {
                        setBit(n, Keys.PATH)
                        pathNodesCount++
                    }
                    n = gridHash[n]?.previous!!
                }
                withContext(Main){
                    setPathMessage("Dijkstra")
                }
                break
            } else {
                if (gridHash[shortNode]?.distance == Int.MAX_VALUE) break
                val nodeN = gridHash[shortNode]
                if (nodeN?.type != Keys.START) {
                    setBit(
                        shortNode,
                        Keys.VISITED
                    )
                    visitedNodesCount++
                }
            }

            /**
             * Fetch all neighbours(top, left, bottom, right) of short node
             */
            val neighbours: Array<Point> = getNeighbours(shortNode)

            /**
             * checking all the neighbours and comparing the distance with short distance + 1
             * if the distance is greater, then assign short distance + 1 to neighbours
             */
            neighbours.forEach {
                val dis = gridHash[shortNode]!!.distance + gridHash[it]!!.weight
                if (dis < gridHash[it]!!.distance) {
                    gridHash[it]!!.distance = dis
                    heapMin.push(it, gridHash)
                }
                gridHash[it]!!.previous = shortNode
            }

        }


    }
}

/**
 * Function to find neighbours (top, left, bottom, right) of the short distance node
 */

internal fun MainActivity.getNeighbours(
    shortPoint: Point
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
            n.add(p)
        }
    }
    return n.toTypedArray()
}

internal fun MainActivity.getData(index: Point) = gridHash.getOrPut(index) { nodes() }
