package com.witness.mobile.ui.screens

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.witness.core.net.ApiConfig
import com.witness.core.net.AsrClient
import com.witness.core.net.LlmClient
import com.witness.core.theme.Catppuccin
import com.witness.core.ui.IconBack
import com.witness.mobile.BuildConfig
import com.witness.mobile.R
import com.witness.mobile.WitnessApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as WitnessApp
    val scope = rememberCoroutineScope()

    var asrUrl by remember { mutableStateOf(BuildConfig.WITNESS_ASR_URL) }
    var asrModel by remember { mutableStateOf(BuildConfig.WITNESS_ASR_MODEL) }
    var asrKey by remember { mutableStateOf(BuildConfig.WITNESS_API_KEY) }
    var llmUrl by remember { mutableStateOf(BuildConfig.WITNESS_LLM_URL) }
    var llmModel by remember { mutableStateOf(BuildConfig.WITNESS_LLM_MODEL) }
    var llmKey by remember { mutableStateOf(BuildConfig.WITNESS_API_KEY) }
    var showAsrKey by remember { mutableStateOf(false) }
    var showLlmKey by remember { mutableStateOf(false) }
    var asrOk by remember { mutableStateOf<Boolean?>(null) }
    var llmOk by remember { mutableStateOf<Boolean?>(null) }
    var asrStatus by remember { mutableStateOf("") }
    var llmStatus by remember { mutableStateOf("") }
    var busyAsr by remember { mutableStateOf(false) }
    var busyLlm by remember { mutableStateOf(false) }
    var editAsr by remember { mutableStateOf(false) }
    var editLlm by remember { mutableStateOf(false) }
    var savedStatus by remember { mutableStateOf("") }
    val lang by app.settings.lang.collectAsState(initial = "en")

    LaunchedEffect(Unit) {
        val cfg = app.settings.config()
        if (cfg.asrUrl.isNotEmpty()) asrUrl = cfg.asrUrl
        if (cfg.asrModel.isNotEmpty()) asrModel = cfg.asrModel
        if (cfg.llmUrl.isNotEmpty()) llmUrl = cfg.llmUrl
        if (cfg.llmModel.isNotEmpty()) llmModel = cfg.llmModel
        if (cfg.asrApiKey.isNotEmpty()) asrKey = cfg.asrApiKey
        if (cfg.llmApiKey.isNotEmpty()) llmKey = cfg.llmApiKey
    }

    fun setLang(l: String) {
        scope.launch {
            app.settings.setLang(l)
            context.getSharedPreferences("witness_locale", android.content.Context.MODE_PRIVATE)
                .edit().putString("lang", l).apply()
            (context as? Activity)?.recreate()
        }
    }

    fun currentCfg() = ApiConfig(asrUrl, asrModel, llmUrl, llmModel, "", asrKey, llmKey)

    fun testAsr() {
        busyAsr = true; asrStatus = context.getString(R.string.settings_testing)
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                val clip = runCatching { context.assets.open("test_16k.wav").readBytes() }.getOrNull()
                runCatching { AsrClient(currentCfg()).transcribe(clip ?: ByteArray(0)) }
            }
            asrOk = res.isSuccess
            asrStatus = if (res.isSuccess) "OK: " + res.getOrNull()!!.text.take(40)
            else "ERR: " + (res.exceptionOrNull()?.message ?: "unknown")
            busyAsr = false
        }
    }

    fun testLlm() {
        busyLlm = true; llmStatus = context.getString(R.string.settings_testing)
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                runCatching { LlmClient(currentCfg()).analyze("Reply with the single word OK.") }
            }
            llmOk = res.isSuccess
            llmStatus = if (res.isSuccess) "OK: " + res.getOrNull()!!.title
            else "ERR: " + (res.exceptionOrNull()?.message ?: "unknown")
            busyLlm = false
        }
    }

    Column(Modifier.fillMaxSize().background(Catppuccin.Base).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(Catppuccin.Surface0).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                IconBack(Catppuccin.Subtext0, Modifier.size(20.dp))
            }
            Text(stringResource(R.string.settings_title), color = Catppuccin.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }

        GroupLabel(stringResource(R.string.settings_lang))
        SetCard {
            SetRow {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(Catppuccin.Lavender.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    IconGlobe(Catppuccin.Lavender, Modifier.size(18.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(if (lang == "ru") "Русский" else "English", color = Catppuccin.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.settings_langsub), color = Catppuccin.Overlay0, fontSize = 11.5.sp)
                }
                Row(Modifier.clip(CircleShape).background(Catppuccin.Mantle).border(1.dp, Catppuccin.Surface0, CircleShape).padding(2.dp)) {
                    LangToggle("RU", lang == "ru", "settings_lang_ru") { setLang("ru") }
                    LangToggle("EN", lang == "en", "settings_lang_en") { setLang("en") }
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        GroupLabel(stringResource(R.string.settings_conn))
        SetCard {
            ConnRow("ASR", asrModel, asrOk, Catppuccin.Blue, "settings_asr_row", { IconClockArc(it, Modifier.size(18.dp)) }) { editAsr = !editAsr }
            if (editAsr) {
                EditPanel {
                    EditField(stringResource(R.string.settings_asr_url), asrUrl, "settings_asr_url") { asrUrl = it }
                    Spacer(Modifier.height(10.dp))
                    EditField(stringResource(R.string.settings_asr_model), asrModel, "settings_asr_model") { asrModel = it }
                    Spacer(Modifier.height(10.dp))
                    KeyField(asrKey, showAsrKey, "settings_asr_key", "settings_asr_eye", { asrKey = it }, { showAsrKey = !showAsrKey })
                    Spacer(Modifier.height(12.dp))
                    TestButton("settings_asr_test", busyAsr, ::testAsr)
                    if (asrStatus.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(asrStatus, color = if (asrStatus.startsWith("ERR")) Catppuccin.Red else Catppuccin.Green, fontSize = 12.5.sp)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Catppuccin.Surface0))
            ConnRow("LLM", llmModel, llmOk, Catppuccin.Mauve, "settings_llm_row", { IconSpark(it, Modifier.size(18.dp)) }) { editLlm = !editLlm }
            if (editLlm) {
                EditPanel {
                    EditField(stringResource(R.string.settings_llm_url), llmUrl, "settings_llm_url") { llmUrl = it }
                    Spacer(Modifier.height(10.dp))
                    EditField(stringResource(R.string.settings_llm_model), llmModel, "settings_llm_model") { llmModel = it }
                    Spacer(Modifier.height(10.dp))
                    KeyField(llmKey, showLlmKey, "settings_llm_key", "settings_llm_eye", { llmKey = it }, { showLlmKey = !showLlmKey })
                    Spacer(Modifier.height(12.dp))
                    TestButton("settings_llm_test", busyLlm, ::testLlm)
                    if (llmStatus.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(llmStatus, color = if (llmStatus.startsWith("ERR")) Catppuccin.Red else Catppuccin.Green, fontSize = 12.5.sp)
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 30.dp)) {
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Catppuccin.Mauve)
                .clickable {
                    scope.launch {
                        app.settings.save(asrUrl, asrModel, llmUrl, llmModel, "", asrKey, llmKey)
                        savedStatus = context.getString(R.string.settings_saved)
                    }
                }.padding(horizontal = 20.dp, vertical = 13.dp)) {
                Text(stringResource(R.string.settings_save), color = Catppuccin.Crust, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (savedStatus.isNotEmpty()) {
            Text(savedStatus, color = Catppuccin.Green, fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))
        }
        Spacer(Modifier.height(26.dp))
    }
}

@Composable
private fun KeyField(value: String, show: Boolean, desc: String, eyeDesc: String, onChange: (String) -> Unit, onEye: () -> Unit) {
    Text(stringResource(R.string.settings_key), color = Catppuccin.Subtext0, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Catppuccin.Base).border(1.dp, Catppuccin.Surface1, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = value, onValueChange = onChange,
            modifier = Modifier.weight(1f).semantics { contentDescription = desc },
            singleLine = true,
            visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
            textStyle = TextStyle(color = Catppuccin.Text, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(Catppuccin.Mauve),
        )
        Spacer(Modifier.width(8.dp))
        Box(Modifier.clickable(onClick = onEye).semantics { contentDescription = eyeDesc }.padding(2.dp)) {
            IconEye(Catppuccin.Overlay0, Modifier.size(17.dp))
        }
    }
    if (value.isEmpty()) {
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.settings_hint), color = Catppuccin.Overlay0, fontSize = 12.sp)
    }
}

@Composable
private fun TestButton(desc: String, busy: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(12.dp)).background(Catppuccin.Surface0)
            .border(1.dp, Catppuccin.Surface2, RoundedCornerShape(12.dp))
            .clickable(enabled = !busy, onClick = onClick).semantics { contentDescription = desc }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(stringResource(R.string.settings_test), color = Catppuccin.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(text.uppercase(), color = Catppuccin.Overlay1, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.7.sp)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun SetCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Catppuccin.Mantle).border(1.dp, Catppuccin.Surface0, RoundedCornerShape(16.dp)), content = content)
}

@Composable
private fun SetRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
private fun LangToggle(label: String, on: Boolean, desc: String, onClick: () -> Unit) {
    Box(Modifier.clip(CircleShape).background(if (on) Catppuccin.Mauve else Color.Transparent)
        .clickable(onClick = onClick).semantics { contentDescription = desc }.padding(horizontal = 11.dp, vertical = 5.dp)) {
        Text(label, color = if (on) Catppuccin.Crust else Catppuccin.Subtext0, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ConnRow(label: String, model: String, ok: Boolean?, accent: Color, desc: String, icon: @Composable (Color) -> Unit, onClick: () -> Unit) {
    val c = when (ok) { true -> Catppuccin.Green; false -> Catppuccin.Red; null -> Catppuccin.Overlay0 }
    SetRow {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(accent.copy(alpha = 0.15f)).clickable(onClick = onClick).semantics { contentDescription = desc }, contentAlignment = Alignment.Center) {
            icon(accent)
        }
        Column(Modifier.weight(1f).clickable(onClick = onClick)) {
            Text(label, color = Catppuccin.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(model, color = Catppuccin.Overlay0, fontSize = 11.5.sp, fontFamily = FontFamily.Monospace)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(c))
            Text(when (ok) { true -> "OK"; false -> "ERR"; null -> "—" }, color = c, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EditPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(Catppuccin.Mantle).padding(horizontal = 16.dp, vertical = 12.dp), content = content)
}

@Composable
private fun EditField(label: String, value: String, desc: String, onChange: (String) -> Unit) {
    Text(label, color = Catppuccin.Subtext0, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(6.dp))
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Catppuccin.Base).border(1.dp, Catppuccin.Surface1, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) {
        BasicTextField(
            value = value, onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = desc },
            singleLine = true,
            textStyle = TextStyle(color = Catppuccin.Text, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(Catppuccin.Mauve),
        )
    }
}

@Composable
private fun IconGlobe(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val w = s * 0.08f
        drawCircle(color, radius = s * 0.42f, style = Stroke(width = w))
        drawLine(color, Offset(s * 0.08f, s * 0.5f), Offset(s * 0.92f, s * 0.5f), w)
        drawOval(color, topLeft = Offset(s * 0.28f, s * 0.08f), size = Size(s * 0.44f, s * 0.84f), style = Stroke(width = w))
    }
}

@Composable
private fun IconClockArc(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val w = s * 0.08f
        drawArc(color, startAngle = -90f, sweepAngle = 300f, useCenter = false, topLeft = Offset(s * 0.1f, s * 0.1f), size = Size(s * 0.8f, s * 0.8f), style = Stroke(width = w, cap = StrokeCap.Round))
        drawLine(color, Offset(s * 0.5f, s * 0.28f), Offset(s * 0.5f, s * 0.5f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.5f, s * 0.5f), Offset(s * 0.66f, s * 0.58f), w, StrokeCap.Round)
    }
}

@Composable
private fun IconSpark(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val p = Path().apply {
            moveTo(s * 0.5f, s * 0.12f)
            lineTo(s * 0.58f, s * 0.36f); lineTo(s * 0.83f, s * 0.42f)
            lineTo(s * 0.58f, s * 0.47f); lineTo(s * 0.5f, s * 0.71f)
            lineTo(s * 0.42f, s * 0.47f); lineTo(s * 0.17f, s * 0.42f)
            lineTo(s * 0.42f, s * 0.36f); close()
        }
        drawPath(p, color, style = Stroke(width = s * 0.07f, join = StrokeJoin.Round))
    }
}

@Composable
private fun IconEye(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val w = s * 0.08f
        val p = Path().apply {
            moveTo(s * 0.04f, s * 0.5f)
            cubicTo(s * 0.2f, s * 0.17f, s * 0.8f, s * 0.17f, s * 0.96f, s * 0.5f)
            cubicTo(s * 0.8f, s * 0.83f, s * 0.2f, s * 0.83f, s * 0.04f, s * 0.5f)
            close()
        }
        drawPath(p, color, style = Stroke(width = w, join = StrokeJoin.Round))
        drawCircle(color, radius = s * 0.13f, center = Offset(s * 0.5f, s * 0.5f), style = Stroke(width = w))
    }
}
