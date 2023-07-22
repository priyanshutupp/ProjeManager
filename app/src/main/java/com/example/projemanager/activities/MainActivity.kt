package com.example.projemanager.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.projemanager.R
import com.example.projemanager.adapters.BoardsItemAdapter
import com.example.projemanager.databinding.ActivityMainBinding
import com.example.projemanager.databinding.NavHeaderMainBinding
import com.example.projemanager.firebase.FirestoreClass
import com.example.projemanager.models.Board
import com.example.projemanager.models.User
import com.example.projemanager.utils.Constants
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.installations.FirebaseInstallations
import kotlinx.coroutines.launch

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var binding: ActivityMainBinding? = null
    private lateinit var mUsername: String
    private lateinit var mSharedPreferences: SharedPreferences

    private val resultLauncherMyProfile: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){ result ->
        if(result.resultCode == Activity.RESULT_OK){
            lifecycleScope.launch{
                FirestoreClass().loadUserData(this@MainActivity)
            }
        }
    }
    private val resultLauncherCreateBoard: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){ result ->
        if(result.resultCode == Activity.RESULT_OK){
            lifecycleScope.launch{
                FirestoreClass().loadUserData(this@MainActivity, true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setUpActionBar()

        binding?.navView?.setNavigationItemSelectedListener(this)

        mSharedPreferences = this.getSharedPreferences(
            Constants.PROJEMANAGER_PREFERENCES, Context.MODE_PRIVATE)

        val tokenUpdated = mSharedPreferences.getBoolean(Constants.FCM_TOKEN_UPDATED, false)

        if(tokenUpdated){
            showCustomProgressDialog()
            lifecycleScope.launch {
                FirestoreClass().loadUserData(this@MainActivity, true)
            }
        }
        else{
            FirebaseInstallations.getInstance().getToken(true)
                .addOnSuccessListener(this@MainActivity){
                        result ->
                    updateFCMToken(result.token)
                }
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    mOnBackPressed()
                }
            })
        lifecycleScope.launch{
            FirestoreClass().loadUserData(this@MainActivity, true)
        }

        binding?.appBarMain?.fabAddBoard?.setOnClickListener {
            val intent = Intent(this, CreateBoardActivity::class.java)
            intent.putExtra(Constants.NAME, mUsername)
            resultLauncherCreateBoard.launch(intent)
        }
    }

    private fun setUpActionBar() {
        setSupportActionBar(binding?.appBarMain?.toolbarMainActivity)
        binding?.appBarMain?.toolbarMainActivity?.setNavigationIcon(R.drawable.ic_action_navigation)
        binding?.appBarMain?.toolbarMainActivity?.setNavigationOnClickListener {
            toggleDrawer()
        }
    }

    private fun toggleDrawer(){
        if(binding?.drawerLayout?.isDrawerOpen(GravityCompat.START) == true){
            binding?.drawerLayout?.closeDrawer(GravityCompat.START)
        }
        else{
            binding?.drawerLayout?.openDrawer(GravityCompat.START)
        }
    }

    private fun mOnBackPressed() {
        if(binding?.drawerLayout?.isDrawerOpen(GravityCompat.START) == true){
            binding?.drawerLayout?.closeDrawer(GravityCompat.START)
        }
        else{
            doubleBackToExit()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.nav_my_profile -> {
                val intent = Intent(this, MyProfileActivity::class.java)
                resultLauncherMyProfile.launch(intent)
            }
            R.id.nav_sign_out -> {
                FirebaseAuth.getInstance().signOut()
                mSharedPreferences.edit().clear().apply()
                val intent = Intent(this, IntroActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }
        binding?.drawerLayout?.closeDrawer(GravityCompat.START)
        return true
    }

    fun updateNavigationUserDetails(user: User, readBoardList: Boolean){
        cancelCustomProgressDialog()
        mUsername = user.name
        val viewHeader = binding?.navView?.getHeaderView(0)
        val headerBinding = viewHeader?.let { NavHeaderMainBinding.bind(it) }
        headerBinding?.navProfileImage?.let {
            Glide
                .with(this)
                .load(user.image)
                .centerCrop()
                .placeholder(R.drawable.ic_user_place_holder)
                .into(it)
        }
        headerBinding?.tvUsername?.text = user.name

        if(readBoardList){
            showCustomProgressDialog()
            FirestoreClass().getBoardsList(this)
        }
    }

    fun populateBoardsListToUI(boardList: ArrayList<Board>){
        cancelCustomProgressDialog()

        if(boardList.isEmpty()){
            binding?.appBarMain?.mainContent?.rvBoardsList?.visibility = GONE
            binding?.appBarMain?.mainContent?.tvNoBoards?.visibility = VISIBLE
        }
        else{
            binding?.appBarMain?.mainContent?.rvBoardsList?.visibility = VISIBLE
            binding?.appBarMain?.mainContent?.tvNoBoards?.visibility = GONE

            binding?.appBarMain?.mainContent?.rvBoardsList?.layoutManager = LinearLayoutManager(this)
            binding?.appBarMain?.mainContent?.rvBoardsList?.setHasFixedSize(true)

            val adapter = BoardsItemAdapter(this, boardList)
            binding?.appBarMain?.mainContent?.rvBoardsList?.adapter = adapter
            adapter.setOnClickListener(object: BoardsItemAdapter.OnClickListener{
                override fun onClick(position: Int, entity: Board) {
                    val boardIntent = Intent(this@MainActivity, TaskListActivity::class.java)
                    boardIntent.putExtra(Constants.DOCUMENT_ID, entity.documentId)
                    startActivity(boardIntent)
                }
            })
        }
    }

    fun tokenUpdateSuccess(){
        cancelCustomProgressDialog()
        val editor : SharedPreferences.Editor = mSharedPreferences.edit()
        editor.putBoolean(Constants.FCM_TOKEN_UPDATED, true)
        editor.apply()
        showCustomProgressDialog()
        lifecycleScope.launch {
            FirestoreClass().loadUserData(this@MainActivity, true)
        }

    }

    private fun updateFCMToken(token: String){
        val userHashMap = HashMap<String, Any>()
        userHashMap[Constants.FCM_TOKEN] = token
        showCustomProgressDialog()
        lifecycleScope.launch{
            FirestoreClass().updateUserProfileData(this@MainActivity, userHashMap)
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}