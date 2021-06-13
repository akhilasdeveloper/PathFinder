package com.akhilasdeveloper.pathfinder.models

data class LinkedPoint (
    var point: Int?,
    var previous: LinkedPoint? = null
        )