package com.example.safewatchapp.adapters

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.safewatchapp.models.ChildDevice
import java.util.Locale
import java.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.safewatchapp.R
import java.util.Date


class AddedDevicesAdapter(private val devices: List<ChildDevice>, private val onDeviceAction: (ChildDevice) -> Unit) :
    RecyclerView.Adapter<AddedDevicesAdapter.DeviceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_added_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.bind(device)
    }

    override fun getItemCount(): Int = devices.size

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val deviceName: TextView = itemView.findViewById(R.id.deviceName)
        private val confirmedTime: TextView = itemView.findViewById(R.id.confirmedTime)

        fun bind(device: ChildDevice) {
            deviceName.text = device.name
            confirmedTime.text = "Дата: ${formatTime(device.confirmedAt)}" // Отображаем дату подтверждения

            itemView.setOnClickListener {
                onDeviceAction(device) // Например, открыть подробности устройства
            }
        }

        private fun formatTime(timeInMillis: Long?): String {
            val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return if (timeInMillis != null) {
                dateFormat.format(Date(timeInMillis))
            } else {
                "Unknown"
            }
        }
    }
}
