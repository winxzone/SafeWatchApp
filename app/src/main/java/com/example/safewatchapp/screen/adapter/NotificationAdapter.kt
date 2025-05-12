package com.example.safewatchapp.screen.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.safewatchapp.databinding.ItemNotificationBinding
import com.example.safewatchapp.models.Notification
import com.example.safewatchapp.utils.TimeFormatter

class NotificationAdapter(
    private val notifications: List<Notification>,
    private val onNotificationClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    companion object {
        fun formatTimestamp(timestamp: Long): String {
            return TimeFormatter.formatDateTime(timestamp)
        }
    }

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Устанавливаем обработчик клика
            binding.root.setOnClickListener {
                onNotificationClick(notifications[adapterPosition])
            }
        }

        // TODO Добавить функционал изменение цвета уведомления и isRead внесения в базу данных
        fun bind(notification: Notification) {
            binding.notificationTitle.text = notification.title
            binding.notificationDescription.text = notification.message
            binding.notificationDate.text = formatTimestamp(notification.timestamp)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int = notifications.size
}

