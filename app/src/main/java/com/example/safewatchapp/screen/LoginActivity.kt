package com.example.safewatchapp.screen

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.safewatchapp.R
import com.example.safewatchapp.databinding.LoginBinding
import com.example.safewatchapp.models.ChildDevice
import com.example.safewatchapp.models.UserLogin
import com.example.safewatchapp.retrofit.ApiClient
import com.example.safewatchapp.utils.RoleManager
import com.example.safewatchapp.utils.TokenManager
import kotlinx.coroutines.launch
import com.example.safewatchapp.manager.DeviceManager
import com.example.safewatchapp.utils.DeviceIdUtils.getDeviceUniqueId


class LoginActivity : AppCompatActivity() {
    private lateinit var binding: LoginBinding
    private lateinit var checkBoxRemindMe: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LoginActivity", "onCreate() called")
        setupUI()
        setupListeners()
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
        finish()
    }

    // Метод для перехода на экран восстановления пароля
    private fun navigateToForgotPassword() {
        val intent = Intent(this, ForgetPasswordActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToMainActivity(){
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToWaitingForConfirmationActivity(deviceId: String?){
        val intent = Intent(this@LoginActivity, WaitingForConfirmationActivity::class.java)
        intent.putExtra("deviceId", deviceId)
        startActivity(intent)
        finish()
    }

    // Новая функция для перехода на основе роли
    private fun navigateByRole(isChild: Boolean, deviceId: String? = null, childProfileId: String? = null) {
        if (!isChild) {
            Log.d("Navigation", "👤 Переход на главный экран для родителя")
            navigateToMainActivity()
            return
        }

        // Логика для детского устройства
        if (!childProfileId.isNullOrEmpty()) {
            Log.d("Navigation", "✅ Детское устройство с профилем, переход на главный экран")
             navigateToMainActivity() // ДОЛЖНО БЫТЬ АКТИВИТИ ДЛЯ РЕБЕНКА
            return
        }

        // Если профиль ребёнка отсутствует, проверяем статус устройства
        if (!deviceId.isNullOrEmpty()) {
            lifecycleScope.launch {
                try {
                    val devices = ApiClient.childDeviceApiService.listChildDevice()
                    val device = devices.find { it.id == deviceId }

                    if (device?.status == "confirmed" && device.childId != null) {
                        Log.d("Navigation", "✅ Устройство уже подтверждено, переходим на главный экран")
                        DeviceManager.saveChildProfileId(this@LoginActivity, device.childId)
                        navigateToMainActivity()
                    } else {
                        Log.d("Navigation", "⌛ Устройство не подтверждено, переходим на экран ожидания")
                        navigateToWaitingForConfirmationActivity(deviceId)
                    }
                } catch (e: Exception) {
                    Log.e("Navigation", "❌ Ошибка при проверке подтверждения устройства: ${e.message}")
                    navigateToWaitingForConfirmationActivity(deviceId)
                }
            }
        } else {
            Log.e("Navigation", "❌ Нет deviceId для детского устройства")
            runOnUiThread {
                Toast.makeText(this@LoginActivity, "Ошибка: ID устройства отсутствует", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkDeviceBinding() {
        // Проверка "Запомнить меня" и токена
        if (TokenManager.isRemindMeEnabled(this) && TokenManager.getToken(this) != null) {
            Log.d("DeviceCheck", "✅ RemindMe включён и токен существует")
            val isChild = RoleManager.isChild(this)
            if (!isChild) {
                // Для родителя сразу переходим на главный экран
                navigateByRole(isChild = false)
                return
            }
            // Для ребёнка проверяем childProfileId
            val savedChildProfileId = DeviceManager.getChildProfileId(this)
            if (!savedChildProfileId.isNullOrEmpty()) {
                // Если childProfileId есть, переходим на главный экран
                navigateByRole(isChild = true, childProfileId = savedChildProfileId)
                return
            }
            // Если childProfileId нет, продолжаем проверку устройства
            Log.d("DeviceCheck", "⚠️ RemindMe включён, но childProfileId отсутствует, продолжаем проверку")
        }

        val token = TokenManager.getToken(this)
        if (token == null) {
            Log.d("DeviceCheck", "❌ Token is null")
            return
        }

        val savedChildProfileId = DeviceManager.getChildProfileId(this)
        Log.d("DeviceCheck", "📦 Полученный childProfileId из SharedPrefs: $savedChildProfileId")
        if (!savedChildProfileId.isNullOrEmpty()) {
            Log.d("DeviceCheck", "✅ Child profile уже сохранен. Переход к главному экрану")
            navigateByRole(isChild = true, childProfileId = savedChildProfileId)
            return
        }

        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        val deviceId = getDeviceUniqueId(this)
        Log.d("DeviceCheck", "📱 Device Info — Name: $deviceName, ID: $deviceId")

        val isChild = RoleManager.isChild(this)
        Log.d("DeviceCheck", "🧒 Is child device: $isChild")

        if (!isChild) {
            Log.d("DeviceCheck", "👤 Не детское устройство. Переход на главный экран.")
            navigateByRole(isChild = false)
            return
        }

        lifecycleScope.launch {
            try {
                val devices = ApiClient.childDeviceApiService.listChildDevice()
                Log.d("DeviceCheck", "📥 Получено устройств: ${devices.size}")

                val currentDevice = devices.find { it.deviceId == deviceId }

                if (currentDevice == null) {
                    Log.w("DeviceCheck", "📭 Устройство не найдено в списке. Регистрация...")
                    registerDevice(deviceName, deviceId)
                    return@launch
                }

                Log.d("DeviceCheck", "🔍 Найдено устройство: id=${currentDevice.id}, status=${currentDevice.status}, childId=${currentDevice.childId}")

                when (currentDevice.status) {
                    "unconfirmed" -> {
                        DeviceManager.saveChildDeviceId(this@LoginActivity, currentDevice.id)
                        Log.d("DeviceCheck", "💾 Сохранён childDeviceId: ${currentDevice.id}")
                        navigateByRole(isChild = true, deviceId = currentDevice.id)
                    }

                    "confirmed" -> {
                        val childDeviceId = currentDevice.id
                        val childProfileId = currentDevice.childId

                        if (!childDeviceId.isNullOrEmpty()) {
                            DeviceManager.saveChildDeviceId(this@LoginActivity, childDeviceId)
                            Log.d("DeviceCheck", "💾 Сохранён childDeviceId: $childDeviceId")
                        }

                        if (!childProfileId.isNullOrEmpty()) {
                            DeviceManager.saveChildProfileId(this@LoginActivity, childProfileId)
                            Log.d("DeviceCheck", "💾 Сохранён childProfileId: $childProfileId")
                            navigateByRole(isChild = true, childProfileId = childProfileId)
                        } else {
                            Log.d("DeviceCheck", "⚠️ Устройство подтверждено, но профиль ребёнка отсутствует.")
                            navigateByRole(isChild = true, deviceId = childDeviceId ?: "")
                        }
                    }

                    else -> {
                        Log.w("DeviceCheck", "⚠️ Неизвестный статус: ${currentDevice.status}")
                    }
                }

            } catch (e: Exception) {
                Log.e("DeviceCheck", "❌ Ошибка при проверке устройств: ${e.message}")
                registerDevice(deviceName, deviceId)
            }
        }
    }

    // Метод для регистрации устройства
    private fun registerDevice(deviceName: String, deviceId: String) {
        val newDevice = ChildDevice(
            name = deviceName,
            deviceId = deviceId,
            id = null,
            userId = null,
            childId = null,
            status = "unconfirmed",
            createdAt = System.currentTimeMillis(),
            confirmedAt = null
        )

        lifecycleScope.launch {
            try {
                val response = ApiClient.childDeviceApiService.registerChildDevice(newDevice)
                if (response.isSuccessful) {
                    val savedDevice = response.body()
                    savedDevice?.id?.let { id ->
                        DeviceManager.saveChildDeviceId(this@LoginActivity, id)
                        Log.d("DeviceRegister", "✅ Устройство зарегистрировано и сохранено: $id")
                        navigateByRole(isChild = true, deviceId = id)
                    } ?: run {
                        Log.e("DeviceRegister", "❌ ID устройства не получен в ответе")
                        runOnUiThread {
                            Toast.makeText(this@LoginActivity, "Ошибка регистрации устройства", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.e("DeviceRegister", "❌ Ошибка регистрации: ${response.code()}")
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Не удалось зарегистрировать устройство", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("DeviceRegister", "❌ Ошибка при регистрации устройства: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Ошибка сети при регистрации", Toast.LENGTH_SHORT).show()
                }
            }
        }
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

        lifecycleScope.launch {
            try {
                val response = ApiClient.authApiService.loginUser(userLogin)
                if (response.isSuccessful) {
                    val token = response.body()?.token
                    Log.d("Login", "Login successful: Token: $token")

                    // Сохраняем токен с помощью TokenManager
                    token?.let {
                        TokenManager.saveToken(applicationContext, it)
                        TokenManager.saveRemindMe(applicationContext, binding.checkboxRemindMe.isChecked)

                        if (TokenManager.getToken(applicationContext) != null) {
                            checkDeviceBinding()
                        } else {
                            Log.e("Login", "Token was not properly saved")
                            Toast.makeText(this@LoginActivity, "Ошибка сохранения сессии", Toast.LENGTH_SHORT).show()
                        }
                    } ?: run {
                        Log.e("Login", "Token is null in response")
                        Toast.makeText(this@LoginActivity, "Не удалось войти: нет токена", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    when (response.code()) {
                        404 -> binding.edEmail.error = getString(R.string.error_user_not_found)
                        401 -> binding.edPassword.error = getString(R.string.error_invalid_password)
                        else -> {
                            Log.e("Login", "Login failed: ${response.code()} - ${response.message()}")
                            Toast.makeText(this@LoginActivity, "Login failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Login", "Network error: ${e.message}")
                Toast.makeText(this@LoginActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
