package com.example.safewatchapp.utils.handlers

import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.safewatchapp.R
import com.example.safewatchapp.databinding.MainBinding
import com.example.safewatchapp.screen.DeviceVerificationActivity
import com.example.safewatchapp.manager.ChildManager
import com.example.safewatchapp.manager.DeviceManager
import com.example.safewatchapp.screen.ChooseUserActivity
import com.example.safewatchapp.utils.RoleManager
import com.example.safewatchapp.utils.TokenManager
import java.lang.ref.WeakReference


class NavigationHandler(
    activity: AppCompatActivity,
    private val binding: MainBinding
) {
    private val activityRef = WeakReference(activity)
    
    fun setupNavigationMenu() {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_confirm_devices -> openDeviceConfirmationScreen()
                R.id.menu_settings -> openSettingsScreen()
                R.id.menu_logout -> logout()
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun openDeviceConfirmationScreen() {
        activityRef.get()?.let { activity ->
            activity.startActivity(Intent(activity, DeviceVerificationActivity::class.java))
        }
    }

    private fun openSettingsScreen() {
        activityRef.get()?.let { activity ->
            Toast.makeText(activity, "Открытие настроек", Toast.LENGTH_SHORT).show()
        }
    }

    private fun logout() {
        activityRef.get()?.let { activity ->
            TokenManager.clearToken(activity)
            TokenManager.clearRemindMe(activity)

            DeviceManager.apply {
                clearChildDeviceId(activity)
                clearChildProfileId(activity)
            }

            ChildManager(activity).clearAllCache()
            RoleManager.clearRole(activity)

            activity.startActivity(Intent(activity, ChooseUserActivity::class.java))
            activity.finish()
        }
    }
}
