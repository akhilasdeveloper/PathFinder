package com.akhilasdeveloper.pathfinder.models

import android.graphics.Color
import com.akhilasdeveloper.pathfinder.views.Keys.EMPTY

data class Square (
    var x1:Float = 0f,
    var y1:Float = 0f,
    var x2:Float = 0f,
    var y2:Float =0f,
    var type: Int = EMPTY,
    var distance: Int = Int.MAX_VALUE,
    var isVisited:Boolean = false,
    var previous: Int? = null
        )