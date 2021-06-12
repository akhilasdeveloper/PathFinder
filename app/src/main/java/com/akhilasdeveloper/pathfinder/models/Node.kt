package com.akhilasdeveloper.pathfinder.models

import com.akhilasdeveloper.pathfinder.views.Keys.EMPTY

data class Node(
    var x:Int = 0,
    var y:Int = 0,
    var type:Int = EMPTY,
    var distance:Double = Double.POSITIVE_INFINITY,
    var isVisited:Boolean = false,
    var previousNode: Node? = null
    )