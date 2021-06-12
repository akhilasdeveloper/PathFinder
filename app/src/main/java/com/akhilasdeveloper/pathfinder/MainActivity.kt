package com.akhilasdeveloper.pathfinder

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import com.akhilasdeveloper.pathfinder.databinding.ActivityMainBinding
import com.akhilasdeveloper.pathfinder.models.Node
import com.akhilasdeveloper.pathfinder.views.Keys.EMPTY
import com.akhilasdeveloper.pathfinder.views.Keys.END
import com.akhilasdeveloper.pathfinder.views.Keys.PATH
import com.akhilasdeveloper.pathfinder.views.Keys.START
import com.akhilasdeveloper.pathfinder.views.Keys.VISITED
import com.akhilasdeveloper.pathfinder.views.OnNodeSelectListener
import com.akhilasdeveloper.pathfinder.views.SpanGrid
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var spanGrid: SpanGrid
    private var data:MutableSet<Node> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolBar)
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)

        spanGrid = SpanGrid(this)
        binding.mainActivity.addView(spanGrid)
        spanGrid.post {
            spanGrid.init()
        }

        binding.lineSlide.addOnChangeListener { _, value, _ ->
            spanGrid.setMargin(value.toInt())
        }
        binding.nodeSlide.addOnChangeListener { _, value, _ ->
            spanGrid.setScale(value.toInt())
        }

        spanGrid.setNodeSelectListener(object : OnNodeSelectListener {
            override fun onEvent(node: Node) {
                if (binding.blockers.isChecked) {
                    if (binding.enableEdit.isChecked)
                        spanGrid.deleteNode(node)
                    else
                        spanGrid.addNode(node)
                }

                if (binding.start.isChecked) {
                    if (binding.enableEdit.isChecked)
                        spanGrid.removeStartNode(node)
                    else
                        spanGrid.addStartNode(node)
                }

                if (binding.end.isChecked) {
                    if (binding.enableEdit.isChecked)
                        spanGrid.removeEndNode(node)
                    else
                        spanGrid.addEndNode(node)
                }

            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.scale -> {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                else
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                true
            }
            R.id.play -> {
                data = spanGrid.data
                findPath()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun findPath() {

        CoroutineScope(Dispatchers.Default).launch {


            val gHeight = spanGrid.heightS
            val gWidth = spanGrid.widthS
            if (!invalidateData(data, gHeight, gWidth)) this.cancel()
            val grid: MutableMap<String, Node> = generateGrid(gWidth, gHeight)
            val removedGrid: MutableList<Node> = mutableListOf()
            data.forEach { node ->
                grid[getKeyFromXY(node.x, node.y)] = node.apply {
                    if (node.type == START)
                        this.distance = 0.0
                }
            }

            while (grid.isNotEmpty()) {

//                val sortedGrid = grid.toList().sortedBy { (_, v) -> v.distance }.toMap()
                val shortNode:Node = shortGridNode(grid)
//                val shortNode = sortedGrid[sortedGrid.keys.first()]
                grid.remove(getKeyFromXY(shortNode.x, shortNode.y))
                removedGrid.add(shortNode)
                if (shortNode.type == END) {
                    Log.d("findPath : removedGrid", "${removedGrid[0].distance}")
                    var n:Node? = shortNode
                    while (n!!.type != START){
                        data.add(n.apply { type = PATH })
                        withContext(Main) {
                            spanGrid.setDatas(data)
                        }
                        n = n.previousNode
                    }
                    break
                }else {
                    if (shortNode.type == EMPTY)
                        data.add(shortNode.apply { type = VISITED })

                    if (shortNode.distance == Double.POSITIVE_INFINITY) this.cancel()

                    withContext(Main) {
                        spanGrid.setDatas(data)
                    }
                }

//                delay(500)

                val neighbours: MutableList<Node> =
                    getNeighbours(gHeight, gWidth, shortNode, grid)

//                if (neighbours.isEmpty()) this.cancel()

                Log.d("findPath : neighbours", "${neighbours.size}")

                neighbours.forEach {
                    val dis = shortNode.distance + 1
                    if (dis < it.distance)
                        grid[getKeyFromXY(it.x,it.y)]?.distance = dis
                    grid[getKeyFromXY(it.x,it.y)]?.previousNode = shortNode
                }

            }
        }
    }

    private fun shortGridNode(grids: MutableMap<String, Node>): Node {
        var shortNode = grids[grids.keys.first()]!!
        grids.forEach {
            if (shortNode.distance > it.value.distance)
                shortNode = it.value
        }

        return shortNode
    }

    private fun getNeighbours(
        gHeight: Int,
        gWidth: Int,
        shortNode: Node,
        grids: MutableMap<String, Node>
    ): MutableList<Node> {
        val n: MutableList<Node> = mutableListOf()

        if (shortNode.y > 0) {
            val v = getKeyFromXY(shortNode.x, shortNode.y - 1)
            grids[v]?.let {
                if (it.type == EMPTY || it.type == END) {
                    
                    n.add(it)
                }
            }
        }

        if (shortNode.x < gWidth - 1) {
            val v = getKeyFromXY(shortNode.x + 1, shortNode.y)
            grids[v]?.let {
                if (it.type == EMPTY || it.type == END) {
                    
                    n.add(it)
                }
            }
        }

        if (shortNode.y < gHeight - 1) {
            val v = getKeyFromXY(shortNode.x, shortNode.y + 1)
            grids[v]?.let {
                if (it.type == EMPTY || it.type == END) {
                    
                    n.add(it)
                }
            }
        }

        if (shortNode.x > 0) {
            val v = getKeyFromXY(shortNode.x - 1, shortNode.y)
            grids[v]?.let {
                if (it.type == EMPTY || it.type == END) {
                    
                    n.add(it)
                }
            }
        }

        return n
    }

    private fun generateGrid(gWidth: Int, gHeight: Int): MutableMap<String, Node> {
        val map: MutableMap<String, Node> = mutableMapOf()
        for (x in 0..gWidth) {
            for (y in 0..gHeight) {
                map[getKeyFromXY(x, y)] = Node(x = x, y = y)
            }
        }
        return map
    }

    private fun getKeyFromXY(x: Int, y: Int) = "$x:$y"

    private fun invalidateData(data: MutableSet<Node>, gHeight: Int, gWidth: Int) =
        data.any { it.type == START && it.x < gWidth && it.y < gHeight } && data.any { it.type == END && it.x < gWidth && it.y < gHeight }

}