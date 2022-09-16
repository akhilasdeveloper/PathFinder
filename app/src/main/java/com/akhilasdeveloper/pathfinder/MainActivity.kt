package com.akhilasdeveloper.pathfinder

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import com.akhilasdeveloper.pathfinder.algorithms.HeapMinHash
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.findPathDijkstra
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.getData
import com.akhilasdeveloper.pathfinder.databinding.ActivityMainBinding
import com.akhilasdeveloper.pathfinder.models.*
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
    internal var gridHash: HashMap<Point, Node> = hashMapOf()
    internal var heapMin: HeapMinHash<Point> = HeapMinHash()
    internal var sleepVal = 0L
    private var selectedItem = 0

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
            popupMenu.setOnMenuItemClickListener { menuItem -> // Toast message on menu item clicked

                when(menuItem.itemId){
                    R.id.digkstra -> {
                        if (startPont!=null && endPont!=null)
                            findPathDijkstra()
                        else
                            Toast.makeText(this, "Please select start point and end point", Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }
            popupMenu.show()
        }

        binding.generateGrid.setOnClickListener {
            val popupMenu = PopupMenu(this@MainActivity, it)

            popupMenu.menuInflater.inflate(R.menu.menu_maze_algorithms, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem -> // Toast message on menu item clicked
                when(menuItem.itemId){
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
        when (val node = getType()) {

            is Node.StartNode -> {

                startPont?.let { start ->
                    clearBit(start)
                }

                startPont = px
                setBit(px, node)

                gridCanvasView.play()
            }
            is Node.EndNode -> {

                endPont?.let { start ->
                    clearBit(start)
                }

                endPont = px
                setBit(px, node)

                gridCanvasView.play()
            }
            is Node.AirNode -> {
                clearBit(px)
                if (px == startPont)
                    startPont = null
                if (px == endPont)
                    endPont = null
                gridCanvasView.play()
            }
            else -> {
                setBit(px, node)
                gridCanvasView.play()
            }
        }
    }

    private fun getType() =
        cellList[selectedItem].cellNode

    private fun init() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        gridCanvasView = SpanGrid(this)
        binding.gridViewHolder.addView(gridCanvasView)
        gridCanvasView.resolution = 30f
        gridCanvasView.post {
            gridCanvasView.init()
        }

        initialiseLists()

        cellSpinnerAdapter = CellSpinnerAdapter(this, cellList)
        binding.cellSelector.adapter = cellSpinnerAdapter

    }

    private fun initialiseLists() {
        cellList.add(
            CellItem(
                cellNode = Node.StartNode(),
                cellIcon = R.drawable.ic_round_stop_24,
                cellName = "Start Node"
            )
        )
        cellList.add(
            CellItem(
                cellNode = Node.EndNode(),
                cellIcon = R.drawable.ic_round_stop_24,
                cellName = "End Node"
            )
        )
        cellList.add(
            CellItem(
                cellNode = Node.WallNode(),
                cellIcon = R.drawable.ic_round_stop_24,
                cellName = "Wall Node"
            )
        )
        cellList.add(
            CellItem(
                cellNode = Node.AirNode(),
                cellIcon = R.drawable.ic_round_stop_24,
                cellName = "Air Node"
            )
        )
        cellList.add(
            CellItem(
                cellNode = Node.GraniteNode(),
                cellIcon = R.drawable.ic_round_stop_24,
                cellName = "Granite Node"
            )
        )
        cellList.add(
            CellItem(
                cellNode = Node.GrassNode(),
                cellIcon = R.drawable.ic_round_stop_24,
                cellName = "Grass Node"
            )
        )

        cellList.add(
            CellItem(
                cellNode = Node.SandNode(),
                cellIcon = R.drawable.ic_round_stop_24,
                cellName = "Sand Node"
            )
        )

        cellList.add(
            CellItem(
                cellNode = Node.SnowNode(),
                cellIcon = R.drawable.ic_round_stop_24,
                cellName = "Snow Node"
            )
        )

        cellList.add(
            CellItem(
                cellNode = Node.StoneNode(),
                cellIcon = R.drawable.ic_round_stop_24,
                cellName = "Stone Node"
            )
        )

        cellList.add(
            CellItem(
                cellNode = Node.WaterNode(),
                cellIcon = R.drawable.ic_round_stop_24,
                cellName = "Water Node"
            )
        )

        cellList.add(
            CellItem(
                cellNode = Node.WaterDeepNode(),
                cellIcon = R.drawable.ic_round_stop_24,
                cellName = "WaterDeep Node"
            )
        )


        pathAlgorithmList.add("Digkstra")
        gridAlgorithmList.add("Recursive Maze")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_pathfinder, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.play -> {
                if (!invalidateData()) {
                    Toast.makeText(
                        this,
                        "Select Start point and end point",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    findPathDijkstra()
                }
                true
            }
            R.id.scale -> {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                else
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                true
            }
            R.id.grid -> {
                if (binding.enableEdit.isChecked)
                    binding.enableEdit.isChecked = false
                binding.blockers.isChecked = true
                generateMaze()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun generateMaze() {
        CoroutineScope(Dispatchers.Default).launch {
            var gHeight = gridCanvasView.gridHeight.toInt()
            var gWidth = gridCanvasView.gridWidth.toInt()
            val startPoint = gridCanvasView.startPoint

            gHeight = if (gHeight % 2 == 0) gHeight else gHeight - 1
            gWidth = if (gWidth % 2 == 0) gWidth  else gWidth - 1

            generateBorder(startPoint, gWidth, gHeight)
            recursiveMaze(startPoint.x + 1, startPoint.y + 1, gWidth + startPoint.x - 1, gHeight + startPoint.y - 1)

        }
    }

    private fun generateBorder(startPoint: Point, gWidth: Int, gHeight: Int) {

        val xEnd = gWidth + startPoint.x
        val yEnd = gHeight + startPoint.y

        for (i in startPoint.x until xEnd) {
            setBit(Point(i,startPoint.y), Node.WallNode())
            setBit(Point(i, yEnd), Node.WallNode())
            gridCanvasView.play()
        }
        for (j in startPoint.y until yEnd + 1) {
            setBit(Point(startPoint.x, j), Node.WallNode())
            setBit(Point(xEnd, j), Node.WallNode())
            gridCanvasView.play()
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
            gridCanvasView.play()

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
            gridCanvasView.play()

            recursiveMaze(x1, y1, cutVer - 1, y2)
            recursiveMaze(cutVer + 1, y1, x2, y2)
        }
    }

    private fun drawLineHor(x1: Int, x2: Int, y: Int) {
        for (i in x1..x2) {
            runBlocking {
                delay(sleepVal)
            }
            setBit(Point(i, y), Node.WallNode())
            gridCanvasView.play()
        }
    }

    private fun drawLineVer(y1: Int, y2: Int, x: Int) {
        for (i in y1..y2) {
            runBlocking {
                delay(sleepVal)
            }
            setBit(Point(x, i), Node.WallNode())
            gridCanvasView.play()
        }
    }

    internal fun setBit(point: Point, node: Node) {
        val data = getData(point)
        gridHash[point] = node.apply {
            nodeData = data.nodeData
        }

        val color = if (node.nodeData.isVisited) R.color.visited.toColor(this) else node.color.toColor(this)

        gridCanvasView.plotPoint(
            point,
            color
        )

    }

    private fun clearBit(point: Point) {
        gridHash.remove(point)
        gridCanvasView.removeRect(point)
    }

    private fun invalidateData() = startPont != null && endPont != null
}