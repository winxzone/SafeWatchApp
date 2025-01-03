package com.example.safewatchapp.utils

import androidx.recyclerview.widget.DiffUtil
import com.example.safewatchapp.models.Notification

class NotificationDiffUtil(
    private val oldList: List<Notification>,
    private val newList: List<Notification>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}