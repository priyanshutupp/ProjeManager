package com.example.projemanager.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projemanager.R
import com.example.projemanager.activities.TaskListActivity
import com.example.projemanager.databinding.ItemTaskBinding
import com.example.projemanager.models.Card
import com.example.projemanager.models.Task
import java.util.Collections

open class TaskListAdapter(private val context: Context, private val list: ArrayList<Task>):
    RecyclerView.Adapter<TaskListAdapter.TaskListViewHolder>() {

    private var mPositionDraggedFrom = -1
    private var mPositionDraggedTo = -1

    inner class TaskListViewHolder(private val itemBinding: ItemTaskBinding):
        RecyclerView.ViewHolder(itemBinding.root){
            fun bindItem(item: Task, position: Int){
                if(position == list.size-1){
                    itemBinding.tvAddTaskList.visibility = View.VISIBLE
                    itemBinding.llTaskItem.visibility = View.GONE
                }
                else{
                    itemBinding.tvAddTaskList.visibility = View.GONE
                    itemBinding.llTaskItem.visibility = View.VISIBLE
                }
                itemBinding.tvAddTaskList.setOnClickListener {
                    itemBinding.tvAddTaskList.visibility = View.GONE
                    itemBinding.cvAddTaskListName.visibility = View.VISIBLE
                }
                itemBinding.ibCloseListName.setOnClickListener {
                    itemBinding.tvAddTaskList.visibility = View.VISIBLE
                    itemBinding.llTaskItem.visibility = View.GONE
                }
                itemBinding.ibDoneListName.setOnClickListener {
                    val listName: String = itemBinding.etTaskListName.text.toString()
                    if(listName.isNotEmpty()){
                        if(context is TaskListActivity){
                            context.createTaskList(listName)
                        }
                    }
                    else{
                        if(context is TaskListActivity){
                            context.showErrorSnackBar("List name cannot be empty!")
                        }
                    }
                }
                itemBinding.tvTaskListTitle.text = item.title

                itemBinding.ibEditListName.setOnClickListener {
                    itemBinding.etEditTaskListName.setText(item.title)
                    itemBinding.llTitleView.visibility = View.GONE
                    itemBinding.cvEditTaskListName.visibility = View.VISIBLE
                }
                itemBinding.ibCloseEditableView.setOnClickListener {
                    itemBinding.llTitleView.visibility = View.VISIBLE
                    itemBinding.cvEditTaskListName.visibility = View.GONE
                }
                itemBinding.ibDoneEditListName.setOnClickListener {
                    val listName: String = itemBinding.etEditTaskListName.text.toString()
                    if(listName.isNotEmpty()){
                        if(context is TaskListActivity){
                            context.updateTaskList(position, listName, item)
                        }
                    }
                    else{
                        if(context is TaskListActivity){
                            context.showErrorSnackBar("List name cannot be empty!")
                        }
                    }
                }

                itemBinding.ibDeleteList.setOnClickListener {
                    showAlertDialog(position, item)
                }

                itemBinding.tvAddCard.setOnClickListener {
                    itemBinding.tvAddCard.visibility = View.GONE
                    itemBinding.cvAddCard.visibility = View.VISIBLE
                }
                itemBinding.ibCloseCardName.setOnClickListener {
                    itemBinding.tvAddCard.visibility = View.VISIBLE
                    itemBinding.cvAddCard.visibility = View.GONE
                }
                itemBinding.ibDoneCardName.setOnClickListener {
                    val cardName: String = itemBinding.etCardName.text.toString()
                    if(cardName.isNotEmpty()){
                        if(context is TaskListActivity){
                            context.addCardToTaskList(position, cardName)
                        }
                    }
                    else{
                        if(context is TaskListActivity){
                            context.showErrorSnackBar("List name cannot be empty!")
                        }
                    }
                }
                itemBinding.rvCardList.layoutManager = LinearLayoutManager(context)
                itemBinding.rvCardList.setHasFixedSize(true)
                val adapter = CardAdapter(context, item.cards)
                itemBinding.rvCardList.adapter = adapter
                adapter.setOnClickListener(object : CardAdapter.OnClickListener{
                    override fun onClick(cardPosition: Int, entity: Card) {
                        if(context is TaskListActivity){
                            context.cardDetails(position, cardPosition)
                        }
                    }
                })
                val dividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
                itemBinding.rvCardList.addItemDecoration(dividerItemDecoration)

                val helper = ItemTouchHelper(
                    object : ItemTouchHelper.SimpleCallback(
                        ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0){
                        override fun onMove(
                            recyclerView: RecyclerView,
                            dragged: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder
                        ): Boolean {
                            val draggedPos = dragged.adapterPosition
                            val targetPos = target.adapterPosition
                            if(mPositionDraggedFrom == -1){
                                mPositionDraggedFrom = draggedPos
                            }
                            mPositionDraggedTo = targetPos
                            Collections.swap(list[position].cards, draggedPos, targetPos)
                            adapter.notifyItemMoved(draggedPos, targetPos)
                            return false
                        }

                        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

                        override fun clearView(
                            recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder
                        ) {
                            super.clearView(recyclerView, viewHolder)
                            if(mPositionDraggedFrom != -1 && mPositionDraggedTo != -1 && mPositionDraggedFrom != mPositionDraggedTo){
                                (context as TaskListActivity).updateCardsInTaskList(position, list[position].cards)
                            }
                            mPositionDraggedFrom = -1
                            mPositionDraggedTo = -1
                        }
                    })
                helper.attachToRecyclerView(itemBinding.rvCardList)
            }
        }

    private fun showAlertDialog(position: Int, item: Task){
        AlertDialog.Builder(context)
            .setIcon(R.drawable.ic_alert_dialog)
            .setTitle("Alert")
            .setMessage("Do you really want to delete the list: ${item.title}?")
            .setPositiveButton("Yes"){ dialog, _ ->
                dialog.dismiss()
                if(context is TaskListActivity){
                    context.deleteTaskList(position)
                }
            }
            .setNegativeButton("No"){ dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()
            .show()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskListViewHolder {
        val viewBinding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false)
        val layoutParams = LinearLayout.LayoutParams((parent.width * 0.7).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.setMargins((15.toDp().toPx()),0,(40.toDp().toPx()),0)
        viewBinding.root.layoutParams=layoutParams

        return TaskListViewHolder(viewBinding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: TaskListViewHolder, position: Int) {
        val item = list[position]
        holder.bindItem(item, position)
    }

    private fun Int.toDp(): Int = (this / Resources.getSystem().displayMetrics.density).toInt()
    private fun Int.toPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()
}