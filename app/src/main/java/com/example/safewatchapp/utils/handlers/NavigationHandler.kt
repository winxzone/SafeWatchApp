package com.example.safewatchapp.utils.handlers

import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.safewatchapp.R
import com.example.safewatchapp.databinding.MainBinding
import com.example.safewatchapp.screen.DeviceVerificationActivity
import com.example.safewatchapp.screen.LoginActivity
import com.example.safewatchapp.utils.TokenManager

class NavigationHandler(
    private val activity: AppCompatActivity,
    private val binding: MainBinding
) {
    fun setupNavigationMenu() {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_confirm_devices -> openDeviceConfirmationScreen()
                R.id.menu_settings -> openSettingsScreen()
                R.id.menu_help -> openHelpScreen()
                R.id.menu_logout -> logout()
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun openDeviceConfirmationScreen() {
        activity.startActivity(Intent(activity, DeviceVerificationActivity::class.java))
    }

    private fun openSettingsScreen() {
        Toast.makeText(activity, "Открытие настроек", Toast.LENGTH_SHORT).show()
    }

    private fun openHelpScreen() {
        Toast.makeText(activity, "Открытие помощи", Toast.LENGTH_SHORT).show()
    }

    private fun logout() {
        TokenManager.clearToken(activity)
        activity.startActivity(Intent(activity, LoginActivity::class.java))
        activity.finish()
    }
}
