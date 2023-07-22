package com.example.projemanager.activities

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.GridLayoutManager
import com.example.projemanager.R
import com.example.projemanager.adapters.CardMembersAdapter
import com.example.projemanager.databinding.ActivityCardDetailsBinding
import com.example.projemanager.dialogs.LabelColorListDialog
import com.example.projemanager.dialogs.MemberListDialog
import com.example.projemanager.firebase.FirestoreClass
import com.example.projemanager.models.Board
import com.example.projemanager.models.Card
import com.example.projemanager.models.SelectedMember
import com.example.projemanager.models.Task
import com.example.projemanager.models.User
import com.example.projemanager.utils.Constants
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CardDetailsActivity : BaseActivity() {
    private var binding: ActivityCardDetailsBinding? = null
    private lateinit var mBoardDetail: Board
    private var taskListPosition: Int = -1
    private var cardListPosition: Int = -1
    private var anyChangesMade: Boolean = false
    private var mSelectedColor = ""
    private lateinit var mMembersDetailsList: ArrayList<User>
    private var mSelectedDueDateMilliSeconds: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCardDetailsBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        getIntentData()
        setUpActionBar()

        binding?.etNameCardDetails?.setText(mBoardDetail.taskList[taskListPosition].cards[cardListPosition].name)
        binding?.etNameCardDetails?.setSelection(binding?.etNameCardDetails?.text?.toString()!!.length)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    mOnBackPressed()
                }
            })

        binding?.tvSelectLabelColor?.setOnClickListener {
            labelColorListDialog()
        }

        setUpSelectedMembersList()

        binding?.tvSelectMembers?.setOnClickListener {
            membersListDialog()
        }

        mSelectedColor = mBoardDetail.taskList[taskListPosition].cards[cardListPosition].labelColor
        if(mSelectedColor.isNotEmpty()){
            setColor()
        }

        mSelectedDueDateMilliSeconds = mBoardDetail.taskList[taskListPosition].cards[cardListPosition].dueDate
        if(mSelectedDueDateMilliSeconds > 0){
            val sdf = SimpleDateFormat("dd/mm/yyyy", Locale.ENGLISH)
            val selectedDate = sdf.format(Date(mSelectedDueDateMilliSeconds))
            binding?.tvSelectDueDate?.text = selectedDate
        }

        binding?.tvSelectDueDate?.setOnClickListener {
            showDatePicker()
        }

        binding?.btnUpdateCardDetails?.setOnClickListener{
            if(binding?.etNameCardDetails?.text?.toString()!!.isEmpty()){
                showErrorSnackBar("Card name cannot be empty!")
            }
            else{
                updateCardDetails()
            }
        }
    }

    private fun mOnBackPressed(){
        if(anyChangesMade){
            setResult(RESULT_OK)
        }
        onBackPressedDispatcher.onBackPressed()
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delete_card, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_delete_card -> {
                deleteCardAlertDialog(mBoardDetail.taskList[taskListPosition].cards[cardListPosition].name)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getIntentData(){
        if(intent.hasExtra(Constants.BOARDS)){
            mBoardDetail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Constants.BOARDS, Board::class.java)!!
            } else{
                intent.getParcelableExtra(Constants.BOARDS)!!
            }
        }

        if(intent.hasExtra(Constants.TASK_LIST_ITEM_POSITION)){
            taskListPosition = intent.getIntExtra(Constants.TASK_LIST_ITEM_POSITION, -1)
        }

        if(intent.hasExtra(Constants.CARD_LIST_ITEM_POSITION)){
            cardListPosition = intent.getIntExtra(Constants.CARD_LIST_ITEM_POSITION, -1)
        }

        if(intent.hasExtra(Constants.BOARD_MEMBERS_LIST)){
            mMembersDetailsList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Constants.BOARD_MEMBERS_LIST, User::class.java)!!
            } else{
                intent.getParcelableArrayListExtra(Constants.BOARD_MEMBERS_LIST)!!
            }
        }
    }

    private fun setUpActionBar() {
        setSupportActionBar(binding?.toolbarCardDetailsActivity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_button_white)
        supportActionBar?.title = mBoardDetail.taskList[taskListPosition].cards[cardListPosition].name
        supportActionBar?.setDisplayShowTitleEnabled(true)
        binding?.toolbarCardDetailsActivity?.setNavigationOnClickListener {
            if(anyChangesMade){
                setResult(RESULT_OK)
            }
            onBackPressedDispatcher.onBackPressed()
            finish()
        }
    }
    private fun updateCardDetails(){
        val card = Card(binding?.etNameCardDetails?.text?.toString()!!,
            mBoardDetail.taskList[taskListPosition].cards[cardListPosition].createdBy,
            mBoardDetail.taskList[taskListPosition].cards[cardListPosition].assignedTo,
            mSelectedColor, mSelectedDueDateMilliSeconds)

        mBoardDetail.taskList.removeAt(mBoardDetail.taskList.size - 1)
        mBoardDetail.taskList[taskListPosition].cards[cardListPosition] = card

        showCustomProgressDialog()
        FirestoreClass().addUpdateTaskList(this@CardDetailsActivity, mBoardDetail)
    }

    private fun deleteCard(){
        val cardList: ArrayList<Card> = mBoardDetail.taskList[taskListPosition].cards
        cardList.removeAt(cardListPosition)

        val taskList: ArrayList<Task> = mBoardDetail.taskList
        taskList.removeAt(taskList.size-1)
        taskList[taskListPosition].cards = cardList

        showCustomProgressDialog()
        FirestoreClass().addUpdateTaskList(this@CardDetailsActivity, mBoardDetail)
    }

    private fun deleteCardAlertDialog(cardName: String){
        val dialog = AlertDialog.Builder(this)
            .setIcon(R.drawable.ic_alert_dialog)
            .setTitle("Alert")
            .setMessage("Do you really want to delete the Card: $cardName?")
            .setPositiveButton("Yes"){ dialog, _ ->
                dialog.dismiss()
                deleteCard()
            }
            .setNegativeButton("No"){ dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()
        dialog.show()
    }

    fun addUpdateTaskListSuccess() {
        cancelCustomProgressDialog()
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun colorsList(): ArrayList<String>{
        val colorsList: ArrayList<String> = ArrayList()
        colorsList.add("#ef476f")
        colorsList.add("#f78c6b")
        colorsList.add("#ffd166")
        colorsList.add("#06d6a0")
        colorsList.add("#118ab2")
        colorsList.add("#073b4c")
        return colorsList
    }

    private fun setColor(){
        binding?.tvSelectLabelColor?.text = ""
        binding?.tvSelectLabelColor?.setBackgroundColor(Color.parseColor(mSelectedColor))
    }

    private fun labelColorListDialog(){
        val colorList: ArrayList<String> = colorsList()
        val listDialog = object : LabelColorListDialog(
            this,
            colorList,
            resources.getString(R.string.str_select_label_color)){
            override fun onItemSelected(color: String) {
                mSelectedColor = color
                setColor()
            }
        }
        listDialog.show()
    }

    private fun membersListDialog(){
        val cardMembersAssignedList = mBoardDetail
            .taskList[taskListPosition]
            .cards[cardListPosition].assignedTo

        if(cardMembersAssignedList.size > 0){
            for (i in mMembersDetailsList.indices){
                for (j in cardMembersAssignedList){
                    if(mMembersDetailsList[i].id == j){
                        mMembersDetailsList[i].selected = true
                    }
                }
            }
        }
        else{
            for (i in mMembersDetailsList.indices){
                mMembersDetailsList[i].selected = false
            }
        }
        val listDialog = object : MemberListDialog(
            this@CardDetailsActivity,
            mMembersDetailsList,
            resources.getString(R.string.str_select_member)){
            override fun onItemSelected(user: User, action: String) {
                if(action == Constants.SELECT){
                    if(!mBoardDetail
                            .taskList[taskListPosition]
                            .cards[cardListPosition]
                            .assignedTo.contains(user.id)){
                        mBoardDetail
                            .taskList[taskListPosition]
                            .cards[cardListPosition]
                            .assignedTo.add(user.id)
                    }
                }
                else{
                    mBoardDetail
                        .taskList[taskListPosition]
                        .cards[cardListPosition]
                        .assignedTo.remove(user.id)

                    for(i in mMembersDetailsList.indices){
                        if(mMembersDetailsList[i].id == user.id){
                            mMembersDetailsList[i].selected = false
                        }
                    }
                }
                setUpSelectedMembersList()
            }
        }

        listDialog.show()
    }

    private fun setUpSelectedMembersList(){
        val cardAssignedMembersList = mBoardDetail
            .taskList[taskListPosition]
            .cards[cardListPosition]
            .assignedTo

        val selectedMembersList: ArrayList<SelectedMember> = ArrayList()

        for (i in mMembersDetailsList.indices){
            for (j in cardAssignedMembersList){
                if(mMembersDetailsList[i].id == j){
                    val selectedMember = SelectedMember(mMembersDetailsList[i].id, mMembersDetailsList[i].image)
                    selectedMembersList.add(selectedMember)
                }
            }
        }
        if(selectedMembersList.size>0){
            selectedMembersList.add(SelectedMember("", ""))
            binding?.tvSelectMembers?.visibility = GONE
            binding?.rvSelectedMembersList?.visibility = VISIBLE
            binding?.rvSelectedMembersList?.layoutManager = GridLayoutManager(this, 6)
            val adapter = CardMembersAdapter(this@CardDetailsActivity, selectedMembersList, true)
            binding?.rvSelectedMembersList?.adapter = adapter
            adapter.setOnClickListener(object: CardMembersAdapter.OnClickListener{
                override fun onClick(position: Int, entity: SelectedMember) {
                    membersListDialog()
                }
            })
        }
        else{
            binding?.tvSelectMembers?.visibility = VISIBLE
            binding?.rvSelectedMembersList?.visibility = GONE
        }
    }

    private fun showDatePicker(){
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val dpd = DatePickerDialog(this,
            { _, sYear, monthOfYear, dayOfMonth ->
                val sDayOfMonth = if(dayOfMonth < 10) "0$dayOfMonth" else "$dayOfMonth"
                val sMonthOfYear = if(monthOfYear+1 < 10) "0${monthOfYear+1}" else "${monthOfYear+1}"
                val selectedDate = "$sDayOfMonth/$sMonthOfYear/$sYear"
                binding?.tvSelectDueDate?.text = selectedDate
                val sdf = SimpleDateFormat("dd/mm/yyyy", Locale.ENGLISH)
                val theDate = sdf.parse(selectedDate)
                mSelectedDueDateMilliSeconds = theDate!!.time
            }, year, month, day)
        dpd.show()
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}