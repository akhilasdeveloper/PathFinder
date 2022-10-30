package com.akhilasdeveloper.pathfinder.algorithms.pathfinding

import com.akhilasdeveloper.pathfinder.R
import com.akhilasdeveloper.spangridview.models.Point
import kotlinx.coroutines.*
import kotlin.math.abs

class FindPath {

    companion object {
        const val DIJKSTRA = "Dijkstra"
        const val ASTAR = "A*"
        const val BFS = "BFS"
        const val DFS = "DFS"

        const val AIR: Int = -2
        const val GRANITE: Int = -3
        const val GRASS: Int = -4
        const val SAND: Int = -5
        const val SNOW: Int = -6
        const val STONE: Int = -7
        const val WATER: Int = -8
        const val WATER_DEEP: Int = -9
        const val WALL: Int = -1
        const val START: Int = -10
        const val END: Int = -11
        const val PATH: Int = -12
        const val VISITED: Int = -13

        val nodes = listOf(
            START,
            END,
            WALL,
            AIR,
            GRANITE,
            GRASS,
            SAND,
            SNOW,
            STONE,
            WATER,
            WATER_DEEP
        )

        fun nodes(type: Int? = null): Square {
            return when (type) {
                WALL -> Square(
                    name = "Wall Node",
                    type = WALL,
                    weight = Int.MAX_VALUE,
                    color = R.color.block
                )
                START -> Square(
                    name = "Start Node",
                    type = START,
                    distance = 0,
                    color = R.color.start
                )
                END -> Square(name = "End Node", type = END, color = R.color.end)
                PATH -> Square(name = "Path Node", type = PATH, color = R.color.path)
                VISITED -> Square(name = "Visited Node", type = VISITED, color = R.color.visited)
                AIR -> Square(name = "Air Node", type = AIR, color = R.color.empty)
                GRANITE -> Square(
                    name = "Granite Node",
                    type = GRANITE,
                    weight = 50,
                    color = R.color.granite
                )
                GRASS -> Square(
                    name = "Grass Node",
                    type = GRASS,
                    weight = 5,
                    color = R.color.grass
                )
                SAND -> Square(name = "Sand Node", type = SAND, weight = 7, color = R.color.sand)
                SNOW -> Square(name = "Snow Node", type = SNOW, weight = 75, color = R.color.snow)
                STONE -> Square(
                    name = "Stone Node",
                    type = STONE,
                    weight = 25,
                    color = R.color.stone
                )
                WATER -> Square(
                    name = "Water Node",
                    type = WATER,
                    weight = 50,
                    color = R.color.water
                )
                WATER_DEEP -> Square(
                    name = "Deep Water Node",
                    type = WATER_DEEP,
                    weight = 100,
                    color = R.color.water_deep
                )
                else -> Square(name = "Air Node", type = AIR, color = R.color.empty)
            }
        }
    }

    private var gridHash: HashMap<Point, Square> = hashMapOf()
    private var gridHashBackup: HashMap<Point, Square> = hashMapOf()
    private var heapMin: HeapMinHash<Point> = HeapMinHash()
    var startPont: Point? = null
    var endPont: Point? = null
    private var xHash: HashMap<Int, Int> = hashMapOf()
    private var yHash: HashMap<Int, Int> = hashMapOf()
    var sleepVal = 0L
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
            copyGridHash()
        } else {
            gridHash.clear()
            heapMin.clear()
            restoreGridHash()
        }
        createBorder(true)
        mListener?.onReset(gridHash.toMap())
    }

    fun resetForDrawing(){
        if (executionCompleted)
            reset()
        gridHashBackup.clear()
    }

    fun resetAll() {
        resetVars()
        gridHashBackup.clear()
        gridHash.clear()
        heapMin.clear()
        xHash.clear()
        yHash.clear()
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

    private fun createBorder(isRemove:Boolean = false) {
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

        generateBorder(minPoint, width, height, isRemove)
    }

    private fun generateBorder(startPoint: Point, gWidth: Int, gHeight: Int, isRemove: Boolean) {

        val xEnd = gWidth + startPoint.x
        val yEnd = gHeight + startPoint.y

        for (i in startPoint.x until xEnd) {
            if (isRemove){
                removeData(Point(i, startPoint.y))
                removeData(Point(i, yEnd))
            }else{
                addData(Point(i, startPoint.y), WALL)
                addData(Point(i, yEnd), WALL)
            }
        }
        for (j in startPoint.y until yEnd + 1) {
            if (isRemove) {
                removeData(Point(startPoint.x, j))
                removeData(Point(xEnd, j))
            }else{
                addData(Point(startPoint.x, j), WALL)
                addData(Point(xEnd, j), WALL)
            }
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

    fun addData(point: Point, type: Int) {
        totalDelayMillis += sleepVal

        setXY(point)

        val data = getData(point).copyToType(type = type)
        gridHash[point] = data

        mListener?.drawPoint(point, data.color, data.fillColor)

    }

    fun removeData(point: Point) {
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

        if (startPont == null || endPont == null) {
            mListener?.onError("Please select start point and end point")
            return
        }


        CoroutineScope(Dispatchers.Default).launch {

            reset()
            createBorder()

            val startP = startPont!!
            val endP = endPont!!

            /** set start node distance as 0 and push to heap*/
            gridHash[startP]?.distance = 0
            heapMin.push(startP, gridHash)

            while (true) {

                /** animation delay*/
                delay(sleepVal)

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
                        if (nodeN?.type != START && nodeN?.type != END) {
                            addData(n, PATH)
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
                    if (nodeN?.type != START) {
                        addData(
                            shortNode,
                            VISITED
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
            if (data.type == END ||
                data.type == AIR ||
                data.type == GRANITE ||
                data.type == GRASS ||
                data.type == SAND ||
                data.type == SNOW ||
                data.type == STONE ||
                data.type == WATER ||
                data.type == WATER_DEEP
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
        fun onError(message: String)
        fun onPathFound(type: String, summary: PathSummary)
        fun drawPoint(px: Point, color1: Int, color2: Int)
        fun clearPoint(px: Point)
        fun onReset(gridHash: Map<Point, Square>)
        fun onResetAll()
    }

    data class Square(
        var name: String,
        var type: Int,
        var distance: Int = Int.MAX_VALUE,
        var weight: Int = 1,
        var previous: Point? = null,
        var color: Int,
        var fillColor: Int = color,

        var f: Int = 0,
        var g: Int = 0,
        var h: Int = 0,
    ) {

        fun copyToType(type: Int): Square {
            val node = nodes(type)
            val color =
                if ((type == PATH || type == VISITED) && this.type != AIR) this.color else node.color
            return node.copy(distance = this.distance, previous = this.previous, color = color)
        }

    }


}