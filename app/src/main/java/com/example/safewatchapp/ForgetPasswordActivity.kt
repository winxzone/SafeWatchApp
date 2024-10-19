package com.example.safewatchapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.safewatchapp.databinding.ForgetPasswordBinding

class ForgetPasswordActivity : AppCompatActivity() {
    private lateinit var bindingClass: ForgetPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        bindingClass = ForgetPasswordBinding.inflate(layoutInflater)
        setContentView(bindingClass.root)

        bindingClass.backButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }


    }
}