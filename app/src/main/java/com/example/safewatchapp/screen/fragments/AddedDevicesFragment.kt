package com.example.safewatchapp.screen.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.safewatchapp.R
import com.example.safewatchapp.databinding.FragmentAddedDevicesBinding
import com.example.safewatchapp.models.ChildDevice
import com.example.safewatchapp.service.ApiClient
import com.example.safewatchapp.utils.TimeFormatter
import com.example.safewatchapp.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddedDevicesFragment : Fragment() {

    private var _binding: FragmentAddedDevicesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddedDevicesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.addedDevicesRecyclerView.layoutManager = LinearLayoutManager(context)
        listChildDevices()
    }

    private fun listChildDevices() {
        val token = TokenManager.getToken(requireContext()) ?: run {
            showToast("Authentication error: Token is missing")
            return
        }

        ApiClient.childDeviceApiService.listChildDevice("Bearer $token").enqueue(object : Callback<List<ChildDevice>> {
            override fun onResponse(call: Call<List<ChildDevice>>, response: Response<List<ChildDevice>>) {
                if (response.isSuccessful) {
                    val devices = response.body()?.filter { it.status == "confirmed" } ?: emptyList()
                    updateRecyclerView(devices)
                } else {
                    showToast("Failed to load devices: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<List<ChildDevice>>, t: Throwable) {
                showToast("Network error: ${t.message}")
            }
        })
    }

    private fun updateRecyclerView(devices: List<ChildDevice>) {
        val adapter = object : RecyclerView.Adapter<DeviceViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_added_device, parent, false)
                return DeviceViewHolder(view)
            }

            override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
                holder.bind(devices[position])
            }

            override fun getItemCount(): Int = devices.size
        }
        binding.addedDevicesRecyclerView.adapter = adapter

        if (devices.isEmpty()) {
            binding.emptyStateCard.visibility = View.VISIBLE
            binding.addedDevicesRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateCard.visibility = View.GONE
            binding.addedDevicesRecyclerView.visibility = View.VISIBLE
        }
    }

    private inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val deviceName: TextView = itemView.findViewById(R.id.deviceName)
        private val confirmedTime: TextView = itemView.findViewById(R.id.confirmedTime)

        fun bind(device: ChildDevice) {
            deviceName.text = device.name
            confirmedTime.text = TimeFormatter.formatDateTime(device.createdAt ?: 0L)

            itemView.setOnClickListener {
                showToast("Clicked on ${device.name}")
                // Добавить возможность отвязывать устройство ребенка
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}