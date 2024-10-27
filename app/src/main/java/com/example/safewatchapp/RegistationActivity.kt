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
        bindingClass.buttonRegister.setOnClickListener{
            if (validateRegistration())
                navigateToLogin()
        }

        bindingClass.backButton?.setOnClickListener{
            navigateToLogin()
        }
    }

    // Метод для перехода на LoginActivity
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun validateRegistration():Boolean {
        var isValid = true

        bindingClass.apply {

            edEmail.error = null
            edName.error = null
            edPassword.error = null
            edPasswordRepeat.error = null

            // Проверка email
            val email = edEmail.text.toString().trim()
            if (email.isEmpty()) {
                edEmail.error = getString(R.string.error_empty)
                isValid = false
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edEmail.error = getString(R.string.error_invalid_email)
                isValid = false
            }

            // Проверка имени
            val name = edName.text.toString().trim()
            if (name.isEmpty()) {
                edName.error = getString(R.string.error_empty)
                isValid = false
            } else if (name.length < 2) {
                edName.error = getString(R.string.error_name_too_short)
                isValid = false
            }

            // Проверка пароля
            val password = edPassword.text.toString().trim()
            if (password.isEmpty()) {
                edPassword.error = getString(R.string.error_empty)
                isValid = false
            } else if (password.length < 6) {
                edPassword.error = getString(R.string.error_short_password)
                isValid = false
            }

            // Проверка подтверждения пароля
            val passwordRepeat = edPasswordRepeat.text.toString().trim()
            if (passwordRepeat.isEmpty()) {
                edPasswordRepeat.error = getString(R.string.error_empty)
                isValid = false
            } else if (password != passwordRepeat) {
                edPasswordRepeat.error = getString(R.string.error_passwords_do_not_match)
                isValid = false
            }

        }

        return isValid
    }
}
