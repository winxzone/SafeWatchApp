package com.example.safewatchapp.screen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.safewatchapp.R
import com.example.safewatchapp.databinding.RegistationBinding
import com.example.safewatchapp.models.UserRegistration
import com.example.safewatchapp.retrofit.ApiClient
import kotlinx.coroutines.launch

class RegistationActivity : AppCompatActivity() {
    private lateinit var binding: RegistationBinding

    // todo: Регистрация нового пользователя с существующими данными проходит(проблема в клиенте)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
        setupListeners()
    }

    // Метод для начальной настройки интерфейса
    private fun setupUI() {
        binding = RegistationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }


    // Метод для установки слушателей кнопок
    private fun setupListeners() {
        binding.buttonRegister.setOnClickListener {
            if (validateRegistration()) {
                registerUser()
            }
        }

        binding.tVNavigateToLogin.setOnClickListener {
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
        val name = binding.edName.text.toString()
        val email = binding.edEmail.text.toString()
        val password = binding.edPassword.text.toString()
        val confirmPassword = binding.edConfirmPassword.text.toString()

        val userRegistration = UserRegistration(name, email, password, confirmPassword)

        lifecycleScope.launch {
            try {
                val response = ApiClient.authApiService.registerUser(userRegistration)

                if (response.isSuccessful) {
                    val user = response.body()
                    Log.d("Register", "User registered successfully: $user")

                    val intent = Intent(this@RegistationActivity, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Обробка помилки, наприклад 409
                    Log.e("Register", "Registration failed: ${response.code()} - ${response.errorBody()?.string()}")

                    when (response.code()) {
                        409 -> binding.edEmail.error = "Користувач з таким email вже існує"
                        else -> Toast.makeText(this@RegistationActivity, "Помилка реєстрації: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("Register", "Exception during registration: ${e.message}")
                Toast.makeText(this@RegistationActivity, "Помилка підключення до сервера", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun validateRegistration(): Boolean {
        var isValid = true

        binding.apply {
            edEmail.error = null
            edName.error = null
            edPassword.error = null
            edConfirmPassword.error = null

            // Проверка email
            val email = edEmail.text.toString().trim()
            if (email.isEmpty()) {
                edEmail.error = getString(R.string.error_empty)
                isValid = false
            } else if (!isValidEmail(email)) {
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

    private fun isValidEmail(email: String): Boolean {
        // Регулярное выражение, которое проверяет, что email имеет хотя бы один символ до и после "@" и хотя бы одну точку в доменной части
        val emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
        return email.matches(emailPattern.toRegex())
    }
}
