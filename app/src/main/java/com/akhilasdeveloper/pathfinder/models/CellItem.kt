package com.akhilasdeveloper.pathfinder.models

import android.graphics.drawable.Icon
import androidx.core.graphics.drawable.IconCompat
import com.akhilasdeveloper.pathfinder.R

data class CellItem(
    val cell: Square,
    val cellIcon: Int = R.drawable.ic_round_stop_24
)