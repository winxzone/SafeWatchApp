package com.example.safewatchapp.screen

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.safewatchapp.R
import com.example.safewatchapp.databinding.DeviceVerificationBinding
import com.example.safewatchapp.screen.fragments.AddedDevicesFragment
import com.example.safewatchapp.screen.fragments.NewDevicesFragment
import com.google.android.material.tabs.TabLayoutMediator

class DeviceVerificationActivity : AppCompatActivity() {

    private lateinit var binding: DeviceVerificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DeviceVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupListeners()

        val deviceId = intent.getStringExtra("deviceId")
        if (deviceId != null) {
            binding.viewPager.currentItem = 0
        }
    }

    private fun setupViewPager() {
        val adapter = DevicePagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.new_devices)
                1 -> getString(R.string.added_devices)
                else -> null
            }
        }.attach()
    }

    private fun setupListeners() {
        binding.backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private inner class DevicePagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> {
                    val fragment = NewDevicesFragment()
                    intent.getStringExtra("deviceId")?.let { deviceId ->
                        fragment.arguments = Bundle().apply { putString("deviceId", deviceId) }
                    }
                    fragment
                }
                1 -> AddedDevicesFragment()
                else -> throw IllegalStateException("Unexpected position $position")
            }
        }
    }
}