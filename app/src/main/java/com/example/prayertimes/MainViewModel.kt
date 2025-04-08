package com.example.prayertimes

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.DateComponents
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.Coordinates
import com.msarhan.ummalqura.calendar.UmmalquraCalendar
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _prayerTimes = MutableLiveData<PrayerTimes>()
    val prayerTimes: LiveData<PrayerTimes> get() = _prayerTimes

    private val _hijriDate = MutableLiveData<String>()
    val hijriDate: LiveData<String> get() = _hijriDate

    fun calculatePrayerTimes(location: Location) {
        val coords = Coordinates(location.latitude, location.longitude)
        val params = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
        params.madhab = Madhab.SHAFI

        val cal = Calendar.getInstance()
        val dateComponents = DateComponents.from(cal)
        val times = PrayerTimes(coords, dateComponents, params)
        _prayerTimes.value = times

        val ummCal = UmmalquraCalendar()
        ummCal.timeInMillis = cal.timeInMillis
        val hijri = "${ummCal.get(Calendar.DAY_OF_MONTH)} " +
            "${ummCal.getDisplayName(Calendar.MONTH, Calendar.LONG, java.util.Locale("ar"))} " +
            "${ummCal.get(Calendar.YEAR)} هـ"

        _hijriDate.value = hijri
    }
}
