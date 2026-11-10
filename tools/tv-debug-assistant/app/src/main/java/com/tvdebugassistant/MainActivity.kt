package com.tvdebugassistant

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
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

    private data class Route(val label: String, val intent: Intent)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (22 * resources.displayMetrics.density).toInt()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(0xFF101820.toInt())
        }
        layout.addView(TextView(this).apply {
            text = "TV Debug Assistant 0.4"
            textSize = 26f; gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt()); setPadding(0, 0, 0, pad)
        })
        layout.addView(TextView(this).apply {
            text = "مساعد ذكي لاكتشاف صفحة المطوّر وتصحيح USB"
            textSize = 18f; gravity = Gravity.CENTER
            setTextColor(0xFFB8C7D1.toInt()); setPadding(0, 0, 0, pad)
        })
        report = TextView(this).apply {
            textSize = 17f; setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, pad, 0, pad); text = deviceReport()
        }
        layout.addView(report)
        layout.addView(makeButton("اكتشاف وفتح خيارات المطوّر تلقائيًا") {
            detectAndOpenDeveloperSettings()
        })
        layout.addView(makeButton("فتح إعدادات النظام") {
            openPage(Intent(Settings.ACTION_SETTINGS))
        })
        layout.addView(makeButton("تحديث معلومات الجهاز") { report.text = deviceReport() })
        layout.addView(TextView(this).apply {
            text = "\nضغطة واحدة: يفحص مسارات إعدادات المطوّر المعروفة، ثم يفتح أول صفحة مطوّر يسمح بها النظام."
            textSize = 15f; setTextColor(0xFFFFD180.toInt()); setPadding(0, pad, 0, 0)
        })
        layout.addView(TextView(this).apply {
            text = "\nلا يوجد في Android رابط عام مضمون يفتح مفتاح USB Debugging مباشرة. تفعيل المفتاح يحتاج موافقة المستخدم داخل إعدادات النظام، ولا يمكن لتطبيق عادي النقر عليه أو تجاوز منع الشركة."
            textSize = 14f; setTextColor(0xFFB8C7D1.toInt()); setPadding(0, pad, 0, 0)
        })
        setContentView(ScrollView(this).apply { addView(layout) })
    }

    private fun makeButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label; isFocusable = true; isFocusableInTouchMode = true
        setOnClickListener { action() }
    }

    private fun detectAndOpenDeveloperSettings() {
        val routes = listOf(
            Route("إعدادات المطوّر الرسمية", Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)),
            Route("إعدادات المطوّر البديلة", Intent("android.settings.DEVELOPMENT_SETTINGS")),
            Route("إعدادات المطوّر البديلة للشركة", Intent("android.settings.APPLICATION_DEVELOPMENT_SETTINGS"))
        )
        val checked = mutableListOf<String>()
        for (route in routes) {
            try {
                val resolved = packageManager.resolveActivity(route.intent, 0)
                if (resolved == null) {
                    checked.add(route.label + ": غير موجود")
                    continue
                }
                report.text = deviceReport() + "\n\nتم اكتشاف: " + route.label +
                    "\nالوجهة: " + (resolved.activityInfo?.packageName ?: "غير معروفة") +
                    "\nجارٍ فتح صفحة المطوّر..."
                startActivity(route.intent)
                Toast.makeText(this, "تم العثور على صفحة المطوّر وطلب فتحها", Toast.LENGTH_LONG).show()
                return
            } catch (_: SecurityException) {
                checked.add(route.label + ": منع النظام الوصول")
            } catch (_: ActivityNotFoundException) {
                checked.add(route.label + ": غير متاحة")
            } catch (_: Exception) {
                checked.add(route.label + ": تعذر الفتح")
            }
        }
        report.text = deviceReport() + "\n\nلم يعثر التطبيق على صفحة مطوّر قابلة للفتح.\n" +
            checked.joinToString("\n") +
            "\n\nهذا يعني أن الشركة قد أخفت الصفحة أو منعت الوصول إليها."
        Toast.makeText(this, "لم يسمح النظام بفتح خيارات المطوّر", Toast.LENGTH_LONG).show()
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
        "حالة USB Debugging: لا يمكن لتطبيق عادي قراءتها أو تغييرها بدقة دون صلاحية نظام."
}
