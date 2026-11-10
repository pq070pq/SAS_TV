package com.tvdebugassistant

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var report: TextView

    private val developerRoutes = listOf(
        "صفحة خيارات المطوّر الرسمية" to Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
        "إعدادات المطوّر البديلة" to Intent("android.settings.APPLICATION_DEVELOPMENT_SETTINGS"),
        "صفحة إعدادات النظام" to Intent(Settings.ACTION_SETTINGS),
        "معلومات الجهاز" to Intent(Settings.ACTION_DEVICE_INFO_SETTINGS),
        "إعدادات الأمان" to Intent(Settings.ACTION_SECURITY_SETTINGS),
        "إعدادات النظام العامة" to Intent("android.settings.SETTINGS")
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
            text = "TV Debug Assistant 0.3"
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
            openDeveloperSettings()
        })
        layout.addView(makeButton("فتح إعدادات النظام") {
            openPage(Intent(Settings.ACTION_SETTINGS))
        })
        layout.addView(makeButton("تحديث معلومات الجهاز") { report.text = deviceReport() })
        layout.addView(TextView(this).apply {
            text = "\nطريقة الاستخدام: ضغطة واحدة على «فتح خيارات المطوّر». سيجرب التطبيق تلقائيًا حتى 6 مسارات نظام بالترتيب، ويبدأ بصفحة المطوّر الأصلية."
            textSize = 15f; setTextColor(0xFFFFD180.toInt()); setPadding(0, pad, 0, 0)
        })
        layout.addView(TextView(this).apply {
            text = "\nتنبيه: هذه محاولات فتح لصفحات Android الرسمية فقط. إذا منع النظام صفحة المطوّر بصلاحية محمية، لا يمكن لتطبيق عادي تجاوز المنع أو تفعيل ADB بنفسه."
            textSize = 14f; setTextColor(0xFFB8C7D1.toInt()); setPadding(0, pad, 0, 0)
        })
        setContentView(ScrollView(this).apply { addView(layout) })
    }

    private fun makeButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label; isFocusable = true; isFocusableInTouchMode = true
        setOnClickListener { action() }
    }

    private fun openDeveloperSettings() {
        val failures = mutableListOf<String>()
        for ((name, intent) in developerRoutes) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent.resolveActivity(packageManager) == null) {
                    failures.add("$name: غير موجود")
                    continue
                }
                report.text = deviceReport() + "\n\nتم العثور على المسار: $name\nجارٍ فتحه..."
                startActivity(intent)
                Toast.makeText(this, "تم إرسال طلب فتح: $name", Toast.LENGTH_LONG).show()
                return
            } catch (e: SecurityException) {
                failures.add("$name: رفض النظام الصلاحية")
            } catch (e: ActivityNotFoundException) {
                failures.add("$name: الصفحة غير متاحة")
            } catch (e: Exception) {
                failures.add("$name: تعذر الفتح")
            }
        }
        report.text = deviceReport() + "\n\nلم ينجح أي مسار:\n" + failures.joinToString("\n")
        Toast.makeText(this, "النظام منع جميع مسارات الإعدادات التي جُرّبت", Toast.LENGTH_LONG).show()
    }

    private fun openPage(intent: Intent) {
        try {
            startActivity(intent)
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
