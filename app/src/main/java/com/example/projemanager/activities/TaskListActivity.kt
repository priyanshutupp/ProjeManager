package com.example.projemanager.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projemanager.R
import com.example.projemanager.adapters.TaskListAdapter
import com.example.projemanager.databinding.ActivityTaskListBinding
import com.example.projemanager.firebase.FirestoreClass
import com.example.projemanager.models.Board
import com.example.projemanager.models.Card
import com.example.projemanager.models.Task
import com.example.projemanager.models.User
import com.example.projemanager.utils.Constants

class TaskListActivity : BaseActivity() {
    private var binding: ActivityTaskListBinding? = null
    private lateinit var mBoardDetail: Board
    private lateinit var mDocumentId: String
    lateinit var mAssignedMembersList: ArrayList<User>

    private val resultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){ result ->
        if(result.resultCode == Activity.RESULT_OK){
            showCustomProgressDialog()
            FirestoreClass().getBoardDetails(this, mDocumentId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskListBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        if (intent.hasExtra(Constants.DOCUMENT_ID)){
            mDocumentId = intent.getStringExtra(Constants.DOCUMENT_ID)!!
        }
        showCustomProgressDialog()
        FirestoreClass().getBoardDetails(this, mDocumentId)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_members, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_members -> {
                val intent = Intent(this, MembersActivity::class.java)
                intent.putExtra(Constants.BOARDS, mBoardDetail)
                resultLauncher.launch(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setUpActionBar() {
        setSupportActionBar(binding?.toolbarTaskListActivity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_button_white)
        supportActionBar?.title = mBoardDetail.name
        supportActionBar?.setDisplayShowTitleEnabled(true)
        binding?.toolbarTaskListActivity?.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            finish()
        }
    }

    fun boardDetails(board: Board){
        mBoardDetail = board
        cancelCustomProgressDialog()
        setUpActionBar()

        showCustomProgressDialog()
        FirestoreClass().getAssignedMembersList(this, mBoardDetail.assignedTo)
    }

    fun addUpdateTaskListSuccess(){
        cancelCustomProgressDialog()
        showCustomProgressDialog()
        FirestoreClass().getBoardDetails(this, mBoardDetail.documentId)
    }

    fun createTaskList(taskListName: String){
        val task = Task(taskListName, FirestoreClass().getCurrentUserId())
        mBoardDetail.taskList.add(0, task)
        mBoardDetail.taskList.removeAt(mBoardDetail.taskList.size-1)
        showCustomProgressDialog()
        FirestoreClass().addUpdateTaskList(this, mBoardDetail)
    }

    fun updateTaskList(position: Int, listName: String, model: Task){
        val task = Task(listName, model.createdBy, model.cards)
        mBoardDetail.taskList.removeAt(mBoardDetail.taskList.size-1)
        mBoardDetail.taskList[position] = task
        showCustomProgressDialog()
        FirestoreClass().addUpdateTaskList(this, mBoardDetail)
    }

    fun deleteTaskList(position: Int){
        mBoardDetail.taskList.removeAt(position)
        mBoardDetail.taskList.removeAt(mBoardDetail.taskList.size-1)
        showCustomProgressDialog()
        FirestoreClass().addUpdateTaskList(this, mBoardDetail)
    }

    fun addCardToTaskList(position: Int, cardName: String){
        mBoardDetail.taskList.removeAt(mBoardDetail.taskList.size-1)
        val cardAssignedUserList: ArrayList<String> = ArrayList()
        cardAssignedUserList.add(FirestoreClass().getCurrentUserId())
        val card = Card(cardName, FirestoreClass().getCurrentUserId(), cardAssignedUserList)
        val cardList = mBoardDetail.taskList[position].cards
        cardList.add(card)
        val task = Task(mBoardDetail.taskList[position].title,
            mBoardDetail.taskList[position].createdBy, cardList)
        mBoardDetail.taskList[position] = task
        showCustomProgressDialog()
        FirestoreClass().addUpdateTaskList(this, mBoardDetail)
    }

    fun cardDetails(taskListPosition: Int, cardPosition: Int){
        val intent = Intent(this, CardDetailsActivity::class.java)
        intent.putExtra(Constants.BOARDS, mBoardDetail)
        intent.putExtra(Constants.TASK_LIST_ITEM_POSITION, taskListPosition)
        intent.putExtra(Constants.CARD_LIST_ITEM_POSITION, cardPosition)
        intent.putExtra(Constants.BOARD_MEMBERS_LIST, mAssignedMembersList)
        resultLauncher.launch(intent)
    }

    fun boardMembersDetailsList(list: ArrayList<User>){
        mAssignedMembersList = list
        cancelCustomProgressDialog()

        val addTask = Task(resources.getString(R.string.add_list))
        mBoardDetail.taskList.add(addTask)

        binding?.rvTaskList?.layoutManager = LinearLayoutManager(
            this, LinearLayoutManager.HORIZONTAL, false)
        binding?.rvTaskList?.setHasFixedSize(true)

        val adapter = TaskListAdapter(this, mBoardDetail.taskList)
        binding?.rvTaskList?.adapter = adapter
    }

    fun updateCardsInTaskList(mTaskListPosition: Int, cards: ArrayList<Card>){
        mBoardDetail.taskList.removeAt(mBoardDetail.taskList.size - 1)
        mBoardDetail.taskList[mTaskListPosition].cards = cards
        showCustomProgressDialog()
        FirestoreClass().addUpdateTaskList(this, mBoardDetail)
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}