package com.akhilasdeveloper.pathfinder.algorithms.pathfinding

import com.akhilasdeveloper.pathfinder.MainActivity
import com.akhilasdeveloper.pathfinder.models.Node
import com.akhilasdeveloper.pathfinder.models.Point
import kotlinx.coroutines.*


internal fun MainActivity.findPathDijkstra() {

    CoroutineScope(Dispatchers.Default).launch {

        val startP = startPont!!
        val endP = endPont!!

        /** set start node distance as 0 and push to heap*/

        heapMin.push(startP, gridHash)

        while (true) {

            /** animation delay*/
            delay(sleepVal)

            /**
             * fetch short node from heap
             * if the heap returns null (heap is empty) the exit the loop. All nodes are visited and destination is not reachable
             */
            val shortNode: Point = heapMin.pull(gridHash) ?: break

            /**
             * if short node == end node, then the destination is reached.
             * Now the shortest path will be drawn by traversing backward using previous value of the node
             * else set node as visited
             */
            if (shortNode == endP) {
                var n: Point = shortNode
                while (n != startP) {
                    delay(sleepVal)
                    if (gridHash[n] !is Node.StartNode && gridHash[n] !is Node.EndNode) {
                        setBit(n, Node.PathNode())
                        gridCanvasView.play()
                    }
                    n = gridHash[n]?.previous!!
                }
                break
            } else {
                if (gridHash[shortNode]?.distance == Int.MAX_VALUE) break
                if (gridHash[shortNode] !is Node.StartNode) {
                    setBit(
                        shortNode,
                        gridHash[shortNode]!!.asVisited()
                    )
                    gridCanvasView.play()
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

private fun MainActivity.getNeighbours(
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
        if (data is Node.AirNode || data is Node.EndNode) {
            n.add(p)
        }
    }
    return n.toTypedArray()
}

internal fun MainActivity.getData(index: Point) = gridHash.getOrPut(index) { Node.AirNode() }
