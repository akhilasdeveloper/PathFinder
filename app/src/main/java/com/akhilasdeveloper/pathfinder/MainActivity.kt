package com.akhilasdeveloper.pathfinder

import android.os.Bundle
import android.view.Menu
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.akhilasdeveloper.pathfinder.algorithms.HeapMinHash
import com.akhilasdeveloper.pathfinder.algorithms.NodeListClickListener
import com.akhilasdeveloper.pathfinder.algorithms.ShareRecyclerAdapter
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.*
import com.akhilasdeveloper.pathfinder.algorithms.pathfinding.findPathDijkstra
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity(), NodeListClickListener {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var bottomSheetSettingsBehavior: BottomSheetBehavior<LinearLayout>
    internal lateinit var gridCanvasView: SpanGridView
    internal var startPont: Point? = null
    internal var endPont: Point? = null
    internal var gridHash: HashMap<Point, Square> = hashMapOf()
    private var gridHashBackup: HashMap<Point, Square> = hashMapOf()
    private var xHash: HashMap<Int, Int> = hashMapOf()
    private var yHash: HashMap<Int, Int> = hashMapOf()
    internal var heapMin: HeapMinHash<Point> = HeapMinHash()
    internal var sleepVal = 0L
    internal var sleepValPath = 0L
    private var startedTimeInMillis = 0L
    internal var visitedNodesCount = 0
    internal var pathNodesCount = 0

    private lateinit var shareListAdapter: ShareRecyclerAdapter

    private var selectedItem = START
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
    private val pathAlgorithmList: ArrayList<String> = arrayListOf()
    private val gridAlgorithmList: ArrayList<String> = arrayListOf()


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

        binding.gridEnabled.setOnClickListener {
            gridCanvasView.lineEnabled = binding.gridEnabled.isChecked
        }

        binding.bottomAppBar.setNavigationOnClickListener {
            bottomSheetSettingsBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        binding.bottomAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.draw -> {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    true
                }
                R.id.clear -> {
                    reset()
                    true
                }
                R.id.grid -> {
                    val items = arrayOf("Recursive")

                    MaterialAlertDialogBuilder(this)
                        .setTitle("Select Maze Algorithm")
                        .setItems(items) { _, which ->
                            when (which) {
                                0 -> {
                                    generateRecursiveMaze()
                                }
                            }
                        }
                        .show()
                    true
                }
                R.id.play -> {

                    val items = arrayOf("Dijkstra", "A*", "BFS", "DFS")

                    MaterialAlertDialogBuilder(this)
                        .setTitle("Select Path Algorithm")
                        .setItems(items) { _, which ->
                            when (which) {
                                0 -> {
                                    if (startPont != null && endPont != null)
                                        findPathDijkstra()
                                    else
                                        Toast.makeText(
                                            this,
                                            "Please select start point and end point",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                }
                                1 -> {
                                    if (startPont != null && endPont != null)
                                        findAStar()
                                    else
                                        Toast.makeText(
                                            this,
                                            "Please select start point and end point",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                }
                                2 -> {
                                    if (startPont != null && endPont != null)
                                        findPathBFS()
                                    else
                                        Toast.makeText(
                                            this,
                                            "Please select start point and end point",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                }
                                3 -> {
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
            sleepVal = value.toLong()
            sleepValPath = value.toLong()
        }

    }

    private fun plotPointOnTouch(px: Point) {

        when (selectedItem) {

            Keys.START -> {
                startPont?.let { start ->
                    clearBit(start)
                }
                startPont = px
                setBit(px, selectedItem)

            }
            Keys.END -> {

                endPont?.let { start ->
                    clearBit(start)
                }

                endPont = px
                setBit(px, selectedItem)

            }
            Keys.AIR -> {
                clearBit(px)
                if (px == startPont)
                    startPont = null
                if (px == endPont)
                    endPont = null
            }
            else -> {
                setBit(px, selectedItem)
            }

        }
    }

    private fun init() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetSettingsBehavior = BottomSheetBehavior.from(binding.bottomSheetSettings)
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

        binding.speedSlide.value = sleepVal.toFloat()
    }


    private fun initialiseLists() {

        val cells = arrayListOf<CellItem>()
        for (node in nodes) {
            cells.add(CellItem(
                cell = nodes(node)
            ))
        }

        cellList.addAll(cells.sortedBy { it.cell.weight }.toList())

        pathAlgorithmList.add("Digkstra")
        gridAlgorithmList.add("Recursive Maze")
    }

    private fun findArrayPosition(type: Int):Int?{
        cellList.forEachIndexed { index, cellItem ->
            if (cellItem.cell.type == type)
                return index
        }
        return null
    }
    private fun clearSelection(type: Int){
        findArrayPosition(type)?.let {
            cellList[it].selected = false
            shareListAdapter.notifyItemChanged(it)
        }
    }

    private fun setSelection() {
        findArrayPosition(selectedItem)?.let {
            cellList[it].selected = true
            shareListAdapter.notifyItemChanged(it)
        }
        setBrushSelectedMessage(selectedItem)
    }

    private fun setBrushSelectedMessage(type: Int){
        val message = "Plot " + nodes(type).name + if(type != START && type != END) "(s)" else ""
        setMessage(message)
    }

    internal fun setPathMessage(title: String){
        val time = System.currentTimeMillis() - startedTimeInMillis
        val message = "$title: Completed in ${time}ms.\nVisited: $visitedNodesCount Nodes\nPath Length:$pathNodesCount"
        setMessage(message)
    }

    internal fun reset(){
        startedTimeInMillis = System.currentTimeMillis()
        visitedNodesCount = 0
        pathNodesCount = 0
        if (gridHashBackup.isEmpty()) {
            createBorder()
            copyGridHash()
        }
        else {
            gridHash.clear()
            heapMin.clear()
            restoreGridHash()
        }
        repopulateGrid()
    }

    private fun restoreGridHash() {
        for (data in gridHashBackup){
            gridHash[data.key] = nodes(type = data.value.type)
        }
    }

    private fun copyGridHash(){
        for (data in gridHash){
            gridHashBackup[data.key] = nodes(type = data.value.type)
        }
    }

    private fun repopulateGrid() {
        gridCanvasView.clearData()
        for(data in gridHash){
            setBit(data.key, data.value.type)
        }
    }

    private fun setMessage(string: String){
        binding.message.text = string
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_pathfinder, menu)
        return true
    }

    private fun setBrushSize() {
        if (selectedItem == START || selectedItem == END)
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

    private fun getMinXY(): Point {
        xHash.keys.toIntArray().minOrNull()?.let { x ->
            yHash.keys.toIntArray().minOrNull()?.let { y ->
                return Point(x, y)
            }
        }

        return Point(0, 0)
    }

    private fun getMaxXY(): Point {
        xHash.keys.toIntArray().maxOrNull()?.let { x ->
            yHash.keys.toIntArray().maxOrNull()?.let { y ->
                return Point(x, y)
            }
        }

        return Point(0, 0)
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

    override fun onItemClicked(cellItem: CellItem) {
        selectedItem = cellItem.cell.type
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

}