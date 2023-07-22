package com.example.projemanager.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.projemanager.databinding.ItemLabelColorBinding

open class LabelColorItemAdapter(private val context: Context,
                            private val list: ArrayList<String>,
                            private val mSelectedColor: String):
    RecyclerView.Adapter<LabelColorItemAdapter.LabelColorViewHolder>() {

    private var onClickListener : LabelColorItemAdapter.OnClickListener? = null

        inner class LabelColorViewHolder(private val itemBinding: ItemLabelColorBinding):
            RecyclerView.ViewHolder(itemBinding.root){
            fun bindItem(item: String){
                itemBinding.viewMain.setBackgroundColor(Color.parseColor(item))
                if(item == mSelectedColor){
                    itemBinding.ivSelectedColor.visibility = View.VISIBLE
                }
                else{
                    itemBinding.ivSelectedColor.visibility = View.GONE
                }
            }
            val bindView = itemBinding.root
        }

    interface OnClickListener{
        fun onClick(position: Int, color: String)
    }

    fun setOnClickListener(onClickListener: LabelColorItemAdapter.OnClickListener){
        this.onClickListener = onClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelColorViewHolder {
        return LabelColorViewHolder(
            ItemLabelColorBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: LabelColorViewHolder, position: Int) {
        val item = list[position]
        holder.bindItem(item)
        holder.bindView.setOnClickListener {
            if(onClickListener != null) {
                onClickListener!!.onClick(position, item)
            }
        }
    }
}