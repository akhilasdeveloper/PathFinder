package com.akhilasdeveloper.pathfinder

import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import com.akhilasdeveloper.pathfinder.algorithms.HeapMinHash
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.*
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.findPathDijkstr
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.generateRecursiveMaze
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.getData
import com.akhilasdeveloper.pathfinder.databinding.ActivityMainBinding
import com.akhilasdeveloper.pathfinder.models.CellItem
import com.akhilasdeveloper.pathfinder.models.Square
import com.akhilasdeveloper.pathfinder.models.nodes
import com.akhilasdeveloper.pathfinder.views.Keys
import com.akhilasdeveloper.pathfinder.views.Keys.END
import com.akhilasdeveloper.pathfinder.views.Keys.START
import com.akhilasdeveloper.spangridview.SpanGridView
import com.akhilasdeveloper.spangridview.models.Point
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    internal lateinit var gridCanvasView: SpanGridView
    internal var startPont: Point? = null
    internal var endPont: Point? = null
    internal var gaps = mutableListOf<Point>()
    internal var gridHash: HashMap<Point, Square> = hashMapOf()
    private var xHash: HashMap<Int, Int> = hashMapOf()
    private var yHash: HashMap<Int, Int> = hashMapOf()
    internal var heapMin: HeapMinHash<Point> = HeapMinHash()
    internal var sleepVal = 0L
    internal var sleepValPath = 5L

    private var selectedItem = 0
        set(value) {
            field = value
            setBrushSize()
        }

    private var brushSize = 2
        set(value) {
            field = value
            setBrushSize()
        }

    private val cellList: ArrayList<CellItem> = arrayListOf()
    private val pathAlgorithmList: ArrayList<String> = arrayListOf()
    private val gridAlgorithmList: ArrayList<String> = arrayListOf()
    private var cellSpinnerAdapter: CellSpinnerAdapter? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        setListeners()
    }

    private fun setListeners() {
        gridCanvasView.setGridSelectListener(object : SpanGridView.OnGridSelectListener {

            override fun onDraw(px: Point) {
                plotPointOnTouch(px)
            }

            override fun onModeChange(mode: Int) {

            }
        })

        binding.speedSlide.addOnChangeListener { _, value, _ ->
            sleepVal = value.toLong()
        }

        binding.cellSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                selectedItem = p2

            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }

        }

        binding.findPath.setOnClickListener {
            val popupMenu = PopupMenu(this@MainActivity, it)

            popupMenu.menuInflater.inflate(R.menu.menu_path_algorithms, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->

                when (menuItem.itemId) {
                    R.id.digkstra -> {
                        if (startPont != null && endPont != null)
                            findPathDijkstr()
                        else
                            Toast.makeText(
                                this,
                                "Please select start point and end point",
                                Toast.LENGTH_SHORT
                            ).show()
                    }
                    R.id.a_star -> {
                        if (startPont != null && endPont != null)
                            findAStar()
                        else
                            Toast.makeText(
                                this,
                                "Please select start point and end point",
                                Toast.LENGTH_SHORT
                            ).show()
                    }
                    R.id.bfs -> {
                        if (startPont != null && endPont != null)
                            findPathBFS()
                        else
                            Toast.makeText(
                                this,
                                "Please select start point and end point",
                                Toast.LENGTH_SHORT
                            ).show()
                    }
                    R.id.dfs -> {
                        if (startPont != null && endPont != null)
                            findPathDFS()
                        else
                            Toast.makeText(
                                this,
                                "Please select start point and end point",
                                Toast.LENGTH_SHORT
                            ).show()
                    }
                }
                true
            }
            popupMenu.show()
        }

        binding.generateGrid.setOnClickListener {
            val popupMenu = PopupMenu(this@MainActivity, it)

            popupMenu.menuInflater.inflate(R.menu.menu_maze_algorithms, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.recursive -> {
                        generateRecursiveMaze()
                    }
                }
                true
            }
            popupMenu.show()
        }

    }

    private fun plotPointOnTouch(px: Point) {
        when (val type = getType()) {

            Keys.START -> {
                startPont?.let { start ->
                    clearBit(start)
                }
                startPont = px
                setBit(px, type)

            }
            Keys.END -> {

                endPont?.let { start ->
                    clearBit(start)
                }

                endPont = px
                setBit(px, type)

            }
            Keys.AIR -> {
                clearBit(px)
                if (px == startPont)
                    startPont = null
                if (px == endPont)
                    endPont = null
            }
            else -> {
                setBit(px, type)
            }
        }
    }

    private fun getType() =
        cellList[selectedItem].cell.type

    private fun init() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        gridCanvasView = binding.gridViewHolder
        gridCanvasView.brushSize = brushSize
        gridCanvasView.post {
            gridCanvasView.init()
        }

        initialiseLists()

        cellSpinnerAdapter = CellSpinnerAdapter(this, cellList)
        binding.cellSelector.adapter = cellSpinnerAdapter

    }

    private fun initialiseLists() {

        for (node in nodes) {
            cellList.add(
                CellItem(
                    cell = nodes(node)
                )
            )
        }

        pathAlgorithmList.add("Digkstra")
        gridAlgorithmList.add("Recursive Maze")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_pathfinder, menu)
        return true
    }

    private fun setBrushSize() {
        if (getType() == START || getType() == END)
            gridCanvasView.brushSize = 1
        else
            gridCanvasView.brushSize = brushSize
    }

    internal fun drawLineHor(x1: Int, x2: Int, y: Int) {
        for (i in x1..x2) {
            runBlocking {
                delay(sleepVal)
            }
            setBit(Point(i, y), Keys.WALL)
        }
    }

    internal fun drawLineVer(y1: Int, y2: Int, x: Int) {
        for (i in y1..y2) {
            runBlocking {
                delay(sleepVal)
            }
            setBit(Point(x, i), Keys.WALL)
        }
    }

    internal fun setBit(point: Point, type: Int) {
        setXY(point)

        val data = getData(point).copyToType(type = type)
        gridHash[point] = data

        gridCanvasView.plotPoint(
            point,
            ContextCompat.getColor(this, data.fillColor),
            ContextCompat.getColor(this, data.color)
        )
    }

    internal fun clearBit(point: Point) {
        clearXY(point)
        gridHash.remove(point)
        gridCanvasView.removeRect(point)
    }

    private fun setXY(point: Point) {
        if (gridHash[point] == null) {
            val x = xHash[point.x] ?: 0
            val y = yHash[point.y] ?: 0
            xHash[point.x] = x + 1
            yHash[point.y] = y + 1
        }
    }

    private fun clearXY(point: Point) {
        gridHash[point]?.let {
            xHash[point.x]?.let {
                if (it <= 1)
                    xHash.remove(point.x)
                else
                    xHash[point.x] = it - 1
            }
            yHash[point.y]?.let {
                if (it <= 1)
                    yHash.remove(point.y)
                else
                    yHash[point.y] = it - 1
            }
        }
    }

    internal fun getMinXY(): Point {
        xHash.keys.toIntArray().minOrNull()?.let { x ->
            yHash.keys.toIntArray().minOrNull()?.let { y ->
                return Point(x, y)
            }
        }

        return Point(0,0)
    }

    private fun getMaxXY(): Point {
        xHash.keys.toIntArray().maxOrNull()?.let { x ->
            yHash.keys.toIntArray().maxOrNull()?.let { y ->
                return Point(x, y)
            }
        }

        return Point(0,0)
    }

    internal fun createBorder() {
        var minPoint = getMinXY()
        var maxPoint = getMaxXY()

        minPoint = minPoint.apply {
            x -= 1
            y -= 1
        }

        maxPoint = maxPoint.apply {
            x += 1
            y += 1
        }

        val width = maxPoint.x - minPoint.x
        val height = maxPoint.y - minPoint.y

        generateBorder(minPoint, width, height)
    }

}