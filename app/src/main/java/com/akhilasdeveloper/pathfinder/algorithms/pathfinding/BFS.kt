package com.akhilasdeveloper.pathfinder.algorithms.pathfinding

import com.akhilasdeveloper.pathfinder.MainActivity
import com.akhilasdeveloper.pathfinder.models.nodes
import com.akhilasdeveloper.pathfinder.views.Keys
import com.akhilasdeveloper.spangridview.models.Point
import kotlinx.coroutines.*


internal fun MainActivity.findPathBFS() {

    CoroutineScope(Dispatchers.Default).launch {

        createBorder()

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
            val shortNode: Point = node ?: break

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
                    }
                    n = gridHash[n]?.previous!!
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
                val dis = gridHash[shortNode]!!.distance + 1
                if (dis < gridHash[it]!!.distance) {
                    gridHash[it]!!.distance = dis
                    heapMin.push(it, gridHash)
                }
                gridHash[it]!!.previous = shortNode
            }

        }


    }
}

