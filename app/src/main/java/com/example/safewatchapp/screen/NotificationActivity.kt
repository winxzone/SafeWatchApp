package com.example.safewatchapp.screen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.safewatchapp.screen.adapter.NotificationAdapter
import com.example.safewatchapp.databinding.ActivityNotificationBinding
import com.example.safewatchapp.databinding.DialogDeleteObjectBinding
import com.example.safewatchapp.models.Notification
import com.example.safewatchapp.retrofit.ApiClient
import com.example.safewatchapp.utils.NotificationDiffUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class NotificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationBinding
    private val notifications = mutableListOf<Notification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        setupSwipeToDelete()
        setupListeners()
        fetchNotifications()
    }

    private fun setupListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchNotifications()
        }

        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        val adapter = NotificationAdapter(notifications) { notification ->
            handleNotificationClick(notification)
        }
        binding.notificationRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NotificationActivity)
            this.adapter = adapter
        }
    }

    private fun handleNotificationClick(notification: Notification) {
        when (notification.type) {
            "device_verification" -> {
                val intent = Intent(this, DeviceVerificationActivity::class.java)
                startActivity(intent)
            }
            "other_notification" -> {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
            else -> {
                showToast("Unknown notification type")
            }
        }
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val removed = notifications.removeAt(position)

                    binding.notificationRecyclerView.adapter?.notifyItemRemoved(position)

                    showDeleteNotificationBottomSheet(removed, position)
                }
            }

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                return 0.1f // коф. перемещения
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.notificationRecyclerView)
    }

    private fun showDeleteNotificationBottomSheet(notification: Notification, position: Int) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bindingSheet = DialogDeleteObjectBinding.inflate(layoutInflater)

        bindingSheet.dialogTitleTextView.text = "Удалить уведомление?"
        bindingSheet.deviceNameTextView.text = notification.title

        bindingSheet.btnConfirmDelete.text = "Удалить"
        bindingSheet.btnConfirmDelete.setOnClickListener {
            bottomSheetDialog.dismiss()
            deleteNotification(notification.id.toString()) // Удаляем на сервере
        }

        bindingSheet.btnCancelDelete.setOnClickListener {
            bottomSheetDialog.dismiss()
            // Возвращаем уведомление обратно в список
            notifications.add(position, notification)
            binding.notificationRecyclerView.adapter?.notifyItemInserted(position)
        }

        bottomSheetDialog.setContentView(bindingSheet.root)
        bottomSheetDialog.show()
    }


    private fun fetchNotifications() {
        lifecycleScope.launch {
            try {
                binding.swipeRefreshLayout.isRefreshing = true
                val notificationsFromServer = ApiClient.notificationApiService.getNotifications()
                binding.swipeRefreshLayout.isRefreshing = false
                
                updateNotificationsList(notificationsFromServer)
                toggleEmptyState(notificationsFromServer.isEmpty())
            } catch (e: Exception) {
                binding.swipeRefreshLayout.isRefreshing = false
                showToast("Error: ${e.message}")
                toggleEmptyState(true)
            }
        }
    }

    private fun deleteNotification(notificationId: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.notificationApiService.deleteNotification(notificationId)
                if (response.isSuccessful) {
                    Log.d("NotificationActivity", "Notification $notificationId deleted successfully")
                } else {
                    showToast("Server Error: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    private fun updateNotificationsList(newNotifications: List<Notification>) {
        val diffUtil = NotificationDiffUtil(notifications, newNotifications)
        val diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(diffUtil)
        notifications.clear()
        notifications.addAll(newNotifications)
        diffResult.dispatchUpdatesTo(binding.notificationRecyclerView.adapter as NotificationAdapter)
    }

    private fun toggleEmptyState(isEmpty: Boolean) {
        binding.notificationRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.emptyAnimation.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.noNotificationsText.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}