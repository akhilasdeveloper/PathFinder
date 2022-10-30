package com.akhilasdeveloper.pathfinder.models

import android.content.Context
import androidx.core.content.ContextCompat
import com.akhilasdeveloper.pathfinder.R
import com.akhilasdeveloper.spangridview.models.Point

data class Square(
    var name: String,
    var type: Int,
    var distance: Int = Int.MAX_VALUE,
    var weight: Int = 1,
    var previous: Point? = null,
    var color: Int,
    var fillColor: Int = color,

    var f:Int = 0,
    var g:Int = 0,
    var h:Int = 0,
) {

    fun copyToType(type: Int): Square {
        val node = nodes(type)
        val color = if ((type == Keys.PATH || type == Keys.VISITED) && this.type != Keys.AIR ) this.color else node.color
        return node.copy(distance = this.distance, previous = this.previous, color = color)
    }

}

fun nodes(type: Int? = null): Square {
    return when (type) {
        Keys.WALL -> Square(
            name = "Wall Node",
            type = Keys.WALL,
            weight = Int.MAX_VALUE,
            color = R.color.block
        )
        Keys.START -> Square(
            name = "Start Node",
            type = Keys.START,
            distance = 0,
            color = R.color.start
        )
        Keys.END -> Square(name = "End Node", type = Keys.END, color = R.color.end)
        Keys.PATH -> Square(name = "Path Node", type = Keys.PATH, color = R.color.path)
        Keys.VISITED -> Square(name = "Visited Node", type = Keys.VISITED, color = R.color.visited)
        Keys.AIR -> Square(name = "Air Node", type = Keys.AIR, color = R.color.empty)
        Keys.GRANITE -> Square(
            name = "Granite Node",
            type = Keys.GRANITE,
            weight = 50,
            color = R.color.granite
        )
        Keys.GRASS -> Square(
            name = "Grass Node",
            type = Keys.GRASS,
            weight = 5,
            color = R.color.grass
        )
        Keys.SAND -> Square(name = "Sand Node", type = Keys.SAND, weight = 7, color = R.color.sand)
        Keys.SNOW -> Square(name = "Snow Node", type = Keys.SNOW, weight = 75, color = R.color.snow)
        Keys.STONE -> Square(
            name = "Stone Node",
            type = Keys.STONE,
            weight = 25,
            color = R.color.stone
        )
        Keys.WATER -> Square(
            name = "Water Node",
            type = Keys.WATER,
            weight = 50,
            color = R.color.water
        )
        Keys.WATER_DEEP -> Square(
            name = "Deep Water Node",
            type = Keys.WATER_DEEP,
            weight = 100,
            color = R.color.water_deep
        )
        else -> Square(name = "Air Node", type = Keys.AIR, color = R.color.empty)
    }
}

val nodes = listOf(
    Keys.START,
    Keys.END,
    Keys.WALL,
    Keys.AIR,
    Keys.GRANITE,
    Keys.GRASS,
    Keys.SAND,
    Keys.SNOW,
    Keys.STONE,
    Keys.WATER,
    Keys.WATER_DEEP
)


fun Int.toColor(context: Context) = ContextCompat.getColor(context, this)