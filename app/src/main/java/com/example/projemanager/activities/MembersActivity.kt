package com.example.projemanager.activities

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projemanager.R
import com.example.projemanager.adapters.MembersAdapter
import com.example.projemanager.databinding.ActivityMembersBinding
import com.example.projemanager.databinding.DialogAddMemberBinding
import com.example.projemanager.firebase.FirestoreClass
import com.example.projemanager.models.Board
import com.example.projemanager.models.User
import com.example.projemanager.utils.Constants

class MembersActivity : BaseActivity() {
    private var binding: ActivityMembersBinding? = null
    private lateinit var mBoardDetail: Board
    private lateinit var mAssignedMembersList: ArrayList<User>
    private var anyChangesMade: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMembersBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        if (intent.hasExtra(Constants.BOARDS)){
            mBoardDetail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Constants.BOARDS, Board::class.java)!!
            } else{
                intent.getParcelableExtra(Constants.BOARDS)!!
            }
        }
        setUpActionBar()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    mOnBackPressed()
                }
            })

        showCustomProgressDialog()
        FirestoreClass().getAssignedMembersList(this, mBoardDetail.assignedTo)
    }

    private fun mOnBackPressed(){
        if(anyChangesMade){
            setResult(RESULT_OK)
        }
        onBackPressedDispatcher.onBackPressed()
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_add_member, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_add_member -> {
                showAddMembersDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showAddMembersDialog(){
        val dialog = Dialog(this)
        val dialogBinding: DialogAddMemberBinding = DialogAddMemberBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialogBinding.tvAdd.setOnClickListener {
            if(dialogBinding.etEmailSearchMember.text.isNullOrEmpty()){
                showErrorSnackBar("Enter an email...")
            }
            else{
                dialog.dismiss()
                showCustomProgressDialog()
                FirestoreClass().getMembersDetails(this, dialogBinding.etEmailSearchMember.text.toString())
            }
        }
        dialogBinding.tvCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.setCancelable(false)
        dialog.create()
        dialog.show()
    }

    fun memberDetails(user: User){
        mBoardDetail.assignedTo.add(user.id)
        FirestoreClass().assignMemberToBoard(this, mBoardDetail, user)
    }

    fun memberAssignedSuccess(user: User){
        anyChangesMade = true
        cancelCustomProgressDialog()
        mAssignedMembersList.add(user)
        setUpMembersList(mAssignedMembersList)
    }

    fun setUpMembersList(list: ArrayList<User>){
        mAssignedMembersList = list
        cancelCustomProgressDialog()
        binding?.rvMembersList?.layoutManager = LinearLayoutManager(this)
        binding?.rvMembersList?.setHasFixedSize(true)
        val adapter = MembersAdapter(this, list)
        binding?.rvMembersList?.adapter = adapter
    }

    private fun setUpActionBar() {
        setSupportActionBar(binding?.toolbarMembersActivity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_button_white)
        supportActionBar?.title = resources.getString(R.string.members)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        binding?.toolbarMembersActivity?.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            finish()
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}