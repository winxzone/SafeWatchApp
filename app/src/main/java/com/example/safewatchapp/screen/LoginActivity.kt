package com.example.safewatchapp.screen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.safewatchapp.R
import com.example.safewatchapp.databinding.LoginBinding
import com.example.safewatchapp.models.TokenResponse
import com.example.safewatchapp.models.UserLogin
import com.example.safewatchapp.service.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


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

        bindingClass.buttonNext.setOnClickListener {
            if(validateLogin()){
                loginUser()
            }
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

    // Метод для перехода на главный экран
    private fun navigateToMainScreen() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Очищаем стек активностей
        startActivity(intent)
        finish()
    }

    // Валидация экрана логина
    private fun validateLogin(): Boolean{
        var isValid = true

        bindingClass.apply{

            edEmail.error = null
            edPassword.error = null

            // Проверка email
            val email = edEmail.text.toString().trim()
            if (email.isEmpty()) {
                edEmail.error = getString(R.string.error_empty)
                isValid = false
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edEmail.error = getString(R.string.error_invalid_email)
                isValid = false
            }

            // Проверка пароля
            val password = edPassword.text.toString().trim()
            if (password.isEmpty()) {
                edPassword.error = getString(R.string.error_empty)
                isValid = false
            }
        }

        return isValid
    }

    private fun loginUser() {
        val email = bindingClass.edEmail.text.toString()
        val password = bindingClass.edPassword.text.toString()

        // Создаём объект UserLogin
        val userLogin = UserLogin(email, password)

        ApiClient.apiService.loginUser(userLogin).enqueue(object : Callback<TokenResponse> {
            override fun onResponse(call: Call<TokenResponse>, response: Response<TokenResponse>) {
                when {
                    response.isSuccessful -> {
                        // Успешный логин
                        val token = response.body()?.token
                        Log.d("Login", "Login successful: Token: $token")

                        // Сохраняем токен
                        saveToken(token)

                        // Переход на главный экран
                        navigateToMainScreen()
                    }
                    response.code() == 404 -> {
                        // Аккаунт не найден
                        bindingClass.edEmail.error = getString(R.string.error_user_not_found)
                    }
                    response.code() == 401 -> {
                        // Неправильный пароль
                        bindingClass.edPassword.error = getString(R.string.error_invalid_password)
                    }
                    else -> {
                        // Другая ошибка
                        Log.e("Login", "Login failed: ${response.code()} - ${response.message()}")
                    }
                }
            }

            override fun onFailure(call: Call<TokenResponse>, t: Throwable) {
                Log.e("Login", "Network error: ${t.message}")
            }
        })
    }



    // Метод для сохранения токена в SharedPreferences
    private fun saveToken(token: String?) {
        val sharedPreferences = getSharedPreferences("YourAppPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("jwt_token", token)
        editor.apply()
    }

}
