package com.example.safewatchapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.safewatchapp.databinding.LoginActivityBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var bindingClass: LoginActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bindingClass = LoginActivityBinding.inflate(layoutInflater)
        setContentView(bindingClass.root)

        // Добавить активити для регистрации и восстановления пароля

//        val textViewForgotPassword = findViewById<TextView>(R.id.textViewForgotPassword)
//        textViewForgotPassword.setOnClickListener {
//            val intent = Intent(this, PasswordResetActivity::class.java)
//            startActivity(intent)
//        }

    }
}