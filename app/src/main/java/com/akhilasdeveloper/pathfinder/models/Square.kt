package com.akhilasdeveloper.pathfinder.models

import com.akhilasdeveloper.pathfinder.views.Keys.EMPTY

data class Square(
    var type: Int = EMPTY,
    var distance: Int = Int.MAX_VALUE,
    var weight: Int = 1,
    var previous: Point? = null
)