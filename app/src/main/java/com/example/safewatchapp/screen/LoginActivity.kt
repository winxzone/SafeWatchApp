package com.example.safewatchapp.screen

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import com.example.safewatchapp.R
import com.example.safewatchapp.databinding.LoginBinding
import com.example.safewatchapp.models.ChildDevice
import com.example.safewatchapp.models.TokenResponse
import com.example.safewatchapp.models.UserLogin
import com.example.safewatchapp.service.ApiClient
import com.example.safewatchapp.utils.Constants
import com.example.safewatchapp.utils.RoleManager
import com.example.safewatchapp.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID


class LoginActivity : AppCompatActivity() {
    private lateinit var binding: LoginBinding
    private lateinit var checkBoxRemindMe: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
        setupListeners()

        // Проверка, если пользователь уже вошел
        if (TokenManager.isRemindMeEnabled(this) && TokenManager.getToken(this) != null) {
            // Переходим на главный экран, если токен существует и Remind me включен
            navigateToMainScreen()
            return
        }

    }

    // Метод для начальной настройки интерфейса
    private fun setupUI() {
        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkBoxRemindMe = binding.checkboxRemindMe
        checkBoxRemindMe.isChecked = TokenManager.isRemindMeEnabled(this)
    }

    // Метод для установки слушателей кнопок
    private fun setupListeners() {
        binding.buttonRegistation.setOnClickListener {
            navigateToRegistration()
        }

        binding.buttonForgotPassword.setOnClickListener {
            navigateToForgotPassword()
        }

        binding.buttonNext.setOnClickListener {
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
        val currentRole = RoleManager.getRole(this)

        if (currentRole == Constants.CHILD) {
            // Переход для ребенка
//            val intent = Intent(this, PermissionsActivity::class.java)
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        } else {
            // Переход для родителя
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun checkDeviceBinding() {
        val token = TokenManager.getToken(this)

        if (token == null) {
            return
        }

        val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        val deviceId = getDeviceUniqueId()

        if (!RoleManager.isChild(this)) {
            navigateToMainScreen()
            return
        }

        ApiClient.apiService.listChildDevice("Bearer $token").enqueue(object : Callback<List<ChildDevice>> {
            override fun onResponse(call: Call<List<ChildDevice>>, response: Response<List<ChildDevice>>) {
                if (response.isSuccessful) {
                    val devices = response.body().orEmpty()
                    val currentDevice = devices.find { it.deviceId == deviceId && it.status != "cancelled"}

                    when {
                        currentDevice == null -> {
                            registerDevice(token, deviceName, deviceId)
                        }
                        currentDevice.status == "unconfirmed" -> {
                            registerDevice(token, deviceName, deviceId)
                        }
                        currentDevice.status == "confirmed" -> {
                            navigateToMainScreen()
                        }
                        else -> {
                            Log.d("DeviceCheck", "Unknown device status: ${currentDevice.status}")
                        }
                    }
                } else {
                    Log.e(
                        "DeviceCheck",
                        "Failed to fetch devices: ${response.code()} - ${response.message()} - ErrorBody: ${response.errorBody()?.string()}"
                    )
                    registerDevice(token, deviceName, deviceId)
                }
            }
            override fun onFailure(call: Call<List<ChildDevice>>, t: Throwable) {
                Log.e("DeviceCheck", "Network error while fetching devices: ${t.message}")
            }
        })
    }

    // Метод для регистрации устройства
    private fun registerDevice(token: String, deviceName: String, deviceId: String) {

        val newDevice = ChildDevice(
            name = deviceName,
            deviceId = deviceId,
            id = null,
            accountId = null,
            childId = null,
            status = "unconfirmed",
            createdAt = System.currentTimeMillis(),
            confirmedAt = null
        )

        ApiClient.apiService.registerChildDevice(newDevice, "Bearer $token").enqueue(object : Callback<ChildDevice> {
            override fun onResponse(call: Call<ChildDevice>, response: Response<ChildDevice>) {

                if (response.isSuccessful) {
                    Log.d("DeviceRegister", "Device registered successfully: $deviceName")
                    navigateToMainScreen()
                } else {
                    Log.e(
                        "DeviceRegister",
                        "Failed to register device: ${response.code()} - ${response.message()} - ErrorBody: ${response.errorBody()?.string()}"
                    )
                }
            }
            override fun onFailure(call: Call<ChildDevice>, t: Throwable) {
                Log.e("DeviceRegister", "Network error while registering device: ${t.message}")
            }
        })
    }

    // Получение уникального идентификатора устройства
    private fun getDeviceUniqueId(): String {
        val sharedPreferences = getSharedPreferences("DevicePrefs", MODE_PRIVATE)
        val uniqueIdKey = "UNIQUE_DEVICE_ID"

        var uniqueId = sharedPreferences.getString(uniqueIdKey, null)

        if (uniqueId == null) {
            uniqueId = try {
                val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                Log.d("DeviceUniqueId", "ANDROID_ID fetched: $androidId")
                androidId ?: UUID.randomUUID().toString()
            } catch (e: Exception) {
                Log.e("DeviceUniqueId", "Error fetching ANDROID_ID: ${e.message}")
                UUID.randomUUID().toString()
            }
            sharedPreferences.edit().putString(uniqueIdKey, uniqueId).apply()
            Log.d("DeviceUniqueId", "Unique ID saved to SharedPreferences: $uniqueId")
        }

        return uniqueId ?: UUID.randomUUID().toString()
    }


    // Валидация экрана логина
    private fun validateLogin(): Boolean{
        var isValid = true

        binding.apply{

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
        val email = binding.edEmail.text.toString()
        val password = binding.edPassword.text.toString()

        val userLogin = UserLogin(email, password)

        ApiClient.apiService.loginUser(userLogin).enqueue(object : Callback<TokenResponse> {
            override fun onResponse(call: Call<TokenResponse>, response: Response<TokenResponse>) {
                when {
                    response.isSuccessful -> {
                        // Успешный логин
                        val token = response.body()?.token
                        Log.d("Login", "Login successful: Token: $token")

                        // Сохраняем токен с помощью TokenManager
                        token?.let {
                            TokenManager.saveToken(applicationContext, it)
                            TokenManager.saveRemindMe(applicationContext, checkBoxRemindMe.isChecked)
                        }
                        checkDeviceBinding()
                    }
                    response.code() == 404 -> {
                        // Аккаунт не найден
                        binding.edEmail.error = getString(R.string.error_user_not_found)
                    }
                    response.code() == 401 -> {
                        // Неправильный пароль
                        binding.edPassword.error = getString(R.string.error_invalid_password)
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
}
