package com.akhilasdeveloper.pathfinder.algorithms.pathfinding

import com.akhilasdeveloper.pathfinder.R
import com.akhilasdeveloper.spangridview.models.Point
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class FindPath {

    companion object {

        const val DIJKSTRA_ALGORITHM = "Dijkstra"
        const val ASTAR_ALGORITHM = "A*"
        const val BFS_ALGORITHM = "BFS"

        const val AIR_NODE: Int = -1
        const val GRANITE_NODE: Int = -2
        const val GRASS_NODE: Int = -3
        const val FOREST_NODE: Int = -4
        const val SAND_NODE: Int = -5
        const val SNOW_NODE: Int = -6
        const val STONE_NODE: Int = -7
        const val WATER_NODE: Int = -8
        const val WATER_DEEP_NODE: Int = -9
        const val WALL_NODE: Int = -100
        const val START_NODE: Int = -110
        const val END_NODE: Int = -120
        const val PATH_NODE: Int = -130
        const val VISITED_NODE: Int = -140

        val nodes = listOf(
            START_NODE,
            END_NODE,
            WALL_NODE,
            AIR_NODE,
            GRANITE_NODE,
            GRASS_NODE,
            FOREST_NODE,
            SAND_NODE,
            SNOW_NODE,
            STONE_NODE,
            WATER_NODE,
            WATER_DEEP_NODE
        )

        fun nodes(type: Int? = null): Square {
            return when (type) {
                WALL_NODE -> Square(
                    name = "Wall Node",
                    type = WALL_NODE,
                    weight = Int.MAX_VALUE,
                    color = R.color.block
                )
                START_NODE -> Square(
                    name = "Start Node",
                    type = START_NODE,
                    distance = 0,
                    color = R.color.start
                )
                END_NODE -> Square(name = "End Node", type = END_NODE, color = R.color.end)
                PATH_NODE -> Square(name = "Path Node", type = PATH_NODE, color = R.color.path)
                VISITED_NODE -> Square(
                    name = "Visited Node",
                    type = VISITED_NODE,
                    color = R.color.visited
                )
                AIR_NODE -> Square(name = "Air Node", type = AIR_NODE, color = R.color.empty)
                GRANITE_NODE -> Square(
                    name = "Granite Node",
                    type = GRANITE_NODE,
                    weight = 50,
                    color = R.color.granite
                )
                GRASS_NODE -> Square(
                    name = "Grass Node",
                    type = GRASS_NODE,
                    weight = 5,
                    color = R.color.grass
                )
                FOREST_NODE -> Square(
                    name = "Forest Node",
                    type = GRASS_NODE,
                    weight = 7,
                    color = R.color.forest
                )
                SAND_NODE -> Square(
                    name = "Sand Node",
                    type = SAND_NODE,
                    weight = 10,
                    color = R.color.sand
                )
                SNOW_NODE -> Square(
                    name = "Snow Node",
                    type = SNOW_NODE,
                    weight = 75,
                    color = R.color.snow
                )
                STONE_NODE -> Square(
                    name = "Stone Node",
                    type = STONE_NODE,
                    weight = 25,
                    color = R.color.stone
                )
                WATER_NODE -> Square(
                    name = "Water Node",
                    type = WATER_NODE,
                    weight = 50,
                    color = R.color.water
                )
                WATER_DEEP_NODE -> Square(
                    name = "Deep Water Node",
                    type = WATER_DEEP_NODE,
                    weight = 100,
                    color = R.color.water_deep
                )
                else -> Square(name = "Air Node", type = AIR_NODE, color = R.color.empty)
            }
        }
    }

    private var gridHash: ConcurrentHashMap<Point, Square> = ConcurrentHashMap()
    private var gridHashBackup: ConcurrentHashMap<Point, Square> = ConcurrentHashMap()
    private var heapMin: HeapMinHash<Point> = HeapMinHash()

    var startPont: Point? = null
    var endPont: Point? = null

    var sleepVal = 0L

    private var totalDelayInMillis = 0L
    private var startedTimeInMillis = 0L
    private var visitedNodesCount = 0
    private var pathNodesCount = 0
    private var executionCompleted = false

    private var pathFindListener: OnPathFindListener? = null

    private var mainJob:Job = Job()
    private var job:Job = Job()

    private fun resetVars() {
        startedTimeInMillis = System.currentTimeMillis()
        visitedNodesCount = 0
        pathNodesCount = 0
        totalDelayInMillis = 0
        executionCompleted = false
    }

    fun reset() {

        resetVars()
        job.cancel()
        mainJob.cancel()
        if (gridHashBackup.isEmpty()) {
            copyGridHash()
        } else {
            gridHash.clear()
            heapMin.clear()
            restoreGridHash()
        }
        pathFindListener?.onClearResult(gridHash.toMap())
    }

    fun resetForDrawing() {
        if (executionCompleted)
            reset()
        gridHashBackup.clear()
    }

    fun resetAll() {
        resetVars()
        job.cancel()
        mainJob.cancel()
        gridHashBackup.clear()
        gridHash.clear()
        heapMin.clear()
        startPont = null
        endPont = null
        pathFindListener?.onClearAll(gridHash.toMap())
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


    fun addData(point: Point, type: Int) {

        val data = getData(point).copyToType(type = type)
        gridHash[point] = data

        pathFindListener?.onDrawPoint(point, data.color, data.fillColor)

    }

    fun getPoint(point: Point) = gridHash[point]

    fun removeData(point: Point) {
        gridHash.remove(point)
        pathFindListener?.onClearPoint(point)
    }

    fun findPath(type: String) {

        reset()

        mainJob = CoroutineScope(Dispatchers.Default).launch {

            if (startPont == null || endPont == null) {
                withContext(Dispatchers.Main) {
                    pathFindListener?.onError("Please select start point and end point")
                }
                return@launch
            }

            var haveSolution = true

            job = launch {
                val abstractFindPath = FindPathAbstract()
                abstractFindPath.startPont = endPont
                abstractFindPath.endPont = startPont
                haveSolution = abstractFindPath.findPath(gridHash.toMap())
            }

            val startP = startPont!!
            val endP = endPont!!

            /** set start node distance as 0 and push to heap*/
            gridHash[startP]?.distance = 0
            heapMin.push(startP, gridHash)

            while (true) {

                /** animation delay*/
                delay(sleepVal)
                totalDelayInMillis += sleepVal

                /**
                 * fetch short node from heap
                 * if the heap returns null (heap is empty) the exit the loop. All nodes are visited and destination is not reachable
                 */
                val node = heapMin.pull(gridHash)
                if (node == null || !haveSolution) {
                    withContext(Dispatchers.Main) {
                        pathFindListener?.onPathNotFound(
                            type = type,
                        )
                        executionCompleted = true
                        job.cancelAndJoin()
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
                        totalDelayInMillis += sleepVal
                        val nodeN = gridHash[n]
                        if (nodeN?.type != START_NODE && nodeN?.type != END_NODE) {
                            addData(n, PATH_NODE)
                            pathNodesCount++
                        }
                        n = gridHash[n]?.previous!!
                    }
                    withContext(Dispatchers.Main) {
                        pathFindListener?.onPathFound(
                            type = type,
                            PathSummary(
                                completedTimeMillis = System.currentTimeMillis() - startedTimeInMillis,
                                totalDelayMillis = totalDelayInMillis,
                                visitedNodesCount = visitedNodesCount,
                                pathNodesCount = pathNodesCount
                            )
                        )
                        executionCompleted = true
                        job.cancelAndJoin()
                    }
                    break
                } else {
                    if (gridHash[shortNode]?.distance == Int.MAX_VALUE) break
                    val nodeN = gridHash[shortNode]
                    if (nodeN?.type != START_NODE) {
                        addData(
                            shortNode,
                            VISITED_NODE
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
                        DIJKSTRA_ALGORITHM -> {
                            val dis = gridHash[shortNode]!!.distance + gridHash[it]!!.weight
                            if (dis < gridHash[it]!!.distance) {
                                gridHash[it]!!.distance = dis
                                heapMin.push(it, gridHash)
                            }
                            gridHash[it]!!.previous = shortNode
                        }
                        ASTAR_ALGORITHM -> {
                            val tempG = gridHash[shortNode]!!.g + gridHash[it]!!.weight
                            gridHash[it]!!.g = tempG

                            gridHash[it]!!.h = heuristic(it, endP)
                            gridHash[it]!!.distance = gridHash[it]!!.g + gridHash[it]!!.h
                            heapMin.push(it, gridHash)

                            gridHash[it]!!.previous = shortNode
                        }
                        BFS_ALGORITHM -> {
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
            if (data.type == END_NODE ||
                data.type == AIR_NODE ||
                data.type == GRANITE_NODE ||
                data.type == GRASS_NODE ||
                data.type == SAND_NODE ||
                data.type == SNOW_NODE ||
                data.type == STONE_NODE ||
                data.type == WATER_NODE ||
                data.type == WATER_DEEP_NODE
            ) {
                n.add(p)
            }
        }
        return n.toTypedArray()
    }

    private fun getData(index: Point) = gridHash.getOrPut(index) { nodes() }

    private fun heuristic(nei: Point, endP: Point): Int = abs(nei.x - endP.x) + abs(nei.y - endP.y)

    fun setPathFindListener(eventListener: OnPathFindListener) {
        pathFindListener = eventListener
    }

    interface OnPathFindListener {
        fun onPathNotFound(type: String)
        fun onPathFound(type: String, summary: PathSummary)
        fun onError(message: String)
        fun onDrawPoint(px: Point, color1: Int, color2: Int)
        fun onClearPoint(px: Point)
        fun onClearResult(gridHash: Map<Point, Square>)
        fun onClearAll(gridHash: Map<Point, Square>)
    }

    data class PathSummary(
        val completedTimeMillis: Long,
        val totalDelayMillis: Long,
        val visitedNodesCount: Int,
        val pathNodesCount: Int,
    )

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
                if ((type == PATH_NODE || type == VISITED_NODE) && this.type != AIR_NODE) this.color else node.color
            return node.copy(distance = this.distance, previous = this.previous, color = color)
        }

    }


}