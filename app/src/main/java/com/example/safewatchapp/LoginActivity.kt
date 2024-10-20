package com.example.safewatchapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.safewatchapp.databinding.LoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var bindingClass: LoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        bindingClass = LoginBinding.inflate(layoutInflater)
        setContentView(bindingClass.root)

        bindingClass.buttonRegistation.setOnClickListener {
            val intent = Intent(this, RegistationActivity::class.java)
            startActivity(intent)
        }

        bindingClass.buttonForgotPassword.setOnClickListener{
            val intent = Intent(this, ForgetPasswordActivity::class.java)
            startActivity(intent)
        }

    }
}