package com.akhilasdeveloper.pathfinder.views

import com.akhilasdeveloper.pathfinder.models.Point

interface OnNodeSelectListener {
   fun onEvent(px: Point)
}