package com.example.projemanager.activities

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projemanager.R
import com.example.projemanager.adapters.MembersAdapter
import com.example.projemanager.databinding.ActivityMembersBinding
import com.example.projemanager.databinding.DialogAddMemberBinding
import com.example.projemanager.firebase.FirestoreClass
import com.example.projemanager.models.Board
import com.example.projemanager.models.User
import com.example.projemanager.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

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
        SendNotificationToUserAsyncTask(mBoardDetail.name, user.fcmToken).startApiCall()
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

    private inner class SendNotificationToUserAsyncTask(val boardName: String, val token: String){
        fun startApiCall() {
            showCustomProgressDialog()
            lifecycleScope.launch(Dispatchers.IO) {
//                delay(5000L)
                val stringResult = makeApiCall()
                afterCallFinish(stringResult)
            }
        }

        fun makeApiCall(): String {
            var result: String
            var connection: HttpURLConnection? = null

            try{
                val url = URL(Constants.FCM_BASE_URL)
                connection = url.openConnection() as HttpURLConnection?
                connection!!.doInput = true
                connection.doOutput = true
                connection.instanceFollowRedirects = false
                connection.requestMethod = "POST"

                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("charset", "utf-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty(Constants.FCM_AUTHORISATION, "${Constants.FCM_KEY}=${Constants.FCM_SERVER_KEY}")

                connection.useCaches = false

                val wr = DataOutputStream(connection.outputStream)
                val jsonRequest = JSONObject()
                val dataObject = JSONObject()
                dataObject.put(Constants.FCM_KEY_TITLE, "Assigned to the board $boardName")
                dataObject.put(Constants.FCM_KEY_MESSAGE, "You have been assigned to the board by ${mAssignedMembersList[0].name}")
                jsonRequest.put(Constants.FCM_KEY_DATA, dataObject)
                jsonRequest.put(Constants.FCM_KEY_TO, token)

                wr.writeBytes(jsonRequest.toString())
                wr.flush()
                wr.close()

                val httpResult: Int = connection.responseCode
                if(httpResult == HttpURLConnection.HTTP_OK){
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val stringBuilder: StringBuilder = StringBuilder()
                    var line: String?
                    try{
                        while (reader.readLine().also { line = it } != null) {
                            stringBuilder.append(line+"\n")
                            Log.i("TAG", "doInBackground: $line\n")
                        }
                    }
                    catch (e: IOException){
                        e.printStackTrace()
                    }
                    finally {
                        try {  //there could be some error while closing the inputStream
                            inputStream.close()
                        }
                        catch (e:IOException){
                            e.printStackTrace()
                        }
                    }
                    result = stringBuilder.toString()
                }
                else{  //if the response code is not OK
                    result = connection.responseMessage
                }
            }
            catch (e: SocketTimeoutException){
                result = "Connection Timeout"
            }
            catch (e:Exception){
                result = "Error + ${e.message}"
            }
            finally {
                connection?.disconnect()
            }

            return result
        }

        fun afterCallFinish(result: String?) {
            cancelCustomProgressDialog()

            Log.i("JSON RESPONSE RESULT", result.toString())
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}