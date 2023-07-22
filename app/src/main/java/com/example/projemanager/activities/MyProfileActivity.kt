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
import android.view.View.GONE
import android.view.View.OnClickListener
import android.view.View.VISIBLE
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.projemanager.R
import com.example.projemanager.databinding.ActivityMyProfileBinding
import com.example.projemanager.firebase.FirestoreClass
import com.example.projemanager.models.User
import com.example.projemanager.utils.Constants
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class MyProfileActivity : BaseActivity(), OnClickListener {
    private var binding : ActivityMyProfileBinding? = null
    private var mSelectedImageUri: Uri? = null
    private var mProfileImageURL: String? = ""
    private lateinit var mUserDetails: User
    private var anyChangesMade: Boolean = false
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
                        binding?.ivMyProfile?.let {
                            Glide
                                .with(this@MyProfileActivity)
                                .load(mSelectedImageUri)
                                .centerCrop()
                                .placeholder(R.drawable.ic_user_place_holder)
                                .into(it)
                        }
                        if(mSelectedImageUri != null){
                            uploadUserImage()
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
        binding = ActivityMyProfileBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        setUpActionBar()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    mOnBackPressed()
                }
            })

        lifecycleScope.launch {
            FirestoreClass().loadUserData(this@MyProfileActivity)
        }

        binding?.fabEditImage?.setOnClickListener(this)
        binding?.ibEditName?.setOnClickListener(this)
        binding?.ibOkName?.setOnClickListener(this)
        binding?.ibEditPhone?.setOnClickListener(this)
        binding?.ibOkPhone?.setOnClickListener(this)
        binding?.tvAddPhone?.setOnClickListener(this)
    }

    private fun mOnBackPressed(){
        if(anyChangesMade){
            setResult(RESULT_OK)
        }
        onBackPressedDispatcher.onBackPressed()
        finish()
    }

    private fun setUpActionBar() {
        setSupportActionBar(binding?.toolbarMyProfile)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_button_white)
        supportActionBar?.title = resources.getString(R.string.my_profile_title)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        binding?.toolbarMyProfile?.setNavigationOnClickListener {
            if(anyChangesMade){
                setResult(RESULT_OK)
            }
            onBackPressedDispatcher.onBackPressed()
            finish()
        }
    }

    fun updateUserProfile(user: User){
        mUserDetails = user
        showCustomProgressDialog()
            binding?.ivMyProfile?.let {
                Glide
                    .with(this@MyProfileActivity)
                    .load(user.image)
                    .centerCrop()
                    .placeholder(R.drawable.ic_user_place_holder)
                    .into(it)
            }
        binding?.tvMyProfileName?.text = user.name
        binding?.tvMyProfileEmail?.text = user.email
        if(user.mobile != 0L){
            binding?.llPhone?.visibility = VISIBLE
            binding?.ibEditPhone?.visibility = VISIBLE
            binding?.tvAddPhone?.visibility = GONE
            binding?.tvMyProfilePhone?.text = user.mobile.toString()
        }
        else{
            binding?.llPhone?.visibility = GONE
            binding?.ibEditPhone?.visibility = GONE
            binding?.tvAddPhone?.visibility = VISIBLE
        }
        cancelCustomProgressDialog()
    }

    override fun onClick(view: View?) {
        when(view!!.id){
            R.id.fab_edit_image->{
                selectFromGallery()
            }
            R.id.ib_edit_name->{
                binding?.ibEditName?.visibility = GONE
                binding?.llName?.visibility = GONE
                binding?.ibOkName?.visibility = VISIBLE
                binding?.tilChangeName?.visibility = VISIBLE
                binding?.etChangeName?.setText(binding?.tvMyProfileName?.text)
            }
            R.id.ib_ok_name->{
                if(binding?.etChangeName?.text?.isEmpty() == true){
                    showErrorSnackBar("Name can't be empty")
                }
                else{
                    binding?.tvMyProfileName?.text = binding?.etChangeName?.text
                    binding?.ibEditName?.visibility = VISIBLE
                    binding?.llName?.visibility = VISIBLE
                    binding?.ibOkName?.visibility = GONE
                    binding?.tilChangeName?.visibility = GONE
                    updateUserProfileData()
                }
            }
            R.id.ib_edit_phone->{
                binding?.ibEditPhone?.visibility = GONE
                binding?.llPhone?.visibility = GONE
                binding?.ibOkPhone?.visibility = VISIBLE
                binding?.tilChangePhone?.visibility = VISIBLE
                binding?.etChangePhone?.setText(binding?.tvMyProfilePhone?.text)
            }
            R.id.ib_ok_phone->{
                if(binding?.etChangePhone?.text?.isEmpty() == true){
                    showErrorSnackBar("Phone can't be empty")
                }
                else{
                    binding?.tvMyProfilePhone?.text = binding?.etChangePhone?.text
                    binding?.ibEditPhone?.visibility = VISIBLE
                    binding?.llPhone?.visibility = VISIBLE
                    binding?.ibOkPhone?.visibility = GONE
                    binding?.tilChangePhone?.visibility = GONE
                    updateUserProfileData()
                }
            }
            R.id.tv_add_phone->{
                binding?.tvAddPhone?.visibility = GONE
                binding?.ibEditPhone?.visibility = GONE
                binding?.llPhone?.visibility = GONE
                binding?.ibOkPhone?.visibility = VISIBLE
                binding?.tilChangePhone?.visibility = VISIBLE
                binding?.etChangePhone?.setText(binding?.tvMyProfilePhone?.text)
            }
        }
    }

    private fun selectFromGallery(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this@MyProfileActivity, galleryPermission)){
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

    private suspend fun uploadUserImage(){
        withContext(Dispatchers.IO){
            if(mSelectedImageUri != null){
                val sRef: StorageReference = FirebaseStorage
                    .getInstance()
                    .reference
                    .child(
                        "USER IMAGE"
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
                        mProfileImageURL = uri.toString()
                        updateUserProfileData()
                    }
                }.addOnFailureListener{
                        exception->
                    runOnUiThread {
                        Toast.makeText(this@MyProfileActivity, exception.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun getFileExtension(uri: Uri): String? {
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(contentResolver.getType(uri))
    }

    private fun updateUserProfileData(){
        anyChangesMade = true
        showCustomProgressDialog()
        val userHashMap = HashMap<String, Any>()

        if(mProfileImageURL!!.isNotEmpty() && mProfileImageURL != mUserDetails.image){
            userHashMap[Constants.IMAGE] = mProfileImageURL!!
        }
        if(binding?.tvMyProfileName?.text.toString() != mUserDetails.name){
            userHashMap[Constants.NAME] = binding?.tvMyProfileName?.text.toString()
        }
        if(binding?.tvAddPhone?.visibility != VISIBLE){
            if(binding?.tvMyProfilePhone?.text?.toString()?.toLong() != mUserDetails.mobile){
                userHashMap[Constants.MOBILE] = binding?.tvMyProfilePhone?.text?.toString()!!.toLong()
            }
        }
        lifecycleScope.launch {
            FirestoreClass().updateUserProfileData(this@MyProfileActivity, userHashMap)
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}