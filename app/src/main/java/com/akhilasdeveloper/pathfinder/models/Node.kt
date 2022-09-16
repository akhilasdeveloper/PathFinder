package com.akhilasdeveloper.pathfinder.models

import android.content.Context
import androidx.core.content.ContextCompat
import com.akhilasdeveloper.pathfinder.R

sealed class Node(
    var weight: Int = 1,
    var color: Int,
    var nodeData: NodeData = NodeData()
) {
    class StartNode() : Node(nodeData = NodeData(distance = 0),color = R.color.start)
    class EndNode() : Node(color = R.color.end)
    class AirNode() : Node( color = R.color.empty)
    class GraniteNode() : Node(weight = 50, color = R.color.granite)
    class GrassNode() : Node(weight = 5, color = R.color.grass)
    class SandNode() : Node(weight = 7, color = R.color.sand)
    class SnowNode() : Node(weight = 75, color = R.color.snow)
    class StoneNode() : Node(weight = 25, color = R.color.stone)
    class WaterNode() : Node(weight = 50, color = R.color.water)
    class WaterDeepNode() : Node(weight = 100, color = R.color.water_deep)
    class WallNode() : Node(weight = Int.MAX_VALUE,color = R.color.block)
    class PathNode() : Node(color = R.color.path)

    var distance = nodeData.distance
         set(value) {
             nodeData.distance = value
             field = value
         }

    var previous = nodeData.previous
        set(value) {
            nodeData.previous = value
            field = value
        }

    fun asVisited(): Node {
        nodeData.isVisited = true
        return this
    }

}

data class NodeData(
    var distance: Int = Int.MAX_VALUE,
    var isVisited: Boolean = false,
    var previous: Point? = null
)

fun Int.toColor(context: Context) = ContextCompat.getColor(context, this)