package com.example.projemanager.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.projemanager.R
import com.example.projemanager.databinding.ItemBoardBinding
import com.example.projemanager.models.Board

open class BoardsItemAdapter(private val context: Context, private val list: ArrayList<Board>):
    RecyclerView.Adapter<BoardsItemAdapter.BoardViewHolder>() {

    private var onClickListener : OnClickListener? = null

    inner class BoardViewHolder(private val itemBinding: ItemBoardBinding):
        RecyclerView.ViewHolder(itemBinding.root){
        fun bindItem(item: Board){
            itemBinding.tvName.text = item.name
            itemBinding.tvCreatedBy.text = item.createdBy
            itemBinding.ivBoardImage.let {
                Glide
                    .with(context)
                    .load(item.image)
                    .centerCrop()
                    .placeholder(R.drawable.ic_default_board)
                    .into(it)
            }
        }
        val bindView = itemBinding.root
    }

    fun setOnClickListener(onClickListener: OnClickListener){
        this.onClickListener = onClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoardViewHolder {
        return BoardViewHolder(ItemBoardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: BoardViewHolder, position: Int) {
        val item = list[position]
        holder.bindItem(item)
        holder.bindView.setOnClickListener{
            if(onClickListener != null){
                onClickListener!!.onClick(position, item)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    interface OnClickListener{
        fun onClick(position: Int, entity: Board)
    }
}