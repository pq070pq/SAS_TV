package com.sas.tv.feature.screensaver

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SasScreensaverDreamService : DreamService() {
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private val zone = ZoneId.of("Asia/Riyadh")
    private var clockView: TextView? = null
    private var dateView: TextView? = null
    private var hijriView: TextView? = null
    private var cityView: TextView? = null
    private var weatherView: TextView? = null
    private var prayerView: TextView? = null
    private var lastRefreshAt = 0L
    private var paletteIndex = 0

    private val palettes = listOf(
        intArrayOf(0xFF0C2944.toInt(), 0xFF071321.toInt(), 0xFF030912.toInt()),
        intArrayOf(0xFF173A52.toInt(), 0xFF101B34.toInt(), 0xFF080F1C.toInt()),
        intArrayOf(0xFF253B52.toInt(), 0xFF101D32.toInt(), 0xFF060C18.toInt()),
        intArrayOf(0xFF123B3A.toInt(), 0xFF0B2029.toInt(), 0xFF050D17.toInt())
    )

    private val tick = object : Runnable {
        override fun run() {
            updateClock()
            if (System.currentTimeMillis() - lastRefreshAt >= TimeUnit.MINUTES.toMillis(30)) {
                refreshInformation()
            }
            handler.postDelayed(this, 1_000L)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = false

        val root = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                palettes[paletteIndex]
            )
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 40, 48, 40)
        }
        clockView = textView(78f, 0xFFFFFFFF.toInt(), Typeface.create("sans-serif-light", Typeface.NORMAL))
        dateView = textView(22f, 0xFFE2EAF2.toInt())
        hijriView = textView(22f, 0xFF80E7DF.toInt())
        cityView = textView(17f, 0xFFB7C8D8.toInt())
        weatherView = textView(18f, 0xFFE2EAF2.toInt())
        prayerView = textView(17f, 0xFFE2EAF2.toInt())

        content.addView(clockView, centeredParams())
        content.addView(dateView, centeredParams(top = 8))
        content.addView(hijriView, centeredParams(top = 6))
        content.addView(cityView, centeredParams(top = 12))
        content.addView(weatherView, centeredParams(top = 30))
        content.addView(prayerView, centeredParams(top = 18))

        root.addView(
            content,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
        )
        setContentView(root)
        updateClock()
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        handler.removeCallbacks(tick)
        handler.post(tick)
        refreshInformation()
    }

    override fun onDreamingStopped() {
        handler.removeCallbacks(tick)
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(tick)
        super.onDetachedFromWindow()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun textView(
        size: Float,
        color: Int,
        typeface: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    ) = TextView(this).apply {
        textSize = size
        setTextColor(color)
        this.typeface = typeface
        gravity = Gravity.CENTER
        textAlignment = View.TEXT_ALIGNMENT_CENTER
        setShadowLayer(14f, 0f, 3f, 0x66000000)
    }

    private fun centeredParams(top: Int = 0) =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = top }

    private fun updateClock() {
        val now = Instant.now().atZone(zone)
        val locale = Locale("ar", "SA")
        clockView?.text = DateTimeFormatter.ofPattern("hh:mm:ss a", locale).format(now)
        dateView?.text = DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", locale).format(now)
        hijriView?.text = DateTimeFormatter.ofPattern("d MMMM yyyy هـ", locale)
            .withChronology(HijrahDate.now(zone).chronology)
            .format(now)
        val prefs = getSharedPreferences("sas_tv_screensaver", MODE_PRIVATE)
        cityView?.text = "بتوقيت ${prefs.getString("city_title", "الرياض") ?: "الرياض"}"
    }

    private fun refreshInformation() {
        val prefs = getSharedPreferences("sas_tv_screensaver", MODE_PRIVATE)
        val city = prefs.getString("city", "Riyadh") ?: "Riyadh"
        val country = prefs.getString("country", "Saudi Arabia") ?: "Saudi Arabia"
        val latitude = prefs.getString("latitude", "24.7136")?.toDoubleOrNull() ?: 24.7136
        val longitude = prefs.getString("longitude", "46.6753")?.toDoubleOrNull() ?: 46.6753
        lastRefreshAt = System.currentTimeMillis()
        executor.execute {
            val weather = loadWeather(latitude, longitude)
            val prayers = loadPrayerTimes(city, country)
            handler.post {
                weatherView?.text = weather ?: "الطقس غير متاح حاليًا"
                prayerView?.text = prayers ?: "مواقيت الصلاة غير متاحة حاليًا"
            }
        }
        paletteIndex = (paletteIndex + 1) % palettes.size
        (window?.decorView as? FrameLayout)?.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            palettes[paletteIndex]
        )
    }

    private fun loadWeather(latitude: Double, longitude: Double): String? = runCatching {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,weather_code&timezone=Asia%2FRiyadh"
        )
        val json = requestJson(url)
        val current = json.getJSONObject("current")
        val temp = current.getDouble("temperature_2m")
        val code = current.getInt("weather_code")
        "الطقس: ${"%.0f".format(Locale.US, temp)}° م • ${weatherDescription(code)}"
    }.getOrNull()

    private fun loadPrayerTimes(city: String, country: String): String? = runCatching {
        val url = URL(
            "https://api.aladhan.com/v1/timingsByCity?city=${java.net.URLEncoder.encode(city, "UTF-8")}&country=${java.net.URLEncoder.encode(country, "UTF-8")}&method=4"
        )
        val timings = requestJson(url).getJSONObject("data").getJSONObject("timings")
        val locale = Locale("ar", "SA")
        val names = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
        val labels = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")
        labels.indices.joinToString("   •   ") { i ->
            val raw = timings.getString(names[i]).substringBefore(" ")
            "${labels[i]} $raw"
        }
    }.getOrNull()

    private fun requestJson(url: URL): JSONObject {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        return try {
            connection.inputStream.bufferedReader(Charsets.UTF_8).use {
                JSONObject(it.readText())
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun weatherDescription(code: Int): String = when (code) {
        0 -> "صحو"
        1, 2 -> "غائم جزئيًا"
        3 -> "غائم"
        45, 48 -> "ضباب"
        51, 53, 55, 56, 57 -> "رذاذ"
        61, 63, 65, 66, 67, 80, 81, 82 -> "أمطار"
        71, 73, 75, 77, 85, 86 -> "ثلوج"
        95, 96, 99 -> "عواصف رعدية"
        else -> "حالة جوية متغيرة"
    }
}
