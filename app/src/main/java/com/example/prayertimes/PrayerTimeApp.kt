package com.example.prayertimes

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

class PrayerTimeApp : Application() {
    override fun attachBaseContext(base: Context) {
        val prefs = base.getSharedPreferences("prefs", MODE_PRIVATE)
        val lang = prefs.getString("lang", "en") ?: "en"

        val locale = Locale(lang)
        Locale.setDefault(locale)

        val config = Configuration(base.resources.configuration)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            config.locale = locale
        }

        val ctx = base.createConfigurationContext(config)
        super.attachBaseContext(ctx)
    }
}
