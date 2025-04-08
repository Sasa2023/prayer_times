package com.example.prayertimes

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.DateComponents
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.Coordinates
import java.text.SimpleDateFormat
import java.util.Locale

class PrayerTimeWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_prayer_time)

            val coords = Coordinates(24.7136, 46.6753)
            val params = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
            val dateComponents = DateComponents.from(java.util.Calendar.getInstance())
            val times = PrayerTimes(coords, dateComponents, params)

            val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

            views.setTextViewText(R.id.widget_title, "Prayer Times")
            views.setTextViewText(R.id.widget_fajr, "Fajr: " + formatter.format(times.fajr))
            views.setTextViewText(R.id.widget_dhuhr, "Dhuhr: " + formatter.format(times.dhuhr))
            views.setTextViewText(R.id.widget_asr, "Asr: " + formatter.format(times.asr))
            views.setTextViewText(R.id.widget_maghrib, "Maghrib: " + formatter.format(times.maghrib))
            views.setTextViewText(R.id.widget_isha, "Isha: " + formatter.format(times.isha))

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
