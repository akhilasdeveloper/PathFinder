package com.akhilasdeveloper.pathfinder.models

import android.graphics.drawable.Icon
import androidx.core.graphics.drawable.IconCompat

data class CellItem(
    val cellNode: Node,
    val cellIcon: Int,
    val cellName: String
)