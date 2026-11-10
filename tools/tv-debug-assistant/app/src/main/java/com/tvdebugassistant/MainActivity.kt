package com.tvdebugassistant

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var report: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (22 * resources.displayMetrics.density).toInt()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(0xFF101820.toInt())
        }
        layout.addView(TextView(this).apply {
            text = "TV Debug Assistant 0.1"
            textSize = 26f; gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt()); setPadding(0, 0, 0, pad)
        })
        layout.addView(TextView(this).apply {
            text = "مساعد تشخيص خيارات المطوّر وتصحيح USB"
            textSize = 18f; gravity = Gravity.CENTER
            setTextColor(0xFFB8C7D1.toInt()); setPadding(0, 0, 0, pad)
        })
        report = TextView(this).apply {
            textSize = 17f; setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, pad, 0, pad); text = deviceReport()
        }
        layout.addView(report)
        layout.addView(makeButton("فتح خيارات المطوّر") {
            openPage(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        })
        layout.addView(makeButton("فتح إعدادات النظام") {
            openPage(Intent(Settings.ACTION_SETTINGS))
        })
        layout.addView(makeButton("تحديث معلومات الجهاز") { report.text = deviceReport() })
        layout.addView(TextView(this).apply {
            text = "\nتنبيه: التطبيق لا يستطيع تجاوز حماية الشركة أو تفعيل ADB بصلاحيات غير ممنوحة. إذا كانت صفحة المطوّر محجوبة، سنسجل النتيجة ونبحث عن مسار رسمي مناسب للجهاز."
            textSize = 15f; setTextColor(0xFFFFD180.toInt()); setPadding(0, pad, 0, 0)
        })
        setContentView(ScrollView(this).apply { addView(layout) })
    }
    private fun makeButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label; isFocusable = true; isFocusableInTouchMode = true
        setOnClickListener { action() }
    }
    private fun openPage(intent: Intent) {
        try { startActivity(intent) }
        catch (_: ActivityNotFoundException) { Toast.makeText(this, "هذه الصفحة غير متاحة على هذا الجهاز", Toast.LENGTH_LONG).show() }
        catch (_: SecurityException) { Toast.makeText(this, "النظام منع الوصول إلى هذه الصفحة", Toast.LENGTH_LONG).show() }
    }
    private fun deviceReport(): String = "معلومات الجهاز\n" +
        "الشركة: ${Build.MANUFACTURER}\n" +
        "الطراز: ${Build.MODEL}\n" +
        "الجهاز: ${Build.DEVICE}\n" +
        "إصدار Android: ${Build.VERSION.RELEASE}\n" +
        "مستوى API: ${Build.VERSION.SDK_INT}\n\n" +
        "حالة ADB: لا يمكن لتطبيق عادي قراءتها بدقة دون صلاحيات نظام."
}
