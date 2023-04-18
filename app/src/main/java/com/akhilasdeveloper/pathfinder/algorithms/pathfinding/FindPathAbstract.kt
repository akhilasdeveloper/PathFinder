package com.akhilasdeveloper.pathfinder.algorithms.pathfinding

import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.*
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.AIR_NODE
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.END_NODE
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.GRANITE_NODE
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.GRASS_NODE
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.SAND_NODE
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.SNOW_NODE
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.START_NODE
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.STONE_NODE
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.VISITED_NODE
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.WATER_NODE
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.WATER_DEEP_NODE
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.nodes
import com.akhilasdeveloper.spangridview.models.Point
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

class FindPathAbstract {

    private var gridHash: ConcurrentHashMap<Point, Square> = ConcurrentHashMap()
    private var heapMin: HeapMinHash<Point> = HeapMinHash()
    var startPont: Point? = null
    var endPont: Point? = null

    private fun setGridHash(gridHash: Map<Point, Square>) {
        for (data in gridHash) {
            this.gridHash[data.key] =
                nodes(type = if (data.value.type == START_NODE) END_NODE else if (data.value.type == END_NODE) START_NODE else data.value.type)
        }
    }

    suspend fun findPath(type: Map<Point, Square>): Boolean {
        setGridHash(type)

        val hasValue: Boolean

        if (startPont == null || endPont == null) {
            return false
        }


        val startP = startPont!!
        val endP = endPont!!

        gridHash[startP]?.distance = 0
        heapMin.push(startP, gridHash)

        while (true) {

            yield()

            val node = heapMin.pull(gridHash)
            if (node == null) {
                hasValue = false
                break
            }
            val shortNode: Point = node

            if (shortNode == endP) {
                hasValue = true
                break
            } else {
                if (gridHash[shortNode]?.distance == Int.MAX_VALUE) {
                    hasValue = false
                    break
                }
                val nodeN = gridHash[shortNode]
                if (nodeN?.type != START_NODE) {
                    val data = getData(shortNode).copyToType(type = VISITED_NODE)
                    gridHash[shortNode] = data
                }
            }

            val neighbours: Array<Point> = getNeighbours(shortNode)

            neighbours.forEach {
                val dis = gridHash[shortNode]!!.distance + 1
                if (dis < gridHash[it]!!.distance) {
                    gridHash[it]!!.distance = dis
                    heapMin.push(it, gridHash)
                }
                gridHash[it]!!.previous = shortNode
            }

            Timber.d("Finding Path ************")
        }
        return hasValue
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

}