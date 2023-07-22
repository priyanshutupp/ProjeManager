package com.example.projemanager.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.projemanager.R
import com.example.projemanager.databinding.ItemMemberBinding
import com.example.projemanager.models.User
import com.example.projemanager.utils.Constants

open class MembersAdapter(private val context: Context, private val list: ArrayList<User>):
    RecyclerView.Adapter<MembersAdapter.MemberViewHolder>() {

    private var onClickListener : OnClickListener? = null

    inner class MemberViewHolder (private val itemBinding: ItemMemberBinding):
        RecyclerView.ViewHolder(itemBinding.root){
        fun bindItem(item: User){
            itemBinding.ivMemberImage.let {
                Glide
                    .with(context)
                    .load(item.image)
                    .centerCrop()
                    .placeholder(R.drawable.ic_user_place_holder)
                    .into(it)
            }
            itemBinding.tvMemberName.text = item.name
            itemBinding.tvMemberEmail.text = item.email

            if(item.selected){
                itemBinding.ivSelectedMember.visibility = View.VISIBLE
            }
            else{
                itemBinding.ivSelectedMember.visibility = View.GONE
            }
        }
        val bindview = itemBinding.root
    }

    interface OnClickListener{
        fun onClick(position: Int, entity: User, action: String)
    }

    fun setOnClickListener(onClickListener: OnClickListener){
        this.onClickListener = onClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        return MemberViewHolder(
            ItemMemberBinding.inflate(
                LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val item = list[position]
        holder.bindItem(item)
        holder.bindview.setOnClickListener {
            if (onClickListener != null) {
                if(item.selected){
                    onClickListener!!.onClick(position, item, Constants.UN_SELECT)
                }
                else{
                    onClickListener!!.onClick(position, item, Constants.SELECT)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}