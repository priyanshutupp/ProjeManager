package com.example.projemanager.firebase

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.example.projemanager.activities.CardDetailsActivity
import com.example.projemanager.activities.CreateBoardActivity
import com.example.projemanager.activities.MainActivity
import com.example.projemanager.activities.MembersActivity
import com.example.projemanager.activities.MyProfileActivity
import com.example.projemanager.activities.SignInActivity
import com.example.projemanager.activities.SignUpActivity
import com.example.projemanager.activities.TaskListActivity
import com.example.projemanager.models.Board
import com.example.projemanager.models.User
import com.example.projemanager.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirestoreClass {
    private val mFirestore = FirebaseFirestore.getInstance()

    fun registerUser(activity: SignUpActivity, userInfo: User){
        mFirestore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .set(userInfo, SetOptions.merge())
            .addOnSuccessListener {
                activity.userRegisteredSuccess()
            }
            .addOnFailureListener { e->
                activity.cancelCustomProgressDialog()
                Log.e(activity.javaClass.simpleName,
                    "Error writing document",
                    e)
            }
    }

    fun getMembersDetails(activity: MembersActivity, email: String){
        mFirestore.collection(Constants.USERS)
            .whereEqualTo(Constants.EMAIL, email)
            .get()
            .addOnSuccessListener {document ->
                if(document.documents.size > 0){
                    val user = document.documents[0].toObject(User::class.java)!!
                    activity.memberDetails(user)
                }
                else{
                    activity.cancelCustomProgressDialog()
                    activity.showErrorSnackBar("No such user found!")
                }
            }
            .addOnFailureListener { e ->
                activity.cancelCustomProgressDialog()
                Log.e(activity.javaClass.simpleName,
                    "Error writing document",
                    e)
            }
    }

    fun assignMemberToBoard(activity: MembersActivity, board: Board, user: User){
        val assignedToHashMap = HashMap<String, Any>()
        assignedToHashMap[Constants.ASSIGNED_TO] = board.assignedTo
        mFirestore.collection(Constants.BOARDS)
            .document(board.documentId)
            .update(assignedToHashMap)
            .addOnSuccessListener {
                activity.memberAssignedSuccess(user)
            }
            .addOnFailureListener { e->
                activity.cancelCustomProgressDialog()
                Log.e(activity.javaClass.simpleName,
                    "Error writing document",
                    e)
            }
    }

    fun getAssignedMembersList(activity: Activity, assignedTo: ArrayList<String>){
        mFirestore.collection(Constants.USERS)
            .whereIn(Constants.ID, assignedTo)
            .get()
            .addOnSuccessListener { document ->
                val usersList: ArrayList<User> = ArrayList()
                for (i in document.documents){
                    val user = i.toObject(User::class.java)!!
                    usersList.add(user)
                }
                if(activity is MembersActivity){
                    activity.setUpMembersList(usersList)
                }
                else if(activity is TaskListActivity){
                    activity.boardMembersDetailsList(usersList)
                }
            }
            .addOnFailureListener { e ->
                if(activity is MembersActivity) {
                    activity.cancelCustomProgressDialog()
                }
                else if(activity is TaskListActivity) {
                    activity.cancelCustomProgressDialog()
                }
                Log.e(activity.javaClass.simpleName,
                    "Error writing document",
                    e)
            }
    }

    fun addUpdateTaskList(activity: Activity, board: Board){
        val taskListHashMap = HashMap<String, Any>()
        taskListHashMap[Constants.TASK_LIST] = board.taskList

        mFirestore.collection(Constants.BOARDS)
            .document(board.documentId)
            .update(taskListHashMap)
            .addOnSuccessListener {
                if(activity is TaskListActivity)
                    activity.addUpdateTaskListSuccess()
                else if(activity is CardDetailsActivity)
                    activity.addUpdateTaskListSuccess()
            }
            .addOnFailureListener { e->
                if(activity is TaskListActivity)
                    activity.cancelCustomProgressDialog()
                else if(activity is CardDetailsActivity)
                    activity.cancelCustomProgressDialog()
                Log.e(activity.javaClass.simpleName,
                    "Error writing document",
                    e)
            }
    }

    fun createBoard(activity: CreateBoardActivity, boardInfo: Board){
        mFirestore.collection(Constants.BOARDS)
            .document()
            .set(boardInfo, SetOptions.merge())
            .addOnSuccessListener {
                activity.createBoardSuccess()
            }
            .addOnFailureListener { e->
                activity.cancelCustomProgressDialog()
                Log.e(activity.javaClass.simpleName,
                    "Error writing document",
                    e)
            }
    }

    fun getBoardDetails(activity: TaskListActivity, documentId: String){
        mFirestore.collection(Constants.BOARDS)
            .document(documentId)
            .get().addOnSuccessListener { document ->
                Log.i(activity.javaClass.simpleName, document.toString())
                val board = document.toObject(Board::class.java)!!
                board.documentId = document.id
                activity.boardDetails(board)
            }
            .addOnFailureListener { e ->
                activity.cancelCustomProgressDialog()
                Log.e(activity.javaClass.simpleName,
                    "Error writing document",
                    e) }
    }

    fun getBoardsList(activity: MainActivity){
        mFirestore.collection(Constants.BOARDS)
            .whereArrayContains(Constants.ASSIGNED_TO, getCurrentUserId())
            .get().addOnSuccessListener { document ->
                Log.i(activity.javaClass.simpleName, document.documents.toString())
                val boardList: ArrayList<Board> = ArrayList()
                for (i in document.documents){
                    val board = i.toObject(Board::class.java)!!
                    board.documentId = i.id
                    boardList.add(board)
                }
                activity.populateBoardsListToUI(boardList)
            }
            .addOnFailureListener { e ->
                activity.cancelCustomProgressDialog()
                Log.e(activity.javaClass.simpleName,
                "Error writing document",
                e) }
    }

    suspend fun loadUserData(activity: Activity, readBoardList: Boolean = false){
        withContext(Dispatchers.IO){
            mFirestore.collection(Constants.USERS)
                .document(getCurrentUserId())
                .get()
                .addOnSuccessListener { document ->
                    val loggedInUser = document.toObject(User::class.java)
                    when(activity){
                        is SignInActivity -> {
                            activity.signInSuccess(loggedInUser!!)
                        }
                        is MainActivity -> {
                            activity.updateNavigationUserDetails(loggedInUser!!, readBoardList)
                        }
                        is MyProfileActivity -> {
                            activity.updateUserProfile(loggedInUser!!)
                        }
                    }
                }
                .addOnFailureListener { e->
                    when(activity){
                        is SignInActivity -> {
                            activity.cancelCustomProgressDialog()
                        }
                        is MainActivity -> {
                            activity.cancelCustomProgressDialog()
                        }
                        is MyProfileActivity -> {
                            activity.cancelCustomProgressDialog()
                        }
                    }
                    Log.e(activity.javaClass.simpleName,
                        "Error writing document",
                        e)
                }
        }
    }

    suspend fun updateUserProfileData(activity: Activity, userHashMap: HashMap<String, Any>){
        withContext(Dispatchers.IO){
            mFirestore.collection(Constants.USERS)
                .document(getCurrentUserId())
                .update(userHashMap)
                .addOnSuccessListener {
                    Log.i(javaClass.simpleName, "Profile Data updated successfully")
                    Toast.makeText(activity.applicationContext, "Profile Data updated successfully", Toast.LENGTH_LONG).show()
                    when(activity){
                        is MainActivity ->{
                            activity.tokenUpdateSuccess()
                        }
                        is MyProfileActivity ->{
                            activity.cancelCustomProgressDialog()
                        }
                    }
                }
                .addOnFailureListener { e->
                    when(activity){
                        is MainActivity ->{
                            activity.cancelCustomProgressDialog()
                        }
                        is MyProfileActivity ->{
                            activity.cancelCustomProgressDialog()
                        }
                    }
                    Log.i(javaClass.simpleName, "Error while creating a board", e)
                    Toast.makeText(activity.applicationContext, "", Toast.LENGTH_LONG).show()
                }
        }

    }

    fun getCurrentUserId(): String{
        val currentUser = FirebaseAuth.getInstance().currentUser
        var currentUserId = ""
        if(currentUser != null){
            currentUserId = currentUser.uid
        }
        return currentUserId
    }
}