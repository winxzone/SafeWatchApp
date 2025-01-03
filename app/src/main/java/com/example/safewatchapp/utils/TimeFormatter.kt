package com.example.safewatchapp.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormatter {
    private const val DATE_TIME_FORMAT = "dd.MM.yy HH:mm"

    fun formatDateTime(timestamp: Long): String {
        val dateFormatter = SimpleDateFormat(DATE_TIME_FORMAT, Locale.getDefault())
        return dateFormatter.format(Date(timestamp))
    }
}
