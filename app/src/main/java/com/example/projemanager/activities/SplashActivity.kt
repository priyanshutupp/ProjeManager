package com.example.projemanager.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.example.projemanager.databinding.ActivitySplashBinding
import com.example.projemanager.firebase.FirestoreClass

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private var binding: ActivitySplashBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        val typeface: Typeface = Typeface.createFromAsset(assets, "neon-sans.regular.ttf")
        binding?.splashText?.typeface = typeface

        Handler(Looper.getMainLooper())
            .postDelayed(
                {
                    val currentUserId = FirestoreClass().getCurrentUserId()
                    if(currentUserId.isEmpty()){
                        startActivity(Intent(this, IntroActivity::class.java))
                    }
                    else{
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                    finish()
                }, 2500)
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}