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
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.DynamicAnimation.ViewProperty
import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.FloatPropertyCompat
import com.akhilasdeveloper.pathfinder.algorithms.HeapMinHash
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.findPathDijkstr
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.getData
import com.akhilasdeveloper.pathfinder.databinding.ActivityMainBinding
import com.akhilasdeveloper.pathfinder.models.CellItem
import com.akhilasdeveloper.pathfinder.models.Point
import com.akhilasdeveloper.pathfinder.models.Square
import com.akhilasdeveloper.pathfinder.models.nodes
import com.akhilasdeveloper.pathfinder.views.Keys
import com.akhilasdeveloper.pathfinder.views.Keys.END
import com.akhilasdeveloper.pathfinder.views.Keys.START
import com.akhilasdeveloper.pathfinder.views.SpanGrid
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.*
import kotlin.math.floor
import kotlin.random.Random


class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    internal lateinit var gridCanvasView: SpanGrid
    internal var startPont: Point? = null
    internal var endPont: Point? = null
    private var gaps = mutableListOf<Point>()
    internal var gridHash: HashMap<Point, Square> = hashMapOf()
    internal var heapMin: HeapMinHash<Point> = HeapMinHash()
    internal var sleepVal = 0L

    private var selectedItem = 0
        set(value) {
            field = value
            setBrushSize()
        }

    private var brushSize = 1
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
//        val duckFlingAnimationX = FlingAnimation(Point(), DynamicAnimation.TRANSLATION_X)
        init()
        setListeners()
    }

    private fun setListeners() {
        gridCanvasView.setGridSelectListener(object : SpanGrid.OnGridSelectListener {

            override fun onEventMove(px: Point) {
                plotPointOnTouch(px)
            }

            override fun onEventUp() {
            }

            override fun onEventDown(px: Point) {
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
                        generateMaze()
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
        gridCanvasView = SpanGrid(this)
        binding.gridViewHolder.addView(gridCanvasView)
        gridCanvasView.resolution = 30f
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


    private fun generateMaze() {
        CoroutineScope(Dispatchers.Default).launch {
            var gHeight = gridCanvasView.gridHeight.toInt()
            var gWidth = gridCanvasView.gridWidth.toInt()
            val startPoint = gridCanvasView.startPoint

            gHeight = if (gHeight % 2 == 0) gHeight else gHeight - 1
            gWidth = if (gWidth % 2 == 0) gWidth else gWidth - 1

            generateBorder(startPoint, gWidth, gHeight)
            recursiveMaze(
                startPoint.x + 1,
                startPoint.y + 1,
                gWidth + startPoint.x - 1,
                gHeight + startPoint.y - 1
            )

        }
    }

    private fun generateBorder(startPoint: Point, gWidth: Int, gHeight: Int) {

        val xEnd = gWidth + startPoint.x
        val yEnd = gHeight + startPoint.y

        for (i in startPoint.x until xEnd) {
            setBit(Point(i, startPoint.y), Keys.WALL)
            setBit(Point(i, yEnd), Keys.WALL)
        }
        for (j in startPoint.y until yEnd + 1) {
            setBit(Point(startPoint.x, j), Keys.WALL)
            setBit(Point(xEnd, j), Keys.WALL)
        }
    }

    private fun recursiveMaze(x1: Int, y1: Int, x2: Int, y2: Int) {

        if (x2 - x1 < 2 || y2 - y1 < 2)
            return

        val isHorizontal =
            if (x2 - x1 < y2 - y1) true else if (x2 - x1 > y2 - y1) false else Random.nextBoolean()

        val rows = mutableListOf<Int>()
        val cols = mutableListOf<Int>()

        if (isHorizontal) {

            for (i in x1 until x2 + 1 step 2)
                rows.add(i)

            for (i in y1 + 1 until y2 step 2)
                cols.add(i)

            val randRow = floor(Math.random() * rows.size).toInt()
            var randCol = floor(Math.random() * cols.size).toInt()

            var cutHor = cols[randCol]

            while (gaps.contains(Point(x1 - 1, cutHor)) || gaps.contains(
                    Point(
                        x2 + 1,
                        cutHor
                    )
                )
            ) {
                if (cols.size < 3)
                    return
                randCol = floor(Math.random() * cols.size).toInt()
                cutHor = cols[randCol]
            }

            drawLineHor(x1, x2, cutHor)

            val gapHor = rows[randRow]
            gaps.add(Point(gapHor, cutHor))
            clearBit(Point(gapHor, cutHor))

            recursiveMaze(x1, y1, x2, cutHor - 1)
            recursiveMaze(x1, cutHor + 1, x2, y2)
        } else {

            for (i in x1 + 1 until x2 step 2)
                rows.add(i)

            for (i in y1 until y2 + 1 step 2)
                cols.add(i)

            var randRow = floor(Math.random() * rows.size).toInt()
            val randCol = floor(Math.random() * cols.size).toInt()

            var cutVer = rows[randRow]

            while (gaps.contains(
                    Point(
                        cutVer,
                        y1 - 1
                    )
                ) || gaps.contains(Point(cutVer, y2 + 1))
            ) {
                if (rows.size < 3)
                    return
                randRow = floor(Math.random() * rows.size).toInt()
                cutVer = rows[randRow]
            }

            drawLineVer(y1, y2, cutVer)

            val gapVer = cols[randCol]
            gaps.add(Point(cutVer, gapVer))
            clearBit(Point(cutVer, gapVer))

            recursiveMaze(x1, y1, cutVer - 1, y2)
            recursiveMaze(cutVer + 1, y1, x2, y2)
        }
    }

    private fun setBrushSize() {
        if (getType() == START || getType() == END)
            gridCanvasView.brushSize = 0
        else
            gridCanvasView.brushSize = brushSize
    }

    private fun drawLineHor(x1: Int, x2: Int, y: Int) {
        for (i in x1..x2) {
            runBlocking {
                delay(sleepVal)
            }
            setBit(Point(i, y), Keys.WALL)
        }
    }

    private fun drawLineVer(y1: Int, y2: Int, x: Int) {
        for (i in y1..y2) {
            runBlocking {
                delay(sleepVal)
            }
            setBit(Point(x, i), Keys.WALL)
        }
    }

    internal fun setBit(point: Point, type: Int) {
        val data = getData(point).copyToType(type = type)
        gridHash[point] = data

        gridCanvasView.plotPoint(
            point,
            ContextCompat.getColor(this, data.fillColor),
            ContextCompat.getColor(this, data.color)
        )
    }

    private fun clearBit(point: Point) {
        gridHash.remove(point)
        gridCanvasView.removeRect(point)
    }

}