package com.tvdebugassistant

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var report: TextView
    private var tapCount = 0
    private var lastTapAt = 0L
    private val routes = listOf(
        "خيارات المطوّر" to Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
        "معلومات الجهاز" to Settings.ACTION_DEVICE_INFO_SETTINGS,
        "إعدادات الأمان" to Settings.ACTION_SECURITY_SETTINGS,
        "إعدادات النظام" to Settings.ACTION_SETTINGS,
        "إدارة التطبيقات" to Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS,
        "إعدادات الشاشة" to Settings.ACTION_DISPLAY_SETTINGS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (22 * resources.displayMetrics.density).toInt()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(0xFF101820.toInt())
        }
        layout.addView(TextView(this).apply {
            text = "TV Debug Assistant 0.2"
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
        layout.addView(makeButton("فتح المطوّر الذكي – اضغط 6 مرات") { smartTap() })
        layout.addView(makeButton("فتح إعدادات النظام") {
            openPage(Intent(Settings.ACTION_SETTINGS))
        })
        layout.addView(makeButton("تحديث معلومات الجهاز") { report.text = deviceReport() })
        layout.addView(TextView(this).apply {
            text = "\nطريقة الاستخدام: اضغط زر «فتح المطوّر الذكي» ست مرات متتالية. سيجرب التطبيق ست صفحات إعدادات رسمية واحدة تلو الأخرى، ويعرض نتيجة كل محاولة."
            textSize = 15f; setTextColor(0xFFFFD180.toInt()); setPadding(0, pad, 0, 0)
        })
        layout.addView(TextView(this).apply {
            text = "\nتنبيه: تكرار الضغط لا يمنح صلاحيات إضافية ولا يتجاوز حماية الشركة. التطبيق يفتح صفحات النظام المتاحة فقط، ولا يغيّر إعداد ADB بنفسه."
            textSize = 14f; setTextColor(0xFFB8C7D1.toInt()); setPadding(0, pad, 0, 0)
        })
        setContentView(ScrollView(this).apply { addView(layout) })
    }

    private fun makeButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label; isFocusable = true; isFocusableInTouchMode = true
        setOnClickListener { action() }
    }

    private fun smartTap() {
        val now = SystemClock.uptimeMillis()
        if (now - lastTapAt > 2500L) tapCount = 0
        lastTapAt = now
        tapCount++
        val route = routes[tapCount - 1]
        report.text = deviceReport() + "\n\nالمحاولة $tapCount من 6: ${route.first}"
        Toast.makeText(this, "المحاولة $tapCount من 6: ${route.first}", Toast.LENGTH_SHORT).show()
        openPage(Intent(route.second))
        if (tapCount == 6) {
            report.append("\n\nاكتملت المحاولات الست. راجع الصفحات التي فتحها النظام ورسائل الرفض.")
            tapCount = 0
        }
    }

    private fun openPage(intent: Intent) {
        try {
            if (intent.resolveActivity(packageManager) == null) {
                Toast.makeText(this, "الصفحة غير موجودة في هذا النظام", Toast.LENGTH_SHORT).show()
            } else startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "هذه الصفحة غير متاحة على هذا الجهاز", Toast.LENGTH_LONG).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, "النظام منع الوصول إلى هذه الصفحة", Toast.LENGTH_LONG).show()
        }
    }

    private fun deviceReport(): String = "معلومات الجهاز\n" +
        "الشركة: ${Build.MANUFACTURER}\n" +
        "الطراز: ${Build.MODEL}\n" +
        "الجهاز: ${Build.DEVICE}\n" +
        "إصدار Android: ${Build.VERSION.RELEASE}\n" +
        "مستوى API: ${Build.VERSION.SDK_INT}\n\n" +
        "حالة ADB: لا يمكن لتطبيق عادي قراءتها بدقة دون صلاحيات نظام."
}
