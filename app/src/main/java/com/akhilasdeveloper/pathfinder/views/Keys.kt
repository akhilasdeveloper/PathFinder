package com.akhilasdeveloper.pathfinder.views

import com.akhilasdeveloper.pathfinder.R

object Keys {
    val BLOCK : Int = 0
    val BLOCK1 : Int = 12
    val START : Int = 1
    val END : Int = 2
    val PATH : Int = 3
    val VISITED : Int = 4
    val EMPTY : Int = 5
    val ERASER : Int = -1
}

val colors = hashMapOf<Int, Int>(
    Keys.BLOCK to R.color.block,
    Keys.BLOCK1 to R.color.block1,
    Keys.START to R.color.start,
    Keys.END to R.color.end,
    Keys.PATH to R.color.path,
    Keys.VISITED to R.color.visited,
    Keys.EMPTY to R.color.empty
)