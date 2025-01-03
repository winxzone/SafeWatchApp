package com.example.safewatchapp.screen

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.safewatchapp.adapters.NotificationAdapter
import com.example.safewatchapp.databinding.ActivityNotificationBinding
import com.example.safewatchapp.models.Notification
import com.example.safewatchapp.service.ApiClient
import com.example.safewatchapp.utils.NotificationDiffUtil
import com.example.safewatchapp.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationActivity : AppCompatActivity() {

    private lateinit var bindingClass: ActivityNotificationBinding
    private lateinit var adapter: NotificationAdapter

    private val notifications = mutableListOf<Notification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingClass = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(bindingClass.root)

        setupRecyclerView()
        setupSwipeToDelete()
        fetchNotifications()
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(notifications)
        bindingClass.notificationRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NotificationActivity)
            adapter = this@NotificationActivity.adapter
        }
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val notification = notifications[position]
                deleteNotification(notification.id, position)
            }
        })
        itemTouchHelper.attachToRecyclerView(bindingClass.notificationRecyclerView)
    }

    private fun fetchNotifications() {
        val token = getTokenOrShowError() ?: return

        ApiClient.apiService.getNotifications("Bearer $token")
            .enqueue(object : Callback<List<Notification>> {
                override fun onResponse(call: Call<List<Notification>>, response: Response<List<Notification>>) {
                    if (response.isSuccessful) {
                        val notificationsFromServer = response.body().orEmpty()

                        updateNotificationsList(notificationsFromServer)
                    } else {
                        showToast("Failed to load notifications")
                    }
                }

                override fun onFailure(call: Call<List<Notification>>, t: Throwable) {
                    showToast("Error: ${t.message}")
                }
            })
    }

    private fun deleteNotification(notificationId: String, position: Int) {
        val token = getTokenOrShowError() ?: return

        ApiClient.apiService.deleteNotification("Bearer $token", notificationId)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        notifications.removeAt(position)
                        adapter.notifyItemRemoved(position)
                        showToast("Notification deleted")
                    } else {
                        adapter.notifyItemChanged(position)
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    showToast("Error: ${t.message}")
                    adapter.notifyItemChanged(position)
                }
            })
    }

    private fun getTokenOrShowError(): String? {
        val token = TokenManager.getToken(this)
        if (token.isNullOrEmpty()) {
            showToast("No token found. Please log in.")
            return null
        }
        return token
    }

    private fun updateNotificationsList(newNotifications: List<Notification>) {
        val diffUtil = NotificationDiffUtil(notifications, newNotifications)
        val diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(diffUtil)
        notifications.clear()
        notifications.addAll(newNotifications)
        diffResult.dispatchUpdatesTo(adapter)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}