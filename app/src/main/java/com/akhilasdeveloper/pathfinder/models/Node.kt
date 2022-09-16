package com.akhilasdeveloper.pathfinder.models

sealed class Node(
    var distance: Int = Int.MAX_VALUE,
    var weight: Int = 1,
    var previous: Point? = null
){
    class StartNode() : Node()
    class EndNode() : Node()
    class AirNode() : Node(weight = 0)
    class GraniteNode() : Node(weight = 50)
    class GrassNode() : Node(weight = 5)
    class SandNode() : Node(weight = 7)
    class SnowNode() : Node(weight = 75)
    class StoneNode() : Node(weight = 25)
    class WaterNode() : Node(weight = 50)
    class WaterDeepNode() : Node(weight = 100)
    class WallNode() : Node()
}
