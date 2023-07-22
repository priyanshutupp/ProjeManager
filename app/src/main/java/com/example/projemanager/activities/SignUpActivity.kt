package com.example.projemanager.activities

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.example.projemanager.R
import com.example.projemanager.databinding.ActivitySignUpBinding
import com.example.projemanager.firebase.FirestoreClass
import com.example.projemanager.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SignUpActivity : BaseActivity() {
    private var binding: ActivitySignUpBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        setUpActionBar()

        binding?.buttonSignUp?.setOnClickListener {
            registerUser()
        }
    }

    private fun setUpActionBar() {
        setSupportActionBar(binding?.tbSignUp)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back_24)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding?.tbSignUp?.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            finish()
        }
    }

    fun userRegisteredSuccess(){
        Toast.makeText(this,
            "You have successfully registered",
            Toast.LENGTH_LONG).show()
        cancelCustomProgressDialog()
        FirebaseAuth.getInstance().signOut()
        finish()
    }

    private fun registerUser(){
        val name: String? = binding?.etNameSignUp?.text?.toString()?.trim{it <= ' '}
        val email: String? = binding?.etEmailSignUp?.text?.toString()?.trim{it <= ' '}
        val password: String? = binding?.etPasswordSignUp?.text?.toString()?.trim{it <= ' '}
        if(validateForm(name, email, password)){
            showCustomProgressDialog()
            FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email!!, password!!)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser: FirebaseUser = task.result!!.user!!
                        val registeredEmail = firebaseUser.email
                        val user = User(firebaseUser.uid, name!!, registeredEmail!!)
                        FirestoreClass().registerUser(this@SignUpActivity, user)
                    } else {
                        Toast.makeText(
                            this,
                            "Registration failed",
                            Toast.LENGTH_SHORT
                        ).show()
                        cancelCustomProgressDialog()
                    }
                }
        }
    }

    private fun validateForm(name: String?, email: String?, password: String?): Boolean{
        return when{
            TextUtils.isEmpty(name) -> {
                showErrorSnackBar("Please Enter a name")
                false
            }
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

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}