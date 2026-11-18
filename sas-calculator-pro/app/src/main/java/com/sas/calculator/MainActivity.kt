package com.sas.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.DecimalFormat
import kotlin.math.*

private val black=Color(0xFF000000); private val gray=Color(0xFF333333); private val pale=Color(0xFFA5A5A5); private val orange=Color(0xFFFF9F0A)
private val number=DecimalFormat("#,##0.########")
class MainActivity:ComponentActivity(){ override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.statusBarColor=android.graphics.Color.BLACK;window.navigationBarColor=android.graphics.Color.BLACK;setContent{MaterialTheme(colorScheme=darkColorScheme()){App()}}} }

@Composable fun App(){
 var page by remember{mutableStateOf("Calculator")}; var display by remember{mutableStateOf("0")}; var left by remember{mutableStateOf<Double?>(null)}; var op by remember{mutableStateOf("")}; var fresh by remember{mutableStateOf(false)}; var scientific by remember{mutableStateOf(false)}
 val history=remember{mutableStateListOf<String>()}
 Surface(Modifier.fillMaxSize(),color=black){Column(Modifier.fillMaxSize().padding(16.dp)){
 Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("SAS Calculator PRO",color=Color.White,fontWeight=FontWeight.Bold);TextButton(onClick={page=if(page=="Calculator")"Menu" else "Calculator"}){Text("☰",color=orange)}}
 when(page){
 "Calculator"->{Spacer(Modifier.weight(1f));Text(display,Modifier.fillMaxWidth().padding(bottom=16.dp),color=Color.White,fontSize=62.sp,textAlign=TextAlign.End,maxLines=1)
 TextButton(onClick={scientific=!scientific}){Text("Scientific",color=orange)}
 if(scientific) listOf(listOf("sin","cos","tan","√"),listOf("x²","π","ln","log")).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach{v->Key(v,gray,Modifier.weight(1f)){val x=display.toDoubleOrNull()?:0.0;display=number.format(when(v){"sin"->sin(Math.toRadians(x));"cos"->cos(Math.toRadians(x));"tan"->tan(Math.toRadians(x));"√"->sqrt(x);"x²"->x*x;"π"->PI;"ln"->ln(x);else->log10(x)});fresh=true}}}}
 listOf(listOf("AC","+/-","%","÷"),listOf("7","8","9","×"),listOf("4","5","6","−"),listOf("1","2","3","+"),listOf("0",".","⌫","=")).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach{v->Key(v,when(v){"AC","+/-","%"->pale;"÷","×","−","+","="->orange;else->gray},Modifier.weight(1f)){val n=display.toDoubleOrNull()?:0.0;when(v){"AC"->{display="0";left=null;op=""};"+/-"->if(display!="0")display=if(display.startsWith("-"))display.drop(1) else "-$display";"%"->display=number.format(n/100);"⌫"->display=if(display.length>1)display.dropLast(1) else "0";"."->if(!display.contains("."))display+=".";"="->{val a=left; if(a!=null){val result=when(op){"+"->a+n;"−"->a-n;"×"->a*n;else->if(n==0.0)Double.NaN else a/n};val out=number.format(result);history.add(0,"$a $op $n = $out");display=out;left=null;op="";fresh=true}};"+","−","×","÷"->{left=n;op=v;fresh=true};else->{display=if(fresh||display=="0")v else display+v;fresh=false}}}}}}
 }
 "Menu"->{Text("Modes",color=Color.White,fontSize=28.sp);listOf("Currency","Math Notes","History").forEach{TextButton(onClick={page=it}){Text(it,color=orange)}}}
 "Currency"->Currency()
 "Math Notes"->Notes()
 "History"->{Text("History",color=Color.White,fontSize=28.sp);history.forEach{TextButton(onClick={display=it.substringAfter("= ").trim();page="Calculator"}){Text(it,color=Color.White)}};TextButton(onClick={history.clear()}){Text("Clear",color=orange)}}
 }
 }}
}
@Composable fun Key(label:String,color:Color,modifier:Modifier,onClick:()->Unit){Box(modifier.padding(vertical=4.dp).height(68.dp).clip(CircleShape).background(color).clickable{onClick()},contentAlignment=Alignment.Center){Text(label,color=if(color==pale)Color.Black else Color.White,fontSize=27.sp)}}

@Composable fun Currency(){
 val currencies=listOf("SAR","USD","EUR","GBP","AED","KWD","BHD","QAR","OMR","EGP","JPY","CHF","CAD","AUD","TRY","INR","CNY")
 var amount by remember{mutableStateOf("100")};var from by remember{mutableStateOf("SAR")};var to by remember{mutableStateOf("USD")};var rate by remember{mutableStateOf<Double?>(null)};var message by remember{mutableStateOf("Fetching reference rate…")}
 LaunchedEffect(from,to){try{val pair=withContext(Dispatchers.IO){val request=Request.Builder().url("https://api.frankfurter.app/latest?from=$from&to=$to").build();OkHttpClient().newCall(request).execute().use{val j=JSONObject(it.body!!.string());j.getJSONObject("rates").getDouble(to)}};rate=pair;message="Reference rate"}catch(e:Exception){rate=null;message="Rate unavailable"}}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.Center){Text("Currency converter",color=Color.White,fontSize=28.sp);OutlinedTextField(amount,{amount=it},label={Text("Amount")});CurrencyMenu("From",from,currencies){from=it};TextButton(onClick={val t=from;from=to;to=t}){Text("⇅ Swap",color=orange)};CurrencyMenu("To",to,currencies){to=it};Text("Result",color=Color.Gray);Text(rate?.let{number.format((amount.toDoubleOrNull()?:0.0)*it)+" $to"}?:"—",color=orange,fontSize=34.sp);Text(message,color=Color.Gray,fontSize=12.sp);Text("Reference exchange rate; not a bank quote.",color=Color.Gray,fontSize=11.sp)}
}
@Composable fun CurrencyMenu(label:String,value:String,options:List<String>,change:(String)->Unit){var open by remember{mutableStateOf(false)};Box{TextButton(onClick={open=true}){Text("$label: $value",color=Color.White)};DropdownMenu(open,{open=false}){options.forEach{DropdownMenuItem(text={Text(it)},onClick={change(it);open=false})}}}}
@Composable fun Notes(){var expression by remember{mutableStateOf("2+2")};var result by remember{mutableStateOf("4")};var fn by remember{mutableStateOf("x²")};Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){Text("Math Notes",color=Color.White,fontSize=28.sp);OutlinedTextField(expression,{expression=it},label={Text("Arithmetic expression")});Button(onClick={result=basicEval(expression)}){Text("Calculate")};Text(result,color=orange,fontSize=32.sp);OutlinedTextField(fn,{fn=it},label={Text("Graph: x², x, sin(x), cos(x)")});Canvas(Modifier.fillMaxWidth().height(220.dp).background(Color(0xFF171717))){val mid=size.height/2;drawLine(Color.DarkGray,androidx.compose.ui.geometry.Offset(0f,mid),androidx.compose.ui.geometry.Offset(size.width,mid),1f);val points=(0..180).map{i->val x=(i-90)/15.0;val y=when(fn.trim()){"x²","x^2"->x*x/4;"sin(x)"->sin(x);"cos(x)"->cos(x);else->x};androidx.compose.ui.geometry.Offset(i/180f*size.width,mid-y.toFloat()*18f)};for(i in 0 until points.lastIndex)drawLine(orange,points[i],points[i+1],3f)}}}
fun basicEval(s:String):String{val m=Regex("""^\s*(-?\d+(?:\.\d+)?)\s*([+*/-])\s*(-?\d+(?:\.\d+)?)\s*$""").matchEntire(s)?:return "Enter a simple expression: 2+2";val a=m.groupValues[1].toDouble();val b=m.groupValues[3].toDouble();val v=when(m.groupValues[2]){"+"->a+b;"-"->a-b;"*"->a*b;else->if(b==0.0)Double.NaN else a/b};return number.format(v)}
