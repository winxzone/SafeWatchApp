package com.example.safewatchapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.safewatchapp.databinding.RegistationBinding

class RegistationActivity : AppCompatActivity() {
    private lateinit var bindingClass: RegistationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
        setupListeners()
    }

    // Метод для начальной настройки интерфейса
    private fun setupUI() {
        enableEdgeToEdge()
        bindingClass = RegistationBinding.inflate(layoutInflater)
        setContentView(bindingClass.root)
    }

    // Метод для установки слушателей кнопок
    private fun setupListeners() {
        bindingClass.buttonLogin.setOnClickListener {
            navigateToLogin()
        }
    }

    // Метод для перехода на LoginActivity
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
