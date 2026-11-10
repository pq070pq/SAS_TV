package com.sas.tv

import android.app.Activity
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

/**
 * SAS TV service hub. All network work is kept off the UI thread.
 * Audio URLs and catalog metadata are fetched from MP3Quran's public API.
 * Tasks are stored locally and survive app updates.
 */
class ServicesActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private var player: MediaPlayer? = null
    private lateinit var body: LinearLayout
    private lateinit var status: TextView
    private val prefs by lazy { getSharedPreferences("sas_tv_tasks", MODE_PRIVATE) }
    private val bg = 0xFF071321.toInt()
    private val panel = 0xFF102438.toInt()
    private val mint = 0xFF00D6C8.toInt()
    private val white = 0xFFFFFFFF.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(34,24,34,24); setBackgroundColor(bg) }
        root.addView(TextView(this).apply { text="SAS TV"; textSize=28f; setTextColor(mint); gravity=Gravity.CENTER })
        status = TextView(this).apply { text="اختر الخدمة"; textSize=15f; setTextColor(white); gravity=Gravity.CENTER; setPadding(0,10,0,16) }
        root.addView(status)
        val tabs = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER }
        listOf("القرآن","الإذاعات","المباريات","المهام","الصلاة").forEach { label ->
            tabs.addView(button(label) { when(label) {
                "القرآن" -> loadReciters()
                "الإذاعات" -> loadRadios()
                "المباريات" -> loadMatches()
                "المهام" -> showTasks()
                else -> startActivity(Intent(this, PrayerAdhanActivity::class.java))
            } }, LinearLayout.LayoutParams(0,58,1f).apply { setMargins(4,4,4,4) })
        }
        root.addView(tabs)
        body = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(8,18,8,8) }
        root.addView(ScrollView(this).apply { addView(body) }, LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
        loadReciters()
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text=label; isAllCaps=false; setTextColor(white); setBackgroundColor(panel); textSize=14f
        setOnClickListener { action() }
    }
    private fun addRow(label: String, detail: String? = null, action: (() -> Unit)? = null) {
        val b=button(if(detail.isNullOrBlank()) label else "$label\n$detail") { action?.invoke() }
        body.addView(b, LinearLayout.LayoutParams(-1,64).apply { setMargins(0,5,0,5) })
    }
    private fun clear(title: String) { body.removeAllViews(); status.text=title }
    private fun request(url: String): String {
        val c= URL(url).openConnection() as HttpURLConnection
        c.connectTimeout=10000; c.readTimeout=12000; c.setRequestProperty("User-Agent","SAS-TV/1.0")
        return try { c.inputStream.bufferedReader().use { it.readText() } } finally { c.disconnect() }
    }
    private fun async(title: String, work: () -> Unit) { runOnUiThread { status.text=title }; worker.execute { try { work() } catch(e: Exception) { runOnUiThread { status.text="تعذر تحميل البيانات: ${e.message ?: "تحقق من الاتصال"}" } } } }

    private fun loadReciters() = async("جاري تحميل القراء…") {
        val json=JSONObject(request("https://mp3quran.net/api/v3/reciters?language=ar"))
        val arr=json.optJSONArray("reciters") ?: throw IllegalStateException("قائمة القراء غير متاحة")
        val items= mutableListOf<Pair<String,String>>()
        for(i in 0 until arr.length()) { val o=arr.getJSONObject(i); val server=o.optString("Server"); if(server.isNotBlank()) items.add(o.optString("name","قارئ") to server) }
        runOnUiThread { clear("القراء • اختر قارئًا"); items.forEach { (name,server) -> addRow(name,"فتح قائمة السور") { loadSurahs(name,server) } } }
    }
    private fun loadSurahs(name: String, server: String) = async("تحميل سور $name…") {
        val json=JSONObject(request("https://mp3quran.net/api/v3/suwar?language=ar"))
        val arr=json.optJSONArray("suwar") ?: throw IllegalStateException("قائمة السور غير متاحة")
        runOnUiThread {
            clear("تلاوة • $name")
            for(i in 0 until arr.length()) {
                val s=arr.getJSONObject(i); val id=s.optString("id").toIntOrNull() ?: continue
                val title=s.optString("name","سورة")
                val audio=server.trimEnd('/') + "/" + "%03d".format(id) + ".mp3"
                addRow(title,"تشغيل السورة") { playAudio(audio,title) }
            }
        }
    }
    private fun loadRadios() = async("جاري تحميل الإذاعات القرآنية…") {
        val json=JSONObject(request("https://mp3quran.net/api/v3/radios?language=ar"))
        val arr=json.optJSONArray("radios") ?: throw IllegalStateException("قائمة الإذاعات غير متاحة")
        val list= mutableListOf<Pair<String,String>>()
        for(i in 0 until arr.length()) { val o=arr.getJSONObject(i); val url=o.optString("url"); if(url.startsWith("http")) list.add(o.optString("name","إذاعة قرآنية") to url) }
        runOnUiThread { clear("الإذاعات القرآنية"); list.forEach { (n,u) -> addRow(n,"تشغيل البث المباشر") { playAudio(u,n) } } }
    }
    private fun playAudio(url: String, title: String) {
        stopAudio(); status.text="جاري تشغيل: $title"
        player=MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            setDataSource(url); setOnPreparedListener { it.start(); status.text="يعمل الآن: $title" }
            setOnErrorListener { _,_,_ -> status.text="تعذر تشغيل المصدر الصوتي"; true }
            prepareAsync()
        }
    }
    private fun stopAudio() { runCatching { player?.stop(); player?.release() }; player=null }
    private fun loadMatches() = async("جاري جلب مباريات كرة القدم…") {
        val data=request("https://site.api.espn.com/apis/site/v2/sports/soccer/eng.1/scoreboard")
        val events=JSONObject(data).optJSONArray("events")
        runOnUiThread {
            clear("المباريات • الدوري الإنجليزي")
            if(events==null || events.length()==0) { addRow("لا توجد مباريات مدرجة حاليًا"); return@runOnUiThread }
            for(i in 0 until events.length()) {
                val e=events.getJSONObject(i); val comp=e.optJSONArray("competitions")?.optJSONObject(0)
                val competitors=comp?.optJSONArray("competitors")
                val home=competitors?.optJSONObject(0)?.optJSONObject("team")
                val away=competitors?.optJSONObject(1)?.optJSONObject("team")
                val h=home?.optString("displayName") ?: "الفريق الأول"
                val a=away?.optString("displayName") ?: "الفريق الثاني"
                val state=comp?.optJSONObject("status")?.optJSONObject("type")?.optString("detail") ?: e.optString("date")
                addRow("$h × $a",state)
            }
        }
    }
    private fun showMarkets() {
        clear("الأسواق • أسعار يومية من Stooq (قد تكون متأخرة)")
        listOf("AAPL.US","MSFT.US","NVDA.US","TSLA.US","SPY.US","QQQ.US","GC.F","BTCUSD").forEach { symbol ->
            addRow(symbol,"تحميل آخر بيانات متاحة") { loadQuote(symbol) }
        }
    }
    private fun loadQuote(symbol: String) = async("جاري جلب $symbol…") {
        val csv=request("https://stooq.com/q/l/?s=${URLEncoder.encode(symbol.lowercase(),"UTF-8")}&f=sd2t2ohlcv&h&e=csv")
        val line=csv.lineSequence().drop(1).firstOrNull()?.split(',') ?: throw IllegalStateException("لا توجد بيانات")
        val text=if(line.size>=7 && line[6]!="N/D") "${line[0]} • إغلاق ${line[6]} • ${line[1]} ${line[2]}" else "لا تتوفر بيانات لهذا الرمز"
        runOnUiThread { status.text="$symbol — $text" }
    }
    private fun showTasks() {
        clear("المهام • محفوظة على الجهاز")
        val input=EditText(this).apply { hint="اكتب مهمة جديدة"; setTextColor(white); setHintTextColor(0xFF9FB2C5.toInt()) }
        body.addView(input,LinearLayout.LayoutParams(-1,60))
        body.addView(button("إضافة المهمة") {
            val value=input.text.toString().trim()
            if(value.isNotEmpty()) { val list=prefs.getStringSet("items", emptySet())!!.toMutableSet(); list.add(value); prefs.edit().putStringSet("items",list).apply(); showTasks() }
        })
        prefs.getStringSet("items",emptySet())!!.toList().sorted().forEach { task ->
            addRow("□ $task","اضغط لإنجاز/حذف المهمة") {
                val list=prefs.getStringSet("items",emptySet())!!.toMutableSet(); list.remove(task); prefs.edit().putStringSet("items",list).apply(); showTasks()
            }
        }
    }
    override fun onDestroy() { stopAudio(); worker.shutdownNow(); super.onDestroy() }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(keyCode==KeyEvent.KEYCODE_MEDIA_STOP || keyCode==KeyEvent.KEYCODE_MEDIA_PAUSE) { stopAudio(); status.text="تم إيقاف الصوت"; return true }
        return super.onKeyDown(keyCode,event)
    }
}
