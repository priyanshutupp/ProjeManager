package com.example.projemanager.activities

import android.Manifest
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.projemanager.R
import com.example.projemanager.databinding.ActivityCreateBoardBinding
import com.example.projemanager.firebase.FirestoreClass
import com.example.projemanager.models.Board
import com.example.projemanager.utils.Constants
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class CreateBoardActivity : BaseActivity(), View.OnClickListener {
    private var binding: ActivityCreateBoardBinding? = null

    private lateinit var mUsername: String

    private var mBoardImageURL: String? = ""
    private var mSelectedImageUri: Uri? = null
    private val galleryPermission = if(Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES
    else Manifest.permission.READ_EXTERNAL_STORAGE

    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if(result.resultCode == RESULT_OK && result.data != null){
                try {
                    showCustomProgressDialog()
                    mSelectedImageUri = result.data!!.data!!
                    lifecycleScope.launch{
                        binding?.ivBoardImage?.let {
                            Glide
                                .with(this@CreateBoardActivity)
                                .load(mSelectedImageUri)
                                .centerCrop()
                                .placeholder(R.drawable.ic_default_board)
                                .into(it)
                        }
                    }
                    cancelCustomProgressDialog()
                }catch (e: IOException){
                    e.printStackTrace()
                }
            }
        }

    private val requestLauncher : ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
                Permissions ->
            Permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if(isGranted){
                    if(permissionName == galleryPermission){
                        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        openGalleryLauncher.launch(galleryIntent)
                    }
                }
                else
                    if (permissionName == galleryPermission){
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                    }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateBoardBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        setUpActionBar()

        if(intent.hasExtra(Constants.NAME)){
            mUsername = intent.getStringExtra(Constants.NAME)!!
        }

        binding?.ivBoardImage?.setOnClickListener(this)
        binding?.etBoardName?.setOnClickListener(this)
        binding?.btnCreateBoard?.setOnClickListener(this)
    }

    private fun setUpActionBar() {
        setSupportActionBar(binding?.toolbarAddBoard)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_button_white)
        supportActionBar?.title = resources.getString(R.string.create_board_title)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        binding?.toolbarAddBoard?.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            finish()
        }
    }

    private fun selectFromGallery(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this@CreateBoardActivity, galleryPermission)){
            openSettingsRationaleDialog()
        }
        else{
            requestLauncher.launch(arrayOf(galleryPermission,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }
    }

    private fun openSettingsRationaleDialog(){
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Permission Required")
            .setMessage("You denied the storage permission. " +
                    "\nThis feature requires storage permission to work." +
                    " To enable storage permission go to Settings.")
            .setNegativeButton("Cancel"){ dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Settings"){ dialog, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException){
                    e.printStackTrace()
                }
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private suspend fun uploadBoardImage(){
        withContext(Dispatchers.IO){
            if(mSelectedImageUri != null){
                val sRef: StorageReference = FirebaseStorage
                    .getInstance()
                    .reference
                    .child(
                        "BOARD IMAGE"
                                + System.currentTimeMillis()
                                + "."
                                + getFileExtension(mSelectedImageUri!!))
                sRef.putFile(mSelectedImageUri!!).addOnSuccessListener {
                        taskSnapshot->
                    Log.i("Firebase Image URI",
                        taskSnapshot.metadata!!.reference!!.downloadUrl.toString())

                    taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener {
                            uri->
                        Log.i("Downloadable Image URL", uri.toString())
                        mBoardImageURL = uri.toString()
                        createBoard()
                    }
                }.addOnFailureListener{
                        exception->
                    runOnUiThread {
                        Toast.makeText(this@CreateBoardActivity, exception.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun getFileExtension(uri: Uri): String? {
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(uri))
    }

    private fun createBoard(){
        val assignedUsersArrayList: ArrayList<String> = ArrayList()
        assignedUsersArrayList.add(getCurrentUserID())
        val board = Board(
            binding?.etBoardName?.text?.toString()!!,
            mBoardImageURL!!,
            mUsername,
            assignedUsersArrayList
        )

        FirestoreClass().createBoard(this, board)
    }

    fun createBoardSuccess(){
        Toast.makeText(this,
            "Board successfully created",
            Toast.LENGTH_LONG).show()
        cancelCustomProgressDialog()
        setResult(RESULT_OK)
        finish()
    }

    override fun onClick(view: View?) {
        when(view!!.id){
            R.id.iv_board_image->{
                selectFromGallery()
            }
            R.id.btn_create_board->{
                if(binding?.etBoardName?.text?.isEmpty() == true){
                    showErrorSnackBar("Board name cannot be empty")
                }
                else{
                    lifecycleScope.launch {
                        uploadBoardImage()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}