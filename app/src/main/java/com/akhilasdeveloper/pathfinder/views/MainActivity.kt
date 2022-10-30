package com.akhilasdeveloper.pathfinder.views

import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.GridLayoutManager
import com.akhilasdeveloper.pathfinder.R
import com.akhilasdeveloper.pathfinder.algorithms.NodeListClickListener
import com.akhilasdeveloper.pathfinder.algorithms.ShareRecyclerAdapter
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.*
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.ASTAR
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.BFS
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.DFS
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.DIJKSTRA
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.generateRecursiveMaze
import com.akhilasdeveloper.pathfinder.databinding.ActivityMainBinding
import com.akhilasdeveloper.pathfinder.models.CellItem
import com.akhilasdeveloper.pathfinder.models.Square
import com.akhilasdeveloper.pathfinder.models.nodes
import com.akhilasdeveloper.pathfinder.models.Keys
import com.akhilasdeveloper.pathfinder.models.Keys.END
import com.akhilasdeveloper.pathfinder.models.Keys.START
import com.akhilasdeveloper.spangridview.SpanGridView
import com.akhilasdeveloper.spangridview.models.Point
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NodeListClickListener {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var bottomSheetSettingsBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var bottomSheetMessagedBehavior: BottomSheetBehavior<NestedScrollView>
    internal lateinit var gridCanvasView: SpanGridView
    internal var findPath: FindPath = FindPath()

    private lateinit var shareListAdapter: ShareRecyclerAdapter

    private var selectedNode = START
        set(value) {
            clearSelection(field)
            field = value
            setSelection()
            setBrushSize()
        }

    private var brushSize = 1
        set(value) {
            field = value
            setBrushSize()
        }

    private val cellList: ArrayList<CellItem> = arrayListOf()

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

                binding.bottomAppBar.menu.findItem(R.id.viewMode)?.let { menuItem ->
                    menuItem.icon = ResourcesCompat.getDrawable(
                        resources,
                        if (mode == gridCanvasView.MODE_DRAW) {
                            /*if (findPath.executionCompleted)
                                reset()
                            clearGridHashBackup()*/
                            R.drawable.ic_eye
                        } else {
                            R.drawable.ic_eye_off
                        }, theme
                    )
                }
            }
        })

        findPath.setPathFindListener(object :FindPath.OnPathFindListener{
            override fun onPathNotFound(type: String) {
                setMessage("$type: Path Not Fount")
                gridCanvasView.drawEnabled = false
            }

            override fun onPathFound(type: String, summary: FindPath.PathSummary) {
                val message =
                    "$title: Completed in ${summary.timeMillis}ms ${if (summary.totalDelayMillis > 0) "(excluding animation delay(${summary.totalDelayMillis}))" else ""}.\nVisited: ${summary.visitedNodesCount} Nodes\nPath Length: ${summary.pathNodesCount}"
                setMessage(message)
                gridCanvasView.drawEnabled = false
            }

            override fun drawPoint(px: Point, color1: Int, color2: Int) {
                this@MainActivity.drawPoint(px, color1, color2)
            }

            override fun clearPoint(px: Point) {
                clearBit(px)
            }

            override fun onReset(gridHash: Map<Point, Square>) {
                repopulateGrid(gridHash)
            }

            override fun onResetAll() {
                gridCanvasView.clearData()
                gridCanvasView.postInvalidate()
                gaps.clear()
            }

        })

        binding.gridEnabled.setOnClickListener {
            gridCanvasView.lineEnabled = binding.gridEnabled.isChecked
        }

        binding.bottomAppBar.setNavigationOnClickListener {
            bottomSheetSettingsBehavior.toggleSheet()
        }

        binding.bottomAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.draw -> {
                    bottomSheetBehavior.toggleSheet()
                    true
                }
                R.id.clear -> {
                    findPath.reset()
                    true
                }
                R.id.clearAll -> {
                    findPath.resetAll()
                    true
                }
                R.id.viewMode -> {
                    gridCanvasView.drawEnabled = !gridCanvasView.drawEnabled
                    true
                }
                R.id.info -> {
                        bottomSheetMessagedBehavior.toggleSheet()
                    true
                }
                R.id.grid -> {
                    val items = arrayOf("Recursive")

                    MaterialAlertDialogBuilder(this)
                        .setTitle("Select Maze Algorithm")
                        .setItems(items) { _, which ->
                            when (which) {
                                0 -> {
                                    gridCanvasView.drawEnabled = true
                                    generateRecursiveMaze()
                                }
                            }
                        }
                        .show()
                    true
                }
                R.id.play -> {

                    val items = arrayOf(DIJKSTRA, ASTAR, BFS, DFS)

                    MaterialAlertDialogBuilder(this)
                        .setTitle("Select Path Algorithm")
                        .setItems(items) { _, which ->
                            if (findPath.startPont != null && findPath.endPont != null)
                                findPath.findPath(items[which])
                            else
                                Toast.makeText(
                                    this,
                                    "Please select start point and end point",
                                    Toast.LENGTH_SHORT
                                ).show()
                        }
                        .show()

                    true
                }
                else -> false
            }
        }

        binding.nodeSlide.addOnChangeListener { _, value, _ ->
            brushSize = value.toInt()
        }

        binding.speedSlide.addOnChangeListener { _, value, _ ->
            findPath.sleepVal = value.toLong()
            findPath.sleepValPath = value.toLong()
        }

    }

    private fun plotPointOnTouch(px: Point) {

        when (selectedNode) {

            Keys.START -> {
                findPath.startPont?.let { start ->
                    findPath.removeData(start)
                }
                findPath.startPont = px
                findPath.addData(px, selectedNode)

            }
            Keys.END -> {

                findPath.endPont?.let { start ->
                    findPath.removeData(start)
                }

                findPath.endPont = px
                findPath.addData(px, selectedNode)

            }
            Keys.AIR -> {
                findPath.removeData(px)
                if (px == findPath.startPont)
                    findPath.startPont = null
                if (px == findPath.endPont)
                    findPath.endPont = null
            }
            else -> {
                findPath.addData(px, selectedNode)
            }

        }
    }

    private fun init() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetSettingsBehavior = BottomSheetBehavior.from(binding.bottomSheetSettings)
        bottomSheetMessagedBehavior = BottomSheetBehavior.from(binding.bottomSheetMessage)
        gridCanvasView = binding.gridViewHolder
        gridCanvasView.brushSize = brushSize
        gridCanvasView.post {
            gridCanvasView.init()
        }

        binding.gridEnabled.isChecked = gridCanvasView.lineEnabled

        initialiseLists()

        shareListAdapter = ShareRecyclerAdapter(this)
        binding.nodeList.layoutManager = GridLayoutManager(this, 3)
        binding.nodeList.adapter = shareListAdapter
        shareListAdapter.submitList(cellList)
        setSelection()

        binding.speedSlide.value = findPath.sleepVal.toFloat()
    }


    private fun initialiseLists() {

        val cells = arrayListOf<CellItem>()
        for (node in nodes) {
            cells.add(
                CellItem(
                    cell = nodes(node)
                )
            )
        }

        cellList.addAll(cells.sortedBy { it.cell.weight }.toList())

    }

    private fun findArrayPosition(type: Int): Int? {
        cellList.forEachIndexed { index, cellItem ->
            if (cellItem.cell.type == type)
                return index
        }
        return null
    }

    private fun clearSelection(type: Int) {
        findArrayPosition(type)?.let {
            cellList[it].selected = false
            shareListAdapter.notifyItemChanged(it)
        }
    }

    private fun setSelection() {
        findArrayPosition(selectedNode)?.let {
            cellList[it].selected = true
            shareListAdapter.notifyItemChanged(it)
        }
    }


    private fun repopulateGrid(gridHash: Map<Point, Square>) {
        gridCanvasView.clearData()
        for (data in gridHash) {
            findPath.addData(data.key, data.value.type)
        }
    }

    internal fun setMessage(string: String) {
        val message = binding.message.text
        val newMessage = "$message\n\n$string"
        binding.message.text = newMessage
        bottomSheetMessagedBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_pathfinder, menu)
        return true
    }

    private fun setBrushSize() {
        if (selectedNode == START || selectedNode == END)
            gridCanvasView.brushSize = 1
        else
            gridCanvasView.brushSize = brushSize
    }

    internal fun drawLineHor(x1: Int, x2: Int, y: Int) {
        for (i in x1..x2) {
            runBlocking {
                delay(findPath.sleepVal)
            }
            findPath.addData(Point(i, y), Keys.WALL)
        }
    }

    internal fun drawLineVer(y1: Int, y2: Int, x: Int) {
        for (i in y1..y2) {
            runBlocking {
                delay(findPath.sleepVal)
            }
            findPath.addData(Point(x, i), Keys.WALL)
        }
    }

    private fun drawPoint(point: Point, color1: Int, color2: Int) {

        gridCanvasView.plotPoint(
            point,
            ContextCompat.getColor(this, color2),
            ContextCompat.getColor(this, color1)
        )
    }

    internal fun clearBit(point: Point) {
        gridCanvasView.removeRect(point)
    }



    override fun onItemClicked(cellItem: CellItem) {
        selectedNode = cellItem.cell.type
        bottomSheetBehavior.toggleSheet()
        gridCanvasView.drawEnabled = true
    }

}

private fun <V : View?> BottomSheetBehavior<V>.toggleSheet() {
    state = if (state == BottomSheetBehavior.STATE_EXPANDED)
        BottomSheetBehavior.STATE_HIDDEN
    else
        BottomSheetBehavior.STATE_EXPANDED
}
