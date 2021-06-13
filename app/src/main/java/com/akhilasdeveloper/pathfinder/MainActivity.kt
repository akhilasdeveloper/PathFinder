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
import kotlin.random.Random
import kotlin.random.nextInt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    internal lateinit var spanGrid: SpanGrid
    internal var data: List<Square>? = null

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
            R.id.play -> {
                data = spanGrid.drawSquares
                generateMaze()
//                findPathDijkstra()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    internal fun invalidateData() = spanGrid.startPont != null && spanGrid.endPont != null

    private fun generateMaze() {
        CoroutineScope(Dispatchers.Default).launch {
            data?.let { data ->

                val gHeight = spanGrid.heightS
                val gWidth = spanGrid.widthS

                val grid: List<Square> = generateBorder(data, gWidth, gHeight)

                recursiveMaze(0, 0, gWidth, gHeight)
//                recursiveMaze(gWidth, gHeight, Point())
            }
        }
    }

    private fun generateBorder(data: List<Square>, gWidth: Int, gHeight: Int): List<Square> {

        for (i in 0 until gWidth) {
            /*data[i].type = BLOCK
            data[i + (gHeight - 1) * gWidth].type = BLOCK*/
            spanGrid.setRect(Point(i, 0), BLOCK)
            spanGrid.setRect(Point(i, gHeight - 1), BLOCK)
        }
        for (j in 0 until gHeight) {
            /*data[j * gWidth].type = BLOCK
            data[(gWidth - 1) + j * gWidth].type = BLOCK*/
            spanGrid.setRect(Point(0, j), BLOCK)
            spanGrid.setRect(Point(gWidth - 1, j), BLOCK)
        }
        return data
    }

    //    private fun recursiveMaze(gWidth: Int, gHeight: Int, point:Point) {
    private fun recursiveMaze(x: Int, y: Int, gWidth: Int, gHeight: Int) {
        if (gWidth < 2 || gHeight < 2)
            return

        val isHorizontal = gWidth < gHeight
        var wx = x + if (isHorizontal) 0 else (0..gWidth - 2).random()
        var wy = y + if (isHorizontal) (0..gHeight - 2).random() else 0

        val px = wx + if (isHorizontal) (0..gWidth).random() else 0
        val py = wy + if (isHorizontal) 0 else (0..gHeight).random()

        val dx = if (isHorizontal) 1 else 0
        val dy = if (isHorizontal) 0 else 1

        val length = if (isHorizontal) gWidth else gHeight

        for (i in 0 until length) {
            spanGrid.setRect(Point(wx, wy), BLOCK)
            if (wx != px || wy != py) {
                wx += dx
                wy += dy
            }
        }

        var nx = x
        var ny = y

        var w = if (isHorizontal) gWidth else wx-x+1
        var h = if (isHorizontal) wy-y+1 else gHeight

        recursiveMaze(nx, ny, w, h)

        nx = if (isHorizontal) gWidth else wx+1
        ny = if (isHorizontal) wy+1 else y
        w = if (isHorizontal) gWidth else x+gWidth-wx-1
        h = if (isHorizontal) y+gHeight-wy-1 else gHeight

        recursiveMaze(nx, ny, w, h)

        /*if (isHorizontal){

            val cutHor = gHeight/2
            for (i in 0 + point.x until gWidth + point.x)
                spanGrid.setRect(Point(i,cutHor  + point.y), BLOCK)
            val gapHor = gWidth / 4
            spanGrid.setRect(Point(gapHor,cutHor), EMPTY)
            recursiveMaze(gWidth, cutHor, point)
            recursiveMaze(gWidth, cutHor, Point(point.x, point.y + cutHor))
        }else{
            val cutVer = gWidth/2
            for (i in 0 + point.y until gHeight + point.y)
                spanGrid.setRect(Point(cutVer  + point.x,i), BLOCK)
            val gapVer = gHeight / 4
            spanGrid.setRect(Point(cutVer,gapVer), EMPTY)
            recursiveMaze(cutVer,gHeight,point)
            recursiveMaze(cutVer,gHeight,Point(point.x+cutVer, point.y))
        }*/

    }
}