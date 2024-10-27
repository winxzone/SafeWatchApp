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
        setupUI()
        setupListeners()
    }

    // Метод для начальной настройки интерфейса
    private fun setupUI() {
        enableEdgeToEdge()
        bindingClass = ForgetPasswordBinding.inflate(layoutInflater)
        setContentView(bindingClass.root)
    }

    // Метод для установки слушателей кнопок
    private fun setupListeners() {
        bindingClass.backButton.setOnClickListener {
            navigateBackToLogin()
            finish()
        }
    }

    // Метод для возврата на LoginActivity
    private fun navigateBackToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
