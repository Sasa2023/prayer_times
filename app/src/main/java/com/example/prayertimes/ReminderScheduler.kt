package com.example.prayertimes

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.DateComponents
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.Coordinates
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    fun scheduleAll(context: Context, coordinates: Coordinates) {
        val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val params = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
        val dateComponents = DateComponents.from(Calendar.getInstance())
        val prayerTimes = PrayerTimes(coordinates, dateComponents, params)

        val prayers = listOf(
            "Fajr" to prayerTimes.fajr,
            "Dhuhr" to prayerTimes.dhuhr,
            "Asr" to prayerTimes.asr,
            "Maghrib" to prayerTimes.maghrib,
            "Isha" to prayerTimes.isha
        )

        for ((name, time) in prayers) {
            if (prefs.getBoolean("reminder_$name", true)) {
                val offsetMinutes = prefs.getInt("offset_$name", 0)
                val adjustedTime = Date(time.time - offsetMinutes * 60_000)
                val delay = adjustedTime.time - System.currentTimeMillis()
                if (delay > 0) {
                    val data = workDataOf("prayer_name" to name)
                    val request = OneTimeWorkRequestBuilder<PrayerReminderWorker>()
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(data)
                        .addTag("reminder_$name")
                        .build()
                    WorkManager.getInstance(context).enqueue(request)
                }
            }
        }
    }
}
