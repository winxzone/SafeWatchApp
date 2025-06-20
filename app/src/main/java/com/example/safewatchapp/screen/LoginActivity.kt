package com.example.safewatchapp.screen

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.example.safewatchapp.utils.PermissionUtils.hasAllPermissions

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: LoginBinding
    private companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")

        // Проверка токена и Remind Me
        val token = TokenManager.getToken(applicationContext)
        val remindMe = TokenManager.isRemindMeEnabled(applicationContext)
        Log.d(TAG, "Token: $token, RemindMe: $remindMe")

        if (token != null && remindMe) {
            Log.d(TAG, "Authenticated user, checking device binding")
            checkDeviceBinding()
            return
        }

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.checkboxRemindMe.isChecked = TokenManager.isRemindMeEnabled(this)
    }

    private fun setupListeners() {
        with(binding) {
            buttonRegistation.setOnClickListener { navigateToRegistration() }
//            buttonForgotPassword.setOnClickListener { navigateToForgotPassword() }
            buttonNext.setOnClickListener {
                if (validateLogin()) loginUser()
            }
        }
    }

    private fun navigateToRegistration() {
        startActivity(Intent(this, RegistationActivity::class.java))
        finish()
    }

//    private fun navigateToForgotPassword() {
//        startActivity(Intent(this, ForgetPasswordActivity::class.java))
//        finish()
//    }

    private fun navigateToMainActivity(isChild: Boolean) {
        if (isChild && !hasAllPermissions(this)) {
            Log.d(TAG, "Not all permissions granted. Redirecting to PermissionsActivity.")
            val intent = Intent(this, PermissionsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        } else {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            Log.d(TAG, "Navigating to MainActivity (isChild=$isChild)")
            startActivity(intent)
        }
        finish()
    }

    private fun navigateToWaitingForConfirmationActivity(deviceId: String) {
        val intent = Intent(this, WaitingForConfirmationActivity::class.java).apply {
            putExtra("deviceId", deviceId)
        }
        Log.d(TAG, "Navigating to WaitingForConfirmationActivity with deviceId=$deviceId")
        startActivity(intent)
        finish()
    }

    private fun navigateByRole(isChild: Boolean, deviceId: String? = null, childProfileId: String? = null) {
        if (!isChild) {
            Log.d(TAG, "Navigating to MainActivity for parent")
            navigateToMainActivity(isChild = false)
            return
        }

        if (!childProfileId.isNullOrEmpty()) {
            Log.d(TAG, "Navigating to MainActivity for child with profileId=$childProfileId")
            navigateToMainActivity(isChild = true)
            return
        }

        if (deviceId.isNullOrEmpty()) {
            Log.e(TAG, "Missing deviceId for child device")
            Toast.makeText(this, "Ошибка: ID устройства отсутствует", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val devices = ApiClient.childDeviceApiService.listChildDevice()
                val device = devices.find { it.id == deviceId }
                Log.d(TAG, "Found device: id=${device?.id}, status=${device?.status}, childId=${device?.childId}")

                when (device?.status) {
                    "confirmed" -> {
                        if (!device.childId.isNullOrEmpty()) {
                            DeviceManager.saveChildProfileId(this@LoginActivity, device.childId)
                            Log.d(TAG, "Confirmed device with childId=${device.childId}, navigating to MainActivity")
                            navigateToMainActivity(isChild = true)
                        } else {
                            Log.w(TAG, "Confirmed device but no childId, navigating to WaitingForConfirmation")
                            navigateToWaitingForConfirmationActivity(deviceId)
                        }
                    }
                    else -> {
                        Log.d(TAG, "Unconfirmed device, navigating to WaitingForConfirmation")
                        navigateToWaitingForConfirmationActivity(deviceId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking device confirmation: ${e.message}")
                navigateToWaitingForConfirmationActivity(deviceId)
            }
        }
    }

    private fun checkDeviceBinding() {
        val isChild = RoleManager.isChild(this)
        Log.d(TAG, "Checking device binding, isChild=$isChild")

        if (!isChild) {
            navigateByRole(isChild = false)
            return
        }

        val savedChildProfileId = DeviceManager.getChildProfileId(this)
        Log.d(TAG, "Retrieved childProfileId: $savedChildProfileId")

        if (!savedChildProfileId.isNullOrEmpty()) {
            Log.d(TAG, "Found saved childProfileId, navigating to MainActivity")
            navigateByRole(isChild = true, childProfileId = savedChildProfileId)
            return
        }

        val deviceId = getDeviceUniqueId(this)
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        Log.d(TAG, "Device info: name=$deviceName, id=$deviceId")

        lifecycleScope.launch {
            try {
                val devices = ApiClient.childDeviceApiService.listChildDevice()
                Log.d(TAG, "Retrieved ${devices.size} devices")

                val currentDevice = devices.find { it.deviceId == deviceId }
                Log.d(TAG, "Current device: id=${currentDevice?.id}, status=${currentDevice?.status}, childId=${currentDevice?.childId}")

                if (currentDevice == null) {
                    Log.w(TAG, "Device not found, registering new device")
                    registerDevice(deviceName, deviceId)
                    return@launch
                }

                when (currentDevice.status) {
                    "confirmed" -> {
                        currentDevice.id?.let { DeviceManager.saveChildDeviceId(this@LoginActivity, it) }
                        currentDevice.childId?.let {
                            DeviceManager.saveChildProfileId(this@LoginActivity, it)
                            Log.d(TAG, "Confirmed device with childId=${it}, navigating to MainActivity")
                            navigateByRole(isChild = true, childProfileId = it)
                        } ?: run {
                            Log.w(TAG, "Confirmed device but no childId, navigating to WaitingForConfirmation")
                            navigateByRole(isChild = true, deviceId = currentDevice.id ?: deviceId)
                        }
                    }
                    "unconfirmed" -> {
                        currentDevice.id?.let { DeviceManager.saveChildDeviceId(this@LoginActivity, it) }
                        Log.d(TAG, "Unconfirmed device, navigating to WaitingForConfirmation")
                        navigateByRole(isChild = true, deviceId = currentDevice.id ?: deviceId)
                    }
                    else -> {
                        Log.w(TAG, "Unknown device status: ${currentDevice.status}")
                        navigateByRole(isChild = true, deviceId = currentDevice.id ?: deviceId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking devices: ${e.message}")
                registerDevice(deviceName, deviceId)
            }
        }
    }

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

        // TODO: Не удалось зарегистрировать устройство - вызывается после полного удаления приложения

        lifecycleScope.launch {
            try {
                val response = ApiClient.childDeviceApiService.registerChildDevice(newDevice)
                if (response.isSuccessful) {
                    response.body()?.id?.let { id ->
                        DeviceManager.saveChildDeviceId(this@LoginActivity, id)
                        Log.d(TAG, "Device registered with id=$id")
                        navigateByRole(isChild = true, deviceId = id)
                    } ?: run {
                        Log.e(TAG, "Device registration failed: no id in response")
                        Toast.makeText(this@LoginActivity, "Ошибка регистрации устройства", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e(TAG, "Device registration failed: ${response.code()}")
                    Toast.makeText(this@LoginActivity, "Не удалось зарегистрировать устройство", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error during device registration: ${e.message}")
                Toast.makeText(this@LoginActivity, "Ошибка сети при регистрации", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateLogin(): Boolean {
        var isValid = true
        with(binding) {
            edEmail.error = null
            edPassword.error = null

            val email = edEmail.text.toString().trim()
            if (email.isEmpty()) {
                edEmail.error = getString(R.string.error_empty)
                isValid = false
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edEmail.error = getString(R.string.error_invalid_email)
                isValid = false
            }

            val password = edPassword.text.toString().trim()
            if (password.isEmpty()) {
                edPassword.error = getString(R.string.error_empty)
                isValid = false
            }
        }
        return isValid
    }

    private fun loginUser() {
        val email = binding.edEmail.text.toString().trim()
        val password = binding.edPassword.text.toString().trim()
        val userLogin = UserLogin(email, password)

        lifecycleScope.launch {
            try {
                val response = ApiClient.authApiService.loginUser(userLogin)
                if (response.isSuccessful) {
                    response.body()?.token?.let { token ->
                        Log.d(TAG, "Login successful: token=$token")
                        TokenManager.saveToken(applicationContext, token)
                        TokenManager.saveRemindMe(applicationContext, binding.checkboxRemindMe.isChecked)
                        checkDeviceBinding()
                    } ?: run {
                        Log.e(TAG, "Login failed: token is null")
                        Toast.makeText(this@LoginActivity, "Не удалось войти: нет токена", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    when (response.code()) {
                        404 -> binding.edEmail.error = getString(R.string.error_user_not_found)
                        401 -> binding.edPassword.error = getString(R.string.error_invalid_password)
                        else -> {
                            Log.e(TAG, "Login failed: ${response.code()} - ${response.message()}")
                            Toast.makeText(this@LoginActivity, "Не удалось войти: ${response.message()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error during login: ${e.message}")
                Toast.makeText(this@LoginActivity, "Ошибка сети: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}