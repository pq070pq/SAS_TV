package com.sas.tv

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.json.JSONObject

class PrayerBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("sas_tv_adhan", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", false)) return
        val city = prefs.getString("city", "الرياض") ?: "الرياض"
        val coordinates = mapOf(
            "الرياض" to (24.7136 to 46.6753), "جدة" to (21.4858 to 39.1925),
            "مكة المكرمة" to (21.3891 to 39.8579), "المدينة المنورة" to (24.5247 to 39.5692),
            "الدمام" to (26.4207 to 50.0888), "أبها" to (18.2164 to 42.5053),
            "تبوك" to (28.3838 to 36.5550), "الطائف" to (21.2703 to 40.4158)
        )
        val point = coordinates[city] ?: coordinates.getValue("الرياض")
        val pending = goAsync()
        Thread {
            try {
                val url = URL("https://api.aladhan.com/v1/timings?latitude=${point.first}&longitude=${point.second}&method=4")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 12000
                val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                connection.disconnect()
                val timings = root.getJSONObject("data").getJSONObject("timings")
                val zone = ZoneId.of("Asia/Riyadh")
                val today = LocalDate.now(zone)
                val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha").forEachIndexed { index, key ->
                    val time = timings.getString(key).take(5).split(":")
                    val at = today.atTime(time[0].toInt(), time[1].toInt()).atZone(zone)
                    val target = if (at.isAfter(ZonedDateTime.now(zone))) at else at.plusDays(1)
                    val names = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")
                    val broadcast = Intent(context, PrayerAlertReceiver::class.java).putExtra("prayer", names[index])
                    val pi = PendingIntent.getBroadcast(context, 100 + index, broadcast, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    if (android.os.Build.VERSION.SDK_INT < 31 || alarm.canScheduleExactAlarms()) {
                        alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.toInstant().toEpochMilli(), pi)
                    } else {
                        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.toInstant().toEpochMilli(), pi)
                    }
                }
            } catch (_: Exception) {
                // A later app launch can retry scheduling if the device was offline at boot.
            } finally {
                pending.finish()
            }
        }.start()
    }
}
