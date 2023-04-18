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
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.AIR_NODE
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.ASTAR_ALGORITHM
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.BFS_ALGORITHM
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.DIJKSTRA_ALGORITHM
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.END_NODE
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.START_NODE
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.WALL_NODE
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.FindPath.Companion.nodes
import com.akhilasdeveloper.pathfinder.databinding.ActivityMainBinding
import com.akhilasdeveloper.pathfinder.models.CellItem
import com.akhilasdeveloper.spangridview.SpanGridView
import com.akhilasdeveloper.spangridview.SpanGridView.Companion.MODE_DRAW
import com.akhilasdeveloper.spangridview.models.Point
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity(), NodeListClickListener {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var bottomSheetSettingsBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var bottomSheetMessagedBehavior: BottomSheetBehavior<NestedScrollView>

    internal lateinit var gridCanvasView: SpanGridView
    private var findPath: FindPath = FindPath()
    private var generateMaze: GenerateMaze = GenerateMaze()
    private var terrain: GenerateTerrain = GenerateTerrain()

    private lateinit var shareListAdapter: ShareRecyclerAdapter

    private var selectedNode = START_NODE
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
                        if (mode == MODE_DRAW) {
                            findPath.resetForDrawing()
                            R.drawable.ic_eye
                        } else {
                            R.drawable.ic_eye_off
                        }, theme
                    )
                }
            }
        })

        generateMaze.setMazeGenerateListener(object : GenerateMaze.OnMazeGenerateListener {
            override fun addData(px: Point) {
                findPath.addData(px, WALL_NODE)
            }

            override fun removeData(px: Point) {
                findPath.removeData(px)
            }

        })

        terrain.setTerrainGenerateListener(object : GenerateTerrain.OnTerrainGenerateListener {
            override fun addData(px: Point, weight: Int) {
                findPath.addData(px, weight)
            }
        })

        findPath.setPathFindListener(object : FindPath.OnPathFindListener {
            override fun onPathNotFound(type: String) {
                setMessage("$type: Path Not Fount")
                gridCanvasView.drawEnabled = false
            }

            override fun onError(message: String) {
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
            }

            override fun onPathFound(type: String, summary: FindPath.PathSummary) {
                val message =
                    "$type: Completed in ${summary.completedTimeMillis - summary.totalDelayMillis}ms ${if (summary.totalDelayMillis > 0) "(excluded animation delay(${summary.totalDelayMillis}))" else ""}.\nVisited: ${summary.visitedNodesCount} Nodes\nPath Length: ${summary.pathNodesCount}"
                setMessage(message)
                gridCanvasView.drawEnabled = false
            }

            override fun onDrawPoint(px: Point, color1: Int, color2: Int) {
                this@MainActivity.drawPoint(px, color1, color2)
            }

            override fun onClearPoint(px: Point) {
                clearBit(px)
            }

            override fun onClearResult(gridHash: Map<Point, FindPath.Square>) {
                repopulateGrid(gridHash)
            }

            override fun onClearAll(gridHash: Map<Point, FindPath.Square>) {
                repopulateGrid(gridHash)
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
                    val items = arrayOf("Recursive", "Terrain")

                    MaterialAlertDialogBuilder(this)
                        .setTitle("Select Maze Algorithm")
                        .setItems(items) { _, which ->
                            when (which) {
                                0 -> {
                                    if (gridCanvasView.pointsOnScreen.isNotEmpty()) {
                                        showAlert(
                                            "Points exist on current screen. Do you want to continue?",
                                            onOk = {
                                                gridCanvasView.drawEnabled = true
                                                generateRecursiveMaze()
                                            })
                                    } else {
                                        gridCanvasView.drawEnabled = true
                                        generateRecursiveMaze()
                                    }
                                }
                                1 -> {
                                    gridCanvasView.drawEnabled = true
                                    generateTerrain()
                                }
                            }
                        }
                        .show()
                    true
                }
                R.id.play -> {

                    val items = arrayOf(DIJKSTRA_ALGORITHM, ASTAR_ALGORITHM, BFS_ALGORITHM)

                    MaterialAlertDialogBuilder(this)
                        .setTitle("Select Path Algorithm")
                        .setItems(items) { _, which ->
                            findPath.findPath(items[which])
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
            generateMaze.sleepVal = value.toLong()
            terrain.sleepVal = value.toLong()
        }

        binding.closeSummary.setOnClickListener {
            bottomSheetMessagedBehavior.toggleSheet()
        }

    }

    private fun generateTerrain() {

        val gHeight = gridCanvasView.gridHeight.toInt()
        val gWidth = gridCanvasView.gridWidth.toInt()
        val startPoint = gridCanvasView.startPoint

        terrain.generateTerrain(startPoint, gHeight, gWidth)
    }

    private fun generateRecursiveMaze() {

        val gHeight = gridCanvasView.gridHeight.toInt()
        val gWidth = gridCanvasView.gridWidth.toInt()
        val startPoint = gridCanvasView.startPoint

        generateMaze.generateRecursiveMaze(startPoint, gHeight, gWidth)
    }

    private fun plotPointOnTouch(px: Point) {

        when (selectedNode) {

            START_NODE -> {
                findPath.startPont?.let { start ->
                    findPath.removeData(start)
                }
                findPath.startPont = px
                findPath.addData(px, selectedNode)

            }
            END_NODE -> {

                findPath.endPont?.let { start ->
                    findPath.removeData(start)
                }

                findPath.endPont = px
                findPath.addData(px, selectedNode)

            }
            AIR_NODE -> {
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


    private fun repopulateGrid(gridHash: Map<Point, FindPath.Square>) {
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
        if (selectedNode == START_NODE || selectedNode == END_NODE)
            gridCanvasView.brushSize = 1
        else
            gridCanvasView.brushSize = brushSize
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

    private fun showAlert(
        message: String,
        onOk: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(
            this,
            R.style.ThemeOverlay_MaterialComponents_Dialog_Alert
        )
            .setMessage(message)
            .setNegativeButton("No") { dialog, _ ->
                onCancel?.invoke()
                dialog.dismiss()
            }
            .setPositiveButton("Yes") { dialog, _ ->
                onOk?.invoke()
                dialog.dismiss()
            }
            .show()
    }

}

private fun <V : View?> BottomSheetBehavior<V>.toggleSheet() {
    state = if (state == BottomSheetBehavior.STATE_EXPANDED)
        BottomSheetBehavior.STATE_HIDDEN
    else
        BottomSheetBehavior.STATE_EXPANDED
}
