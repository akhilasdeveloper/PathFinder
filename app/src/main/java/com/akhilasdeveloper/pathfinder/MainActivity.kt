package com.akhilasdeveloper.pathfinder

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.IconCompat
import com.akhilasdeveloper.pathfinder.algorithms.HeapMinHash
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.findPathDijkstra
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.getData
import com.akhilasdeveloper.pathfinder.databinding.ActivityMainBinding
import com.akhilasdeveloper.pathfinder.models.CellItem
import com.akhilasdeveloper.pathfinder.models.Point
import com.akhilasdeveloper.pathfinder.models.Square
import com.akhilasdeveloper.pathfinder.views.Keys
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

    private val cellList: ArrayList<CellItem> = arrayListOf()
    private var cellSpinnerAdapter:CellSpinnerAdapter? = null

    private val colors = hashMapOf<Int, Int>(
        Keys.BLOCK to R.color.block,
        Keys.START to R.color.start,
        Keys.END to R.color.end,
        Keys.PATH to R.color.path,
        Keys.VISITED to R.color.visited,
        Keys.EMPTY to R.color.empty
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolBar)
        init()
        setListeners()
    }

    private fun setListeners() {
        gridCanvasView.setGridSelectListener(object : SpanGrid.OnGridSelectListener {

            override fun onEventMove(px: Point) {
                if (binding.blockers.isChecked) {
                    setBit(px, Keys.BLOCK)
                    gridCanvasView.play()
                }
            }

            override fun onEventUp() {
            }

            override fun onEventDown(px: Point) {
                when (val type = getType()) {
                    Keys.BLOCK -> {

                        if (binding.enableEdit.isChecked) {
                            clearBit(px)
                        }else{
                            setBit(px, type)
                        }
                        gridCanvasView.play()
                    }
                    Keys.START -> {
                        startPont = if (binding.enableEdit.isChecked) {
                            clearBit(px)
                            null
                        }else{
                            startPont?.let { start ->
                                clearBit(start)
                            }
                            setBit(px, type)
                            px
                        }
                        gridCanvasView.play()
                    }
                    Keys.END -> {

                        endPont = if (binding.enableEdit.isChecked) {
                            clearBit(px)
                            null
                        }else{
                            endPont?.let { end ->
                                clearBit(end)
                            }
                            setBit(px, type)
                            px
                        }
                        gridCanvasView.play()
                    }
                }
            }

            override fun onModeChange(mode: Int) {

            }
        })

        binding.speedSlide.addOnChangeListener { _, value, _ ->
            sleepVal = value.toLong()
        }
    }

    private fun getType() =
        if (binding.start.isChecked) Keys.START else if (binding.end.isChecked) Keys.END else Keys.BLOCK

    private fun init() {
        binding.toolBar.title = "Path finder"
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        gridCanvasView = SpanGrid(this)
        binding.gridViewHolder.addView(gridCanvasView)
        gridCanvasView.resolution = 30f
        gridCanvasView.post {
            gridCanvasView.init()
        }


        cellList.add(CellItem(R.drawable.ic_round_start_24,"Start Node"))
        cellList.add(CellItem(R.drawable.ic_round_stop_24,"End Node"))
        cellList.add(CellItem(R.drawable.ic_round_block_24,"Block Node"))
        cellList.add(CellItem(R.drawable.ic_eraser_solid,"Eraser"))

        cellSpinnerAdapter = CellSpinnerAdapter(this,cellList)
        binding.cellSelector.adapter = cellSpinnerAdapter

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

            gHeight = if (gHeight % 2 == 0) gHeight - 1 else gHeight
            gWidth = if (gWidth % 2 == 0) gWidth - 1 else gWidth

            generateBorder(gWidth, gHeight)
            recursiveMaze(1 , 1 , gWidth - 1 , gHeight - 1 )

        }
    }

    private fun generateBorder(gWidth: Int, gHeight: Int) {

        for (i in 0  until gWidth ) {
            setBit(Point(i), Keys.BLOCK)
            setBit(Point(i, gHeight - 1 ), Keys.BLOCK)
            gridCanvasView.play()
        }
        for (j in 0  until gHeight ) {
            setBit(Point(0 , j), Keys.BLOCK)
            setBit(Point(gWidth - 1 , j), Keys.BLOCK)
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
            setBit(Point(i, y), Keys.BLOCK)
            gridCanvasView.play()
        }
    }

    private fun drawLineVer(y1: Int, y2: Int, x: Int) {
        for (i in y1..y2) {
            runBlocking {
                delay(sleepVal)
            }
            setBit(Point(x, i), Keys.BLOCK)
            gridCanvasView.play()
        }
    }

    internal fun setBit(point: Point, type: Int) {
        val data = getData(point)
        gridHash[point] = data.copy(type = type)
        colors[type]?.let {
            gridCanvasView.plotPoint(
                point,
                ContextCompat.getColor(this, it)
            )
        }

    }

    private fun clearBit(point: Point) {
        gridHash.remove(point)
        gridCanvasView.removeRect(point)
    }

    private fun invalidateData() = startPont != null && endPont != null
}