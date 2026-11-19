package com.sas.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.DecimalFormat
import kotlin.math.*

private val black = Color(0xFF000000)
private val gray = Color(0xFF333333)
private val pale = Color(0xFFA5A5A5)
private val orange = Color(0xFFFF9F0A)
private val card = Color(0xFF171717)
private val number = DecimalFormat("#,##0.########")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    App()
                }
            }
        }
    }
}

@Composable
fun App() {
    var page by remember { mutableStateOf("currency") }
    var display by remember { mutableStateOf("0") }
    var left by remember { mutableStateOf<Double?>(null) }
    var op by remember { mutableStateOf("") }
    var fresh by remember { mutableStateOf(false) }
    var scientific by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<String>() }

    Surface(Modifier.fillMaxSize(), color = black) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth().height(58.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(54.dp).clip(CircleShape).background(Color(0xFF242426)).clickable { page = if (page == "calculator") "currency" else "calculator" }, contentAlignment = Alignment.Center) { Text("▦", color = Color.White, fontSize = 30.sp) }
                Box(Modifier.size(54.dp).clip(CircleShape).background(Color(0xFF242426)).clickable { page = "more" }, contentAlignment = Alignment.Center) { Text("◷", color = Color.White, fontSize = 32.sp) }
            }
            Spacer(Modifier.height(8.dp))

            when (page) {
                "calculator" -> {
                    Spacer(Modifier.weight(1f))
                    Text(
                        display,
                        Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        color = Color.White,
                        fontSize = 58.sp,
                        textAlign = TextAlign.Start,
                        maxLines = 1
                    )
                    TextButton(onClick = { scientific = !scientific }) {
                        Text(if (scientific) "إخفاء العلمي" else "الحاسبة العلمية", color = orange)
                    }
                    if (scientific) {
                        listOf(listOf("sin", "cos", "tan", "√"), listOf("x²", "π", "ln", "log")).forEach { row ->
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { key ->
                                        Key(key, gray, Modifier.weight(1f)) {
                                            val x = display.toDoubleOrNull() ?: 0.0
                                            display = number.format(
                                                when (key) {
                                                    "sin" -> sin(Math.toRadians(x))
                                                    "cos" -> cos(Math.toRadians(x))
                                                    "tan" -> tan(Math.toRadians(x))
                                                    "√" -> sqrt(x)
                                                    "x²" -> x * x
                                                    "π" -> PI
                                                    "ln" -> ln(x)
                                                    else -> log10(x)
                                                }
                                            )
                                            fresh = true
                                        }
                                    }
                                }
                            }
                        }
                    }
                    val rows = listOf(
                        listOf("AC", "+/-", "%", "÷"),
                        listOf("7", "8", "9", "×"),
                        listOf("4", "5", "6", "−"),
                        listOf("1", "2", "3", "+"),
                        listOf("0", ".", "⌫", "=")
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        rows.forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { key ->
                                    val keyColor = when (key) {
                                        "AC", "+/-", "%" -> pale
                                        "÷", "×", "−", "+", "=" -> orange
                                        else -> gray
                                    }
                                    Key(key, keyColor, Modifier.weight(1f)) {
                                        val n = display.toDoubleOrNull() ?: 0.0
                                        when (key) {
                                            "AC" -> { display = "0"; left = null; op = "" }
                                            "+/-" -> if (display != "0") display = if (display.startsWith("-")) display.drop(1) else "-$display"
                                            "%" -> display = number.format(n / 100)
                                            "⌫" -> display = if (display.length > 1) display.dropLast(1) else "0"
                                            "." -> if (!display.contains(".")) display += "."
                                            "=" -> {
                                                val a = left
                                                if (a != null) {
                                                    val result = when (op) {
                                                        "+" -> a + n
                                                        "−" -> a - n
                                                        "×" -> a * n
                                                        else -> if (n == 0.0) Double.NaN else a / n
                                                    }
                                                    val out = number.format(result)
                                                    history.add(0, "$a $op $n = $out")
                                                    display = out
                                                    left = null
                                                    op = ""
                                                    fresh = true
                                                }
                                            }
                                            "+", "−", "×", "÷" -> { left = n; op = key; fresh = true }
                                            else -> { display = if (fresh || display == "0") key else display + key; fresh = false }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "currency" -> CurrencyScreen()
                "more" -> {
                    Text("المزيد", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    MoreItem("ملاحظات رياضية") { page = "notes" }
                    MoreItem("سجل العمليات") { page = "history" }
                }
                "notes" -> NotesArabic()
                "history" -> {
                    Text("سجل العمليات", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    history.forEach { item ->
                        TextButton(onClick = { display = item.substringAfter("= ").trim(); page = "calculator" }) {
                            Text(item, color = Color.White)
                        }
                    }
                    TextButton(onClick = { history.clear() }) { Text("مسح السجل", color = orange) }
                }
            }
        }
    }
}

@Composable
fun NavChip(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = if (selected) orange else Color.LightGray),
        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Text(label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun MoreItem(label: String, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = card),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(label, Modifier.fillMaxWidth().padding(18.dp), color = Color.White, fontSize = 17.sp)
    }
}

@Composable
fun Key(label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.padding(vertical = 4.dp).height(68.dp).clip(CircleShape).background(color).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (color == pale) Color.Black else Color.White, fontSize = 27.sp)
    }
}

private data class CurrencyUnit(val code: String, val name: String)

private val currencyUnits = listOf(
    CurrencyUnit("SAR", "ريال سعودي"), CurrencyUnit("USD", "دولار أمريكي"),
    CurrencyUnit("EUR", "يورو"), CurrencyUnit("GBP", "جنيه إسترليني"),
    CurrencyUnit("AED", "درهم إماراتي"), CurrencyUnit("KWD", "دينار كويتي"),
    CurrencyUnit("BHD", "دينار بحريني"), CurrencyUnit("QAR", "ريال قطري"),
    CurrencyUnit("OMR", "ريال عماني"), CurrencyUnit("EGP", "جنيه مصري"),
    CurrencyUnit("JOD", "دينار أردني"), CurrencyUnit("JPY", "ين ياباني"),
    CurrencyUnit("CHF", "فرنك سويسري"), CurrencyUnit("CAD", "دولار كندي"),
    CurrencyUnit("AUD", "دولار أسترالي"), CurrencyUnit("TRY", "ليرة تركية"),
    CurrencyUnit("INR", "روبية هندية"), CurrencyUnit("CNY", "يوان صيني"),
    CurrencyUnit("PKR", "روبية باكستانية"), CurrencyUnit("PHP", "بيزو فلبيني"),
    CurrencyUnit("NZD", "دولار نيوزيلندي"), CurrencyUnit("RUB", "روبل روسي"),
    CurrencyUnit("ZAR", "راند جنوب أفريقي"), CurrencyUnit("KRW", "وون كوري"),
    CurrencyUnit("SGD", "دولار سنغافوري"), CurrencyUnit("HKD", "دولار هونغ كونغ"),
    CurrencyUnit("SEK", "كرونة سويدية"), CurrencyUnit("NOK", "كرونة نرويجية"),
    CurrencyUnit("DKK", "كرونة دنماركية"), CurrencyUnit("IDR", "روبية إندونيسية"),
    CurrencyUnit("THB", "بات تايلندي"), CurrencyUnit("MYR", "رينغيت ماليزي")
)

@Composable
fun CurrencyScreen() {
    var amount by remember { mutableStateOf("29") }
    var display by remember { mutableStateOf("29") }
    var left by remember { mutableStateOf<Double?>(null) }
    var op by remember { mutableStateOf("") }
    var fresh by remember { mutableStateOf(false) }
    var from by remember { mutableStateOf("SAR") }
    var to by remember { mutableStateOf("USD") }
    var rate by remember { mutableStateOf<Double?>(null) }
    var status by remember { mutableStateOf("جارٍ تحميل سعر الصرف...") }
    var refresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(from, to, refresh) {
        if (from == to) { rate = 1.0; status = "العملتان متطابقتان" }
        else try {
            status = "جارٍ تحديث سعر الصرف..."
            val data = withContext(Dispatchers.IO) {
                val request = Request.Builder().url("https://open.er-api.com/v6/latest/$from").build()
                OkHttpClient().newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("HTTP " + response.code)
                    JSONObject(response.body?.string() ?: "{}")
                }
            }
            if (data.optString("result") != "success") error("Rate unavailable")
            rate = data.getJSONObject("rates").getDouble(to)
            status = "سعر صرف مرجعي محدث"
        } catch (e: Exception) { rate = null; status = "تعذر جلب السعر؛ تحقق من الإنترنت." }
    }
    val converted = rate?.let { (amount.toDoubleOrNull() ?: 0.0) * it }
    val keys = listOf(listOf("⌫", "حذف", "%", "÷"), listOf("٧", "٨", "٩", "×"), listOf("٤", "٥", "٦", "−"), listOf("١", "٢", "٣", "+"), listOf("+/−", "٠", "٫", "="))
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(0.25f))
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⇅", color = orange, fontSize = 30.sp, modifier = Modifier.clickable { val old = from; from = to; to = old })
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Text(amount, color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    Spacer(Modifier.width(10.dp)); CurrencyPickerCompact(from) { from = it }
                }
                Divider(color = Color(0xFF292929), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Text(converted?.let { number.format(it) } ?: "—", color = Color(0xFF85858B), fontSize = 48.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    Spacer(Modifier.width(10.dp)); CurrencyPickerCompact(to) { to = it }
                }
            }
        }
        Text(status, color = Color.DarkGray, fontSize = 9.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, maxLines = 1)
        Spacer(Modifier.height(12.dp))
        keys.forEach { row ->
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key ->
                        val c = when (key) { "⌫", "حذف", "%" -> Color(0xFF666668); "÷", "×", "−", "+", "=" -> orange; else -> Color(0xFF29292B) }
                        Box(Modifier.weight(1f).padding(vertical = 4.dp).aspectRatio(1f).clip(CircleShape).background(c).clickable {
                            val n = display.toDoubleOrNull() ?: 0.0
                            when (key) {
                                "⌫" -> { display = if (display.length > 1) display.dropLast(1) else "0"; amount = display }
                                "حذف" -> { display = "0"; amount = "0"; left = null; op = ""; fresh = false }
                                "%" -> { display = number.format(n / 100); amount = display; fresh = true }
                                "+/−" -> { display = if (display.startsWith("-")) display.drop(1) else "-$display"; amount = display }
                                "٫" -> { if (!display.contains(".")) display += "."; amount = display; fresh = false }
                                "+", "−", "×", "÷" -> { left = n; op = key; fresh = true }
                                "=" -> if (left != null) {
                                    val result = when (op) { "+" -> left!! + n; "−" -> left!! - n; "×" -> left!! * n; else -> if (n == 0.0) Double.NaN else left!! / n }
                                    display = number.format(result); amount = display; left = null; op = ""; fresh = true
                                }
                                else -> { display = if (fresh || display == "0") key else display + key; amount = display; fresh = false }
                            }
                        }, contentAlignment = Alignment.Center) { Text(key, color = Color.White, fontSize = if (key == "حذف") 21.sp else 34.sp, fontWeight = FontWeight.Medium) }
                    }
                }
            }
        }
        Spacer(Modifier.weight(0.08f))
    }
}

@Composable
fun CurrencyPickerCompact(selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val unit = currencyUnits.firstOrNull { it.code == selected }
    Row(Modifier.clickable { expanded = true }, verticalAlignment = Alignment.CenterVertically) {
        Text(unit?.name ?: selected, color = Color(0xFFB8B8BD), fontSize = 17.sp, maxLines = 1)
        Text("  ↕", color = Color(0xFF77777D), fontSize = 20.sp)
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 420.dp).background(card)) {
            currencyUnits.forEach { item -> DropdownMenuItem(text = { Text(item.name + " (" + item.code + ")", color = Color.White) }, onClick = { onSelected(item.code); expanded = false }) }
        }
    }
}

@Composable
fun CurrencyPicker(selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val unit = currencyUnits.firstOrNull { it.code == selected }
    Box(Modifier.fillMaxWidth()) {
        Card(
            Modifier.fillMaxWidth().clickable { expanded = true },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF292929)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(unit?.name ?: selected, color = Color.White, fontSize = 17.sp)
                Text("$selected  ▾", color = orange, fontWeight = FontWeight.Bold)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 420.dp).background(card)) {
            currencyUnits.forEach { item ->
                DropdownMenuItem(
                    text = { Text("${item.name}  (${item.code})", color = Color.White) },
                    onClick = { onSelected(item.code); expanded = false }
                )
            }
        }
    }
}

@Composable
fun NotesArabic() {
    var expression by remember { mutableStateOf("2+2") }
    var result by remember { mutableStateOf("4") }
    var fn by remember { mutableStateOf("x²") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("ملاحظات رياضية", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(expression, { expression = it }, label = { Text("اكتب العملية الحسابية") })
        Button(onClick = { result = basicEval(expression) }, colors = ButtonDefaults.buttonColors(containerColor = orange)) {
            Text("احسب", color = Color.Black)
        }
        Text(result, color = orange, fontSize = 32.sp)
        OutlinedTextField(fn, { fn = it }, label = { Text("الرسم: x² أو x أو sin(x) أو cos(x)") })
        Canvas(Modifier.fillMaxWidth().height(220.dp).background(card)) {
            val mid = size.height / 2
            drawLine(Color.DarkGray, androidx.compose.ui.geometry.Offset(0f, mid), androidx.compose.ui.geometry.Offset(size.width, mid), 1f)
            val points = (0..180).map { i ->
                val x = (i - 90) / 15.0
                val y = when (fn.trim()) {
                    "x²", "x^2" -> x * x / 4
                    "sin(x)" -> sin(x)
                    "cos(x)" -> cos(x)
                    else -> x
                }
                androidx.compose.ui.geometry.Offset(i / 180f * size.width, mid - y.toFloat() * 18f)
            }
            for (i in 0 until points.lastIndex) drawLine(orange, points[i], points[i + 1], 3f)
        }
    }
}

fun basicEval(s: String): String {
    val match = Regex("""^\s*(-?\d+(?:\.\d+)?)\s*([+*/-])\s*(-?\d+(?:\.\d+)?)\s*$""").matchEntire(s)
        ?: return "أدخل عملية بسيطة مثل 2+2"
    val a = match.groupValues[1].toDouble()
    val b = match.groupValues[3].toDouble()
    val value = when (match.groupValues[2]) {
        "+" -> a + b
        "-" -> a - b
        "*" -> a * b
        else -> if (b == 0.0) Double.NaN else a / b
    }
    return number.format(value)
}
