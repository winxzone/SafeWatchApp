package com.example.safewatchapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.safewatchapp.R
import com.example.safewatchapp.models.ChildDevice
import com.example.safewatchapp.utils.TimeFormatter

class NewDevicesAdapter(private val devices: List<ChildDevice>, private val onDeviceAction: (ChildDevice, Boolean) -> Unit) :
    RecyclerView.Adapter<NewDevicesAdapter.DeviceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_new_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.bind(device)
    }

    override fun getItemCount(): Int = devices.size

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val deviceName: TextView = itemView.findViewById(R.id.deviceName)
        private val requestTime: TextView = itemView.findViewById(R.id.requestTime)
        private val confirmButton: Button = itemView.findViewById(R.id.confirmButton)
        private val rejectButton: Button = itemView.findViewById(R.id.rejectButton)

        fun bind(device: ChildDevice) {
            deviceName.text = device.name
            requestTime.text = TimeFormatter.formatDateTime(device.createdAt)

            confirmButton.setOnClickListener {
                onDeviceAction(device, true)
            }

            rejectButton.setOnClickListener {
                onDeviceAction(device, false)
            }
        }
    }
}
