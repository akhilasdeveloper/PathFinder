package com.akhilasdeveloper.pathfinder

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.findPathDijkstra
import com.akhilasdeveloper.pathfinder.databinding.ActivityMainBinding
import com.akhilasdeveloper.pathfinder.models.Point
import com.akhilasdeveloper.pathfinder.models.Square
import com.akhilasdeveloper.pathfinder.views.Keys.BLOCK
import com.akhilasdeveloper.pathfinder.views.Keys.EMPTY
import com.akhilasdeveloper.pathfinder.views.Keys.END
import com.akhilasdeveloper.pathfinder.views.Keys.PATH
import com.akhilasdeveloper.pathfinder.views.Keys.START
import com.akhilasdeveloper.pathfinder.views.Keys.VISITED
import com.akhilasdeveloper.pathfinder.views.OnNodeSelectListener
import com.akhilasdeveloper.pathfinder.views.SpanGrid
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.*
import kotlin.math.floor
import kotlin.random.Random
import kotlin.random.nextInt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    internal lateinit var spanGrid: SpanGrid
    private var dataGrid: List<Square>? = null
    internal var data: List<Square>? = null
    private var gaps = mutableListOf<Point>()
    internal var sleepVal = 0L

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
        binding.speedSlide.addOnChangeListener { _, value, _ ->
            sleepVal = value.toLong()
        }

        spanGrid.setNodeSelectListener(object : OnNodeSelectListener {
            override fun onEvent(px: Point) {

                if (binding.enableEdit.isChecked) {
                    spanGrid.setRect(px, EMPTY)
                } else {
                    if (binding.blockers.isChecked)
                        spanGrid.setRect(px, BLOCK)

                    if (binding.start.isChecked) {
                        spanGrid.setStart(px)
                    }
                    if (binding.end.isChecked) {
                        spanGrid.setEnd(px)
                    }
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
            R.id.grid -> {
                dataGrid = spanGrid.drawSquares
                generateMaze()
                true
            }
            R.id.play -> {
                data = spanGrid.drawSquares
                findPathDijkstra()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    internal fun invalidateData() = spanGrid.startPont != null && spanGrid.endPont != null

    private fun generateMaze() {
        CoroutineScope(Dispatchers.Default).launch {
            dataGrid?.let { data ->

                var gHeight = spanGrid.heightS
                var gWidth = spanGrid.widthS

                gHeight = if (gHeight % 2 == 0) gHeight - 1 else gHeight
                gWidth = if (gWidth % 2 == 0) gWidth - 1 else gWidth

                generateBorder(gWidth, gHeight)
                recursiveMaze(1, 1, gWidth - 1, gHeight - 1)
            }
        }
    }

    private fun generateBorder(gWidth: Int, gHeight: Int) {

        for (i in 0 until gWidth) {
            spanGrid.setRect(Point(i, 0), BLOCK)
            spanGrid.setRect(Point(i, gHeight - 1), BLOCK)
        }
        for (j in 0 until gHeight) {
            spanGrid.setRect(Point(0, j), BLOCK)
            spanGrid.setRect(Point(gWidth - 1, j), BLOCK)
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

            while (gaps.contains(Point(x1 - 1, cutHor)) || gaps.contains(Point(x2 + 1, cutHor))) {
                if (cols.size < 3)
                    return
                randCol = floor(Math.random() * cols.size).toInt()
                cutHor = cols[randCol]
            }

            drawLineHor(x1, x2, cutHor)

            val gapHor = rows[randRow]
            gaps.add(Point(gapHor, cutHor))
            spanGrid.setRect(Point(gapHor, cutHor), EMPTY)

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

            while (gaps.contains(Point(cutVer, y1 - 1)) || gaps.contains(Point(cutVer, y2 + 1))) {
                if (rows.size < 3)
                    return
                randRow = floor(Math.random() * rows.size).toInt()
                cutVer = rows[randRow]
            }

            drawLineVer(y1, y2, cutVer)

            val gapVer = cols[randCol]
            gaps.add(Point(cutVer, gapVer))
            spanGrid.setRect(Point(cutVer, gapVer), EMPTY)

            recursiveMaze(x1, y1, cutVer - 1, y2)
            recursiveMaze(cutVer + 1, y1, x2, y2)
        }
    }

    private fun drawLineHor(x1: Int, x2: Int, y: Int) {
        for (i in x1..x2) {
            runBlocking {
                delay(sleepVal)
            }
            Log.d("Output Horizontal : ", "$i , $y")
            spanGrid.setRect(Point(i, y), BLOCK)
        }
    }

    private fun drawLineVer(y1: Int, y2: Int, x: Int) {
        for (i in y1..y2) {
            runBlocking {
                delay(sleepVal)
            }
            Log.d("Output Vertical : ", "$x , $i")
            spanGrid.setRect(Point(x, i), BLOCK)
        }
    }
}