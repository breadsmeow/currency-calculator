package com.yendollar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToLong

private val Bg = Color(0xFF0F1218)
private val Surface1 = Color(0xFF171B24)
private val Raised = Color(0xFF1F2430)
private val Line = Color(0xFF2A3040)
private val Fg = Color(0xFFEEF0F5)
private val Muted = Color(0xFF8A92A6)
private val Gold = Color(0xFFD9B56A)
private val GoldInk = Color(0xFF1A1408)
private val Good = Color(0xFF7FC59A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        RateWorker.schedule(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Surface1, primary = Gold)) {
                CalculatorScreen()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalculatorScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    var rate by remember { mutableDoubleStateOf(RateRepository.rate(context)) }
    var updatedAt by remember { mutableLongStateOf(RateRepository.updatedAt(context)) }
    var refreshing by remember { mutableStateOf(false) }
    var live by remember { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("10000") }
    var reversed by rememberSaveable { mutableStateOf(false) }

    fun refresh() {
        if (refreshing) return
        scope.launch {
            refreshing = true
            val ok = RateRepository.refresh(context)
            if (ok) {
                rate = RateRepository.rate(context)
                updatedAt = RateRepository.updatedAt(context)
                Fmt.refreshWidgets(context)
            }
            live = ok
            refreshing = false
        }
    }
    LaunchedEffect(Unit) { refresh() }

    val amount = input.toDoubleOrNull() ?: 0.0
    val result = if (reversed) amount * rate else amount / rate
    val resultText = if (reversed) Fmt.num(result, 0) else Fmt.num(result, 2)

    fun press(key: String) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        input = when (key) {
            "⌫" -> input.dropLast(1)
            "." -> if (reversed && !input.contains('.')) input.ifEmpty { "0" } + "." else input
            else -> {
                val decimals = input.substringAfter('.', "")
                when {
                    input.contains('.') && decimals.length >= 2 -> input
                    input.replace(".", "").length >= 12 -> input
                    input == "0" -> key
                    else -> input + key
                }
            }
        }
    }

    fun swap() {
        val converted = if (reversed) amount * rate else amount / rate
        reversed = !reversed
        input = if (reversed) {
            String.format(Locale.US, "%.2f", converted).trimEnd('0').trimEnd('.')
        } else {
            converted.roundToLong().toString()
        }
        if (input == "0") input = ""
    }

    val status = when {
        refreshing -> "Updating…"
        live -> "Live · ${Fmt.time(updatedAt)}"
        updatedAt != 0L -> "Offline · saved ${Fmt.time(updatedAt)}"
        else -> "Offline · sample rate"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Yen → Dollar", color = Fg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("JPY · USD", color = Muted, fontSize = 11.sp, letterSpacing = 1.5.sp)
        }

        val cardShape = RoundedCornerShape(22.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(Surface1)
                .border(1.dp, Line, cardShape)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            AmountRow(
                glyph = if (reversed) "$" else "¥",
                code = if (reversed) "USD" else "JPY",
                value = formatInput(input),
                primary = true
            )
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Raised)
                        .border(1.dp, Line, CircleShape)
                        .clickable { swap() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⇅", color = Fg, fontSize = 18.sp)
                }
            }
            AmountRow(
                glyph = if (reversed) "¥" else "$",
                code = if (reversed) "JPY" else "USD",
                value = resultText,
                primary = false
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(if (live) Good else Muted))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "\$1 = ¥${Fmt.num(rate, 2)}  ·  $status",
                color = Muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Refresh",
                color = if (refreshing) Muted else Gold,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = !refreshing) { refresh() }
                    .padding(8.dp)
            )
        }

        val quickValues = if (reversed) listOf(10, 50, 100, 500) else listOf(1000, 5000, 10000, 50000)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            quickValues.forEach { v ->
                val label = if (reversed) "\$$v" else "¥${v / 1000}k"
                val pill = RoundedCornerShape(50)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(pill)
                        .border(1.dp, Line, pill)
                        .clickable { input = v.toString() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = Fg, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        val rows = listOf(
            listOf("7", "8", "9"),
            listOf("4", "5", "6"),
            listOf("1", "2", "3"),
            listOf(".", "0", "⌫")
        )
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { key ->
                        val hidden = key == "." && !reversed
                        val keyModifier = if (hidden) Modifier else Modifier
                            .background(Surface1)
                            .combinedClickable(
                                onClick = { press(key) },
                                onLongClick = {
                                    if (key == "⌫") {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        input = ""
                                    } else {
                                        press(key)
                                    }
                                }
                            )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(18.dp))
                                .then(keyModifier),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!hidden) {
                                Text(
                                    text = key,
                                    color = if (key == "⌫") Muted else Fg,
                                    fontSize = if (key == "⌫") 22.sp else 26.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Light
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "Hold ⌫ to clear. Mid-market rate; banks and card fees will differ.",
            color = Muted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AmountRow(glyph: String, code: String, value: String, primary: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (primary) Gold else Raised),
            contentAlignment = Alignment.Center
        ) {
            Text(glyph, color = if (primary) GoldInk else Fg, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
        Spacer(Modifier.width(10.dp))
        Text(code, color = Muted, fontSize = 12.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(12.dp))
        val size = when {
            value.length > 13 -> if (primary) 22.sp else 18.sp
            value.length > 9 -> if (primary) 28.sp else 22.sp
            else -> if (primary) 36.sp else 28.sp
        }
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            color = if (primary) Fg else Gold,
            fontSize = size,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (primary) FontWeight.Normal else FontWeight.Light,
            maxLines = 1,
            softWrap = false
        )
    }
}

private fun formatInput(s: String): String {
    if (s.isEmpty()) return "0"
    val intPart = s.substringBefore('.').ifEmpty { "0" }
    val formatted = Fmt.num(intPart.toDouble(), 0)
    return if (s.contains('.')) formatted + "." + s.substringAfter('.') else formatted
}
