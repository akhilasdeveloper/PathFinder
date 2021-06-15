package com.akhilasdeveloper.pathfinder

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import com.akhilasdeveloper.pathfinder.algorithms.HeapMin
import com.akhilasdeveloper.pathfinder.algorithms.mazegeneration.generateMaze
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
    internal var dataGrid: List<Square>? = null
    internal var data: List<Square>? = null
    internal var sleepVal = 0L
    internal var heapMin: HeapMin = HeapMin()

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

}