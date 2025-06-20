package com.example.safewatchapp.screen

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.safewatchapp.databinding.ActivityPermissionsBinding
import android.provider.Settings
import androidx.core.net.toUri

class PermissionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPermissionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (areAllPermissionsGranted()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding = ActivityPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupListeners()
        updateSwitchStates()
    }

    override fun onResume() {
        super.onResume()
        updateSwitchStates()
        logPermissionsState()
    }

    private fun logPermissionsState() {
        Log.d("PermissionsActivity", "UsageStats: ${hasUsageStatsPermission()}")
        Log.d("PermissionsActivity", "Notifications: ${isNotificationServiceEnabled()}")
        Log.d("PermissionsActivity", "BatteryOptimizationsIgnored: ${isBatteryOptimizationIgnored()}")
    }


    private fun setupListeners() {
        binding.switchUsageStats.setOnClickListener {
            if (!hasUsageStatsPermission()) {
                openSettings(Settings.ACTION_USAGE_ACCESS_SETTINGS, "Включите доступ к статистике в настройках")
            } else {
                showToast("Доступ к статистике уже предоставлен")
            }
        }

        binding.switchBatteryOptimization.setOnClickListener {
            if (!isBatteryOptimizationIgnored()) {
                openBatteryOptimizationSettings()
            } else {
                showToast("Оптимизация уже отключена")
            }
        }

        binding.switchNotifications.setOnClickListener {
            if (!isNotificationServiceEnabled()) {
                openSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, "Включите доступ к уведомлениям в настройках")
            } else {
                showToast("Доступ к уведомлениям уже предоставлен")
            }
        }

        binding.switchOpenAutostartSettings.setOnClickListener {
            try {
                val intent = Intent().apply {
                    component = ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Автозапуск можно включить в настройках телефона вручную", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:$packageName".toUri()
                })
            }
        }


        binding.btnConfirmPermissions.setOnClickListener {
            if (areAllPermissionsGranted()) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                showToast("Пожалуйста, предоставьте все разрешения")
            }
        }
    }

    private fun updateSwitchStates() {
        binding.switchUsageStats.isChecked = hasUsageStatsPermission()
        binding.switchNotifications.isChecked = isNotificationServiceEnabled()
        binding.switchBatteryOptimization.isChecked = isBatteryOptimizationIgnored()
    }

    @SuppressLint("InlinedApi")
    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        return try {
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    packageName
                )
            } else {
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Log.e("PermissionsActivity", "Ошибка при проверке UsageStats", e)
            false
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(packageName) == true
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(packageName)
        } else {
            true
        }
    }

    private fun openBatteryOptimizationSettings() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = "package:$packageName".toUri()
            startActivity(intent)
        } catch (e: Exception) {
            showToast("Не удалось открыть настройки батареи")
            Log.e("PermissionsActivity", "Ошибка открытия настроек батареи", e)
        }
    }

    private fun openSettings(action: String, errorMessage: String) {
        try {
            startActivity(Intent(action).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (e: Exception) {
            showToast(errorMessage)
            Log.e("PermissionsActivity", "Ошибка открытия настроек $action", e)
        }
    }

    private fun areAllPermissionsGranted(): Boolean {
        return hasUsageStatsPermission()
                && isNotificationServiceEnabled()
                && isBatteryOptimizationIgnored()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}