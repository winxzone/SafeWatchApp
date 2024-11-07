package com.example.safewatchapp.screen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.safewatchapp.R
import com.example.safewatchapp.databinding.RegistationBinding
import com.example.safewatchapp.models.User
import com.example.safewatchapp.models.UserRegistration
import com.example.safewatchapp.service.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
        bindingClass.buttonRegister.setOnClickListener {
            if (validateRegistration()) {
                registerUser()
            }
        }

        bindingClass.backButton?.setOnClickListener {
            navigateToLogin()
        }
    }

    // Метод для перехода на LoginActivity
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun registerUser() {
        val name = bindingClass.edName.text.toString()
        val email = bindingClass.edEmail.text.toString()
        val password = bindingClass.edPassword.text.toString()
        val confirmPassword = bindingClass.edConfirmPassword.text.toString()

        // Создаём объект UserRegistration
        val userRegistration = UserRegistration(name, email, password, confirmPassword)

        // Теперь вызываем функцию для регистрации
        ApiClient.apiService.registerUser(userRegistration).enqueue(object : Callback<User> {
            override fun onResponse(call: Call<User>, response: Response<User>) {
                if (response.isSuccessful) {
                    // Обработка успешной регистрации
                    Log.d("Register", "User registered successfully: ${response.body()}")

                    // Перенаправление на экран входа после успешной регистрации
                    val intent = Intent(this@RegistationActivity, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Log.e("Register", "Registration failed: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<User>, t: Throwable) {
                Log.e("Register", "Network error: ${t.message}")
            }
        })
    }

    private fun validateRegistration(): Boolean {
        var isValid = true

        bindingClass.apply {
            edEmail.error = null
            edName.error = null
            edPassword.error = null
            edConfirmPassword.error = null

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
            val passwordRepeat = edConfirmPassword.text.toString().trim()
            if (passwordRepeat.isEmpty()) {
                edConfirmPassword.error = getString(R.string.error_empty)
                isValid = false
            } else if (password != passwordRepeat) {
                edConfirmPassword.error = getString(R.string.error_passwords_do_not_match)
                isValid = false
            }
        }
        return isValid
    }
}
