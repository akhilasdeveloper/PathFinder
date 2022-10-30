package com.akhilasdeveloper.pathfinder.algorithms.pathfinding

import com.akhilasdeveloper.pathfinder.algorithms.HeapMinHash
import com.akhilasdeveloper.pathfinder.models.Keys
import com.akhilasdeveloper.pathfinder.models.Square
import com.akhilasdeveloper.pathfinder.models.nodes
import com.akhilasdeveloper.spangridview.models.Point
import kotlinx.coroutines.*
import kotlin.math.abs

class FindPath {

    companion object {
        const val DIJKSTRA = "Dijkstra"
        const val ASTAR = "A*"
        const val BFS = "BFS"
        const val DFS = "DFS"
    }

    private var gridHash: HashMap<Point, Square> = hashMapOf()
    private var gridHashBackup: HashMap<Point, Square> = hashMapOf()
    private var heapMin: HeapMinHash<Point> = HeapMinHash()
    var startPont: Point? = null
    var endPont: Point? = null
    private var xHash: HashMap<Int, Int> = hashMapOf()
    private var yHash: HashMap<Int, Int> = hashMapOf()
    var sleepVal = 0L
    var sleepValPath = 0L
    private var totalDelayMillis = 0L
    private var startedTimeInMillis = 0L
    private var visitedNodesCount = 0
    private var pathNodesCount = 0
    var executionCompleted = false
        private set
    private var mListener: OnPathFindListener? = null

    data class PathSummary(
        val timeMillis: Long,
        val totalDelayMillis: Long,
        val visitedNodesCount: Int,
        val pathNodesCount: Int,
    )

    private fun resetVars() {
        startedTimeInMillis = System.currentTimeMillis()
        visitedNodesCount = 0
        pathNodesCount = 0
        totalDelayMillis = 0
        executionCompleted = false
    }

    fun reset() {
        resetVars()
        if (gridHashBackup.isEmpty()) {
            createBorder()
            copyGridHash()
        } else {
            gridHash.clear()
            heapMin.clear()
            restoreGridHash()
        }
        mListener?.onReset(gridHash.toMap())
    }

    fun resetAll() {
        resetVars()
        gridHashBackup.clear()
        gridHash.clear()
        heapMin.clear()
        xHash.clear()
        yHash.clear()
        gaps.clear()
        startPont = null
        endPont = null
        mListener?.onResetAll()
    }

    private fun restoreGridHash() {
        for (data in gridHashBackup) {
            gridHash[data.key] = nodes(type = data.value.type)
        }
    }

    private fun copyGridHash() {
        for (data in gridHash) {
            gridHashBackup[data.key] = nodes(type = data.value.type)
        }
    }

    private fun createBorder() {
        var minPoint = getMinXY()
        var maxPoint = getMaxXY()

        minPoint = minPoint.apply {
            x -= 1
            y -= 1
        }

        maxPoint = maxPoint.apply {
            x += 1
            y += 1
        }

        val width = maxPoint.x - minPoint.x
        val height = maxPoint.y - minPoint.y

        generateBorder(minPoint, width, height)
    }

    fun generateBorder(startPoint: Point, gWidth: Int, gHeight: Int) {

        val xEnd = gWidth + startPoint.x
        val yEnd = gHeight + startPoint.y

        for (i in startPoint.x until xEnd) {
            addData(Point(i, startPoint.y), Keys.WALL)
            addData(Point(i, yEnd), Keys.WALL)
        }
        for (j in startPoint.y until yEnd + 1) {
            addData(Point(startPoint.x, j), Keys.WALL)
            addData(Point(xEnd, j), Keys.WALL)
        }
    }

    private fun getMinXY(): Point {
        xHash.keys.toIntArray().minOrNull()?.let { x ->
            yHash.keys.toIntArray().minOrNull()?.let { y ->
                return Point(x, y)
            }
        }

        return Point(0, 0)
    }

    private fun getMaxXY(): Point {
        xHash.keys.toIntArray().maxOrNull()?.let { x ->
            yHash.keys.toIntArray().maxOrNull()?.let { y ->
                return Point(x, y)
            }
        }

        return Point(0, 0)
    }

    fun addData(point: Point, type: Int){
        totalDelayMillis += sleepVal

        setXY(point)

        val data = getData(point).copyToType(type = type)
        gridHash[point] = data

        mListener?.drawPoint(point, data.color, data.fillColor)

    }

    fun removeData(point: Point){
        clearXY(point)
        gridHash.remove(point)
        mListener?.clearPoint(point)
    }

    private fun setXY(point: Point) {
        if (gridHash[point] == null) {
            val x = xHash[point.x] ?: 0
            val y = yHash[point.y] ?: 0
            xHash[point.x] = x + 1
            yHash[point.y] = y + 1
        }
    }

    private fun clearXY(point: Point) {
        gridHash[point]?.let {
            xHash[point.x]?.let {
                if (it <= 1)
                    xHash.remove(point.x)
                else
                    xHash[point.x] = it - 1
            }
            yHash[point.y]?.let {
                if (it <= 1)
                    yHash.remove(point.y)
                else
                    yHash[point.y] = it - 1
            }
        }
    }

    fun findPath(type: String) {

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
                if (node == null) {
                    withContext(Dispatchers.Main) {
                        mListener?.onPathNotFound(
                            type = type,
                        )
                        executionCompleted = true
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
                            addData(n, Keys.PATH)
                            pathNodesCount++
                        }
                        n = gridHash[n]?.previous!!
                    }
                    withContext(Dispatchers.Main) {
                        mListener?.onPathFound(
                            type = type,
                            PathSummary(
                                timeMillis = (System.currentTimeMillis() - startedTimeInMillis) - totalDelayMillis,
                                totalDelayMillis = totalDelayMillis,
                                visitedNodesCount = visitedNodesCount,
                                pathNodesCount = pathNodesCount
                            )
                        )
                        executionCompleted = true
                    }
                    break
                } else {
                    if (gridHash[shortNode]?.distance == Int.MAX_VALUE) break
                    val nodeN = gridHash[shortNode]
                    if (nodeN?.type != Keys.START) {
                        addData(
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
                    when (type) {
                        DIJKSTRA -> {
                            val dis = gridHash[shortNode]!!.distance + gridHash[it]!!.weight
                            if (dis < gridHash[it]!!.distance) {
                                gridHash[it]!!.distance = dis
                                heapMin.push(it, gridHash)
                            }
                            gridHash[it]!!.previous = shortNode
                        }
                        ASTAR -> {
                            val tempG = gridHash[shortNode]!!.g + gridHash[it]!!.weight
                            gridHash[it]!!.g = tempG

                            gridHash[it]!!.h = heuristic(it, endP)
                            gridHash[it]!!.distance = gridHash[it]!!.g + gridHash[it]!!.h
                            heapMin.push(it, gridHash)

                            gridHash[it]!!.previous = shortNode
                        }
                        BFS -> {
                            val dis = gridHash[shortNode]!!.distance + 1
                            if (dis < gridHash[it]!!.distance) {
                                gridHash[it]!!.distance = dis
                                heapMin.push(it, gridHash)
                            }
                            gridHash[it]!!.previous = shortNode
                        }
                        DFS -> {

                        }
                    }

                }

            }


        }
    }

    private fun getNeighbours(
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

    private fun getData(index: Point) = gridHash.getOrPut(index) { nodes() }

    private fun heuristic(nei: Point, endP: Point): Int = abs(nei.x - endP.x) + abs(nei.y - endP.y)

    fun setPathFindListener(eventListener: OnPathFindListener) {
        mListener = eventListener
    }

    interface OnPathFindListener {
        fun onPathNotFound(type: String)
        fun onPathFound(type: String, summary: PathSummary)
        fun drawPoint(px: Point, color1: Int, color2: Int)
        fun clearPoint(px: Point)
        fun onReset(gridHash: Map<Point, Square>)
        fun onResetAll()
    }
}