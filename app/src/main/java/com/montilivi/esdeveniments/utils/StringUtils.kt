package com.montilivi.esdeveniments.utils

import android.content.Context
import com.montilivi.esdeveniments.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object StringUtils{
    fun normalizeCategoryId(name: String): String {
        return name.lowercase()
            .replace("[àáâä]".toRegex(), "a")
            .replace("[èéêë]".toRegex(), "e")
            .replace("[ìíîï]".toRegex(), "i")
            .replace("[òóôö]".toRegex(), "o")
            .replace("[ùúûü]".toRegex(), "u")
            .replace("[ç]".toRegex(), "c")
            .replace("[^a-z0-9]".toRegex(), "")
    }

    fun formatEventDate(context: Context, start: Date, end: Date): String {
        val calendarStart = Calendar.getInstance().apply { time = start }
        val calendarEnd = Calendar.getInstance().apply { time = end }

        val dayStart = calendarStart.get(Calendar.DAY_OF_MONTH)
        val dayEnd = calendarEnd.get(Calendar.DAY_OF_MONTH)
        val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(start)
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(start)

        return when {
            calendarStart.get(Calendar.YEAR) != calendarEnd.get(Calendar.YEAR) -> {
                val fullStart = SimpleDateFormat("d 'de' MMMM yyyy", Locale.getDefault()).format(start)
                val fullEnd = SimpleDateFormat("d 'de' MMMM yyyy", Locale.getDefault()).format(end)
                context.getString(R.string.event_range_different_years, fullStart, fullEnd, time)
            }
            dayStart == dayEnd -> {
                context.getString(R.string.event_one_day, dayStart, monthName, time)
            }
            dayEnd - dayStart == 1 -> {
                context.getString(R.string.event_two_consecutive_days, dayStart, dayEnd, monthName, time)
            }
            else -> {
                context.getString(R.string.event_range_same_month, dayStart, dayEnd, monthName, time)
            }
        }
    }

    fun formatEventLocation(locationName: String?): String {
        if (locationName.isNullOrBlank()) return ""

        val parts = locationName.split(",").map { it.trim() }

        val name = parts.getOrNull(0) ?: return locationName

        // Buscar bloque con CP
        val cpBlock = parts.firstOrNull { it.contains(Regex("\\d{5}")) }

        return if (cpBlock != null && cpBlock != name) {
            "$name, $cpBlock"
        } else {
            name
        }
    }
}
