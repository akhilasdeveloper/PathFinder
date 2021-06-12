package com.akhilasdeveloper.pathfinder.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.*
import com.akhilasdeveloper.pathfinder.R
import com.akhilasdeveloper.pathfinder.databinding.LayoutBinding
import com.akhilasdeveloper.pathfinder.models.Node

class NodeAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Node>() {

        override fun areItemsTheSame(
            oldItem: Node,
            newItem: Node
        ): Boolean {
            return (oldItem.x == newItem.x && oldItem.y == newItem.y)
        }

        override fun areContentsTheSame(
            oldItem: Node,
            newItem: Node
        ): Boolean {
            return oldItem == newItem
        }

    }
    private val differ = AsyncListDiffer(
        NodeChangeCallback(this),
        AsyncDifferConfig.Builder(DIFF_CALLBACK).build())


    internal inner class NodeChangeCallback(
        private val adapter: NodeAdapter
    ) : ListUpdateCallback {

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            adapter.notifyDataSetChanged()
        }

        override fun onInserted(position: Int, count: Int) {
            adapter.notifyItemInserted(position)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            adapter.notifyItemMoved(fromPosition, toPosition)
        }

        override fun onRemoved(position: Int, count: Int) {
            adapter.notifyItemRemoved(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return NodeViewHolder(
            LayoutBinding.inflate(LayoutInflater.from(parent.context),parent, false),
            context
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is NodeViewHolder -> {
                holder.bind(differ.currentList[position])
            }
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    fun submitList(list: List<Node>) {
        Log.d("RECY : ","list ${list.size}")
        differ.submitList(list)
    }

    class NodeViewHolder
    constructor(
        itemView: LayoutBinding,
        private val contexts: Context
    ) : RecyclerView.ViewHolder(itemView.root) {

        fun bind(item: Node) = with(itemView) {
            Log.d("RCYC : ","x ${item.x}, y ${item.y}")
            /*if (item.isSelected)
                itemView.background = ResourcesCompat.getDrawable(contexts.resources,R.color.selected,null)
            else*/
                itemView.background = ResourcesCompat.getDrawable(contexts.resources,R.color.unselected,null)
        }
    }
}