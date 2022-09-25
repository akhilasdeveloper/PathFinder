package com.akhilasdeveloper.pathfinder.algorithms.quadtree

import com.akhilasdeveloper.pathfinder.models.Point
import com.akhilasdeveloper.pathfinder.models.PointF

data class QuadTree(
    val boundary: RectangleCentered,
    val capacity: Int
) {

    private val points: MutableSet<Point> = mutableSetOf()
    private var isSubDivided: Boolean = false

    private var northWest: QuadTree? = null
    private var northEast: QuadTree? = null
    private var southWest: QuadTree? = null
    private var southEast: QuadTree? = null

    fun insert(point: Point): Boolean {

        if (!boundary.contains(point))
            return false

        return if (points.size < capacity) {
            points.add(point)
            true
        } else {
            if (!isSubDivided)
                subdivide()
            when {
                northWest?.insert(point) == true -> true
                northEast?.insert(point) == true -> true
                southWest?.insert(point) == true -> true
                southEast?.insert(point) == true -> true
                else -> false
            }
        }
    }

    fun pull(range: RectangleCentered, found: ArrayList<Point> = arrayListOf()): ArrayList<Point> {

        if(!boundary.intersects(range))
            return found
        else{
            for (p in points){
                if (range.contains(p))
                    found.add(p)
            }

            if (isSubDivided){
                northWest?.pull(range, found)
                northEast?.pull(range, found)
                southEast?.pull(range, found)
                southWest?.pull(range, found)
            }

            return found
        }
    }

    private fun subdivide() {
        val x = boundary.x
        val y = boundary.y
        val w = boundary.w
        val h = boundary.h

        val nw = RectangleCentered(x + w / 2, y - h / 2, w / 2, h / 2)
        northWest = QuadTree(nw, capacity)
        val ne = RectangleCentered(x - w / 2, y - h / 2, w / 2, h / 2)
        northEast = QuadTree(ne, capacity)
        val sw = RectangleCentered(x + w / 2, y + h / 2, w / 2, h / 2)
        southWest = QuadTree(sw, capacity)
        val se = RectangleCentered(x - w / 2, y + h / 2, w / 2, h / 2)
        southEast = QuadTree(se, capacity)

        isSubDivided = true
    }

}
