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
    var page by remember { mutableStateOf("calculator") }
    var display by remember { mutableStateOf("0") }
    var left by remember { mutableStateOf<Double?>(null) }
    var op by remember { mutableStateOf("") }
    var fresh by remember { mutableStateOf(false) }
    var scientific by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<String>() }

    Surface(Modifier.fillMaxSize(), color = black) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("SAS", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    NavChip("الحاسبة", page == "calculator") { page = "calculator" }
                    NavChip("تحويل", page == "currency") { page = "currency" }
                    NavChip("المزيد", page == "more") { page = "more" }
                }
            }
            Spacer(Modifier.height(12.dp))

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
    var amount by remember { mutableStateOf("100") }
    var from by remember { mutableStateOf("SAR") }
    var to by remember { mutableStateOf("USD") }
    var rate by remember { mutableStateOf<Double?>(null) }
    var status by remember { mutableStateOf("جارٍ تحميل سعر الصرف...") }
    var updated by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(from, to, refresh) {
        if (from == to) {
            rate = 1.0
            status = "العملتان متطابقتان"
            updated = ""
        } else {
            status = "جارٍ تحديث سعر الصرف..."
            try {
                val data = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("https://open.er-api.com/v6/latest/$from")
                        .build()
                    OkHttpClient().newCall(request).execute().use { response ->
                        if (!response.isSuccessful) error("HTTP ${response.code}")
                        JSONObject(response.body?.string() ?: "{}")
                    }
                }
                if (data.optString("result") != "success") error("Rate unavailable")
                rate = data.getJSONObject("rates").getDouble(to)
                status = "سعر صرف مرجعي مباشر"
                updated = data.optString("time_last_update_utc", "")
            } catch (e: Exception) {
                rate = null
                status = "تعذر جلب السعر. تحقق من الإنترنت وحاول التحديث."
                updated = ""
            }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("تحويل العملات", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("حوّل بين العملات بأسعار صرف محدثة", color = Color.Gray, fontSize = 13.sp)
        Spacer(Modifier.height(22.dp))

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = card),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("المبلغ", color = Color.LightGray, fontSize = 14.sp)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' }.take(16) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    placeholder = { Text("أدخل المبلغ") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = orange,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = orange
                    )
                )
                Spacer(Modifier.height(18.dp))
                Text("من", color = Color.LightGray, fontSize = 13.sp)
                CurrencyPicker(from) { from = it }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Button(
                        onClick = { val old = from; from = to; to = old },
                        colors = ButtonDefaults.buttonColors(containerColor = orange),
                        shape = CircleShape
                    ) {
                        Text("⇅  تبديل العملتين", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                Text("إلى", color = Color.LightGray, fontSize = 13.sp)
                CurrencyPicker(to) { to = it }
            }
        }

        Spacer(Modifier.height(18.dp))
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF202020)),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("النتيجة", color = Color.LightGray, fontSize = 15.sp)
                Text(
                    rate?.let { number.format((amount.toDoubleOrNull() ?: 0.0) * it) } ?: "—",
                    color = orange,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(to, color = Color.White, fontSize = 17.sp)
                Spacer(Modifier.height(8.dp))
                Text(status, color = Color.LightGray, fontSize = 12.sp, textAlign = TextAlign.Center)
                if (updated.isNotBlank()) Text("آخر تحديث للمصدر: $updated", color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.Center)
                TextButton(onClick = { refresh++ }) { Text("تحديث السعر ↻", color = orange) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("الأسعار مرجعية وقد تختلف عن أسعار البنوك والصرافة.", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
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
