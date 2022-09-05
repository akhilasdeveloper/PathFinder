package com.akhilasdeveloper.pathfinder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.akhilasdeveloper.pathfinder.databinding.CellSpinnerRowBinding
import com.akhilasdeveloper.pathfinder.models.CellItem

class CellSpinnerAdapter(context: Context, list:ArrayList<CellItem>):ArrayAdapter<CellItem>(context,0,list) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    private fun initView(position: Int, convertView: View?, parent: ViewGroup):View{
        val view:CellSpinnerRowBinding = if (convertView == null){
            CellSpinnerRowBinding.inflate(LayoutInflater.from(context), parent, false)
        }else{
            CellSpinnerRowBinding.bind(convertView)
        }

        getItem(position)?.let { item->
            view.cellIcon.setImageResource(item.cellIcon)
            view.cellName.text = item.cellName
        }

        return view.root
    }
}