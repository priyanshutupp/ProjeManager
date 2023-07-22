package com.example.projemanager.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projemanager.adapters.MembersAdapter
import com.example.projemanager.databinding.DialogListBinding
import com.example.projemanager.models.User

abstract class MemberListDialog(
    context: Context,
    private var list: ArrayList<User>,
    private val title: String = ""): Dialog(context) {

    private var binding: DialogListBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogListBinding.inflate(layoutInflater)
        binding?.root?.let { setContentView(it) }

        setCanceledOnTouchOutside(true)
        setCancelable(true)
        setUpRecyclerView()
    }

    private fun setUpRecyclerView(){
        binding?.tvTitle?.text = title
        if (list.size > 0){
            binding?.rvList?.layoutManager = LinearLayoutManager(context)
            val adapter = MembersAdapter(context, list)
            binding?.rvList?.adapter = adapter

            adapter.setOnClickListener(object : MembersAdapter.OnClickListener{
                override fun onClick(position: Int, user: User, action: String) {
                    dismiss()
                    onItemSelected(user, action)
                }
            })
        }
    }

    protected abstract fun onItemSelected(user: User, action: String)
}