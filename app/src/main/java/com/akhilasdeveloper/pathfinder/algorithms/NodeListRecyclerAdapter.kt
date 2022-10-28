package com.akhilasdeveloper.pathfinder.algorithms

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.*
import com.akhilasdeveloper.pathfinder.databinding.CellSpinnerRowBinding
import com.akhilasdeveloper.pathfinder.models.CellItem

class ShareRecyclerAdapter(
    private val interaction: NodeListClickListener? = null
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val bindingPhoto = CellSpinnerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(bindingPhoto, interaction)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = differ.currentList[position]

        val photoItemViewHolder = holder as PhotoViewHolder
        currentItem?.let {
            photoItemViewHolder.bindPhoto(currentItem)
        }
    }


    class PhotoViewHolder(
        private val binding: CellSpinnerRowBinding,
        private val interaction: NodeListClickListener?
    ) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindPhoto(photo: CellItem) {
            binding.apply {
                val color = ContextCompat.getColor(this.root.context, photo.cell.color)
                cellIcon.setImageResource(photo.cellIcon)
                ImageViewCompat.setImageTintList(
                    cellIcon,
                    ColorStateList.valueOf(color)
                )
                val string = photo.cell.name + " \n(" + (if (photo.cell.weight == Int.MAX_VALUE) "Infinity" else photo.cell.weight) + ")"
                cellName.text = string
                if (photo.selected)
                    selected.visibility = View.VISIBLE
                else
                    selected.visibility = View.GONE

            }

            binding.root.setOnClickListener {
                interaction?.onItemClicked(photo)
            }
        }

    }

    fun submitList(list: List<CellItem>) {
        differ.submitList(list)
    }

    private val differ = AsyncListDiffer(
        RoverRecyclerChangeCallback(this),
        AsyncDifferConfig.Builder(DataDiffUtil).build()
    )

    internal inner class RoverRecyclerChangeCallback(
        private val adapter: ShareRecyclerAdapter
    ) : ListUpdateCallback {

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            adapter.notifyItemChanged(position)
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

    companion object {
        private val DataDiffUtil = object : DiffUtil.ItemCallback<CellItem>() {
            override fun areItemsTheSame(
                oldItem: CellItem,
                newItem: CellItem
            ) =
                oldItem.cell.type == newItem.cell.type

            override fun areContentsTheSame(
                oldItem: CellItem,
                newItem: CellItem
            ) =
                oldItem == newItem

        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
}

interface NodeListClickListener {
    fun onItemClicked(cellItem: CellItem)
}