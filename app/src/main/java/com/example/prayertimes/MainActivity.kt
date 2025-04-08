package com.example.prayertimes

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import com.example.prayertimes.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved night mode
        AppCompatDelegate.setDefaultNightMode(getSavedThemeMode())
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                fetchLocation()
            } else {
                binding.prayerTimes.text = getString(R.string.location_permission_required)
            }
        }
        locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        viewModel.prayerTimes.observe(this) { times ->
            val fmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val text = getString(R.string.fajr) + ": " + fmt.format(times.fajr) + "\n" +
                       getString(R.string.dhuhr) + ": " + fmt.format(times.dhuhr) + "\n" +
                       getString(R.string.asr) + ": " + fmt.format(times.asr) + "\n" +
                       getString(R.string.maghrib) + ": " + fmt.format(times.maghrib) + "\n" +
                       getString(R.string.isha) + ": " + fmt.format(times.isha)
            binding.prayerTimes.text = text

            // schedule reminders
            ReminderScheduler.scheduleAll(this, times.coordinates)
        }

        viewModel.hijriDate.observe(this) { hijri ->
            val gregorian = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            binding.dates.text = getString(R.string.gregorian) + ": " + gregorian + "\n" +
                                 getString(R.string.hijri) + ": " + hijri
        }

        initReminderSwitchesAndOffsets()

        binding.resetReminders.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.reset_title))
                .setMessage(getString(R.string.reset_message))
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    resetReminderSettings()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                viewModel.calculatePrayerTimes(location)
            } else {
                binding.prayerTimes.text = getString(R.string.unable_to_get_location)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when(item.itemId){
            R.id.action_toggle_theme -> {
                toggleTheme()
                true
            }
            R.id.action_switch_language -> {
                showLanguageDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleTheme() {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        val newMode = if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(newMode)
        saveThemeMode(newMode)
    }

    private fun saveThemeMode(mode: Int) {
        getSharedPreferences("prefs", MODE_PRIVATE).edit()
            .putInt("theme_mode", mode)
            .apply()
    }

    private fun getSavedThemeMode(): Int {
        return getSharedPreferences("prefs", MODE_PRIVATE)
            .getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_NO)
    }

    private fun showLanguageDialog() {
        val options = arrayOf(getString(R.string.language_english), getString(R.string.language_arabic))
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.action_switch_language))
            .setItems(options) { _, which ->
                val langCode = if(which == 0) "en" else "ar"
                setLocale(langCode)
            }.show()
    }

    private fun setLocale(lang: String) {
        getSharedPreferences("prefs", MODE_PRIVATE).edit()
            .putString("lang", lang)
            .apply()
        recreate()
    }

    private fun initReminderSwitchesAndOffsets(){
        binding.switchFajr.isChecked = isReminderEnabled("Fajr")
        binding.switchDhuhr.isChecked = isReminderEnabled("Dhuhr")
        binding.switchAsr.isChecked = isReminderEnabled("Asr")
        binding.switchMaghrib.isChecked = isReminderEnabled("Maghrib")
        binding.switchIsha.isChecked = isReminderEnabled("Isha")

        binding.switchFajr.setOnCheckedChangeListener { _, isChecked -> saveReminderPreference("Fajr", isChecked) }
        binding.switchDhuhr.setOnCheckedChangeListener { _, isChecked -> saveReminderPreference("Dhuhr", isChecked) }
        binding.switchAsr.setOnCheckedChangeListener { _, isChecked -> saveReminderPreference("Asr", isChecked) }
        binding.switchMaghrib.setOnCheckedChangeListener { _, isChecked -> saveReminderPreference("Maghrib", isChecked) }
        binding.switchIsha.setOnCheckedChangeListener { _, isChecked -> saveReminderPreference("Isha", isChecked) }

        binding.offsetFajr.setText(getReminderOffset("Fajr").toString())
        binding.offsetDhuhr.setText(getReminderOffset("Dhuhr").toString())
        binding.offsetAsr.setText(getReminderOffset("Asr").toString())
        binding.offsetMaghrib.setText(getReminderOffset("Maghrib").toString())
        binding.offsetIsha.setText(getReminderOffset("Isha").toString())

        binding.offsetFajr.setOnFocusChangeListener { _, hasFocus ->
            if(!hasFocus){
                val offset = binding.offsetFajr.text.toString().toIntOrNull() ?: 0
                saveReminderOffset("Fajr", offset)
            }
        }
        binding.offsetDhuhr.setOnFocusChangeListener { _, hasFocus ->
            if(!hasFocus){
                val offset = binding.offsetDhuhr.text.toString().toIntOrNull() ?: 0
                saveReminderOffset("Dhuhr", offset)
            }
        }
        binding.offsetAsr.setOnFocusChangeListener { _, hasFocus ->
            if(!hasFocus){
                val offset = binding.offsetAsr.text.toString().toIntOrNull() ?: 0
                saveReminderOffset("Asr", offset)
            }
        }
        binding.offsetMaghrib.setOnFocusChangeListener { _, hasFocus ->
            if(!hasFocus){
                val offset = binding.offsetMaghrib.text.toString().toIntOrNull() ?: 0
                saveReminderOffset("Maghrib", offset)
            }
        }
        binding.offsetIsha.setOnFocusChangeListener { _, hasFocus ->
            if(!hasFocus){
                val offset = binding.offsetIsha.text.toString().toIntOrNull() ?: 0
                saveReminderOffset("Isha", offset)
            }
        }
    }

    private fun saveReminderPreference(name: String, isEnabled: Boolean){
        getSharedPreferences("prefs", MODE_PRIVATE).edit()
            .putBoolean("reminder_$name", isEnabled)
            .apply()
    }

    private fun isReminderEnabled(name: String): Boolean {
        return getSharedPreferences("prefs", MODE_PRIVATE).getBoolean("reminder_$name", true)
    }

    private fun saveReminderOffset(prayer: String, minutes: Int){
        getSharedPreferences("prefs", MODE_PRIVATE).edit()
            .putInt("offset_$prayer", minutes)
            .apply()
    }

    private fun getReminderOffset(prayer: String): Int {
        return getSharedPreferences("prefs", MODE_PRIVATE).getInt("offset_$prayer", 0)
    }

    private fun resetReminderSettings() {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE).edit()
        val prayers = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
        prayers.forEach {
            prefs.putBoolean("reminder_$it", true)
            prefs.putInt("offset_$it", 0)
        }
        prefs.apply()

        binding.switchFajr.isChecked = true
        binding.switchDhuhr.isChecked = true
        binding.switchAsr.isChecked = true
        binding.switchMaghrib.isChecked = true
        binding.switchIsha.isChecked = true

        binding.offsetFajr.setText("0")
        binding.offsetDhuhr.setText("0")
        binding.offsetAsr.setText("0")
        binding.offsetMaghrib.setText("0")
        binding.offsetIsha.setText("0")

        Toast.makeText(this, getString(R.string.reset_toast), Toast.LENGTH_SHORT).show()
    }
}
