package com.example.safewatchapp.screen

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.safewatchapp.databinding.ActivityPermissionsBinding
import android.provider.Settings


class PermissionsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPermissionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    private fun setupListeners() {
        binding.switchUsageStats.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                openSettings(Settings.ACTION_USAGE_ACCESS_SETTINGS, "Включите доступ к статистике в настройках")
            } else {
                showToast("Доступ к статистике отключён")
            }
        }

        binding.switchBatteryOptimization.setOnClickListener {
            openSettings(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, "Не удалось открыть настройки батареи")
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                openSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, "Включите доступ к уведомлениям в настройках")
            } else {
                showToast("Доступ к уведомлениям отключён")
            }
        }

        binding.btnConfirmPermissions.setOnClickListener {
            if (hasUsageStatsPermission()) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                showToast("Включите доступ к статистике приложений")
            }
        }
    }

    private fun updateSwitchStates() {
        binding.switchUsageStats.isChecked = hasUsageStatsPermission()
        binding.switchNotifications.isChecked = isNotificationServiceEnabled()
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

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}