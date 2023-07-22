package com.example.projemanager.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projemanager.activities.TaskListActivity
import com.example.projemanager.databinding.ItemCardBinding
import com.example.projemanager.models.Card
import com.example.projemanager.models.SelectedMember

open class CardAdapter (private val context: Context, private val list: ArrayList<Card>):
    RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    private var onClickListener : OnClickListener? = null

    inner class CardViewHolder (private val itemBinding: ItemCardBinding):
    RecyclerView.ViewHolder(itemBinding.root){
        fun bindItem(item: Card){
            itemBinding.tvCardName.text = item.name
            if(item.labelColor.isNotEmpty()){
                itemBinding.viewLabelColor.visibility = VISIBLE
                itemBinding.viewLabelColor.setBackgroundColor(Color.parseColor(item.labelColor))
            }
            else{
                itemBinding.viewLabelColor.visibility = GONE
            }
            if((context as TaskListActivity).mAssignedMembersList.size > 0){
                val selectedMembersList: ArrayList<SelectedMember> = ArrayList()
                for(i in context.mAssignedMembersList.indices){
                    for(j in item.assignedTo){
                        if(context.mAssignedMembersList[i].id == j){
                            val selectedMember = SelectedMember(
                                context.mAssignedMembersList[i].id,
                                context.mAssignedMembersList[i].image)
                            selectedMembersList.add(selectedMember)
                        }
                    }
                }
                if(selectedMembersList.size > 0){
                    if(selectedMembersList.size == 1 && selectedMembersList[0].id == item.createdBy){
                        itemBinding.rvCardSelectedMembersList.visibility = GONE
                    }
                    else{
                        itemBinding.rvCardSelectedMembersList.visibility = VISIBLE
                        itemBinding.rvCardSelectedMembersList.layoutManager = GridLayoutManager(context, 4)
                        val adapter = CardMembersAdapter(context, selectedMembersList, false)
                        itemBinding.rvCardSelectedMembersList.adapter = adapter
                        adapter.setOnClickListener(object: CardMembersAdapter.OnClickListener{
                            override fun onClick(position: Int, entity: SelectedMember) {
                                if (onClickListener != null) {
                                    onClickListener!!.onClick(position, item)
                                }
                            }
                        })
                    }
                }
                else{
                    itemBinding.rvCardSelectedMembersList.visibility = GONE
                }
            }
        }
        val bindView = itemBinding.root
    }

    interface OnClickListener{
        fun onClick(position: Int, entity: Card)
    }

    fun setOnClickListener(onClickListener: OnClickListener){
        this.onClickListener = onClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        return CardViewHolder(
            ItemCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val item = list[position]
        holder.bindItem(item)
        holder.bindView.setOnClickListener {
            if (onClickListener != null) {
                onClickListener!!.onClick(position, item)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}