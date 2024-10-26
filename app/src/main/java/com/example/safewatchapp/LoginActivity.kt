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
        setupUI()
        setupListeners()
    }

    // Метод для начальной настройки интерфейса
    private fun setupUI() {
        enableEdgeToEdge()
        bindingClass = LoginBinding.inflate(layoutInflater)
        setContentView(bindingClass.root)
    }

    // Метод для установки слушателей кнопок
    private fun setupListeners() {
        bindingClass.buttonRegistation.setOnClickListener {
            navigateToRegistration()
        }

        bindingClass.buttonForgotPassword.setOnClickListener {
            navigateToForgotPassword()
        }
    }

    // Метод для перехода на экран регистрации
    private fun navigateToRegistration() {
        val intent = Intent(this, RegistationActivity::class.java)
        startActivity(intent)
    }

    // Метод для перехода на экран восстановления пароля
    private fun navigateToForgotPassword() {
        val intent = Intent(this, ForgetPasswordActivity::class.java)
        startActivity(intent)
    }
}
