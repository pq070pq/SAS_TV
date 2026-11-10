package com.sas.tv

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Night = Color(0xFF071321)
private val Panel = Color(0xFF102438)
private val Mint = Color(0xFF00D6C8)

private data class TvApp(val label: String, val packageName: String)

class MainActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = 4 or 1024 or 4096
        setContent { SasTvHome() }
    }
}

@Composable
private fun SasTvHome() {
    val context = LocalContext.current
    var now by remember { mutableStateOf(Date()) }
    var showAll by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf("") }
    val apps = remember {
        val pm = context.packageManager
        val intents = listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        )
        intents.flatMap { intent ->
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL).map {
                TvApp(it.loadLabel(pm).toString(), it.activityInfo.packageName)
            }
        }.filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }
    LaunchedEffect(Unit) {
        while (true) { now = Date(); delay(30_000) }
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF102B43), Night, Color(0xFF030912)))
            ).padding(horizontal = 54.dp, vertical = 34.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("SAS TV", color = Mint, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("شاشتك.. بطريقتك", color = Color.White.copy(alpha = .72f), fontSize = 13.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(SimpleDateFormat("hh:mm a", Locale("ar")).format(now), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Light)
                        Text(SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar")).format(now), color = Color.White.copy(alpha = .65f), fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(38.dp))
                Text(if (showAll) "كل التطبيقات" else "تطبيقاتك", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(18.dp))
                val shown = if (showAll) apps else apps.take(6)
                if (shown.isEmpty()) {
                    Text("لم يتم العثور على تطبيقات بعد", color = Color.White.copy(alpha = .7f))
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(shown, key = { it.packageName }) { app ->
                            AppTile(app, focused == app.packageName, { focused = app.packageName }) {
                                context.packageManager.getLaunchIntentForPackage(app.packageName)?.let(context::startActivity)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionTile("كل التطبيقات", Icons.Default.Apps) { showAll = !showAll }
                    ActionTile("الإعدادات", Icons.Default.Settings) {
                        context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                    ActionTile("الشبكة", Icons.Default.Wifi) {
                        context.startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("واجهة خفيفة • تحكم بالريموت • خصوصيتك أولاً", color = Color.White.copy(alpha = .42f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AppTile(app: TvApp, selected: Boolean, onFocus: () -> Unit, onClick: () -> Unit) {
    var hasFocus by remember { mutableStateOf(false) }
    Column(
        Modifier.width(142.dp).height(116.dp).clip(RoundedCornerShape(18.dp))
            .background(if (hasFocus || selected) Color(0xFF1A5260) else Panel)
            .onFocusChanged { hasFocus = it.isFocused; if (it.isFocused) onFocus() }
            .clickable(onClick = onClick).focusable().padding(14.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(app.label.take(2), color = Mint, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(app.label, color = Color.White, fontSize = 13.sp, maxLines = 2)
    }
}

@Composable
private fun ActionTile(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    var hasFocus by remember { mutableStateOf(false) }
    Row(
        Modifier.clip(RoundedCornerShape(12.dp)).background(if (hasFocus) Color(0xFF1A5260) else Panel)
            .onFocusChanged { hasFocus = it.isFocused }.clickable(onClick = onClick).focusable()
            .padding(horizontal = 17.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Mint, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = Color.White, fontSize = 13.sp)
    }
}
