package com.example.projemanager.activities

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.projemanager.R
import com.example.projemanager.databinding.ActivitySignInBinding
import com.example.projemanager.firebase.FirestoreClass
import com.example.projemanager.models.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class SignInActivity : BaseActivity() {
    private var binding: ActivitySignInBinding? = null
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        setUpActionBar()
        auth = FirebaseAuth.getInstance()

        binding?.buttonSignIn?.setOnClickListener {
            signInUser()
        }
    }

    private fun setUpActionBar() {
        setSupportActionBar(binding?.tbSignIn)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_24)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding?.tbSignIn?.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            finish()
        }
    }

    private fun signInUser(){
        val email: String? = binding?.etEmailSignIn?.text?.toString()?.trim{it <= ' '}
        val password: String? = binding?.etPasswordSignIn?.text?.toString()?.trim{it <= ' '}
        if(validateForm(email, password)){
            showCustomProgressDialog()
            auth.signInWithEmailAndPassword(email!!, password!!)
                .addOnCompleteListener {
                        task ->
                    if(task.isSuccessful){
                        Toast.makeText(this,
                            "Successfully Signed In",
                            Toast.LENGTH_LONG).show()
                        lifecycleScope.launch {
                            FirestoreClass().loadUserData(this@SignInActivity)
                        }
                    }else{
                        Toast.makeText(this,
                            "Authentication failed",
                            Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun validateForm(email: String?, password: String?): Boolean{
        return when{
            TextUtils.isEmpty(email) -> {
                showErrorSnackBar("Please Enter an email")
                false
            }
            TextUtils.isEmpty(password) -> {
                showErrorSnackBar("Please Enter a password")
                false
            }
            else -> true
        }
    }

    fun signInSuccess(user: User){
        cancelCustomProgressDialog()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}