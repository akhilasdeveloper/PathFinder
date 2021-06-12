package com.akhilasdeveloper.pathfinder.views

import com.akhilasdeveloper.pathfinder.models.Node

interface OnNodeSelectListener {
   fun onEvent(node: Node)
}