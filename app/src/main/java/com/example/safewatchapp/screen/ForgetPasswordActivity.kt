package com.example.safewatchapp.screen

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.safewatchapp.databinding.ForgetPasswordBinding

class ForgetPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ForgetPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
        setupListeners()
    }

    // Метод для начальной настройки интерфейса
    private fun setupUI() {

        binding = ForgetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    // Метод для установки слушателей кнопок
    private fun setupListeners() {
        binding.backButton.setOnClickListener {
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
