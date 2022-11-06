package com.akhilasdeveloper.pathfinder.algorithms.pathfinding

import com.akhilasdeveloper.pathfinder.R
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.*
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.AIR
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.ASTAR
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.BFS
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.DIJKSTRA
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.END
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.GRANITE
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.GRASS
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.PATH
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.SAND
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.SNOW
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.START
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.STONE
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.VISITED
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.WALL
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.WATER
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.WATER_DEEP
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.nodes
import com.akhilasdeveloper.spangridview.models.Point
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.math.abs

class FindPathAbstract {

    private var gridHash: HashMap<Point, Square> = hashMapOf()
    private var heapMin: HeapMinHash<Point> = HeapMinHash()
    var startPont: Point? = null
    var endPont: Point? = null

    private fun setGridHash(gridHash: Map<Point, Square>) {
        for (data in gridHash) {
            this.gridHash[data.key] =
                nodes(type = if (data.value.type == START) END else if (data.value.type == END) START else data.value.type)
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
                if (nodeN?.type != START) {
                    val data = getData(shortNode).copyToType(type = VISITED)
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

}