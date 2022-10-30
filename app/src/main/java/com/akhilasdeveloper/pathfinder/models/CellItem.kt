package com.akhilasdeveloper.pathfinder.models

import com.akhilasdeveloper.pathfinder.R
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath

data class CellItem(
    val cell: FindPath.Square,
    var selected: Boolean = false,
    val cellIcon: Int = R.drawable.ic_round_stop_24
)