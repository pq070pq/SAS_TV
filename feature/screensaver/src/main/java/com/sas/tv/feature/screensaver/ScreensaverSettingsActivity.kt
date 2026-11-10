package com.sas.tv.feature.screensaver

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView

private data class CityOption(
    val title: String,
    val city: String,
    val country: String,
    val latitude: Double,
    val longitude: Double
) {
    override fun toString(): String = title
}

class ScreensaverSettingsActivity : Activity() {
    private val cities = listOf(
        CityOption("الرياض", "Riyadh", "Saudi Arabia", 24.7136, 46.6753),
        CityOption("جدة", "Jeddah", "Saudi Arabia", 21.4858, 39.1925),
        CityOption("مكة المكرمة", "Makkah", "Saudi Arabia", 21.3891, 39.8579),
        CityOption("المدينة المنورة", "Madinah", "Saudi Arabia", 24.5247, 39.5692),
        CityOption("الدمام", "Dammam", "Saudi Arabia", 26.4207, 50.0888),
        CityOption("أبها", "Abha", "Saudi Arabia", 18.2164, 42.5053),
        CityOption("تبوك", "Tabuk", "Saudi Arabia", 28.3838, 36.5550),
        CityOption("الطائف", "Taif", "Saudi Arabia", 21.2703, 40.4158)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("sas_tv_screensaver", MODE_PRIVATE)
        val background = 0xFF071321.toInt()
        val white = 0xFFFFFFFF.toInt()
        val mint = 0xFF00D6C8.toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 32, 40, 32)
            setBackgroundColor(background)
        }
        val title = TextView(this).apply {
            text = "إعدادات شاشة التوقف"
            textSize = 26f
            setTextColor(mint)
            gravity = Gravity.CENTER
        }
        val description = TextView(this).apply {
            text = "اختر المدينة لحساب مواقيت الصلاة والطقس. الساعة تعرض توقيت المدينة المختارة."
            textSize = 16f
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 22)
        }
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, cities)
        val savedCity = prefs.getString("city", "Riyadh")
        val selectedIndex = cities.indexOfFirst { it.city == savedCity }.coerceAtLeast(0)
        spinner.setSelection(selectedIndex)

        val save = Button(this).apply {
            text = "حفظ الإعدادات"
            setOnClickListener {
                val selected = cities[spinner.selectedItemPosition]
                prefs.edit()
                    .putString("city", selected.city)
                    .putString("country", selected.country)
                    .putString("city_title", selected.title)
                    .putString("latitude", selected.latitude.toString())
                    .putString("longitude", selected.longitude.toString())
                    .apply()
                finish()
            }
        }
        val activate = Button(this).apply {
            text = "فتح إعدادات شاشة التوقف في التلفزيون"
            setOnClickListener {
                val dreamSettings = Intent("android.settings.DREAM_SETTINGS")
                runCatching { startActivity(dreamSettings) }
                    .onFailure { startActivity(Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS)) }
            }
        }
        root.addView(title, LinearLayout.LayoutParams(-1, -2))
        root.addView(description, LinearLayout.LayoutParams(-1, -2))
        root.addView(spinner, LinearLayout.LayoutParams(-1, -2))
        root.addView(save, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 20 })
        root.addView(activate, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 12 })
        setContentView(root)
    }
}
