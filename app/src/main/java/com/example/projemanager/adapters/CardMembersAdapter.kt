package com.example.projemanager.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.projemanager.R
import com.example.projemanager.databinding.ItemCardSelectedMembersBinding
import com.example.projemanager.models.SelectedMember

open class CardMembersAdapter (private val context: Context,
                               private val list: ArrayList<SelectedMember>,
                               private val assignMembers: Boolean):
    RecyclerView.Adapter<CardMembersAdapter.CardMembersViewHolder>() {

    private var onClickListener: OnClickListener? = null

    inner class CardMembersViewHolder(private val itemBinding: ItemCardSelectedMembersBinding):
        RecyclerView.ViewHolder(itemBinding.root){
            fun bindItem(item: SelectedMember, position: Int){
                if(position == list.size-1 && assignMembers){
                    itemBinding.ivAddMember.visibility = View.VISIBLE
                    itemBinding.ivSelectedMemberImage.visibility = View.GONE
                }
                else{
                    itemBinding.ivAddMember.visibility = View.GONE
                    itemBinding.ivSelectedMemberImage.visibility = View.VISIBLE
                    itemBinding.ivSelectedMemberImage.let {
                        Glide
                            .with(context)
                            .load(item.image)
                            .centerCrop()
                            .placeholder(R.drawable.ic_user_place_holder)
                            .into(it)
                    }
                }
            }
            val bindView = itemBinding.root
    }

    interface OnClickListener{
        fun onClick(position: Int, entity: SelectedMember)
    }

    fun setOnClickListener(onClickListener: OnClickListener){
        this.onClickListener = onClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardMembersViewHolder {
        return CardMembersViewHolder(ItemCardSelectedMembersBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: CardMembersViewHolder, position: Int) {
        val item = list[position]
        holder.bindItem(item, position)
        holder.bindView.setOnClickListener {
            if (onClickListener != null) {
                onClickListener!!.onClick(position, item)
            }
        }
    }
}