package com.example.safewatchapp.screen

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.safewatchapp.databinding.ActivityWaitingConfirmationBinding
import com.example.safewatchapp.manager.DeviceManager
import com.example.safewatchapp.models.ChildDevice
import com.example.safewatchapp.retrofit.ApiClient
import com.example.safewatchapp.utils.DeviceIdUtils.getDeviceUniqueId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WaitingForConfirmationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWaitingConfirmationBinding
    private var pollingJobActive = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWaitingConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        startPollingDeviceStatus()

        binding.btnRetryRequest.setOnClickListener {
            Log.d("WaitingActivity", "Повторный запрос отправлен")
            registerDeviceAgain()
        }
    }
    
    private fun startPollingDeviceStatus() {
        lifecycleScope.launch {
            while (pollingJobActive) {
                Log.d("WaitingActivity", "Polling запущен")
                delay(5000)

                val childDeviceId = DeviceManager.getChildDeviceId(this@WaitingForConfirmationActivity)

                if (childDeviceId == null) {
                    Log.e("WaitingActivity", "❌ Нет сохраненного childDeviceId в SharedPreferences")
                    break
                }

                Log.d("WaitingActivity", "👉 Сохранённый childDeviceId: $childDeviceId")

                try {
                    val devices = ApiClient.childDeviceApiService.listChildDevice()
                    Log.d("WaitingActivity", "📥 Получено устройств: ${devices.size}")
                    devices.forEachIndexed { index, device ->
                        Log.d("WaitingActivity", "📄[$index] device.id=${device.id}, status=${device.status}, deviceId=${device.deviceId}, childId=${device.childId}")
                    }

                    val currentDevice = devices.find { it.id == childDeviceId }

                    if (currentDevice == null) {
                        Log.w("WaitingActivity", "⚠️ Устройство с ID $childDeviceId НЕ найдено в списке устройств")
                    } else {
                        Log.d("WaitingActivity", "✅ Найдено устройство: id=${currentDevice.id}, status=${currentDevice.status}, childId=${currentDevice.childId}")

                        if (currentDevice.status == "confirmed" && currentDevice.childId != null) {
                            Log.d("WaitingActivity", "🎉 Устройство подтверждено. Переход к разрешениям")
                            DeviceManager.saveChildProfileId(this@WaitingForConfirmationActivity, currentDevice.childId)
                            pollingJobActive = false
                            goToPermissionsScreen()
                            break
                        }
                    }

                } catch (e: Exception) {
                    Log.e("WaitingActivity", "❌ Ошибка при проверке подтверждения: ${e.message}")
                }
            }
        }
    }



    private fun goToPermissionsScreen() {
        startActivity(Intent(this, PermissionsActivity::class.java))
        finish()
    }

    private fun registerDeviceAgain() {
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        val deviceId = getDeviceUniqueId(this)

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
                    val newId = response.body()?.id
                    if (newId != null) {
                        Log.d("WaitingActivity", "Повторная регистрация успешна: $newId")
                        DeviceManager.saveChildDeviceId(this@WaitingForConfirmationActivity, newId)
                    } else {
                        Log.e("WaitingActivity", "Не удалось получить ID нового устройства")
                    }
                } else {
                    Log.e("WaitingActivity", "Ошибка регистрации: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("WaitingActivity", "Ошибка повторной регистрации: ${e.message}")
            }
        }
    }
}
