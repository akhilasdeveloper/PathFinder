package com.akhilasdeveloper.pathfinder

import android.content.res.Resources

private val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density + 0.5f).toInt()