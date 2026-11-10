package com.sas.tv

import android.app.*
import android.content.*
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.*
import java.util.concurrent.Executors

class PrayerAdhanActivity : Activity() {
    private val io=Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("sas_tv_adhan",MODE_PRIVATE) }
    private lateinit var info: TextView
    private lateinit var list: LinearLayout
    private val cities=listOf(City("الرياض",24.7136,46.6753),City("جدة",21.4858,39.1925),City("مكة المكرمة",21.3891,39.8579),City("المدينة المنورة",24.5247,39.5692),City("الدمام",26.4207,50.0888),City("أبها",18.2164,42.5053),City("تبوك",28.3838,36.5550),City("الطائف",21.2703,40.4158))
    data class City(val name:String,val lat:Double,val lon:Double)
    private val names=listOf("الفجر","الشروق","الظهر","العصر","المغرب","العشاء")
    private val keys=listOf("Fajr","Sunrise","Dhuhr","Asr","Maghrib","Isha")

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(30,22,30,22); setBackgroundColor(0xFF071321.toInt()) }
        root.addView(TextView(this).apply { text="مواقيت الصلاة والتنبيه"; textSize=25f; setTextColor(0xFF00D6C8.toInt()); gravity=Gravity.CENTER })
        info=TextView(this).apply { text="اختر مدينتك لتحميل المواقيت"; textSize=16f; setTextColor(-1); gravity=Gravity.CENTER; setPadding(0,16,0,16) }
        root.addView(info)
        val spinner=Spinner(this)
        spinner.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,cities.map{it.name})
        val saved=prefs.getString("city","الرياض")
        spinner.setSelection(cities.indexOfFirst{it.name==saved}.coerceAtLeast(0))
        root.addView(spinner)
        root.addView(Button(this).apply { text="تحديث المواقيت"; setOnClickListener { prefs.edit().putString("city",cities[spinner.selectedItemPosition].name).apply(); load(cities[spinner.selectedItemPosition]) } })
        val enabled=Switch(this).apply { text="تفعيل تنبيهات الصلاة"; setTextColor(-1); isChecked=prefs.getBoolean("enabled",false) }
        root.addView(enabled)
        root.addView(Button(this).apply { text="حفظ التنبيهات"; setOnClickListener { prefs.edit().putBoolean("enabled",enabled.isChecked).apply(); load(cities[spinner.selectedItemPosition]) } })
        list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply{addView(list)},LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
        load(cities.first{it.name==saved})
    }
    private fun load(city:City) {
        info.text="جاري جلب مواقيت " + city.name + "…"
        io.execute {
            try {
                val url=URL("https://api.aladhan.com/v1/timings?latitude="+city.lat+"&longitude="+city.lon+"&method=4")
                val c=url.openConnection() as HttpURLConnection
                c.connectTimeout=10000;c.readTimeout=12000
                val json=JSONObject(c.inputStream.bufferedReader().use{it.readText()});c.disconnect()
                val timings=json.getJSONObject("data").getJSONObject("timings")
                val today=LocalDate.now(ZoneId.of("Asia/Riyadh"))
                val schedule=keys.mapIndexed{i,key-> names[i] to timings.getString(key).take(5)}
                runOnUiThread {
                    info.text="مواقيت " + city.name + " • " + today.toString()
                    list.removeAllViews()
                    schedule.forEach{(n,t)->list.addView(TextView(this).apply{text=n+"     "+t;textSize=20f;setTextColor(-1);setPadding(18,12,18,12)})}
                    if(prefs.getBoolean("enabled",false)) scheduleAlerts(schedule)
                }
            } catch(e:Exception) { runOnUiThread { info.text="تعذر جلب المواقيت. تحقق من الإنترنت ثم أعد المحاولة." } }
        }
    }
    private fun scheduleAlerts(times:List<Pair<String,String>>) {
        val am=getSystemService(ALARM_SERVICE) as AlarmManager
        val zone=ZoneId.of("Asia/Riyadh")
        val now=ZonedDateTime.now(zone)
        times.filter{it.first!="الشروق"}.forEachIndexed{index,pair->
            val parts=pair.second.split(":")
            val at=now.toLocalDate().atTime(parts[0].toInt(),parts[1].toInt()).atZone(zone)
            val target=if(at.isAfter(now)) at else at.plusDays(1)
            val intent=Intent(this,PrayerAlertReceiver::class.java).putExtra("prayer",pair.first)
            val pi=PendingIntent.getBroadcast(this,100+index,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,target.toInstant().toEpochMilli(),pi)
        }
    }
    override fun onDestroy(){io.shutdownNow();super.onDestroy()}
}

class PrayerAlertReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        val nm=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel="sas_prayer_alerts"
        if(android.os.Build.VERSION.SDK_INT>=26) nm.createNotificationChannel(NotificationChannel(channel,"تنبيهات الصلاة",NotificationManager.IMPORTANCE_HIGH))
        val prayer=intent.getStringExtra("prayer") ?: "الصلاة"
        val notification=Notification.Builder(context,channel).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("حان وقت "+prayer).setContentText("تنبيه الصلاة من SAS TV").setAutoCancel(true).build()
        nm.notify(prayer.hashCode(),notification)
    }
}

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
