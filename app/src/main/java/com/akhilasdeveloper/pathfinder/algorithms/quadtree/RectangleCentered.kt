package com.akhilasdeveloper.pathfinder.algorithms.quadtree

import com.akhilasdeveloper.pathfinder.models.Point
import com.akhilasdeveloper.pathfinder.models.PointF

data class RectangleCentered(val x: Int, val y: Int, val w: Int, val h: Int) {
    fun contains(point: Point): Boolean = (point.x >= x - w &&
            point.x <= x + w &&
            point.y >= y - h &&
            point.y <= y + h)

    fun intersects(range: RectangleCentered): Boolean =
        !(range.x - range.w > x + w ||
                range.x + range.w < x - w ||
                range.y - range.h > y + h ||
                range.y + range.h < y - h)

}
