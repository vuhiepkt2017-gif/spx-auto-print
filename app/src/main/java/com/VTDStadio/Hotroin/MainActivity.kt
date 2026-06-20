package com.VTDStadio.Hotroin

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// Theme Design System Tokens
val ThemeBgColorDark = Color(0xFF0C0D24)
val ThemeBgColorLight = Color(0xFFF5F6FC)
val ThemeSurfaceColorDark = Color(0xFF131536)
val ThemeSurfaceColorLight = Color(0xFFFFFFFF)

val LocalThemeIsDark = compositionLocalOf { true }

val ThemePrimary: Color
    @Composable
    get() = if (LocalThemeIsDark.current) Color(0xFFB19FFB) else Color(0xFF4A34E0)

val ThemeAccent: Color
    @Composable
    get() = if (LocalThemeIsDark.current) Color(0xFFFFB8D1) else Color(0xFFB5007F)

val ThemeCyan: Color
    @Composable
    get() = if (LocalThemeIsDark.current) Color(0xFF80DEEA) else Color(0xFF0097A7)

val GlassBorderDark = Color(0xFF282B5A)
val GlassBorderLight = Color(0xFFE2E7F5)
val TextDarkColor = Color(0xFF141933)
val TextLightColor = Color(0xFFE5EEFC)

fun getThemeBgColor(isDark: Boolean): Color = if (isDark) ThemeBgColorDark else ThemeBgColorLight
fun getThemeSurfaceColor(isDark: Boolean): Color = if (isDark) ThemeSurfaceColorDark else ThemeSurfaceColorLight
fun getGlassBorderColor(isDark: Boolean): Color = if (isDark) GlassBorderDark else GlassBorderLight
fun getTextColor(isDark: Boolean): Color = if (isDark) TextLightColor else TextDarkColor

@Composable
fun getButtonTextColor(): Color = if (LocalThemeIsDark.current) Color(0xFF0C0D24) else Color.White

@Composable
fun getButtonGradientBrush(isDark: Boolean): Brush {
    val startColor = if (isDark) Color(0xFFC5B8FF) else Color(0xFF8575F9)
    val endColor = if (isDark) Color(0xFFFFD1E3) else Color(0xFFEA8BCB)
    return Brush.linearGradient(
        listOf(
            startColor,
            endColor
        )
    )
}

fun getThemeBgBrush(isDark: Boolean): Brush {
    return if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0B0A1F),
                Color(0xFF131133)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF4F6FD),
                Color(0xFFE6EAF9)
            )
        )
    }
}

@Composable
fun Premium3DButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerBrush: Brush,
    shadowColor: Color,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val offset by animateDpAsState(
        targetValue = if (isPressed && enabled) 2.dp else 6.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pressOffset"
    )

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    ) {
        // Shadow/Bottom depth layer (renders the 3D part)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(
                    color = if (enabled) shadowColor else Color.Gray.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
        )

        // Foreground top layer (the pressable surface)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .offset(y = -offset)
                .background(
                    brush = if (enabled) containerBrush else Brush.verticalGradient(
                        colors = listOf(Color.LightGray, Color.Gray)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .drawWithContent {
                    drawContent()
                    if (enabled) {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.15f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx(), 12.dp.toPx())
                        )
                    }
                }
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                content()
            }
        }
    }
}

@Composable
fun Premium3DButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color,
    shadowColor: Color,
    content: @Composable RowScope.() -> Unit
) {
    val isDark = LocalThemeIsDark.current
    // In light mode, make gradients much softer/lighter (0.45f -> 0.25f)
    val c1 = if (isDark) containerColor.copy(alpha = 0.65f) else containerColor.copy(alpha = 0.4f)
    val c2 = if (isDark) containerColor.copy(alpha = 0.45f) else containerColor.copy(alpha = 0.25f)
    Premium3DButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        containerBrush = Brush.linearGradient(listOf(c1, c2)),
        shadowColor = if (isDark) shadowColor.copy(alpha = 0.5f) else shadowColor.copy(alpha = 0.3f), // softer shadow in light mode
        content = content
    )
}

fun extractSpxCodes(textContent: String): List<String> {
    val regex = Regex("([A-Za-z0-9/\\\\|._-]{8,25})")
    val foundCodes = mutableListOf<String>()
    if (textContent.isBlank()) return foundCodes
    
    val lines = textContent.split("\n")
    for (line in lines) {
        val stripped = line.replace("\\s".toRegex(), "")
        val matches = regex.findAll(stripped)
        for (match in matches) {
            var rawCode = match.value.uppercase(Locale.ROOT)
            var code = rawCode.replace(Regex("[^A-Z0-9]"), "")
            
            // Sửa lỗi OCR viết tay/in mờ phổ biến:
            if (code.startsWith("5PX")) {
                code = "SPX" + code.substring(3)
            }
            if (code.startsWith("SPV") || code.startsWith("5PV")) {
                code = "SPX" + code.substring(3)
            }
            code = code.replace("Р", "P")
            if (code.startsWith("SPXVN") && code.length == 17 && code.endsWith("8")) {
                code = code.substring(0, 16) + "B"
            }
            
            // Tự động sửa lỗi nhận diện chữ cái thành số trong phần thân số của mã SPX
            val prefix = when {
                code.startsWith("SPXVN") -> "SPXVN"
                code.startsWith("SPX") -> "SPX"
                else -> null
            }
            if (prefix != null && code.length > prefix.length) {
                val length = code.length
                val middleEnd = if (prefix == "SPXVN" && length == 17) 16 else length
                val middlePart = code.substring(prefix.length, middleEnd)
                val cleanedMiddle = middlePart.map { char ->
                    when (char) {
                        'O' -> '0'
                        'I', 'L', 'l' -> '1'
                        'S' -> '5'
                        'G' -> '6'
                        'Z' -> '2'
                        else -> char
                    }
                }.joinToString("")
                val suffix = if (prefix == "SPXVN" && length == 17) code.substring(16) else ""
                code = prefix + cleanedMiddle + suffix
            }
            
            if (code.length >= 8) {
                foundCodes.add(code)
            }
        }
    }
    return foundCodes.distinct()
}

fun extractTargetCodes(text: String): List<String> {
    val cleanedText = text.uppercase(Locale.ROOT)
    val found = mutableListOf<String>()
    
    // Split by common delimiters to find individual words/codes
    val words = cleanedText.split(Regex("[\\s,;.:|()\\-\\\\]+"))
    for (word in words) {
        var cleanWord = word.replace(Regex("[^A-Z0-9]"), "")
        
        // Correct prefix-level OCR errors
        if (cleanWord.startsWith("0PXVN") || cleanWord.startsWith("OPXVN") || cleanWord.startsWith("5PXVN")) {
            cleanWord = "SPXVN" + cleanWord.substring(5)
        } else if (cleanWord.startsWith("0PX") || cleanWord.startsWith("OPX") || cleanWord.startsWith("5PX")) {
            cleanWord = "SPX" + cleanWord.substring(3)
        } else if (cleanWord.startsWith("UN") && cleanWord.length >= 10) {
            cleanWord = "VN" + cleanWord.substring(2)
        }
        
        if (cleanWord.startsWith("SPXVN")) {
            val digitsPart = cleanWord.substring(5)
            val cleanedDigits = digitsPart.map { char ->
                when (char) {
                    'O' -> '0'
                    'I', 'L', 'l' -> '1'
                    'S' -> '5'
                    'G' -> '6'
                    'Z' -> '2'
                    else -> char
                }
            }.joinToString("")
            val finalCode = "SPXVN" + cleanedDigits
            if (finalCode.matches(Regex("SPXVN\\d+")) && finalCode.length >= 10) {
                found.add(finalCode)
            }
        } else if (cleanWord.startsWith("VN")) {
            val digitsPart = cleanWord.substring(2)
            val cleanedDigits = digitsPart.map { char ->
                when (char) {
                    'O' -> '0'
                    'I', 'L', 'l' -> '1'
                    'S' -> '5'
                    'G' -> '6'
                    'Z' -> '2'
                    else -> char
                }
            }.joinToString("")
            val finalCode = "VN" + cleanedDigits
            if (finalCode.matches(Regex("VN\\d+")) && finalCode.length >= 10) {
                found.add(finalCode)
            }
        } else if (cleanWord.startsWith("SPX")) {
            val digitsPart = cleanWord.substring(3)
            val cleanedDigits = digitsPart.map { char ->
                when (char) {
                    'O' -> '0'
                    'I', 'L', 'l' -> '1'
                    'S' -> '5'
                    'G' -> '6'
                    'Z' -> '2'
                    else -> char
                }
            }.joinToString("")
            val finalCode = "SPX" + cleanedDigits
            if (finalCode.matches(Regex("SPX\\d+")) && finalCode.length >= 10) {
                found.add(finalCode)
            }
        }
    }
    
    // Fallback regex scan on the dense text in case delimiters merged them
    if (found.isEmpty()) {
        val mergedText = cleanedText.replace("\\s".toRegex(), "")
        val matchesSpxvn = Regex("SPXVN[0-9OILSGZ]{8,15}").findAll(mergedText)
        for (m in matchesSpxvn) {
            val raw = m.value
            val digitsPart = raw.substring(5)
            val cleanedDigits = digitsPart.map { char ->
                when (char) {
                    'O' -> '0'
                    'I', 'L', 'l' -> '1'
                    'S' -> '5'
                    'G' -> '6'
                    'Z' -> '2'
                    else -> char
                }
            }.joinToString("")
            found.add("SPXVN" + cleanedDigits)
        }
        
        val matchesVn = Regex("VN[0-9OILSGZ]{8,15}").findAll(mergedText)
        for (m in matchesVn) {
            val raw = m.value
            val digitsPart = raw.substring(2)
            val cleanedDigits = digitsPart.map { char ->
                when (char) {
                    'O' -> '0'
                    'I', 'L', 'l' -> '1'
                    'S' -> '5'
                    'G' -> '6'
                    'Z' -> '2'
                    else -> char
                }
            }.joinToString("")
            found.add("VN" + cleanedDigits)
        }
    }
    
    return found.distinct()
}

class MainActivity : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        
        // Uncaught Exception Handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val prefs = getSharedPreferences("ShopeeAutoPrintPrefs", Context.MODE_PRIVATE)
                val sw = java.io.StringWriter()
                throwable.printStackTrace(java.io.PrintWriter(sw))
                val stackTrace = sw.toString()
                prefs.edit().putString("last_crash_trace", stackTrace).commit()
                Log.e("ShopeeAutoPrintCrash", "CRASH DETECTED: $stackTrace")
            } catch (e: Exception) {
                // Ignore
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        sharedPreferences = getSharedPreferences("ShopeeAutoPrintPrefs", Context.MODE_PRIVATE)
        cameraExecutor = Executors.newSingleThreadExecutor()

        val isLoggedInVal = sharedPreferences.getBoolean("is_logged_in", false)
        if (isLoggedInVal) {
            val serviceIntent = Intent(this, BackgroundNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }

        setContent {
            var isDark by remember { mutableStateOf(sharedPreferences.getBoolean("is_dark_theme", true)) }
            CompositionLocalProvider(LocalThemeIsDark provides isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = getThemeBgColor(isDark)
                ) {
                    ShopeeAutoPrintApp(sharedPreferences, cameraExecutor) { newTheme ->
                        isDark = newTheme
                        sharedPreferences.edit().putBoolean("is_dark_theme", newTheme).apply()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

data class PrintHistoryItem(
    val code: String,
    val timestamp: String,
    val status: String,
    val errorMessage: String? = null
)

data class SupportPushItem(
    val docId: String,
    val pupCode: String,
    val senderName: String,
    val receiver: String,
    val status: String
)

@Composable
fun AIParticleBackground() {
    val isDark = LocalThemeIsDark.current
    val glow1 = if (isDark) Color(0xFFB19FFB).copy(alpha = 0.15f) else Color(0xFFE3DCFF).copy(alpha = 0.6f)
    val glow2 = if (isDark) Color(0xFFFFB8D1).copy(alpha = 0.12f) else Color(0xFFFFE3EE).copy(alpha = 0.5f)
    val glow3 = if (isDark) Color(0xFF80DEEA).copy(alpha = 0.12f) else Color(0xFFE0F7FA).copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val w = size.width
                val h = size.height
                if (w == 0f || h == 0f) {
                    onDrawBehind {}
                } else {
                    val brush1 = Brush.radialGradient(
                        colors = listOf(glow1, Color.Transparent),
                        center = Offset(w * 0.1f, h * 0.1f),
                        radius = w * 0.8f
                    )
                    val brush2 = Brush.radialGradient(
                        colors = listOf(glow2, Color.Transparent),
                        center = Offset(w * 0.9f, h * 0.8f),
                        radius = w * 0.7f
                    )
                    val brush3 = Brush.radialGradient(
                        colors = listOf(glow3, Color.Transparent),
                        center = Offset(w * 0.5f, h * 0.5f),
                        radius = w * 0.6f
                    )
                    onDrawBehind {
                        drawCircle(brush = brush1, radius = w * 0.8f, center = Offset(w * 0.1f, h * 0.1f))
                        drawCircle(brush = brush2, radius = w * 0.7f, center = Offset(w * 0.9f, h * 0.8f))
                        drawCircle(brush = brush3, radius = w * 0.6f, center = Offset(w * 0.5f, h * 0.5f))
                    }
                }
            }
    )
}

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    borderColor: Color = Color.Transparent,
    glowColor: Color = Color.Transparent,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalThemeIsDark.current
    val surfaceColor = getThemeSurfaceColor(isDark).copy(alpha = if (isDark) 0.65f else 0.85f)
    val borderStrokeColor = if (borderColor != Color.Transparent) borderColor else getGlassBorderColor(isDark)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(surfaceColor)
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            borderStrokeColor.copy(alpha = 0.8f),
                            borderStrokeColor.copy(alpha = 0.2f)
                        )
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopeeAutoPrintApp(
    sharedPreferences: SharedPreferences,
    cameraExecutor: ExecutorService,
    onThemeToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = LocalThemeIsDark.current

    // Authentication States
    var showSplashScreen by remember { mutableStateOf(true) }
    var isLoggedIn by remember { mutableStateOf(sharedPreferences.getBoolean("is_logged_in", false)) }
    var currentUserEmail by remember { mutableStateOf(sharedPreferences.getString("user_email", "") ?: "") }
    var currentUserName by remember { mutableStateOf(sharedPreferences.getString("user_name", "") ?: "") }
    var currentUserRole by remember { mutableStateOf(sharedPreferences.getString("user_role", "") ?: "") }
    var currentRiderCode by remember { mutableStateOf(sharedPreferences.getString("rider_code", "") ?: "") }

    var isPrintLocked by remember { mutableStateOf(false) }
    var isChuyenPickLocked by remember { mutableStateOf(false) }
    var printLockedUntil by remember { mutableStateOf<Date?>(null) }
    var chuyenPickLockedUntil by remember { mutableStateOf<Date?>(null) }
    var chuyenPickLockedReports by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    // App Configurations (Admin Synced)
    var webAppUrl by remember { mutableStateOf("https://script.google.com/macros/s/AKfycbzdPN27raSNnCeH19dpKUYzWmccLkCjFyoLjKlxEeKg3cLOJA4JezXZbboEgG6g0GpQZA/exec") }
    var autoSubmitOnScan by remember { mutableStateOf(sharedPreferences.getBoolean("auto_submit_on_scan", false)) }
    var sheetUsername by remember { mutableStateOf(sharedPreferences.getString("sheet_username", "") ?: "") }
    var sheetPassword by remember { mutableStateOf(sharedPreferences.getString("sheet_password", "") ?: "") }
    var currentRiderRealName by remember { mutableStateOf(sharedPreferences.getString("rider_real_name", "") ?: "") }

    // Active Screen States
    var activeTab by remember { mutableStateOf(0) } // 0: Quét & In, 1: Quản trị (Chỉ Admin)
    var orderCode by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    // Camera Scan States
    var showCameraScanner by remember { mutableStateOf(false) }
    var isOcrMode by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var showAppInfoDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    // Checking for previous crash details
    var crashTrace by remember { 
        mutableStateOf(sharedPreferences.getString("last_crash_trace", "") ?: "") 
    }

    if (crashTrace.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Ứng dụng bị lỗi đột ngột trước đó", color = Color(0xFFFF5722), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Vui lòng chụp ảnh hoặc sao chép toàn bộ lỗi dưới đây gửi cho kỹ thuật:", color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = crashTrace,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF5722),
                            focusedLabelColor = Color(0xFFFF5722)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Crash Trace", crashTrace)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Đã sao chép lỗi!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Log.e("ShopeeAutoPrint", "Lỗi sao chép clipboard", e)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                ) {
                    Text("Sao chép")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        sharedPreferences.edit().remove("last_crash_trace").apply()
                        crashTrace = ""
                    }
                ) {
                    Text("Bỏ qua", color = Color.Gray)
                }
            }
        )
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            showCameraScanner = true
        } else {
            Toast.makeText(context, "Quyền Camera bị từ chối", Toast.LENGTH_SHORT).show()
        }
    }

    // Photo selection scanner
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        val inputImage = InputImage.fromBitmap(bitmap, 0)
                        
                        // Step 1: Scan for QR and Barcodes first
                        val barcodeScanner = BarcodeScanning.getClient()
                        barcodeScanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                val found = mutableListOf<String>()
                                
                                // Priority 1: QR code values
                                val qrCodes = barcodes.filter { it.format == com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE }
                                for (qr in qrCodes) {
                                    val rawValue = qr.rawValue ?: ""
                                    found.addAll(extractTargetCodes(rawValue))
                                }
                                
                                // Priority 2: 1D barcodes/other barcode values (if no codes found in QR)
                                if (found.isEmpty()) {
                                    val otherBarcodes = barcodes.filter { it.format != com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE }
                                    for (bc in otherBarcodes) {
                                        val rawValue = bc.rawValue ?: ""
                                        found.addAll(extractTargetCodes(rawValue))
                                    }
                                }
                                
                                if (found.isNotEmpty()) {
                                    coroutineScope.launch(Dispatchers.Main) {
                                        val existingList = orderCode.split("\n").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
                                        var added = 0
                                        found.distinct().forEach { code ->
                                            if (!existingList.contains(code)) {
                                                existingList.add(code)
                                                added++
                                            }
                                        }
                                        orderCode = existingList.joinToString("\n")
                                        Toast.makeText(context, "Đã quét QR/Barcode thành công, thêm $added mã!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    // Priority 3: Fallback to OCR Text Recognition if barcode scanning yielded nothing
                                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                                    recognizer.process(inputImage)
                                        .addOnSuccessListener { visionText ->
                                            val textContent = visionText.text
                                            val foundOcr = extractTargetCodes(textContent)
                                            coroutineScope.launch(Dispatchers.Main) {
                                                if (foundOcr.isNotEmpty()) {
                                                    val existingList = orderCode.split("\n").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
                                                    var added = 0
                                                    foundOcr.distinct().forEach { code ->
                                                        if (!existingList.contains(code)) {
                                                            existingList.add(code)
                                                            added++
                                                        }
                                                    }
                                                    orderCode = existingList.joinToString("\n")
                                                    Toast.makeText(context, "Đã quét chữ (OCR) thành công, thêm $added mã!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Không tìm thấy mã SPX/VN trong ảnh!", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            coroutineScope.launch(Dispatchers.Main) {
                                                Toast.makeText(context, "Lỗi nhận dạng chữ: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                // If barcode scan completely failed, try OCR as last resort
                                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                                recognizer.process(inputImage)
                                    .addOnSuccessListener { visionText ->
                                        val textContent = visionText.text
                                        val foundOcr = extractTargetCodes(textContent)
                                        coroutineScope.launch(Dispatchers.Main) {
                                            if (foundOcr.isNotEmpty()) {
                                                val existingList = orderCode.split("\n").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
                                                var added = 0
                                                foundOcr.distinct().forEach { code ->
                                                    if (!existingList.contains(code)) {
                                                        existingList.add(code)
                                                        added++
                                                    }
                                                }
                                                orderCode = existingList.joinToString("\n")
                                                Toast.makeText(context, "Đã quét chữ (OCR) thành công, thêm $added mã!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Không tìm thấy mã SPX/VN trong ảnh!", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                            }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Không thể đọc ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    var historyList by remember { mutableStateOf(listOf<PrintHistoryItem>()) }
    val db = remember { FirebaseFirestore.getInstance() }

    var lastPrintTime by remember { mutableStateOf(0L) }
    var isPollingActive by remember { mutableStateOf(false) }
    var notifiedErrors by remember { mutableStateOf(setOf<String>()) }

    fun triggerVibration() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(100)
                }
            }
        } catch (e: Exception) {
            Log.e("ShopeeAutoPrint", "Lỗi rung", e)
        }
    }

    fun updateHistoryList(newList: List<PrintHistoryItem>) {
        historyList = newList
        if (currentUserEmail.isNotBlank()) {
            val itemsMap = newList.map { item ->
                mapOf(
                    "code" to item.code,
                    "timestamp" to item.timestamp,
                    "status" to item.status,
                    "errorMessage" to (item.errorMessage ?: "")
                )
            }
            db.collection("print_history").document(currentUserEmail)
                .set(mapOf("items" to itemsMap))
                .addOnFailureListener { e ->
                    Log.e("ShopeeAutoPrint", "Error saving history to firestore", e)
                }
        }
    }

    // Helper to parse dates and clean old history for Rider role
    fun cleanOldHistoryIfNeeded(currentRole: String, currentList: List<PrintHistoryItem>): List<PrintHistoryItem> {
        if (currentRole == "Admin") return currentList
        val sdf = SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault())
        val now = Date()
        val sixtyDaysInMillis = 60L * 24 * 60 * 60 * 1000
        return currentList.filter { item ->
            try {
                val itemDate = sdf.parse(item.timestamp)
                if (itemDate != null) {
                    // Set correct year since HH:mm:ss dd/MM doesn't have a year
                    val calItem = java.util.Calendar.getInstance().apply { time = itemDate }
                    val calNow = java.util.Calendar.getInstance().apply { time = now }
                    calItem.set(java.util.Calendar.YEAR, calNow.get(java.util.Calendar.YEAR))
                    
                    // If the item date parsed is somehow in the future compared to now, it might be from last year
                    if (calItem.timeInMillis > calNow.timeInMillis) {
                        calItem.add(java.util.Calendar.YEAR, -1)
                    }
                    val diff = calNow.timeInMillis - calItem.timeInMillis
                    diff < sixtyDaysInMillis
                } else {
                    true
                }
            } catch (e: Exception) {
                true // Keep if parsing fails to avoid losing items unexpectedly
            }
        }
    }

    // Load history when user changes (login/logout/launch)
    LaunchedEffect(currentUserEmail) {
        if (currentUserEmail.isNotBlank()) {
            db.collection("print_history").document(currentUserEmail).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val items = doc.get("items") as? List<Map<String, Any>>
                        if (items != null) {
                            val loadedList = items.map { map ->
                                PrintHistoryItem(
                                    code = map["code"]?.toString() ?: "",
                                    timestamp = map["timestamp"]?.toString() ?: "",
                                    status = map["status"]?.toString() ?: "",
                                    errorMessage = map["errorMessage"]?.toString()?.takeIf { it.isNotEmpty() }
                                )
                            }
                            historyList = cleanOldHistoryIfNeeded(currentUserRole, loadedList)
                        }
                    } else {
                        historyList = emptyList()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ShopeeAutoPrint", "Error loading history from firestore", e)
                }
        } else {
            historyList = emptyList()
        }
    }

    // Clean old history on launch or login role resolution
    LaunchedEffect(currentUserRole, historyList.size) {
        val cleaned = cleanOldHistoryIfNeeded(currentUserRole, historyList)
        if (cleaned.size != historyList.size) {
            updateHistoryList(cleaned)
        }
    }

    // Periodic check for error codes from Google Sheet WebApp
    LaunchedEffect(currentUserEmail, webAppUrl, isPollingActive) {
        if (currentUserEmail.isBlank() || webAppUrl.isBlank() || !isPollingActive) return@LaunchedEffect
        
        while (isPollingActive) {
            // Auto timeout fast polling after 1 minute (60 seconds) of inactivity
            if (System.currentTimeMillis() - lastPrintTime > 60000L) {
                isPollingActive = false
                break
            }

            try {
                val emailToSend = currentUserEmail.ifBlank { sharedPreferences.getString("user_email", "") ?: "unregistered@gmail.com" }
                val nameToSend = currentUserName.ifBlank { sharedPreferences.getString("user_name", "") ?: "Unregistered" }
                val codeToSend = currentRiderCode.ifBlank { sharedPreferences.getString("rider_code", "") ?: "NONE" }
                val finalRiderRealName = sharedPreferences.getString("rider_real_name", "") ?: currentRiderRealName
                val formattedRiderName = if (currentUserRole == "Admin") {
                    nameToSend
                } else if (finalRiderRealName.isNotBlank()) {
                    "$finalRiderRealName ( $codeToSend - $nameToSend )"
                } else {
                    "$nameToSend ( $codeToSend - $nameToSend )"
                }
                val passToSend = sheetPassword.ifBlank { sharedPreferences.getString("sheet_password", "") ?: "NOPASSWORD" }

                val bodyJson = JSONObject().apply {
                    put("action", "get_errors")
                    put("email", emailToSend)
                    put("riderName", formattedRiderName)
                    put("riderCode", codeToSend)
                    put("password", passToSend)
                }

                val body = bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(webAppUrl)
                    .post(body)
                    .build()

                val client = OkHttpClient()
                withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val responseBody = response.body?.string()
                            if (responseBody != null) {
                                val respJson = JSONObject(responseBody)
                                val statusVal = respJson.optString("status")
                                if (statusVal == "success") {
                                    val errArray = respJson.optJSONArray("errorCodes")
                                    if (errArray != null && errArray.length() > 0) {
                                        val errList = mutableListOf<String>()
                                        for (i in 0 until errArray.length()) {
                                            errList.add(errArray.getString(i).uppercase(Locale.ROOT))
                                        }

                                        withContext(Dispatchers.Main) {
                                            var historyChanged = false
                                            val newNotified = notifiedErrors.toMutableSet()
                                            val updatedHistory = historyList.map { item ->
                                                val cleanItemCode = item.code.uppercase(Locale.ROOT)
                                                if (errList.contains(cleanItemCode)) {
                                                    val hasNotified = newNotified.contains(cleanItemCode)
                                                    if (!hasNotified && !item.status.contains("lỗi") && !item.status.contains("Lỗi") && !item.status.contains("Thất bại")) {
                                                        historyChanged = true
                                                        triggerVibration()
                                                        newNotified.add(cleanItemCode)
                                                        item.copy(status = "Mã lỗi", errorMessage = "Gặp lỗi in trên Sheet")
                                                    } else if (item.status != "Mã lỗi") {
                                                        historyChanged = true
                                                        item.copy(status = "Mã lỗi", errorMessage = "Gặp lỗi in trên Sheet")
                                                    } else {
                                                        item
                                                    }
                                                } else {
                                                    item
                                                }
                                            }
                                            if (historyChanged) {
                                                notifiedErrors = newNotified
                                                updateHistoryList(updatedHistory)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ShopeeAutoPrint", "Error fetching error codes: ${e.message}")
            }
            
            // Poll every 3 seconds while active
            delay(3000L)
        }
    }

    // Listen to firestore configurations (WebApp URL and sheet credentials)
    var configListener by remember { mutableStateOf<ListenerRegistration?>(null) }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            configListener?.remove()
            configListener = db.collection("config").document("app_settings")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseConfig", "Error listening to config changes", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val url = snapshot.getString("webAppUrl")
                        if (!url.isNullOrBlank()) {
                            webAppUrl = url
                        }
                        val user = snapshot.getString("sheetUsername")
                        if (user != null) {
                            sheetUsername = user
                            sharedPreferences.edit().putString("sheet_username", user).apply()
                        }
                        val pass = snapshot.getString("sheetPassword")
                        if (pass != null) {
                            sheetPassword = pass
                            sharedPreferences.edit().putString("sheet_password", pass).apply()
                        }
                    }
                }
        } else {
            configListener?.remove()
            configListener = null
        }
    }

    // Safeguard: Lock Rider role out of Admin Dashboard tab
    LaunchedEffect(currentUserRole) {
        if (currentUserRole != "Admin" && activeTab == 2) {
            activeTab = 0
        }
    }

    var userNotifications by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    DisposableEffect(isLoggedIn, currentUserEmail) {
        var userLockListener: ListenerRegistration? = null
        var userNotifListener: ListenerRegistration? = null
        if (isLoggedIn && currentUserEmail.isNotBlank()) {
            userLockListener = db.collection("active_riders").document(currentUserEmail)
                .addSnapshotListener { snapshot, error ->
                    if (snapshot != null && snapshot.exists()) {
                        isPrintLocked = snapshot.getBoolean("isPrintLocked") ?: false
                        isChuyenPickLocked = snapshot.getBoolean("isChuyenPickLocked") ?: false
                        printLockedUntil = snapshot.getDate("printLockedUntil")
                        chuyenPickLockedUntil = snapshot.getDate("chuyenPickLockedUntil")
                        val reportsList = snapshot.get("reports") as? List<Map<String, Any>>
                        chuyenPickLockedReports = reportsList ?: emptyList()
                        
                        // Dynamically update user role from Firestore if it changes or exists
                        val serverRole = snapshot.getString("role")
                        if (!serverRole.isNullOrBlank() && serverRole != currentUserRole) {
                            currentUserRole = serverRole
                            sharedPreferences.edit().putString("user_role", serverRole).apply()
                        }
                    }
                }
            userNotifListener = db.collection("active_riders").document(currentUserEmail)
                .collection("notifications")
                .addSnapshotListener { snapshot, error ->
                    if (snapshot != null) {
                        val notifs = snapshot.documents.mapNotNull { it.data }.sortedByDescending { it["timeMs"] as? Long ?: 0L }
                        userNotifications = notifs
                    }
                }
        }
        onDispose {
            userLockListener?.remove()
            userNotifListener?.remove()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            configListener?.remove()
        }
    }

    val handleSignOut: () -> Unit = {
        isLoggedIn = false
        currentUserEmail = ""
        currentUserName = ""
        currentUserRole = ""
        currentRiderCode = ""
        currentRiderRealName = ""
        activeTab = 0
        historyList = emptyList()
        sharedPreferences.edit().apply {
            putBoolean("is_logged_in", false)
            putString("user_email", "")
            putString("user_name", "")
            putString("user_role", "")
            putString("rider_code", "")
            putString("rider_real_name", "")
            apply()
        }
        val serviceIntent = Intent(context, BackgroundNotificationService::class.java)
        context.stopService(serviceIntent)
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                googleSignInClient.signOut()
                FirebaseAuth.getInstance().signOut()
            } catch (e: Exception) {
                Log.e("ShopeeAutoPrint", "Sign out background error", e)
            }
        }
    }

    

    fun sendPrintRequest(codeToSend: String) {
        if (codeToSend.isBlank()) {
            statusText = "Lỗi: Mã vận đơn rỗng"
            return
        }
        val rawCodes = codeToSend.split("\n")
        val cleanCodes = rawCodes.map { it.trim().uppercase(Locale.ROOT) }.filter { it.isNotBlank() }
        if (cleanCodes.isEmpty()) {
            statusText = "Lỗi: Không tìm thấy mã vận đơn!"
            return
        }

        isSending = true
        statusText = "Đang truyền dữ liệu..."
        
        // Kích hoạt quét lỗi nhanh khi vừa nhấn nút gửi in
        lastPrintTime = System.currentTimeMillis()
        isPollingActive = true

        val sdf = SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault())
        val timeString = sdf.format(Date())

        val batchCodesText = cleanCodes.joinToString("\n")
        val newItems = listOf(PrintHistoryItem(batchCodesText, timeString, "Đang truyền..."))
        updateHistoryList(cleanOldHistoryIfNeeded(currentUserRole, newItems + historyList))

        // Ensure we fetch latest values from sharedPreferences directly in case state is out of sync
        val emailToSend = currentUserEmail.ifBlank { sharedPreferences.getString("user_email", "") ?: "unregistered@gmail.com" }
        val nameToSend = currentUserName.ifBlank { sharedPreferences.getString("user_name", "") ?: "Unregistered" }
        val codeToSend = currentRiderCode.ifBlank { sharedPreferences.getString("rider_code", "") ?: "NONE" }
        val batchId = "BATCH_${codeToSend}_${System.currentTimeMillis()}"

        val finalRiderRealName = sharedPreferences.getString("rider_real_name", "") ?: currentRiderRealName
        val formattedRiderName = if (currentUserRole == "Admin") {
            nameToSend
        } else if (finalRiderRealName.isNotBlank()) {
            "$finalRiderRealName ( $codeToSend - $nameToSend )"
        } else {
            "$nameToSend ( $codeToSend - $nameToSend )"
        }

        val passToSend = sheetPassword.ifBlank { sharedPreferences.getString("sheet_password", "") ?: "NOPASSWORD" }

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val bodyJson = JSONObject().apply {
                    val jsonArray = JSONArray()
                    cleanCodes.forEach { jsonArray.put(it) }
                    put("codes", jsonArray)
                    put("batchId", batchId)
                    put("username", formattedRiderName)
                    put("riderName", formattedRiderName)
                    put("email", emailToSend)
                    put("riderCode", codeToSend)
                    put("password", passToSend)
                }

                val body = bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(webAppUrl)
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.d("ShopeeAutoPrint", "Sheet Response: $responseBody")
                    withContext(Dispatchers.Main) {
                        isSending = false
                        if (response.isSuccessful && responseBody != null) {
                            try {
                                val respJson = JSONObject(responseBody)
                                val statusVal = respJson.optString("status")
                                val messageVal = respJson.optString("message", "")
                                
                                if (statusVal == "success" || statusVal == "true" || respJson.optBoolean("status") == true) {
                                    statusText = "Gửi in thành công ${cleanCodes.size} mã!"
                                    updateHistoryList(historyList.map { item ->
                                        if (item.code == batchCodesText && item.status.contains("Đang")) {
                                            item.copy(status = "Thành công")
                                        } else {
                                            item
                                        }
                                    })
                                    triggerVibration()
                                } else {
                                    statusText = "Thất bại: $messageVal"
                                    updateHistoryList(historyList.map { item ->
                                        if (item.code == batchCodesText && item.status.contains("Đang")) {
                                            item.copy(status = "Thất bại", errorMessage = messageVal)
                                        } else {
                                            item
                                        }
                                    })
                                }
                            } catch (e: Exception) {
                                statusText = "Thất bại: Lỗi định dạng phản hồi từ Google Sheets"
                                updateHistoryList(historyList.map { item ->
                                    if (item.code == batchCodesText && item.status.contains("Đang")) {
                                        item.copy(status = "Thất bại", errorMessage = e.message)
                                    } else {
                                        item
                                    }
                                })
                            }
                        } else {
                            statusText = "Thất bại: Mã phản hồi HTTP ${response.code}"
                            updateHistoryList(historyList.map { item ->
                                if (item.code == batchCodesText && item.status.contains("Đang")) {
                                    item.copy(status = "Thất bại", errorMessage = "HTTP ${response.code}")
                                } else {
                                    item
                                }
                            })
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isSending = false
                    statusText = "Thất bại: ${e.message}"
                    updateHistoryList(historyList.map { item ->
                        if (item.code == batchCodesText && item.status.contains("Đang")) {
                            item.copy(status = "Thất bại", errorMessage = e.message)
                        } else {
                            item
                        }
                    })
                }
            }
        }
    }

    if (showSplashScreen) {
        SplashScreen(isDark = isDark, onFinished = { showSplashScreen = false })
    } else if (!isLoggedIn) {
        LoginScreen(
            sharedPreferences = sharedPreferences,
            onLoginSuccess = { email, name, role, isVerified, rCode, rRealName ->
                currentUserEmail = email
                currentUserName = name
                currentUserRole = role
                currentRiderCode = rCode
                currentRiderRealName = rRealName
                isLoggedIn = true

                sharedPreferences.edit().apply {
                    putBoolean("is_logged_in", true)
                    putString("user_email", email)
                    putString("user_name", name)
                    putString("user_role", role)
                    putString("rider_code", rCode)
                    putString("rider_real_name", rRealName)
                    apply()
                }

                val serviceIntent = Intent(context, BackgroundNotificationService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        )
    } else {
        if (showCameraScanner && hasCameraPermission) {
            CameraScannerView(
                isOcrMode = isOcrMode,
                onModeToggle = { isOcrMode = !isOcrMode },
                onCodeScanned = { code ->
                    if (orderCode.isNotBlank()) {
                        val existingList = orderCode.split("\n").map { it.trim() }
                        if (!existingList.contains(code)) {
                            orderCode = orderCode.trim() + "\n" + code
                            triggerVibration()
                        }
                    } else {
                        orderCode = code
                        triggerVibration()
                    }
                    showCameraScanner = false
                    if (autoSubmitOnScan) {
                        sendPrintRequest(orderCode)
                        orderCode = ""
                    }
                },
                onOcrBulkCodes = { codes ->
                    val existingList = orderCode.split("\n").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
                    var added = 0
                    codes.forEach { code ->
                        if (!existingList.contains(code)) {
                            existingList.add(code)
                            added++
                        }
                    }
                    orderCode = existingList.joinToString("\n")
                    if (added > 0) {
                        triggerVibration()
                        Toast.makeText(context, "Đã quét được $added mã mới!", Toast.LENGTH_SHORT).show()
                    }
                    showCameraScanner = false
                },
                onDismiss = { showCameraScanner = false },
                cameraExecutor = cameraExecutor
            )
        } else {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            TopBarLogo(isDark = isDark)
                        },
                        actions = {
                            IconButton(onClick = { showNotificationsDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Thông báo",
                                    tint = getTextColor(isDark)
                                )
                            }
                            IconButton(onClick = { onThemeToggle(!isDark) }) {
                                Icon(
                                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Chuyển giao diện",
                                    tint = getTextColor(isDark)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = getThemeBgColor(isDark)
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(containerColor = getThemeBgColor(isDark)) {
                        NavigationBarItem(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            icon = {
                                Icon(
                                    Icons.Default.Print,
                                    contentDescription = "Quét & In",
                                    tint = if (activeTab == 0) ThemePrimary else getTextColor(isDark).copy(alpha = 0.6f)
                                )
                            },
                            label = { Text("Quét & In", color = getTextColor(isDark)) }
                        )
                        NavigationBarItem(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            icon = {
                                Icon(
                                    Icons.Default.LocalShipping,
                                    contentDescription = "Chuyển Pick",
                                    tint = if (activeTab == 1) ThemePrimary else getTextColor(isDark).copy(alpha = 0.6f)
                                )
                            },
                            label = { Text("Chuyển Pick", color = getTextColor(isDark)) }
                        )
                        if (currentUserRole == "Admin") {
                            NavigationBarItem(
                                selected = activeTab == 2,
                                onClick = { activeTab = 2 },
                                icon = {
                                    Icon(
                                        Icons.Default.AdminPanelSettings,
                                        contentDescription = "Quản trị",
                                        tint = if (activeTab == 2) ThemePrimary else getTextColor(isDark).copy(alpha = 0.6f)
                                    )
                                },
                                label = { Text("Quản trị", color = getTextColor(isDark)) }
                            )
                        } else {
                            NavigationBarItem(
                                selected = activeTab == 3,
                                onClick = { activeTab = 3 },
                                icon = {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = "Tài khoản",
                                        tint = if (activeTab == 3) ThemePrimary else getTextColor(isDark).copy(alpha = 0.6f)
                                    )
                                },
                                label = { Text("Tài khoản", color = getTextColor(isDark)) }
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(getThemeBgBrush(isDark))
                ) {
                    AIParticleBackground()

                    if (showNotificationsDialog) {
                        AlertDialog(
                            onDismissRequest = { showNotificationsDialog = false },
                            title = { Text("Thông báo lỗi đẩy sai", color = ThemePrimary, fontWeight = FontWeight.Bold) },
                            text = {
                                if (userNotifications.isEmpty()) {
                                    Text("Bạn không có thông báo nào.", color = getTextColor(isDark))
                                } else {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(userNotifications) { notif ->
                                            val message = notif["message"]?.toString() ?: ""
                                            val timestamp = notif["timestamp"]?.toString() ?: ""
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = getThemeSurfaceColor(isDark).copy(alpha = 0.4f)),
                                                border = BorderStroke(1.dp, getGlassBorderColor(isDark)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text(message, color = getTextColor(isDark), fontSize = 13.sp)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(timestamp, color = getTextColor(isDark).copy(alpha = 0.5f), fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = { showNotificationsDialog = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary)
                                ) {
                                    Text("ĐÓNG", color = Color.White)
                                }
                            }
                        )
                    }

                    if (activeTab == 0) {
                        val isPrintLockedActive = isPrintLocked && (printLockedUntil == null || printLockedUntil!!.time > System.currentTimeMillis())
                        if (isPrintLockedActive) {
                            val remainingText = getRemainingTimeText(printLockedUntil, isPrintLockedActive)
                            PrintLockedScreen(remainingText)
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AnimatedVisibility(visible = statusText.isNotEmpty()) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (statusText.contains("thành công")) Color(0xFF1B5E20)
                                            else if (statusText.contains("Lỗi") || statusText.contains("Thất bại")) Color(0xFFB71C1C)
                                            else Color(0xFFE65100)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                    ) {
                                        Text(
                                            text = statusText,
                                            color = Color.White,
                                            modifier = Modifier.padding(12.dp),
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = orderCode,
                                    onValueChange = { orderCode = it.uppercase(java.util.Locale.ROOT) },
                                    placeholder = { Text("Danh sách mã vận đơn (Mỗi mã 1 dòng)", color = getTextColor(isDark).copy(alpha = 0.5f)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 120.dp, max = 220.dp),
                                    singleLine = false,
                                    maxLines = 15,
                                    trailingIcon = {
                                        if (orderCode.isNotEmpty()) {
                                            IconButton(onClick = { orderCode = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Xóa tất cả", tint = getTextColor(isDark))
                                            }
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = getTextColor(isDark),
                                        unfocusedTextColor = getTextColor(isDark),
                                        focusedBorderColor = ThemePrimary,
                                        unfocusedBorderColor = if (isDark) ThemePrimary.copy(alpha = 0.4f) else ThemePrimary.copy(alpha = 0.5f),
                                        unfocusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = if (isDark) 0.15f else 0.8f),
                                        focusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = if (isDark) 0.25f else 0.95f)
                                    )
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Button 1: Quét Mã Vạch (Live camera stream)
                                    Premium3DButton(
                                         onClick = {
                                             isOcrMode = false
                                             if (hasCameraPermission) {
                                                 showCameraScanner = true
                                             } else {
                                                 requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                                             }
                                         },
                                         modifier = Modifier.weight(1f),
                                         containerColor = getThemeSurfaceColor(isDark).copy(alpha = 0.3f),
                                         shadowColor = getGlassBorderColor(isDark).copy(alpha = 0.5f)
                                    ) {
                                         Icon(Icons.Default.QrCodeScanner, contentDescription = "Quét vạch", tint = getTextColor(isDark), modifier = Modifier.size(18.dp))
                                         Spacer(modifier = Modifier.width(4.dp))
                                         Text("Quét Vạch", color = getTextColor(isDark), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Button 2: Chụp Quét Chữ (Take photo to OCR)
                                    Premium3DButton(
                                         onClick = {
                                             isOcrMode = true
                                             if (hasCameraPermission) {
                                                 showCameraScanner = true
                                             } else {
                                                 requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                                             }
                                         },
                                         modifier = Modifier.weight(1f),
                                         containerColor = getThemeSurfaceColor(isDark).copy(alpha = 0.3f),
                                         shadowColor = getGlassBorderColor(isDark).copy(alpha = 0.5f)
                                    ) {
                                         Icon(Icons.Default.CameraAlt, contentDescription = "Chụp quét chữ", tint = ThemeCyan, modifier = Modifier.size(18.dp))
                                         Spacer(modifier = Modifier.width(4.dp))
                                         Text("Chụp Chữ", color = getTextColor(isDark), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Button 3: Chọn Ảnh Quét (Select from gallery)
                                    Premium3DButton(
                                         onClick = {
                                             photoPickerLauncher.launch("image/*")
                                         },
                                         modifier = Modifier.weight(1f),
                                         containerColor = getThemeSurfaceColor(isDark).copy(alpha = 0.3f),
                                         shadowColor = getGlassBorderColor(isDark).copy(alpha = 0.5f)
                                     ) {
                                         Icon(Icons.Default.Image, contentDescription = "Chọn ảnh quét", tint = ThemePrimary, modifier = Modifier.size(18.dp))
                                         Spacer(modifier = Modifier.width(4.dp))
                                         Text("Chọn Ảnh", color = getTextColor(isDark), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                     }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Premium3DButton(
                                    onClick = {
                                        if (isSending) return@Premium3DButton
                                        if (orderCode.isBlank()) {
                                            Toast.makeText(context, "Vui lòng nhập hoặc quét mã để in!", Toast.LENGTH_SHORT).show()
                                            return@Premium3DButton
                                        }
                                        focusManager.clearFocus()
                                        sendPrintRequest(orderCode)
                                        orderCode = ""
                                    },
                                    enabled = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp),
                                    containerBrush = if (isSending) {
                                        Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.5f), Color.DarkGray.copy(alpha = 0.5f)))
                                    } else {
                                        getButtonGradientBrush(isDark)
                                    },
                                    shadowColor = if (isSending) Color.DarkGray else ThemeAccent.copy(alpha = 0.5f)
                                ) {
                                    if (isSending) {
                                        CircularProgressIndicator(color = getButtonTextColor(), modifier = Modifier.size(24.dp))
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Default.CloudUpload, contentDescription = "Truyền in", tint = getButtonTextColor())
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("IN NHANH CLOUD", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = getButtonTextColor())
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("LỊCH SỬ GỬI IN", fontWeight = FontWeight.Bold, color = getTextColor(isDark).copy(alpha = 0.6f), fontSize = 14.sp)
                                    if (historyList.isNotEmpty() && currentUserRole == "Admin") {
                                        Text(
                                            "XÓA HẾT",
                                            color = ThemePrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable { updateHistoryList(emptyList()) }
                                        )
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 8.dp), color = getGlassBorderColor(isDark))

                                if (historyList.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Chưa có lịch sử gửi", color = getTextColor(isDark).copy(alpha = 0.4f))
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(historyList, key = { it.code + "_" + it.timestamp }) { item ->
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = getThemeSurfaceColor(isDark).copy(alpha = 0.3f)),
                                                border = BorderStroke(1.dp, getGlassBorderColor(isDark)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(item.code, fontWeight = FontWeight.Bold, color = getTextColor(isDark))
                                                        Text(item.timestamp, fontSize = 12.sp, color = getTextColor(isDark).copy(alpha = 0.6f))
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        val statusColor = when {
                                                            item.status.contains("thành công") || item.status.contains("Thành công") -> Color(0xFF4CAF50)
                                                            item.status.contains("Đang") -> Color(0xFFFF9800)
                                                            item.status.contains("lỗi") || item.status.contains("Lỗi") || item.status.contains("Thất bại") -> Color(0xFFE57373)
                                                            else -> ThemePrimary
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .background(statusColor, shape = CircleShape)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            item.status,
                                                            color = statusColor,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        IconButton(
                                                            onClick = {
                                                                try {
                                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                                    val clip = android.content.ClipData.newPlainText("Mã gửi in", item.code)
                                                                    clipboard.setPrimaryClip(clip)
                                                                    Toast.makeText(context, "Đã sao chép mã!", Toast.LENGTH_SHORT).show()
                                                                } catch (e: Exception) {
                                                                    Log.e("ShopeeAutoPrint", "Lỗi sao chép mã", e)
                                                                }
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.ContentCopy,
                                                                contentDescription = "Sao chép mã",
                                                                tint = getTextColor(isDark).copy(alpha = 0.6f),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (activeTab == 1) {
                        val isChuyenPickLockedActive = isChuyenPickLocked && (chuyenPickLockedUntil == null || chuyenPickLockedUntil!!.time > System.currentTimeMillis())
                        if (isChuyenPickLockedActive) {
                            val remainingText = getRemainingTimeText(chuyenPickLockedUntil, isChuyenPickLockedActive)
                            ChuyenPickLockedScreen(
                                remainingText = remainingText,
                                reports = chuyenPickLockedReports,
                                context = context
                            )
                        } else {
                            ChuyenPickView(
                                db = db,
                                currentUserEmail = currentUserEmail,
                                currentUserName = currentUserName,
                                currentRiderCode = currentRiderCode,
                                currentRiderRealName = currentRiderRealName,
                                currentUserRole = currentUserRole,
                                webAppUrl = webAppUrl,
                                sharedPreferences = sharedPreferences,
                                coroutineScope = coroutineScope,
                                context = context
                            )
                        }
                    } else if (activeTab == 2 && currentUserRole == "Admin") {
                        AdminDashboardView(
                            db = db,
                            currentUserEmail = currentUserEmail,
                            currentUserName = currentUserName,
                            currentUserRole = currentUserRole,
                            webAppUrl = webAppUrl,
                            sheetUsername = sheetUsername,
                            sheetPassword = sheetPassword,
                            onLogout = handleSignOut,
                            onUpdateWebAppUrl = { newUrl ->
                                webAppUrl = newUrl
                                db.collection("config").document("app_settings")
                                    .set(mapOf("webAppUrl" to newUrl), com.google.firebase.firestore.SetOptions.merge())
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Đã cập nhật WebApp URL!", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Lỗi cập nhật: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            },
                            onUpdateCredentials = { user, pass ->
                                sheetUsername = user
                                sheetPassword = pass
                                db.collection("config").document("app_settings")
                                    .set(mapOf(
                                        "sheetUsername" to user,
                                        "sheetPassword" to pass
                                    ), com.google.firebase.firestore.SetOptions.merge())
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Đã lưu tài khoản Google Sheets!", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Lỗi lưu tài khoản: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        )
                    } else if (activeTab == 3 && currentUserRole != "Admin") {
                        UserAccountView(
                            currentUserEmail = currentUserEmail,
                            currentUserName = currentUserName,
                            currentUserRole = currentUserRole,
                            onLogout = handleSignOut
                        )
                    }
                }
            }
        if (showAppInfoDialog) {
            AlertDialog(
                onDismissRequest = { showAppInfoDialog = false },
                title = { Text("Thông tin ứng dụng", color = ThemePrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Tên ứng dụng: Hỗ trợ vận hành", color = getTextColor(isDark))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Phiên bản: 1.0.0", color = getTextColor(isDark))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Ngày phát hành: 2026", color = getTextColor(isDark))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Nhà phát triển: VTD Stadio", color = getTextColor(isDark))
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAppInfoDialog = false }) {
                        Text("ĐÓNG", color = ThemePrimary)
                    }
                },
                containerColor = getThemeSurfaceColor(isDark)
            )
        }

        if (showPrivacyPolicyDialog) {
            AlertDialog(
                onDismissRequest = { showPrivacyPolicyDialog = false },
                title = { Text("Chính sách bảo mật", color = ThemePrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                        Text("ĐIỀU KHOẢN SỬ DỤNG DỊCH VỤ", fontWeight = FontWeight.Bold, color = getTextColor(isDark))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Chào mừng bạn đến với ứng dụng Hỗ trợ vận hành. Khi sử dụng ứng dụng này, bạn đồng ý tuân thủ các điều khoản dịch vụ của chúng tôi.", color = getTextColor(isDark))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("CAM KẾT BẢO MẬT THÔNG TIN", fontWeight = FontWeight.Bold, color = getTextColor(isDark))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Chúng tôi cam kết bảo mật tuyệt đối mọi thông tin cá nhân, lịch sử chuyển pick và in ấn của bạn. Tất cả dữ liệu được lưu trữ an toàn bằng công nghệ cơ sở dữ liệu đám mây tiên tiến bảo mật cao. Chúng tôi không bao giờ chia sẻ dữ liệu của bạn cho bất kỳ bên thứ ba nào khi chưa được phép.", color = getTextColor(isDark))
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPrivacyPolicyDialog = false }) {
                        Text("ĐỒNG Ý", color = ThemePrimary)
                    }
                },
                containerColor = getThemeSurfaceColor(isDark)
            )
        }
        }
    }
}

@Composable
fun UserAccountView(
    currentUserEmail: String,
    currentUserName: String,
    currentUserRole: String,
    onLogout: () -> Unit
) {
    val isDark = LocalThemeIsDark.current
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = getThemeSurfaceColor(isDark).copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, getGlassBorderColor(isDark)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Thông tin tài khoản", color = ThemePrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Thông tin đăng nhập:", fontWeight = FontWeight.Bold, color = getTextColor(isDark), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Email: $currentUserEmail", color = getTextColor(isDark), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Text("Tên: $currentUserName", color = getTextColor(isDark), fontSize = 13.sp)
                
                val roleDisplayName = when (currentUserRole) {
                    "Driver" -> "Tài xế (Driver)"
                    else -> currentUserRole
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("Vai trò: $roleDisplayName", color = ThemeAccent, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                
                Spacer(modifier = Modifier.height(28.dp))
                
                val logoutBrush = if (isDark) {
                    Brush.linearGradient(listOf(ThemePrimary.copy(alpha = 0.8f), ThemeAccent.copy(alpha = 0.8f)))
                } else {
                    Brush.linearGradient(listOf(ThemePrimary.copy(alpha = 0.4f), ThemeAccent.copy(alpha = 0.4f)))
                }

                Premium3DButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    containerBrush = logoutBrush,
                    shadowColor = ThemePrimary.copy(alpha = if (isDark) 0.4f else 0.2f)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = "Đăng xuất", tint = getButtonTextColor())
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đăng xuất", color = getButtonTextColor(), fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        AppInfoSettingsSection(isDark = isDark)
    }
}

@Composable
fun AdminDashboardView(
    db: FirebaseFirestore,
    currentUserEmail: String,
    currentUserName: String,
    currentUserRole: String,
    webAppUrl: String,
    sheetUsername: String,
    sheetPassword: String,
    onLogout: () -> Unit,
    onUpdateWebAppUrl: (String) -> Unit,
    onUpdateCredentials: (String, String) -> Unit
) {
    val isDark = LocalThemeIsDark.current
    var inputWebAppUrl by remember { mutableStateOf(webAppUrl) }
    var isEditingUrl by remember { mutableStateOf(false) }

    var inputSheetUser by remember { mutableStateOf(sheetUsername) }
    var inputSheetPass by remember { mutableStateOf(sheetPassword) }
    var isEditingCreds by remember { mutableStateOf(false) }

    var selectedSubTab by remember { mutableStateOf(0) } // 0: Cấu hình, 1: Tài khoản, 2: Chuyển Pick

    LaunchedEffect(webAppUrl) {
        inputWebAppUrl = webAppUrl
    }

    LaunchedEffect(sheetUsername, sheetPassword) {
        inputSheetUser = sheetUsername
        inputSheetPass = sheetPassword
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Account information card at the very top for Admin
        Card(
            colors = CardDefaults.cardColors(containerColor = getThemeSurfaceColor(isDark).copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, getGlassBorderColor(isDark)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Thông tin đăng nhập:", fontWeight = FontWeight.Bold, color = getTextColor(isDark), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Email: $currentUserEmail", color = getTextColor(isDark), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Tên: $currentUserName", color = getTextColor(isDark), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Vai trò: Quản trị viên (Admin)", color = Color(0xFFFFA000), fontWeight = FontWeight.Medium, fontSize = 12.sp)
                    }
                    val adminLogoutBrush = if (isDark) {
                        Brush.linearGradient(listOf(ThemePrimary.copy(alpha = 0.8f), ThemeAccent.copy(alpha = 0.8f)))
                    } else {
                        Brush.linearGradient(listOf(ThemePrimary.copy(alpha = 0.4f), ThemeAccent.copy(alpha = 0.4f)))
                    }
                    Premium3DButton(
                        onClick = onLogout,
                        modifier = Modifier.width(110.dp).height(36.dp),
                        containerBrush = adminLogoutBrush,
                        shadowColor = ThemePrimary.copy(alpha = if (isDark) 0.4f else 0.2f)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Đăng xuất", tint = getButtonTextColor(), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Đăng xuất", color = getButtonTextColor(), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(getThemeSurfaceColor(isDark).copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf("Cấu hình", "Tài khoản", "Chuyển Pick")
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedSubTab == index
                val backgroundBrush = if (isSelected) {
                    getButtonGradientBrush(isDark)
                } else {
                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush = backgroundBrush)
                        .clickable { selectedSubTab = index },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isSelected) Color.White else getTextColor(isDark).copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedSubTab == 0) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = getThemeSurfaceColor(isDark).copy(alpha = 0.3f)),
                        border = BorderStroke(1.dp, getGlassBorderColor(isDark)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Cấu hình Web App URL", color = ThemePrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                IconButton(onClick = { isEditingUrl = !isEditingUrl }) {
                                    Icon(
                                        imageVector = if (isEditingUrl) Icons.Default.Check else Icons.Default.Edit,
                                        contentDescription = "Sửa URL",
                                        tint = if (isEditingUrl) Color.Green else getTextColor(isDark)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (isEditingUrl) {
                                OutlinedTextField(
                                    value = inputWebAppUrl,
                                    onValueChange = { inputWebAppUrl = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = getTextColor(isDark),
                                        unfocusedTextColor = getTextColor(isDark),
                                        focusedBorderColor = ThemePrimary,
                                        unfocusedBorderColor = if (isDark) ThemePrimary.copy(alpha = 0.4f) else ThemePrimary.copy(alpha = 0.5f),
                                        unfocusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = if (isDark) 0.15f else 0.8f),
                                        focusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = if (isDark) 0.25f else 0.95f)
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Premium3DButton(
                                    onClick = {
                                        onUpdateWebAppUrl(inputWebAppUrl)
                                        isEditingUrl = false
                                    },
                                    modifier = Modifier.align(Alignment.End).width(120.dp),
                                    containerColor = ThemePrimary,
                                    shadowColor = ThemePrimary.copy(alpha = 0.5f)
                                ) {
                                    Text("Lưu URL", color = getButtonTextColor(), fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text(
                                    text = webAppUrl,
                                    color = getTextColor(isDark).copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
                item {
                    AppInfoSettingsSection(isDark = isDark)
                }
            } else if (selectedSubTab == 1) {
                item {
                    AdminCreateRiderSection(db = db)
                }
                item {
                    AdminRiderListSection(db = db)
                }
                item {
                    AdminRiderListSectionOnlyActive(db = db)
                }
            } else if (selectedSubTab == 2) {
                item {
                    AdminPupDriversSection(db = db)
                }
            }
        }
    }
}

@Composable
fun AdminCreateRiderSection(db: FirebaseFirestore) {
    val isDark = LocalThemeIsDark.current
    val context = LocalContext.current
    var riderCode by remember { mutableStateOf("") }
    var riderName by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = getThemeSurfaceColor(isDark).copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, getGlassBorderColor(isDark)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Tạo / Cập nhật Code Rider", color = ThemePrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)

            OutlinedTextField(
                value = riderCode,
                onValueChange = { riderCode = it },
                placeholder = { Text("Mã Code Rider (Ví dụ: VTD01)", color = getTextColor(isDark).copy(alpha = 0.6f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = getTextColor(isDark),
                    unfocusedTextColor = getTextColor(isDark),
                    focusedBorderColor = ThemePrimary,
                    unfocusedBorderColor = if (isDark) ThemePrimary.copy(alpha = 0.4f) else ThemePrimary.copy(alpha = 0.5f),
                    unfocusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = if (isDark) 0.15f else 0.8f),
                    focusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = if (isDark) 0.25f else 0.95f)
                )
            )

            OutlinedTextField(
                value = riderName,
                onValueChange = { riderName = it },
                placeholder = { Text("Tên Rider tương ứng", color = getTextColor(isDark).copy(alpha = 0.6f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = getTextColor(isDark),
                    unfocusedTextColor = getTextColor(isDark),
                    focusedBorderColor = ThemePrimary,
                    unfocusedBorderColor = if (isDark) ThemePrimary.copy(alpha = 0.4f) else ThemePrimary.copy(alpha = 0.5f),
                    unfocusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = if (isDark) 0.15f else 0.8f),
                    focusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = if (isDark) 0.25f else 0.95f)
                )
            )

            Premium3DButton(
                onClick = {
                    if (riderCode.isBlank() || riderName.isBlank()) {
                        Toast.makeText(context, "Vui lòng điền đủ thông tin!", Toast.LENGTH_SHORT).show()
                        return@Premium3DButton
                    }
                    isProcessing = true
                    val codeDoc = riderCode.trim().uppercase(Locale.ROOT)
                    db.collection("rider_codes").document(codeDoc)
                        .set(mapOf(
                            "code" to codeDoc,
                            "name" to riderName.trim(),
                            "createdBy" to "Admin"
                        ))
                        .addOnSuccessListener {
                            isProcessing = false
                            Toast.makeText(context, "Đã tạo/cập nhật Code Rider!", Toast.LENGTH_SHORT).show()
                            riderCode = ""
                            riderName = ""
                        }
                        .addOnFailureListener { e ->
                            isProcessing = false
                            Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                },
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth(),
                containerBrush = getButtonGradientBrush(isDark),
                shadowColor = ThemePrimary.copy(alpha = 0.5f)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = getButtonTextColor(), modifier = Modifier.size(24.dp))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = "Tạo mới", tint = getButtonTextColor())
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tạo mới / Cập nhật", fontWeight = FontWeight.Bold, color = getButtonTextColor())
                    }
                }
            }
        }
    }
}

@Composable
fun AdminRiderListSection(db: FirebaseFirestore) {
    val isDark = LocalThemeIsDark.current
    var riderCodesList by remember { mutableStateOf(listOf<Map<String, Any>>()) }

    LaunchedEffect(Unit) {
        db.collection("rider_codes")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val list = mutableListOf<Map<String, Any>>()
                    for (doc in snapshot.documents) {
                        val data = doc.data
                        if (data != null) {
                            list.add(data)
                        }
                    }
                    riderCodesList = list
                }
            }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Created Rider Codes List
        Card(
            colors = CardDefaults.cardColors(containerColor = getThemeSurfaceColor(isDark).copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, getGlassBorderColor(isDark)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Danh sách Rider Code đã tạo", color = ThemePrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                if (riderCodesList.isEmpty()) {
                    Text("Chưa có Rider Code nào được tạo.", color = getTextColor(isDark).copy(alpha = 0.5f))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        riderCodesList.forEachIndexed { index, data ->
                            val code = data["code"]?.toString() ?: ""
                            val name = data["name"]?.toString() ?: ""
                            if (index > 0) {
                                Divider(
                                    color = getGlassBorderColor(isDark).copy(alpha = 0.3f),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(getThemeSurfaceColor(isDark).copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
                                    .border(1.dp, getGlassBorderColor(isDark), shape = RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(code, fontWeight = FontWeight.Bold, color = getTextColor(isDark))
                                    Text("Tên: $name", fontSize = 13.sp, color = getTextColor(isDark).copy(alpha = 0.7f))
                                }
                                IconButton(onClick = {
                                    db.collection("rider_codes").document(code).delete()
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Xóa Code", tint = ThemePrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminRiderListSectionOnlyActive(db: FirebaseFirestore) {
    val isDark = LocalThemeIsDark.current
    var activeRidersList by remember { mutableStateOf(listOf<Map<String, Any>>()) }

    LaunchedEffect(Unit) {
        db.collection("active_riders")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val list = mutableListOf<Map<String, Any>>()
                    for (doc in snapshot.documents) {
                        val data = doc.data
                        if (data != null) {
                            list.add(data)
                        }
                    }
                    activeRidersList = list
                }
            }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = getThemeSurfaceColor(isDark).copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, getGlassBorderColor(isDark)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tài khoản đang hoạt động", color = ThemePrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            if (activeRidersList.isEmpty()) {
                Text("Không có tài khoản nào hoạt động.", color = getTextColor(isDark).copy(alpha = 0.5f))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeRidersList.forEach { data ->
                        val email = data["email"]?.toString() ?: ""
                        val name = data["name"]?.toString() ?: ""
                        val code = data["riderCode"]?.toString() ?: ""
                        val isPrintLockedVal = data["isPrintLocked"] as? Boolean ?: false
                        val isChuyenPickLockedVal = data["isChuyenPickLocked"] as? Boolean ?: false

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(getThemeSurfaceColor(isDark).copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
                                .border(1.dp, getGlassBorderColor(isDark), shape = RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.Bold, color = getTextColor(isDark))
                                Text("Email: $email", fontSize = 12.sp, color = getTextColor(isDark).copy(alpha = 0.7f))
                                Text("Mã Code: $code", fontSize = 12.sp, color = ThemeCyan, fontWeight = FontWeight.Medium)
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        androidx.compose.material3.Checkbox(
                                            checked = isPrintLockedVal,
                                            onCheckedChange = { isChecked ->
                                                db.collection("active_riders").document(email).update(
                                                    "isPrintLocked", isChecked,
                                                    "printLockedUntil", if (isChecked) Date(System.currentTimeMillis() + 24 * 3600 * 1000L) else null
                                                )
                                            },
                                            colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = ThemePrimary)
                                        )
                                        Text("Khóa In", fontSize = 12.sp, color = getTextColor(isDark))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        androidx.compose.material3.Checkbox(
                                            checked = isChuyenPickLockedVal,
                                            onCheckedChange = { isChecked ->
                                                db.collection("active_riders").document(email).update(
                                                    "isChuyenPickLocked", isChecked,
                                                    "chuyenPickLockedUntil", if (isChecked) Date(System.currentTimeMillis() + 24 * 3600 * 1000L) else null
                                                )
                                            },
                                            colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = ThemePrimary)
                                        )
                                        Text("Khóa Pick", fontSize = 12.sp, color = getTextColor(isDark))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun LoginScreen(
    sharedPreferences: SharedPreferences,
    onLoginSuccess: (String, String, String, Boolean, String, String) -> Unit
) {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val isDark = LocalThemeIsDark.current

    var step by remember { mutableStateOf(1) } // 1: Google Login, 2: Enter Rider Code
    var email by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var riderCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val requestGoogleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            if (account != null) {
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                isLoading = true
                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener { authResult ->
                        if (authResult.isSuccessful) {
                            val user = authResult.result?.user
                            if (user != null) {
                                val userEmail = user.email ?: ""
                                val userName = user.displayName ?: ""
                                email = userEmail
                                displayName = userName

                                if (userEmail == "vuhiepkt2017@gmail.com" || userEmail == "vuhiepkt2027@gmail.com") {
                                    // Admin bypassed directly without rider code
                                    db.collection("active_riders").document(userEmail)
                                        .set(mapOf(
                                            "email" to userEmail,
                                            "name" to userName,
                                            "role" to "Admin",
                                            "riderCode" to "ADMIN",
                                            "lastActive" to Date()
                                        ))
                                        .addOnSuccessListener {
                                            isLoading = false
                                            onLoginSuccess(userEmail, userName, "Admin", true, "ADMIN", "Admin")
                                        }
                                        .addOnFailureListener { e ->
                                            isLoading = false
                                            onLoginSuccess(userEmail, userName, "Admin", true, "ADMIN", "Admin")
                                        }
                                } else if (userEmail == "tester@gmail.com" || userEmail.contains("tester")) {
                                    // Tester bypassed directly without rider code
                                    db.collection("active_riders").document(userEmail)
                                        .set(mapOf(
                                            "email" to userEmail,
                                            "name" to userName,
                                            "role" to "Rider",
                                            "riderCode" to "TESTER",
                                            "riderRealName" to "Tester",
                                            "lastActive" to Date()
                                        ))
                                        .addOnSuccessListener {
                                            isLoading = false
                                            onLoginSuccess(userEmail, userName, "Rider", true, "TESTER", "Tester")
                                        }
                                        .addOnFailureListener { e ->
                                            isLoading = false
                                            onLoginSuccess(userEmail, userName, "Rider", true, "TESTER", "Tester")
                                        }
                                } else {
                                    // Normal user: Check if already registered
                                    db.collection("active_riders").document(userEmail).get()
                                        .addOnSuccessListener { doc ->
                                            isLoading = false
                                            if (doc.exists()) {
                                                val rCode = doc.getString("riderCode") ?: ""
                                                val rRole = doc.getString("role") ?: "Rider"
                                                val rRealName = doc.getString("riderRealName") ?: ""
                                                onLoginSuccess(userEmail, userName, rRole, true, rCode, rRealName)
                                            } else {
                                                step = 2
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            isLoading = false
                                            step = 2
                                        }
                                }
                            } else {
                                isLoading = false
                                errorMessage = "Lỗi: Không lấy được thông tin User từ Google."
                            }
                        } else {
                            isLoading = false
                            errorMessage = authResult.exception?.message ?: "Đăng nhập Firebase thất bại."
                        }
                    }
            } else {
                errorMessage = "Không thể lấy tài khoản Google."
            }
        } catch (e: Exception) {
            errorMessage = "Lỗi Google Sign-In: ${e.message}"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(getThemeBgBrush(isDark))
            .systemBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AIParticleBackground()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Premium App Logo
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.VTDStadio.Hotroin.R.drawable.app_logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        1.5.dp,
                        Brush.linearGradient(listOf(ThemePrimary, ThemeCyan)),
                        RoundedCornerShape(20.dp)
                    )
            )

            Text(
                "HỖ TRỢ VẬN HÀNH",
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                color = getTextColor(isDark),
                letterSpacing = 2.sp
            )

            if (errorMessage.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C).copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, ThemePrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }

            if (isLoading) {
                CircularProgressIndicator(color = ThemeCyan)
            } else {
                val scale = remember { Animatable(1f) }
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                LaunchedEffect(isPressed) {
                    scale.animateTo(
                        targetValue = if (isPressed) 0.95f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                    )
                }

                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth().scale(scale.value),
                    borderColor = getGlassBorderColor(isDark),
                    glowColor = ThemeCyan
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (step == 1) {
                            Text(
                                "Hệ Thống Đăng Nhập Cloud",
                                fontWeight = FontWeight.Bold,
                                color = getTextColor(isDark),
                                fontSize = 16.sp
                            )
                            Text(
                                "Vui lòng đăng nhập tài khoản Google được cấp quyền hoạt động.",
                                color = getTextColor(isDark).copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Premium3DButton(
                                onClick = {
                                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestIdToken(context.getString(com.VTDStadio.Hotroin.R.string.default_web_client_id))
                                        .requestEmail()
                                        .build()
                                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                    requestGoogleSignInLauncher.launch(googleSignInClient.signInIntent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(58.dp),
                                containerBrush = getButtonGradientBrush(isDark),
                                shadowColor = ThemeAccent.copy(alpha = 0.5f)
                            ) {
                                Icon(Icons.Default.Login, contentDescription = "Login", tint = getButtonTextColor())
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ĐĂNG NHẬP GOOGLE", fontWeight = FontWeight.Bold, color = getButtonTextColor())
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            OutlinedButton(
                                onClick = {
                                    // Bypass Google Login and log in directly as a Tester
                                    onLoginSuccess("tester@gmail.com", "Tester Account", "Rider", true, "TESTER", "Tester")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                border = BorderStroke(1.dp, ThemePrimary.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Science,
                                    contentDescription = "Tester Login",
                                    tint = ThemePrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "ĐĂNG NHẬP TESTER (Bypass)",
                                    fontWeight = FontWeight.Bold,
                                    color = getTextColor(isDark)
                                )
                            }
                        } else {
                            Text(
                                "Nhập Code Rider Kích Hoạt",
                                fontWeight = FontWeight.Bold,
                                color = getTextColor(isDark),
                                fontSize = 16.sp
                            )
                            Text(
                                "Nhập chính xác mã được cấp bởi quản trị viên để hoàn tất kích hoạt.",
                                color = getTextColor(isDark).copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            OutlinedTextField(
                                value = riderCode,
                                onValueChange = { riderCode = it },
                                placeholder = { Text("Mã kích hoạt (Rider Code)", color = getTextColor(isDark).copy(alpha = 0.6f)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = getTextColor(isDark),
                                    unfocusedTextColor = getTextColor(isDark),
                                    focusedBorderColor = ThemePrimary,
                                    unfocusedBorderColor = if (isDark) ThemePrimary.copy(alpha = 0.4f) else ThemePrimary.copy(alpha = 0.5f),
                                    unfocusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = if (isDark) 0.15f else 0.8f),
                                    focusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = if (isDark) 0.25f else 0.95f)
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Premium3DButton(
                                    onClick = {
                                        try {
                                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                                .requestEmail()
                                                .build()
                                            val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                            googleSignInClient.signOut().addOnCompleteListener {
                                                FirebaseAuth.getInstance().signOut()
                                                step = 1
                                                errorMessage = ""
                                                riderCode = ""
                                            }
                                        } catch (e: Exception) {
                                            FirebaseAuth.getInstance().signOut()
                                            step = 1
                                            errorMessage = ""
                                            riderCode = ""
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    containerColor = getThemeSurfaceColor(isDark).copy(alpha = 0.3f),
                                    shadowColor = getGlassBorderColor(isDark).copy(alpha = 0.5f)
                                ) {
                                    Text("Quay Lại", color = getTextColor(isDark), fontWeight = FontWeight.Bold)
                                }
                                Premium3DButton(
                                    onClick = {
                                        if (riderCode.isBlank()) {
                                            errorMessage = "Vui lòng nhập Code Rider!"
                                            return@Premium3DButton
                                        }
                                        isLoading = true
                                        val codeToCheck = riderCode.trim().uppercase(Locale.ROOT)
                                        db.collection("rider_codes").document(codeToCheck).get()
                                            .addOnSuccessListener { doc ->
                                                isLoading = false
                                                if (doc.exists()) {
                                                    val riderRealName = doc.getString("name") ?: ""
                                                    db.collection("active_riders").document(email)
                                                        .set(mapOf(
                                                            "email" to email,
                                                            "name" to displayName,
                                                            "role" to "Rider",
                                                            "riderCode" to codeToCheck,
                                                            "riderRealName" to riderRealName,
                                                            "lastActive" to Date()
                                                        ))
                                                        .addOnSuccessListener {
                                                            onLoginSuccess(email, displayName, "Rider", true, codeToCheck, riderRealName)
                                                        }
                                                        .addOnFailureListener { e ->
                                                            errorMessage = "Lỗi lưu trạng thái: ${e.message}"
                                                        }
                                                } else {
                                                    errorMessage = "Mã Code Rider không đúng hoặc đã bị xóa!"
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                isLoading = false
                                                errorMessage = "Lỗi xác thực: ${e.message}"
                                            }
                                    },
                                    modifier = Modifier.weight(1f),
                                    containerBrush = Brush.linearGradient(listOf(ThemePrimary, ThemeAccent)),
                                    shadowColor = ThemeAccent.copy(alpha = 0.5f)
                                ) {
                                    Text("Xác Nhận", color = getButtonTextColor(), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScannerView(
    isOcrMode: Boolean,
    onModeToggle: () -> Unit,
    onCodeScanned: (String) -> Unit,
    onOcrBulkCodes: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    cameraExecutor: ExecutorService
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewViewReference: PreviewView? by remember { mutableStateOf(null) }
    var isProcessingOcr by remember { mutableStateOf(false) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (isOcrMode) "Chụp quét chữ đơn hàng" else "Quét mã vạch tự động",
                        fontSize = 16.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Trở lại", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onModeToggle) {
                        Icon(
                            if (isOcrMode) Icons.Default.QrCodeScanner else Icons.Default.CameraAlt,
                            contentDescription = "Chuyển chế độ",
                            tint = ThemePrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Black)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        previewViewReference = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(185.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Transparent)
                        .border(BorderStroke(2.dp, ThemePrimary), shape = RoundedCornerShape(16.dp))
                        .align(Alignment.Center)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isOcrMode) {
                    if (isProcessingOcr) {
                        CircularProgressIndicator(color = ThemePrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Đang xử lý ảnh & nhận dạng...", color = Color.White, fontSize = 13.sp)
                    } else {
                        Button(
                            onClick = {
                                try {
                                    isProcessingOcr = true
                                    imageCapture.takePicture(
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageCapturedCallback() {
                                            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                                try {
                                                    val mediaImage = imageProxy.image
                                                    if (mediaImage != null) {
                                                        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                                        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                                                        recognizer.process(inputImage)
                                                            .addOnSuccessListener { visionText ->
                                                                val textContent = visionText.text
                                                                val foundCodes = extractSpxCodes(textContent)

                                                                if (foundCodes.isNotEmpty()) {
                                                                    onOcrBulkCodes(foundCodes)
                                                                } else {
                                                                    Toast.makeText(context, "Không tìm thấy mã nào trong ảnh!", Toast.LENGTH_LONG).show()
                                                                }
                                                            }
                                                            .addOnFailureListener { e ->
                                                                Toast.makeText(context, "Lỗi quét chữ: ${e.message}", Toast.LENGTH_SHORT).show()
                                                            }
                                                            .addOnCompleteListener {
                                                                isProcessingOcr = false
                                                                imageProxy.close()
                                                            }
                                                    } else {
                                                        isProcessingOcr = false
                                                        imageProxy.close()
                                                    }
                                                } catch (ex: Exception) {
                                                    isProcessingOcr = false
                                                    imageProxy.close()
                                                }
                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                isProcessingOcr = false
                                                Toast.makeText(context, "Lỗi chụp ảnh: ${exception.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                } catch (e: Exception) {
                                    isProcessingOcr = false
                                    Toast.makeText(context, "Không thể chụp: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.size(75.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(ThemePrimary, shape = CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Bấm nút hồng để quét chữ nhãn dán đơn hàng",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    Text(
                        text = "Đưa Camera hướng vào mã vạch của đơn hàng",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                }
            }

            LaunchedEffect(previewViewReference, isOcrMode) {
                val viewRef = previewViewReference ?: return@LaunchedEffect
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(viewRef.surfaceProvider)
                    }
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        if (isOcrMode) {
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } else {
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(cameraExecutor, ScanAnalyzer(
                                        onBarcodeDetected = { code ->
                                            ContextCompat.getMainExecutor(context).execute {
                                                onCodeScanned(code)
                                            }
                                        }
                                    ))
                                }
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("CameraScannerView", "Lỗi bind camera", e)
                    }
                }, ContextCompat.getMainExecutor(context))
            }

            DisposableEffect(Unit) {
                onDispose {
                    try {
                        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                        cameraProvider.unbindAll()
                    } catch (e: Exception) {
                        Log.e("CameraScannerView", "Lỗi unbind camera", e)
                    }
                }
            }
        }
    }
}

class ScanAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private val barcodeScanner = BarcodeScanning.getClient()

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        try {
            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                imageProxy.close()
                return
            }
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (rawValue != null && rawValue.isNotBlank()) {
                            onBarcodeDetected(rawValue.trim().uppercase(Locale.ROOT))
                            break
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ScanAnalyzer", "Lỗi quét mã vạch", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } catch (e: Exception) {
            Log.e("ScanAnalyzer", "Lỗi phân tích ảnh", e)
            imageProxy.close()
        }
    }
}

fun updateHistoryFailed(historyList: MutableList<PrintHistoryItem>, code: String, error: String) {}

fun extractPupCode(text: String): String? {
    if (text.isBlank()) return null
    // 1. Remove all spaces and newlines
    val cleanText = text.replace("\\s".toRegex(), "").uppercase(Locale.ROOT)
    
    // 2. Match: PUP (or variants) + 8 date chars + 4-to-8 suffix chars
    val regex = Regex("(PUP|FUP|PLP|P0P|POP|POD|PU|PLP|F0P|PUPP)([0-9OILSGZ]{8})([A-Z0-9]{4,8})")
    val match = regex.find(cleanText)
    if (match != null) {
        val datePart = match.groupValues[2].map { char ->
            when (char) {
                'O' -> '0'
                'I', 'L' -> '1'
                'S' -> '5'
                'G' -> '6'
                'Z' -> '2'
                else -> char
            }
        }.joinToString("")
        val suffixPart = match.groupValues[3]
        return "PUP" + datePart + suffixPart
    }
    
    // Fallback: match PUP (or variants) + 12-to-16 alphanumeric chars
    val simpleRegex = Regex("(PUP|FUP|PLP|P0P|POP|POD|PU)([A-Z0-9]{12,16})")
    val simpleMatch = simpleRegex.find(cleanText)
    if (simpleMatch != null) {
        val full = simpleMatch.groupValues[2]
        val datePart = full.substring(0, 8).map { char ->
            when (char) {
                'O' -> '0'
                'I', 'L' -> '1'
                'S' -> '5'
                'G' -> '6'
                'Z' -> '2'
                else -> char
            }
        }.joinToString("")
        val suffixPart = full.substring(8)
        return "PUP" + datePart + suffixPart
    }
    
    // Backup fallback: Just find any sequence of 8 digits + 4-to-8 alphanumeric chars
    val backupRegex = Regex("([0-9]{8})([A-Z0-9]{4,8})")
    val backupMatch = backupRegex.find(cleanText)
    if (backupMatch != null) {
        return "PUP" + backupMatch.value
    }
    return null
}

@Composable
fun Icon3D(
    imageVector: ImageVector,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 24.dp
) {
    Box(modifier = modifier.size(size)) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = Color.Black.copy(alpha = 0.3f),
            modifier = Modifier
                .size(size)
                .offset(y = 2.dp)
        )
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size)
        )
    }
}

@Composable
fun BadgeItem(text: String, isDark: Boolean) {
    Box(
        modifier = Modifier
            .background(
                color = if (isDark) Color(0xFF131536).copy(alpha = 0.8f) else Color(0xFFE2E7F5).copy(alpha = 0.8f),
                shape = RoundedCornerShape(6.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(ThemePrimary, ThemeCyan)),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = if (isDark) ThemeCyan else Color(0xFF00838F),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AndroidMascotHead() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        // Draw head (semi-circle or rounded rectangle at bottom)
        val headPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.2f, h * 0.8f)
            lineTo(w * 0.8f, h * 0.8f)
            quadraticTo(w * 0.8f, h * 0.4f, w * 0.5f, h * 0.4f)
            quadraticTo(w * 0.2f, h * 0.4f, w * 0.2f, h * 0.8f)
        }
        drawPath(headPath, color = Color(0xFF00C853))
        
        // Draw eyes
        drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(w * 0.38f, h * 0.6f))
        drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(w * 0.62f, h * 0.6f))
        
        // Draw antennas
        drawLine(
            color = Color(0xFF00C853),
            start = Offset(w * 0.35f, h * 0.45f),
            end = Offset(w * 0.25f, h * 0.25f),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = Color(0xFF00C853),
            start = Offset(w * 0.65f, h * 0.45f),
            end = Offset(w * 0.75f, h * 0.25f),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
fun TopBarLogo(isDark: Boolean) {
    val textMeasurer = rememberTextMeasurer()
    var stateTime by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16) // ~60fps
            stateTime = (stateTime + 0.016f) % 24f // Loop animation every 24s
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            
            // Mockup Theme Colors
            val capsuleBg = Color(0xFFE8EDF9)   // Light purple/blue grey capsule plate
            val capsuleBorder = Color(0xFFC5D2F0) // Subtle blue border
            val fontBlue = Color(0xFF1E29B9)      // Deep blue/purple text color
            val robotTeal = Color(0xFF009688)     // Teal color for Robot 1
            val robotMagenta = Color(0xFFC2185B)  // Magenta color for Robot 2

            // Helper to draw futuristic letter/text with glowing neon, chip structure, and 3D layers
            val drawFuturisticText = { text: String, tx: Float, ty: Float, opacity: Float, wireType: Int ->
                if (opacity > 0f) {
                    val paint = android.graphics.Paint().apply {
                        color = fontBlue.copy(alpha = opacity).toArgb()
                        textSize = 12.dp.toPx()
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        isAntiAlias = true
                    }
                    
                    val bounds = android.graphics.Rect()
                    paint.getTextBounds(text, 0, text.length, bounds)
                    val lw = bounds.width().toFloat()
                    val lh = bounds.height().toFloat()

                    // Safe box padding
                    val boxW = lw + 12.dp.toPx()
                    val boxH = lh + 8.dp.toPx()
                    val bx = tx - boxW / 2
                    val by = ty - lh - 4.dp.toPx() // Adjusted to use height bounds properly

                    // Draw Futuristic Wires (Capsule Wires from Image)
                    if (wireType == 1) { // TRỢ: wire down to base
                        drawLine(
                            color = Color(0xFF00B0FF).copy(alpha = opacity),
                            start = Offset(bx + boxW / 2, by + boxH),
                            end = Offset(bx + boxW / 2 - 6.dp.toPx(), by + boxH + 8.dp.toPx()),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawLine(
                            color = Color(0xFF00B0FF).copy(alpha = opacity),
                            start = Offset(bx + boxW / 2 - 6.dp.toPx(), by + boxH + 8.dp.toPx()),
                            end = Offset(bx + boxW / 2 + 4.dp.toPx(), by + boxH + 16.dp.toPx()),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawLine(
                            color = Color.Gray.copy(alpha = opacity),
                            start = Offset(bx + boxW / 2 - 15.dp.toPx(), by + boxH + 16.dp.toPx()),
                            end = Offset(bx + boxW / 2 + 15.dp.toPx(), by + boxH + 16.dp.toPx()),
                            strokeWidth = 2.dp.toPx()
                        )
                    } else if (wireType == 2) { // VẬN: wire up to top
                        drawOval(
                            color = fontBlue.copy(alpha = opacity),
                            topLeft = Offset(bx + boxW / 2 - 10.dp.toPx(), by - 12.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(20.dp.toPx(), 4.dp.toPx())
                        )
                        drawLine(
                            color = Color.Gray.copy(alpha = opacity),
                            start = Offset(bx + boxW / 2 - 4.dp.toPx(), by),
                            end = Offset(bx + boxW / 2 - 4.dp.toPx(), by - 10.dp.toPx()),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = Color.Gray.copy(alpha = opacity),
                            start = Offset(bx + boxW / 2 + 4.dp.toPx(), by),
                            end = Offset(bx + boxW / 2 + 4.dp.toPx(), by - 10.dp.toPx()),
                            strokeWidth = 1.dp.toPx()
                        )
                    } else if (wireType == 3) { // HÀNH: 2 wires right
                        drawLine(
                            color = robotMagenta.copy(alpha = opacity),
                            start = Offset(bx + boxW, by + boxH / 2 - 2.dp.toPx()),
                            end = Offset(bx + boxW + 8.dp.toPx(), by + boxH / 2 - 2.dp.toPx()),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawLine(
                            color = robotMagenta.copy(alpha = opacity),
                            start = Offset(bx + boxW, by + boxH / 2 + 2.dp.toPx()),
                            end = Offset(bx + boxW + 8.dp.toPx(), by + boxH / 2 + 2.dp.toPx()),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawRect(
                            color = robotMagenta.copy(alpha = opacity),
                            topLeft = Offset(bx + boxW + 8.dp.toPx(), by + boxH / 2 - 4.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(4.dp.toPx(), 8.dp.toPx())
                        )
                    }

                    // Draw capsule container box
                    drawRoundRect(
                        color = capsuleBg.copy(alpha = opacity),
                        topLeft = Offset(bx, by),
                        size = androidx.compose.ui.geometry.Size(boxW, boxH),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                    drawRoundRect(
                        color = capsuleBorder.copy(alpha = opacity),
                        topLeft = Offset(bx, by),
                        size = androidx.compose.ui.geometry.Size(boxW, boxH),
                        cornerRadius = CornerRadius(8.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f.dp.toPx())
                    )

                    // Capsule inner circuitry dashes
                    val dashW = 2.dp.toPx()
                    val dashH = 1.dp.toPx()
                    drawRect(color = fontBlue.copy(alpha = opacity * 0.4f), topLeft = Offset(bx + 3.dp.toPx(), by + boxH/2 - 2.dp.toPx()), size = androidx.compose.ui.geometry.Size(dashW, dashH))
                    drawRect(color = fontBlue.copy(alpha = opacity * 0.4f), topLeft = Offset(bx + 3.dp.toPx(), by + boxH/2), size = androidx.compose.ui.geometry.Size(dashW, dashH))
                    drawRect(color = fontBlue.copy(alpha = opacity * 0.4f), topLeft = Offset(bx + 3.dp.toPx(), by + boxH/2 + 2.dp.toPx()), size = androidx.compose.ui.geometry.Size(dashW, dashH))
                    
                    drawRect(color = fontBlue.copy(alpha = opacity * 0.4f), topLeft = Offset(bx + boxW - 5.dp.toPx(), by + boxH/2 - 2.dp.toPx()), size = androidx.compose.ui.geometry.Size(dashW, dashH))
                    drawRect(color = fontBlue.copy(alpha = opacity * 0.4f), topLeft = Offset(bx + boxW - 5.dp.toPx(), by + boxH/2), size = androidx.compose.ui.geometry.Size(dashW, dashH))
                    drawRect(color = fontBlue.copy(alpha = opacity * 0.4f), topLeft = Offset(bx + boxW - 5.dp.toPx(), by + boxH/2 + 2.dp.toPx()), size = androidx.compose.ui.geometry.Size(dashW, dashH))

                    // Draw text centered natively to avoid maxWidth layout engine crashes completely
                    drawContext.canvas.nativeCanvas.drawText(
                        text,
                        tx - lw / 2,
                        ty,
                        paint
                    )
                }
            }

            // Draw Robot 1 (Cute Teal Stick Droid holding HỖ box on head)
            val drawRobot1 = { rx: Float, ry: Float, isFlat: Boolean, throwProgress: Float ->
                if (isFlat) {
                    // Knocked out rotated layout on floor
                    drawRoundRect(
                        color = robotTeal.copy(alpha = 0.5f),
                        topLeft = Offset(rx - 8.dp.toPx(), ry + 4.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(16.dp.toPx(), 10.dp.toPx()),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                    // Stars
                    val angle1 = (stateTime * 5f)
                    val starDistance = 8.dp.toPx()
                    for (i in 0..2) {
                        val sa = angle1 + (i * 120f) * (Math.PI / 180f)
                        val sx = rx + starDistance * java.lang.Math.cos(sa).toFloat()
                        val sy = ry - 6.dp.toPx() + (2.dp.toPx()) * java.lang.Math.sin(sa).toFloat()
                        drawCircle(color = Color.Yellow, radius = 1.5f.dp.toPx(), center = Offset(sx, sy))
                    }
                } else {
                    // Teal block body
                    drawRoundRect(
                        color = robotTeal,
                        topLeft = Offset(rx - 8.dp.toPx(), ry - 6.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(16.dp.toPx(), 11.dp.toPx()),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                    // Two cute circular white eyes
                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(rx - 3.dp.toPx(), ry - 1.dp.toPx()))
                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(rx + 3.dp.toPx(), ry - 1.dp.toPx()))
                    
                    // Walking legs animation (Alternating leg slant angles using sine wave)
                    val legSwing = java.lang.Math.sin(stateTime.toDouble() * 15.0).toFloat() * 3.dp.toPx()
                    drawLine(color = Color.DarkGray, start = Offset(rx - 4.dp.toPx(), ry + 5.dp.toPx()), end = Offset(rx - 4.dp.toPx() - 2.dp.toPx() + legSwing, ry + 11.dp.toPx()), strokeWidth = 2.dp.toPx())
                    drawLine(color = Color.DarkGray, start = Offset(rx + 4.dp.toPx(), ry + 5.dp.toPx()), end = Offset(rx + 4.dp.toPx() - 2.dp.toPx() - legSwing, ry + 11.dp.toPx()), strokeWidth = 2.dp.toPx())

                    // Carrying/Throwing arms (slanted magenta stick arms up towards HỖ box, opening wide when throwing)
                    val leftArmEndX = rx - 11.dp.toPx() - 4.dp.toPx() * throwProgress
                    val leftArmEndY = ry - 11.dp.toPx() + 7.dp.toPx() * throwProgress
                    val rightArmEndX = rx + 11.dp.toPx() + 4.dp.toPx() * throwProgress
                    val rightArmEndY = ry - 11.dp.toPx() + 7.dp.toPx() * throwProgress

                    drawLine(color = Color(0xFFD81B60), start = Offset(rx - 7.dp.toPx(), ry - 2.dp.toPx()), end = Offset(leftArmEndX, leftArmEndY), strokeWidth = 2.dp.toPx())
                    drawLine(color = Color(0xFFD81B60), start = Offset(rx + 7.dp.toPx(), ry - 2.dp.toPx()), end = Offset(rightArmEndX, rightArmEndY), strokeWidth = 2.dp.toPx())
                }
            }

            // Draw Robot 2 (Clone of Robot 1 with Flipped Color Palette: Magenta body, Magenta arms, Teal antenna/eyes)
            val drawRobot2 = { rx: Float, ry: Float, hammerProg: Float ->
                // Magenta body
                drawRoundRect(
                    color = robotMagenta,
                    topLeft = Offset(rx - 8.dp.toPx(), ry - 6.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(16.dp.toPx(), 11.dp.toPx()),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
                // Teal eyes
                drawCircle(color = robotTeal, radius = 2.dp.toPx(), center = Offset(rx - 3.dp.toPx(), ry - 1.dp.toPx()))
                drawCircle(color = robotTeal, radius = 2.dp.toPx(), center = Offset(rx + 3.dp.toPx(), ry - 1.dp.toPx()))
                
                // Walking legs animation
                val legSwing2 = java.lang.Math.sin(stateTime.toDouble() * 15.0).toFloat() * 3.dp.toPx()
                drawLine(color = Color.DarkGray, start = Offset(rx - 4.dp.toPx(), ry + 5.dp.toPx()), end = Offset(rx - 4.dp.toPx() - 2.dp.toPx() + legSwing2, ry + 11.dp.toPx()), strokeWidth = 2.dp.toPx())
                drawLine(color = Color.DarkGray, start = Offset(rx + 4.dp.toPx(), ry + 5.dp.toPx()), end = Offset(rx + 4.dp.toPx() - 2.dp.toPx() - legSwing2, ry + 11.dp.toPx()), strokeWidth = 2.dp.toPx())

                // Draw hammer (Robot 2 always carries the hammer throughout its appearance)
                val swingAngle = -45f + (135f * hammerProg)
                val angleRad = swingAngle * (Math.PI / 180f)
                val handleLen = 14.dp.toPx()
                val startX = rx - 4.dp.toPx()
                val startY = ry - 1.dp.toPx()
                val endX = startX + handleLen * java.lang.Math.cos(angleRad).toFloat()
                val endY = startY - handleLen * java.lang.Math.sin(angleRad).toFloat()
                
                // Left arm holds hammer
                drawLine(color = robotTeal, start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = 2.dp.toPx())
                drawRoundRect(
                    color = Color.DarkGray,
                    topLeft = Offset(endX - 4.dp.toPx(), endY - 3.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(8.dp.toPx(), 6.dp.toPx()),
                    cornerRadius = CornerRadius(1.dp.toPx())
                )
                // Right arm in default state
                drawLine(color = robotMagenta, start = Offset(rx + 7.dp.toPx(), ry - 2.dp.toPx()), end = Offset(rx + 11.dp.toPx(), ry - 11.dp.toPx()), strokeWidth = 2.dp.toPx())
            }

            // --- ANIMATION TIMELINE CALCULATIONS ---
            // Center the baseline vertically correctly: yBase h/2 + 2.dp.toPx() keeps it centered vertically
            val yBase = h / 2 + 2.dp.toPx()

            if (stateTime in 0.0f..5.0f) {
                // Phase 1: Robot 1 carrying "HỖ" runs back and forth strictly on the left (w*0.06f to w*0.26f)
                val cycle = java.lang.Math.sin((stateTime / 5.0f) * 3.0f * Math.PI * 2.0f).toFloat()
                val r1X = w * 0.16f + (w * 0.10f * cycle)
                val r1Y = yBase + 12.dp.toPx()
                
                // Centered coordinates for HỖ TRỢ VẬN HÀNH
                // Spacing w*0.38, w*0.60, w*0.82
                drawFuturisticText("TRỢ", w * 0.38f, yBase, 1.0f, 1)
                drawFuturisticText("VẬN", w * 0.60f, yBase, 1.0f, 2)
                drawFuturisticText("HÀNH", w * 0.82f, yBase, 1.0f, 3)

                // Carrying "HỖ" on head - lowered y-offset down to r1Y - 17.dp.toPx()
                drawFuturisticText("HỖ", r1X, r1Y - 17.dp.toPx(), 1.0f, 0)
                drawRobot1(r1X, r1Y, false, 0f)

            } else if (stateTime in 5.0f..6.0f) {
                // Phase 2: Robot 1 stops and throws "HỖ" up to disappear
                val t = stateTime - 5.0f
                val r1X = w * 0.16f
                val r1Y = yBase + 12.dp.toPx()
                
                drawFuturisticText("TRỢ", w * 0.38f, yBase, 1.0f, 1)
                drawFuturisticText("VẬN", w * 0.60f, yBase, 1.0f, 2)
                drawFuturisticText("HÀNH", w * 0.82f, yBase, 1.0f, 3)

                // "HỖ" flying up & fading
                val flyY = (r1Y - 17.dp.toPx()) - (t * 80.dp.toPx())
                val hoOpacity = (1.0f - t).coerceIn(0f, 1f)
                drawFuturisticText("HỖ", r1X, flyY, hoOpacity, 0)
                drawRobot1(r1X, r1Y, false, t) // Arm throw gesture progress 't'

            } else if (stateTime in 6.0f..7.5f) {
                // Phase 3: Robot 1 walks to "TRỢ" and kicks it to the right off-screen
                val t = stateTime - 6.0f
                val startR1X = w * 0.16f
                val endR1X = w * 0.34f // stops just left of TRỢ (w * 0.38) to kick
                val r1X = startR1X + (endR1X - startR1X) * (t / 1.5f).coerceIn(0f, 1f)
                val r1Y = yBase + 12.dp.toPx()

                drawFuturisticText("VẬN", w * 0.60f, yBase, 1.0f, 2)
                drawFuturisticText("HÀNH", w * 0.82f, yBase, 1.0f, 3)

                // "TRỢ" flies off if kick has happened (let's say kick at t > 0.8s)
                if (t < 0.8f) {
                    drawFuturisticText("TRỢ", w * 0.38f, yBase, 1.0f, 1)
                } else {
                    val kickProgress = ((t - 0.8f) / 0.7f).coerceIn(0f, 1f)
                    val troX = w * 0.38f + kickProgress * w * 0.8f
                    val troY = yBase - (java.lang.Math.sin(kickProgress * Math.PI).toFloat() * 15.dp.toPx())
                    val troOpacity = (1.0f - kickProgress).coerceIn(0f, 1f)
                    drawFuturisticText("TRỢ", troX, troY, troOpacity, 1)
                }
                drawRobot1(r1X, r1Y, false, 0f)

            } else if (stateTime in 7.5f..9.0f) {
                // Phase 4: Robot 1 jumps on top of "VẬN" breaking and sinking it
                val t = stateTime - 7.5f
                val startR1X = w * 0.38f
                val endR1X = w * 0.64f // on top of VẬN (w * 0.60)
                val r1X = startR1X + (endR1X - startR1X) * (t / 1.5f).coerceIn(0f, 1f)
                
                // Jump arc
                val jumpProgress = (t / 1.5f).coerceIn(0f, 1f)
                val r1Y = (yBase + 12.dp.toPx()) - (java.lang.Math.sin(jumpProgress * Math.PI).toFloat() * 20.dp.toPx())

                drawFuturisticText("HÀNH", w * 0.82f, yBase, 1.0f, 3)

                // VẬN behavior: breaks and sinks on land (land happens at jumpProgress > 0.8s)
                if (jumpProgress < 0.8f) {
                    drawFuturisticText("VẬN", w * 0.60f, yBase, 1.0f, 2)
                } else {
                    val sinkProgress = (jumpProgress - 0.8f) / 0.2f
                    val vanY = yBase + sinkProgress * 40.dp.toPx()
                    // Draw VẬN broken/split
                    drawFuturisticText("VẬ", w * 0.60f - 5f * sinkProgress, vanY, 1.0f - sinkProgress, 0)
                    drawFuturisticText("N", w * 0.65f + 5f * sinkProgress, vanY, 1.0f - sinkProgress, 0)
                }
                drawRobot1(r1X, r1Y, false, 0f)

            } else if (stateTime in 9.0f..10.5f) {
                // Phase 5: Robot 1 walks to drag "HÀNH" to the left (approx w * 0.22f)
                val t = stateTime - 9.0f
                val walkProgress = (t / 1.5f).coerceIn(0f, 1f)
                
                // Robot runs right, grabs HÀNH at w * 0.88f, drags it left to w * 0.22f
                val r1X = if (walkProgress < 0.4f) {
                    w * 0.64f + (w * 0.26f * (walkProgress / 0.4f)) // walks to grab point w*0.90
                } else {
                    w * 0.90f - (w * 0.72f * ((walkProgress - 0.4f) / 0.6f)) // drags to w*0.18
                }
                val r1Y = yBase + 12.dp.toPx()

                // "HÀNH" position
                val hanhX = if (walkProgress < 0.4f) {
                    w * 0.82f
                } else {
                    w * 0.82f - (w * 0.64f * ((walkProgress - 0.4f) / 0.6f)) // drags HÀNH to w*0.18
                }
                drawFuturisticText("HÀNH", hanhX, yBase, 1.0f, 3)
                drawRobot1(r1X, r1Y, false, 0f)

            } else if (stateTime in 10.5f..12.5f) {
                // Phase 6: Robot 1 runs to w * 0.50f, jumps to pull down "LÀ"
                val t = stateTime - 10.5f
                val progress = (t / 2.0f).coerceIn(0f, 1f)
                
                val r1X: Float
                val r1Y: Float
                if (progress < 0.4f) {
                    val runProg = progress / 0.4f
                    r1X = w * 0.18f + (w * 0.32f * runProg)
                    r1Y = yBase + 12.dp.toPx()
                } else if (progress < 0.7f) {
                    val jumpProg = (progress - 0.4f) / 0.3f
                    r1X = w * 0.50f
                    r1Y = (yBase + 12.dp.toPx()) - (java.lang.Math.sin(jumpProg * Math.PI).toFloat() * 18.dp.toPx())
                } else {
                    r1X = w * 0.50f
                    r1Y = yBase + 12.dp.toPx()
                }

                drawFuturisticText("HÀNH", w * 0.18f, yBase, 1.0f, 0)

                // "LÀ" drops from top after Robot 1 jumps and pulls it at progress > 0.7
                if (progress > 0.7f) {
                    val dropProgress = (progress - 0.7f) / 0.3f
                    val laY = -15.dp.toPx() + (yBase + 15.dp.toPx()) * dropProgress
                    drawFuturisticText("LÀ", w * 0.50f, laY, 1.0f, 0)
                }
                drawRobot1(r1X, r1Y, false, 0f)

            } else if (stateTime in 12.5f..14.5f) {
                // Phase 7: Robot 1 walks right to pull/drag "CHÍNH" in from off-screen right to w * 0.78f
                val t = stateTime - 12.5f
                val walkProgress = (t / 2.0f).coerceIn(0f, 1f)

                drawFuturisticText("HÀNH", w * 0.18f, yBase, 1.0f, 0)
                drawFuturisticText("LÀ", w * 0.50f, yBase, 1.0f, 0)

                val r1X = if (walkProgress < 0.5f) {
                    w * 0.50f + (w * 0.46f * (walkProgress / 0.5f)) // walks all the way to w*0.96
                } else {
                    w * 0.96f - (w * 0.14f * ((walkProgress - 0.5f) / 0.5f)) // drags to w*0.82
                }
                val r1Y = yBase + 12.dp.toPx()

                // "CHÍNH" dragged from w * 1.0f to w * 0.82f
                val chinX = if (walkProgress < 0.5f) {
                    w * 1.0f
                } else {
                    w * 1.0f - (w * 0.18f * ((walkProgress - 0.5f) / 0.5f))
                }
                drawFuturisticText("CHÍNH", chinX, yBase, 1.0f, 0)
                drawRobot1(r1X, r1Y, false, 0f)

            } else if (stateTime in 14.5f..16.0f) {
                // Phase 8: Robot 2 enters, swings hammer, hits Robot 1, Robot 1 knocked out
                val t = stateTime - 14.5f
                val enterProgress = (t / 1.5f).coerceIn(0f, 1f)

                drawFuturisticText("HÀNH", w * 0.18f, yBase, 1.0f, 0)
                drawFuturisticText("LÀ", w * 0.50f, yBase, 1.0f, 0)
                drawFuturisticText("CHÍNH", w * 0.82f, yBase, 1.0f, 0)

                val r2X: Float
                val hammerProgress: Float
                val isR1KnockedOut: Boolean

                if (enterProgress < 0.6f) {
                    val walkProg = enterProgress / 0.6f
                    r2X = w * 1.0f - (w * 0.10f * walkProg) // walks from w*1.0 to w*0.90
                    hammerProgress = 0f
                    isR1KnockedOut = false
                } else {
                    val swingProg = (enterProgress - 0.6f) / 0.4f
                    r2X = w * 0.90f
                    hammerProgress = swingProg
                    isR1KnockedOut = swingProg > 0.8f // Robot 1 only falls after hammer hit
                }

                val r2Y = yBase + 12.dp.toPx()

                drawRobot1(w * 0.82f, yBase + 12.dp.toPx(), isR1KnockedOut, 0f)
                drawRobot2(r2X, r2Y, hammerProgress)

            } else if (stateTime in 16.0f..17.5f) {
                // Phase 9: Robot 2 walks to w * 0.86f (close to CHÍNH) and kicks it, holds hammer up (1.0f)
                val t = stateTime - 16.0f
                val progress = (t / 1.5f).coerceIn(0f, 1f)

                drawFuturisticText("HÀNH", w * 0.18f, yBase, 1.0f, 0)
                drawFuturisticText("LÀ", w * 0.50f, yBase, 1.0f, 0)

                val r2X: Float
                if (progress < 0.4f) {
                    val walkProg = progress / 0.4f
                    r2X = w * 0.90f - (w * 0.04f * walkProg) // walks close to w*0.86
                    drawFuturisticText("CHÍNH", w * 0.82f, yBase, 1.0f, 0)
                } else {
                    val kickProg = (progress - 0.4f) / 0.6f
                    r2X = w * 0.86f
                    // CHÍNH flies right and disappears
                    val chinX = w * 0.82f + kickProg * w * 0.6f
                    val chinY = yBase - (java.lang.Math.sin(kickProg * Math.PI).toFloat() * 12.dp.toPx())
                    val chinOpacity = (1.0f - kickProg).coerceIn(0f, 1f)
                    drawFuturisticText("CHÍNH", chinX, chinY, chinOpacity, 0)
                }

                val r2Y = yBase + 12.dp.toPx()
                // Robot 1 is knocked out on floor
                drawRobot1(w * 0.82f, yBase + 12.dp.toPx(), true, 0f)
                drawRobot2(r2X, r2Y, 1.0f)

            } else if (stateTime in 17.5f..19.0f) {
                // Phase 10: Robot 2 walks close to w * 0.54f (next to LÀ) and kicks it, holds hammer up (1.0f)
                val t = stateTime - 17.5f
                val progress = (t / 1.5f).coerceIn(0f, 1f)

                drawFuturisticText("HÀNH", w * 0.18f, yBase, 1.0f, 0)

                val r2X: Float
                if (progress < 0.5f) {
                    val walkProg = progress / 0.5f
                    r2X = w * 0.86f - (w * 0.32f * walkProg) // walks close to w*0.54
                    drawFuturisticText("LÀ", w * 0.50f, yBase, 1.0f, 0)
                } else {
                    val kickProg = (progress - 0.5f) / 0.5f
                    r2X = w * 0.54f
                    // "LÀ" flies up
                    val laY = yBase - kickProg * 80.dp.toPx()
                    val laOpacity = (1.0f - kickProg).coerceIn(0f, 1f)
                    drawFuturisticText("LÀ", w * 0.50f, laY, laOpacity, 0)
                }

                val r2Y = yBase + 12.dp.toPx()
                drawRobot1(w * 0.82f, yBase + 12.dp.toPx(), true, 0f)
                drawRobot2(r2X, r2Y, 1.0f)

            } else if (stateTime in 19.0f..20.0f) {
                // Phase 11: Robot 2 jumps on top of "HÀNH" sinking it, holds hammer up (1.0f)
                val t = stateTime - 19.0f
                val progress = (t / 1.0f).coerceIn(0f, 1f)

                // Jump arc of Robot 2 (lands on HÀNH at w * 0.18f)
                val r2X = w * 0.54f - (w * 0.36f * progress)
                val r2Y = (yBase + 12.dp.toPx()) - (java.lang.Math.sin(progress * Math.PI).toFloat() * 16.dp.toPx())

                // "HÀNH" sinks when Robot 2 lands (progress > 0.8)
                if (progress < 0.8f) {
                    drawFuturisticText("HÀNH", w * 0.18f, yBase, 1.0f, 0)
                } else {
                    val sinkProgress = ((progress - 0.8f) / 0.2f).coerceIn(0f, 1f)
                    val hanhOpacity = (1.0f - sinkProgress).coerceIn(0f, 1f)
                    drawFuturisticText("HÀNH", w * 0.18f, yBase + sinkProgress * 40.dp.toPx(), hanhOpacity, 0)
                }

                drawRobot1(w * 0.82f, yBase + 12.dp.toPx(), true, 0f)
                drawRobot2(r2X, r2Y, 1.0f)

            } else if (stateTime in 20.0f..21.5f) {
                // Phase 12: Robot 2 stands on top of HÀNH (removed sinking animation), holds hammer up (1.0f)
                val r2X = w * 0.18f
                val r2Y = yBase + 12.dp.toPx()

                drawRobot1(w * 0.82f, yBase + 12.dp.toPx(), true, 0f)
                drawRobot2(r2X, r2Y, 1.0f)

            } else if (stateTime in 21.5f..22.5f) {
                // Phase 13: Robot 2 walks off-screen to the left from w*0.22, holds hammer up (1.0f)
                val t = stateTime - 21.5f
                val progress = (t / 1.0f).coerceIn(0f, 1f)

                val r2X = w * 0.22f - progress * w * 0.5f
                val r2Y = yBase + 12.dp.toPx()

                // Robot 1 is still flat on floor
                drawRobot1(w * 0.78f, yBase + 12.dp.toPx(), true, 0f)
                drawRobot2(r2X, r2Y, 1.0f)

            } else if (stateTime in 22.5f..24.0f) {
                // Phase 14: Robot 1 wakes up, runs left to grab HỖ and reset
                val t = stateTime - 22.5f
                val progress = (t / 1.5f).coerceIn(0f, 1f)

                val r1X = w * 0.78f - progress * w * 0.78f
                val r1Y = yBase + 12.dp.toPx()

                drawRobot1(r1X, r1Y, false, 0f)
            }
        }
    }
}

@Composable
fun SplashScreen(isDark: Boolean, onFinished: () -> Unit) {
    val cyanColor = ThemeCyan
    LaunchedEffect(Unit) {
        delay(2500)
        onFinished()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(getThemeBgBrush(isDark)),
        contentAlignment = Alignment.Center
    ) {
        AIParticleBackground()
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Rotating circle of dots
            val infiniteTransition = rememberInfiniteTransition(label = "dotsRotation")
            val angle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "angle"
            )
            
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                // Glow circles around the app logo
                Canvas(modifier = Modifier.size(240.dp)) {
                    val r = size.width / 2
                    val center = Offset(r, r)
                    val dotRadius = 4.dp.toPx()
                    val orbitalRadius = r - 10.dp.toPx()
                    
                    for (i in 0 until 8) {
                        val currentAngle = (angle + i * 45) * (Math.PI / 180f)
                        val x = center.x + orbitalRadius * Math.cos(currentAngle).toFloat()
                        val y = center.y + orbitalRadius * Math.sin(currentAngle).toFloat()
                        drawCircle(
                            color = cyanColor.copy(alpha = 0.8f),
                            radius = dotRadius,
                            center = Offset(x, y)
                        )
                    }
                }
                
                // App Logo in the center
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.VTDStadio.Hotroin.R.drawable.app_logo),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(30.dp))
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Badges HỖ, TRỢ, VẬN, HÀNH
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BadgeItem(text = "HỖ", isDark = isDark)
                BadgeItem(text = "TRỢ", isDark = isDark)
                BadgeItem(text = "VẬN", isDark = isDark)
                BadgeItem(text = "HÀNH", isDark = isDark)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Powered by VTD Stadio & Logistics Tech",
                color = getTextColor(isDark).copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AdminPupDriversSection(db: FirebaseFirestore) {
    val isDark = LocalThemeIsDark.current
    val context = LocalContext.current
    var driverName by remember { mutableStateOf("") }
    var driverCode by remember { mutableStateOf("") }
    var isOutHub by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var driversList by remember { mutableStateOf(listOf<Map<String, Any>>()) }

    LaunchedEffect(Unit) {
        db.collection("pup_drivers")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val list = mutableListOf<Map<String, Any>>()
                    for (doc in snapshot.documents) {
                        val data = doc.data
                        if (data != null) {
                            list.add(data)
                        }
                    }
                    driversList = list
                }
            }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = getThemeSurfaceColor(isDark).copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, getGlassBorderColor(isDark)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Quản lý Rider Nhận (Chuyển Pick)", color = ThemePrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = driverName,
                onValueChange = { driverName = it },
                placeholder = { Text("Tên Rider nhận", color = getTextColor(isDark).copy(alpha = 0.6f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = getTextColor(isDark),
                    unfocusedTextColor = getTextColor(isDark),
                    focusedBorderColor = ThemePrimary,
                    unfocusedBorderColor = if (isDark) ThemePrimary.copy(alpha = 0.4f) else ThemePrimary.copy(alpha = 0.5f),
                    unfocusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = if (isDark) 0.15f else 0.8f),
                    focusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = if (isDark) 0.25f else 0.95f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = driverCode,
                onValueChange = { driverCode = it },
                placeholder = { Text("Mã số Rider nhận (ví dụ: 93005)", color = getTextColor(isDark).copy(alpha = 0.6f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = getTextColor(isDark),
                    unfocusedTextColor = getTextColor(isDark),
                    focusedBorderColor = ThemePrimary,
                    unfocusedBorderColor = if (isDark) ThemePrimary.copy(alpha = 0.4f) else ThemePrimary.copy(alpha = 0.5f),
                    unfocusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = if (isDark) 0.15f else 0.8f),
                    focusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = if (isDark) 0.25f else 0.95f)
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { isOutHub = !isOutHub }
            ) {
                Checkbox(
                    checked = isOutHub,
                    onCheckedChange = { isOutHub = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ngoài Hub (Out of Hub rider)", color = getTextColor(isDark))
            }
            Spacer(modifier = Modifier.height(12.dp))

            Premium3DButton(
                onClick = {
                    if (driverName.isBlank() || driverCode.isBlank()) {
                        Toast.makeText(context, "Vui lòng điền đủ thông tin!", Toast.LENGTH_SHORT).show()
                        return@Premium3DButton
                    }
                    isProcessing = true
                    val codeDoc = driverCode.trim()
                    db.collection("pup_drivers").document(codeDoc)
                        .set(mapOf(
                            "name" to driverName.trim(),
                            "code" to codeDoc,
                            "isOutHub" to isOutHub
                        ))
                        .addOnSuccessListener {
                            isProcessing = false
                            Toast.makeText(context, "Đã thêm Rider nhận!", Toast.LENGTH_SHORT).show()
                            driverName = ""
                            driverCode = ""
                            isOutHub = false
                        }
                        .addOnFailureListener { e ->
                            isProcessing = false
                            Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                },
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth(),
                containerBrush = getButtonGradientBrush(isDark),
                shadowColor = ThemeAccent.copy(alpha = 0.5f)
            ) {
                Text("Thêm Rider Nhận", color = getButtonTextColor(), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Danh sách Rider nhận đã đồng bộ", color = ThemePrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            if (driversList.isEmpty()) {
                Text("Chưa có Rider nhận nào.", color = getTextColor(isDark).copy(alpha = 0.5f))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    driversList.forEachIndexed { index, data ->
                        val code = data["code"]?.toString() ?: ""
                        val name = data["name"]?.toString() ?: ""
                        val oHub = data["isOutHub"] as? Boolean ?: false
                        val suffix = if (oHub) " (Ngoài Hub)" else ""
                        if (index > 0) {
                            Divider(
                                color = getGlassBorderColor(isDark).copy(alpha = 0.4f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(getThemeSurfaceColor(isDark).copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
                                .border(1.dp, getGlassBorderColor(isDark), shape = RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$name - $code$suffix", color = getTextColor(isDark), fontWeight = FontWeight.Medium)
                            IconButton(onClick = {
                                db.collection("pup_drivers").document(code).delete()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Xóa Rider",
                                    tint = ThemePrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun ChuyenPickView(
    db: FirebaseFirestore,
    currentUserEmail: String,
    currentUserName: String,
    currentRiderCode: String,
    currentRiderRealName: String,
    currentUserRole: String,
    webAppUrl: String,
    sharedPreferences: SharedPreferences,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context
) {
    val isDark = LocalThemeIsDark.current
    var pupCode by remember { mutableStateOf("") }
    var selectedDriver by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var driversList by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var expanded by remember { mutableStateOf(false) }

    var activeSupportList by remember { mutableStateOf(listOf<SupportPushItem>()) }

    if (currentUserRole == "Admin") {
        LaunchedEffect(Unit) {
            db.collection("support_pushes")
                .orderBy("timeMs", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snap, err ->
                    if (snap != null) {
                        activeSupportList = snap.documents.mapNotNull { doc ->
                            val data = doc.data ?: return@mapNotNull null
                            SupportPushItem(
                                docId = doc.id,
                                pupCode = data["pupCode"]?.toString() ?: "",
                                senderName = data["senderName"]?.toString() ?: "Rider ẩn danh",
                                receiver = data["receiver"]?.toString() ?: "",
                                status = data["status"]?.toString() ?: "Pending"
                            )
                        }
                    }
                }
        }
    }

    LaunchedEffect(Unit) {
        db.collection("pup_drivers")
            .addSnapshotListener { snapshot, error ->
                if (snapshot != null) {
                    val list = mutableListOf<Map<String, Any>>()
                    for (doc in snapshot.documents) {
                        val data = doc.data
                        if (data != null) {
                            list.add(data)
                        }
                    }
                    driversList = list
                }
            }
    }

    val ocrPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        val inputImage = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
                        val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS)
                        recognizer.process(inputImage)
                            .addOnSuccessListener { visionText ->
                                val code = extractPupCode(visionText.text)
                                if (code != null) {
                                    pupCode = code
                                    Toast.makeText(context, "Đã tìm thấy mã PUP: $code", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Không tìm thấy mã PUP trong ảnh!", Toast.LENGTH_LONG).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Lỗi đọc chữ: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Lỗi mở ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    var supportPup by remember { mutableStateOf("") }
    var supportAddress by remember { mutableStateOf("") }
    var isSupportSubmitting by remember { mutableStateOf(false) }

    val supportOcrPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        val inputImage = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
                        val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS)
                        recognizer.process(inputImage)
                            .addOnSuccessListener { visionText ->
                                val code = extractPupCode(visionText.text)
                                if (code != null) {
                                    supportPup = code
                                    Toast.makeText(context, "Đã tìm thấy mã PUP hỗ trợ: $code", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Không tìm thấy mã PUP trong ảnh!", Toast.LENGTH_LONG).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Lỗi đọc chữ: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Lỗi mở ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    var searchDriverQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                "XỬ LÝ PICK",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = ThemePrimary,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = getGlassBorderColor(isDark),
                glowColor = ThemeCyan
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Button 1: Chọn Ảnh để quét mã PUP
                    Premium3DButton(
                        onClick = {
                            ocrPhotoLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        containerBrush = getButtonGradientBrush(isDark),
                        shadowColor = ThemePrimary.copy(alpha = 0.4f)
                    ) {
                        Icon3D(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Chọn ảnh quét PUP",
                            tint = getButtonTextColor(),
                            size = 20.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Chọn ảnh cần chuyển", color = getButtonTextColor(), fontWeight = FontWeight.Bold)
                    }

                    // TextField: Mã PUP
                    OutlinedTextField(
                        value = pupCode,
                        onValueChange = { pupCode = it.uppercase(java.util.Locale.ROOT) },
                        placeholder = { Text("Mã PUP (Pickup Point ID)", color = getTextColor(isDark).copy(alpha = 0.6f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = getTextColor(isDark),
                            unfocusedTextColor = getTextColor(isDark),
                            focusedBorderColor = ThemePrimary,
                            unfocusedBorderColor = if (isDark) ThemePrimary.copy(alpha = 0.4f) else ThemePrimary.copy(alpha = 0.5f),
                            unfocusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = if (isDark) 0.15f else 0.8f),
                            focusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = if (isDark) 0.25f else 0.95f)
                        )
                    )

                    // Dropdown/Selector: Chuyển Rider nhận
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedDriver,
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Chuyển Rider nhận", color = getTextColor(isDark).copy(alpha = 0.6f)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = true },
                            enabled = false, // disabled to pass click event to container clickable
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = getTextColor(isDark),
                                disabledBorderColor = if (isDark) ThemePrimary.copy(alpha = 0.4f) else ThemePrimary.copy(alpha = 0.5f),
                                disabledContainerColor = getThemeSurfaceColor(isDark).copy(alpha = if (isDark) 0.15f else 0.8f),
                                disabledPlaceholderColor = getTextColor(isDark).copy(alpha = 0.6f)
                            ),
                            trailingIcon = {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = getTextColor(isDark))
                                }
                            }
                        )
                        // Invisible clickable overlay to ensure clicks work anywhere on the field
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { expanded = true }
                        )
                        if (expanded) {
                            var searchDriverQueryDialog by remember { mutableStateOf("") }
                            AlertDialog(
                                onDismissRequest = { expanded = false },
                                title = { Text("Chọn Rider nhận", color = ThemePrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                                text = {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = searchDriverQueryDialog,
                                            onValueChange = { searchDriverQueryDialog = it },
                                            placeholder = { Text("Tìm kiếm Rider...", color = getTextColor(isDark).copy(alpha = 0.5f)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = getTextColor(isDark),
                                                unfocusedTextColor = getTextColor(isDark),
                                                focusedBorderColor = ThemePrimary,
                                                unfocusedBorderColor = ThemePrimary.copy(alpha = 0.4f),
                                                unfocusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = 0.1f),
                                                focusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = 0.15f)
                                            )
                                        )

                                        val filteredDrivers = driversList
                                            .filter { it["isOutHub"] as? Boolean != true }
                                            .filter { driver ->
                                                val name = driver["name"]?.toString() ?: ""
                                                val code = driver["code"]?.toString() ?: ""
                                                name.contains(searchDriverQueryDialog, ignoreCase = true) || code.contains(searchDriverQueryDialog, ignoreCase = true)
                                            }

                                        LazyColumn(
                                            modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (filteredDrivers.isEmpty()) {
                                                item {
                                                    Text("Không tìm thấy Rider nào", color = getTextColor(isDark).copy(alpha = 0.6f), modifier = Modifier.padding(8.dp))
                                                }
                                            } else {
                                                items(filteredDrivers) { driver ->
                                                    val dName = driver["name"]?.toString() ?: ""
                                                    val dCode = driver["code"]?.toString() ?: ""
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                selectedDriver = "$dName - $dCode"
                                                                expanded = false
                                                            },
                                                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                                    ) {
                                                        Column(modifier = Modifier.padding(12.dp)) {
                                                            Text("$dName - $dCode", color = getTextColor(isDark), fontWeight = FontWeight.Medium)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { expanded = false }) {
                                        Text("ĐÓNG", color = ThemePrimary)
                                    }
                                },
                                containerColor = getThemeSurfaceColor(isDark)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Button Chuyển
                    Premium3DButton(
                        onClick = {
                            if (pupCode.isBlank()) {
                                Toast.makeText(context, "Vui lòng nhập hoặc quét mã PUP!", Toast.LENGTH_SHORT).show()
                                return@Premium3DButton
                            }
                            if (selectedDriver.isBlank()) {
                                Toast.makeText(context, "Vui lòng chọn Rider nhận!", Toast.LENGTH_SHORT).show()
                                return@Premium3DButton
                            }
                            isSending = true
                            val timeString = SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault()).format(Date())
                            val senderName = if (currentUserRole == "Admin") {
                                currentUserName
                            } else {
                                val finalRiderRealName = sharedPreferences.getString("rider_real_name", "") ?: currentRiderRealName
                                if (finalRiderRealName.isNotBlank()) {
                                    "$finalRiderRealName ($currentRiderCode)"
                                } else {
                                    "$currentUserName ($currentRiderCode)"
                                }
                            }

                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val client = okhttp3.OkHttpClient()
                                    val bodyJson = JSONObject().apply {
                                        put("action", "chuyen_pick")
                                        put("riderName", senderName)
                                        put("pupCode", pupCode.trim().uppercase(Locale.ROOT))
                                        put("receiverRider", selectedDriver)
                                        put("timestamp", timeString)
                                        put("password", sharedPreferences.getString("sheet_password", "") ?: "")
                                    }
                                    val body = bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                                    val request = okhttp3.Request.Builder()
                                        .url(webAppUrl)
                                        .post(body)
                                        .build()

                                    client.newCall(request).execute().use { response ->
                                        val responseBody = response.body?.string()
                                        withContext(Dispatchers.Main) {
                                            isSending = false
                                            if (response.isSuccessful && responseBody != null) {
                                                // Save transfer log in Firestore transfers collection
                                                val targetPupClean = pupCode.trim().uppercase(Locale.ROOT)
                                                db.collection("transfers").document(targetPupClean)
                                                    .set(mapOf(
                                                        "pupCode" to targetPupClean,
                                                        "senderEmail" to currentUserEmail,
                                                        "senderName" to senderName,
                                                        "receiverRider" to selectedDriver,
                                                        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                                    ))

                                                Toast.makeText(context, "Chuyển Pick thành công!", Toast.LENGTH_SHORT).show()
                                                pupCode = ""
                                                selectedDriver = ""
                                            } else {
                                                Toast.makeText(context, "Lỗi chuyển: ${response.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isSending = false
                                        Toast.makeText(context, "Lỗi mạng: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        enabled = !isSending,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        containerBrush = getButtonGradientBrush(isDark),
                        shadowColor = ThemeAccent.copy(alpha = 0.5f)
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(color = getButtonTextColor(), modifier = Modifier.size(24.dp))
                        } else {
                            Icon3D(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Chuyển",
                                tint = getButtonTextColor(),
                                size = 20.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CHUYỂN", color = getButtonTextColor(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // TỐ CÁO ĐẨY SAI
        item {
            var reportPupCode by remember { mutableStateOf("") }
            var isReporting by remember { mutableStateOf(false) }

            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = getGlassBorderColor(isDark).copy(alpha = 0.8f),
                glowColor = ThemeAccent
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "TỐ CÁO ĐẨY SAI",
                        fontWeight = FontWeight.Bold,
                        color = ThemeAccent,
                        fontSize = 15.sp
                    )

                    OutlinedTextField(
                        value = reportPupCode,
                        onValueChange = { reportPupCode = it },
                        placeholder = { Text("Nhập mã PUP bị đẩy sai", color = getTextColor(isDark).copy(alpha = 0.6f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = getTextColor(isDark),
                            unfocusedTextColor = getTextColor(isDark),
                            focusedBorderColor = ThemeAccent,
                            unfocusedBorderColor = ThemeAccent.copy(alpha = 0.5f),
                            unfocusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = if (isDark) 0.15f else 0.8f),
                            focusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = if (isDark) 0.25f else 0.95f)
                        )
                    )

                    Premium3DButton(
                        onClick = {
                            if (reportPupCode.isBlank()) {
                                Toast.makeText(context, "Vui lòng nhập mã PUP cần tố cáo!", Toast.LENGTH_SHORT).show()
                                return@Premium3DButton
                            }
                            val targetPup = reportPupCode.trim().uppercase(Locale.ROOT)
                            isReporting = true

                            // Call Google Sheets webAppUrl to verify transfer
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val client = okhttp3.OkHttpClient()
                                    val bodyJson = JSONObject().apply {
                                        put("action", "check_transfer")
                                        put("pupCode", targetPup)
                                        put("password", sharedPreferences.getString("sheet_password", "") ?: "")
                                    }
                                    val body = bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                                    val request = okhttp3.Request.Builder()
                                        .url(webAppUrl)
                                        .post(body)
                                        .build()

                                    client.newCall(request).execute().use { response ->
                                        val responseBody = response.body?.string()
                                        withContext(Dispatchers.Main) {
                                            if (response.isSuccessful && responseBody != null) {
                                                try {
                                                    val respJson = JSONObject(responseBody)
                                                    val statusVal = respJson.optString("status")
                                                    if (statusVal == "success" || statusVal == "true" || respJson.optBoolean("status") == true) {
                                                        // Found in Sheet!
                                                        val reportedName = respJson.optString("senderName", "")
                                                        val reportedEmail = respJson.optString("senderEmail", "")
                                                        
                                                        if (reportedEmail.isBlank()) {
                                                            isReporting = false
                                                            Toast.makeText(context, "Không xác định được email người đẩy pick!", Toast.LENGTH_SHORT).show()
                                                            return@withContext
                                                        }

                                                        if (reportedEmail == currentUserEmail) {
                                                            isReporting = false
                                                            Toast.makeText(context, "Bạn không thể tự tố cáo chính mình!", Toast.LENGTH_SHORT).show()
                                                            return@withContext
                                                        }

                                                        // Add report to Firestore
                                                        val reporterName = if (currentUserRole == "Admin") {
                                                            currentUserName
                                                        } else {
                                                            val finalRiderRealName = sharedPreferences.getString("rider_real_name", "") ?: currentRiderRealName
                                                            if (finalRiderRealName.isNotBlank()) "$finalRiderRealName ($currentRiderCode)" else "$currentUserName ($currentRiderCode)"
                                                        }

                                                        val reportId = "${reportedEmail}_${targetPup}_${System.currentTimeMillis()}"
                                                        val timestampStr = SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault()).format(Date())
                                                        val reportData = mapOf(
                                                            "pupCode" to targetPup,
                                                            "reporterEmail" to currentUserEmail,
                                                            "reporterName" to reporterName,
                                                            "reportedEmail" to reportedEmail,
                                                            "reportedName" to reportedName,
                                                            "timestamp" to timestampStr,
                                                            "timeMs" to System.currentTimeMillis()
                                                        )

                                                        db.collection("reports").document(reportId).set(reportData)
                                                            .addOnSuccessListener {
                                                                // Send notification to the reported user
                                                                val notifId = "NOTIF_${System.currentTimeMillis()}"
                                                                val notifData = mapOf(
                                                                    "id" to notifId,
                                                                    "message" to "Bạn bị tố cáo đẩy sai mã PUP: $targetPup bởi $reporterName vào lúc $timestampStr",
                                                                    "timestamp" to timestampStr,
                                                                    "timeMs" to System.currentTimeMillis(),
                                                                    "read" to false
                                                                )
                                                                db.collection("active_riders").document(reportedEmail)
                                                                    .collection("notifications").document(notifId).set(notifData)

                                                                // Apply Lock penalties
                                                                db.collection("reports")
                                                                    .whereEqualTo("reportedEmail", reportedEmail)
                                                                    .get()
                                                                    .addOnSuccessListener { querySnapshot ->
                                                                        val allReports = querySnapshot.documents.mapNotNull { it.data }.sortedBy { it["timeMs"] as? Long ?: 0L }
                                                                        val nowMs = System.currentTimeMillis()
                                                                        val oneDayMs = 24 * 3600 * 1000L
                                                                        val oneWeekMs = 7 * 24 * 3600 * 1000L

                                                                        val reportsLast24h = allReports.filter { nowMs - (it["timeMs"] as? Long ?: 0L) <= oneDayMs }
                                                                        val reportsLast7d = allReports.filter { nowMs - (it["timeMs"] as? Long ?: 0L) <= oneWeekMs }

                                                                        var lockDurationMs = 0L
                                                                        var baseTimeMs = nowMs

                                                                        if (reportsLast7d.size >= 7) {
                                                                            lockDurationMs = 3 * 24 * 3600 * 1000L
                                                                            val thirdReportTime = reportsLast7d.getOrNull(2)?.get("timeMs") as? Long ?: nowMs
                                                                            baseTimeMs = thirdReportTime
                                                                        } else if (reportsLast24h.size >= 3) {
                                                                            lockDurationMs = 24 * 3600 * 1000L
                                                                            val thirdReportTime = reportsLast24h.getOrNull(2)?.get("timeMs") as? Long ?: nowMs
                                                                            baseTimeMs = thirdReportTime
                                                                        }

                                                                        if (lockDurationMs > 0L) {
                                                                            val lockUntilMs = baseTimeMs + lockDurationMs
                                                                            val lockUntilDate = Date(lockUntilMs)

                                                                            val reportsListToStore = allReports.takeLast(7).map {
                                                                                mapOf(
                                                                                    "reporterName" to (it["reporterName"]?.toString() ?: ""),
                                                                                    "pupCode" to (it["pupCode"]?.toString() ?: ""),
                                                                                    "timestamp" to (it["timestamp"]?.toString() ?: "")
                                                                                )
                                                                            }

                                                                            db.collection("active_riders").document(reportedEmail)
                                                                                .update(mapOf(
                                                                                    "isChuyenPickLocked" to true,
                                                                                    "chuyenPickLockedUntil" to lockUntilDate,
                                                                                    "reports" to reportsListToStore
                                                                                ))
                                                                        }

                                                                        isReporting = false
                                                                        reportPupCode = ""
                                                                        Toast.makeText(context, "Đã tố cáo thành công người trước đó đẩy pick sai: $reportedName", Toast.LENGTH_LONG).show()
                                                                    }
                                                                    .addOnFailureListener {
                                                                        isReporting = false
                                                                        Toast.makeText(context, "Lỗi tính hình phạt: ${it.message}", Toast.LENGTH_SHORT).show()
                                                                    }
                                                            }
                                                            .addOnFailureListener { e ->
                                                                isReporting = false
                                                                Toast.makeText(context, "Lỗi tạo tố cáo: ${e.message}", Toast.LENGTH_SHORT).show()
                                                            }
                                                    } else {
                                                        isReporting = false
                                                        Toast.makeText(context, "Rất tiếc không có ai từng chuyển pick cho bạn.", Toast.LENGTH_LONG).show()
                                                    }
                                                } catch (e: Exception) {
                                                    isReporting = false
                                                    Toast.makeText(context, "Lỗi phân tích phản hồi: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                isReporting = false
                                                Toast.makeText(context, "Rất tiếc không có ai từng chuyển pick cho bạn.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isReporting = false
                                        Toast.makeText(context, "Lỗi mạng: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        enabled = !isReporting,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = ThemeAccent.copy(alpha = 0.8f),
                        shadowColor = ThemeAccent.copy(alpha = 0.4f)
                    ) {
                        if (isReporting) {
                            CircularProgressIndicator(color = getButtonTextColor(), modifier = Modifier.size(24.dp))
                        } else {
                            Text("TỐ CÁO", color = getButtonTextColor(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // HỖ TRỢ ĐẨY PICK (HIỂN THỊ CHO RIDER, ADMIN KHÔNG CẦN PHẦN NÀY)
        if (currentUserRole != "Admin") {
            item {
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = getGlassBorderColor(isDark),
                    glowColor = ThemeCyan
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "HỖ TRỢ ĐẨY PICK",
                            fontWeight = FontWeight.Bold,
                            color = ThemePrimary,
                            fontSize = 15.sp
                        )

                        // Button: Chọn ảnh hỗ trợ
                        Premium3DButton(
                            onClick = {
                                supportOcrPhotoLauncher.launch("image/*")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            containerBrush = getButtonGradientBrush(isDark),
                            shadowColor = ThemePrimary.copy(alpha = 0.4f)
                        ) {
                            Icon3D(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Chọn ảnh hỗ trợ",
                                tint = getButtonTextColor(),
                                size = 20.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Chọn ảnh hỗ trợ", color = getButtonTextColor(), fontWeight = FontWeight.Bold)
                        }

                        OutlinedTextField(
                            value = supportPup,
                            onValueChange = { supportPup = it },
                            placeholder = { Text("Nhập mã PUP cần hỗ trợ", color = getTextColor(isDark).copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = getTextColor(isDark),
                                unfocusedTextColor = getTextColor(isDark),
                                focusedBorderColor = ThemePrimary,
                                unfocusedBorderColor = ThemePrimary.copy(alpha = 0.5f),
                                unfocusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = if (isDark) 0.15f else 0.8f),
                                focusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = if (isDark) 0.25f else 0.95f)
                            )
                        )

                        OutlinedTextField(
                            value = supportAddress,
                            onValueChange = { supportAddress = it },
                            placeholder = { Text("Ghi rõ Địa Chỉ hoặc Rider nhận hỗ trợ", color = getTextColor(isDark).copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = getTextColor(isDark),
                                unfocusedTextColor = getTextColor(isDark),
                                focusedBorderColor = ThemePrimary,
                                unfocusedBorderColor = ThemePrimary.copy(alpha = 0.5f),
                                unfocusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = if (isDark) 0.15f else 0.8f),
                                focusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = if (isDark) 0.25f else 0.95f)
                            )
                        )

                        Premium3DButton(
                            onClick = {
                                if (supportPup.isBlank()) {
                                    Toast.makeText(context, "Vui lòng nhập mã PUP!", Toast.LENGTH_SHORT).show()
                                    return@Premium3DButton
                                }
                                val receiverStr = supportAddress
                                if (receiverStr.isBlank()) {
                                    Toast.makeText(context, "Vui lòng điền địa chỉ hoặc Rider nhận hỗ trợ!", Toast.LENGTH_SHORT).show()
                                    return@Premium3DButton
                                }

                                isSupportSubmitting = true
                                val pCode = supportPup.trim().uppercase(Locale.ROOT)
                                val docId = "${pCode}_${System.currentTimeMillis()}"

                                val emailToSend = currentUserEmail.ifBlank { sharedPreferences.getString("user_email", "") ?: "unregistered@gmail.com" }
                                val nameToSend = currentUserName.ifBlank { sharedPreferences.getString("user_name", "") ?: "Unregistered" }
                                val codeToSend = currentRiderCode.ifBlank { sharedPreferences.getString("rider_code", "") ?: "NONE" }
                                val finalRiderRealName = sharedPreferences.getString("rider_real_name", "") ?: currentRiderRealName
                                val formattedRiderName = if (finalRiderRealName.isNotBlank()) {
                                    "$finalRiderRealName ( $codeToSend - $nameToSend )"
                                } else {
                                    "$nameToSend ( $codeToSend - $nameToSend )"
                                }

                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val client = okhttp3.OkHttpClient()
                                        val timeString = SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault()).format(Date())
                                        val bodyJson = JSONObject().apply {
                                            put("action", "gui_yeu_cau_ho_tro")
                                            put("riderName", formattedRiderName)
                                            put("pupCode", pCode)
                                            put("timestamp", timeString)
                                            put("status", "chờ chuyển")
                                            put("device", "Android App")
                                            put("password", sharedPreferences.getString("sheet_password", "") ?: "")
                                        }
                                        val body = bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                                        val request = okhttp3.Request.Builder()
                                            .url(webAppUrl)
                                            .post(body)
                                            .build()

                                        client.newCall(request).execute().close()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }

                                    withContext(Dispatchers.Main) {
                                        db.collection("support_pushes").document(docId).set(mapOf(
                                            "docId" to docId,
                                            "pupCode" to pCode,
                                            "senderName" to formattedRiderName,
                                            "receiver" to receiverStr,
                                            "timeMs" to System.currentTimeMillis(),
                                            "status" to "Pending"
                                        )).addOnSuccessListener {
                                            isSupportSubmitting = false
                                            supportPup = ""
                                            supportAddress = ""
                                            Toast.makeText(context, "Đã gửi yêu cầu hỗ trợ!", Toast.LENGTH_SHORT).show()
                                        }.addOnFailureListener {
                                            isSupportSubmitting = false
                                            Toast.makeText(context, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            enabled = !isSupportSubmitting,
                            modifier = Modifier.fillMaxWidth(),
                            containerBrush = getButtonGradientBrush(isDark),
                            shadowColor = ThemeAccent.copy(alpha = 0.5f)
                        ) {
                            Text("GỬI YÊU CẦU", color = getButtonTextColor(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }


        // DANH SÁCH YÊU CẦU HỖ TRỢ (CHỈ HIỂN THỊ CHO ADMIN)
        if (currentUserRole == "Admin") {
            item {
                Text(
                    "Danh sách yêu cầu hỗ trợ:",
                    fontWeight = FontWeight.Bold,
                    color = getTextColor(isDark),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    fontSize = 14.sp
                )
            }

            if (activeSupportList.isEmpty()) {
                item {
                    Text(
                        "Không có yêu cầu hỗ trợ nào.",
                        color = getTextColor(isDark).copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(activeSupportList, key = { it.docId }) { item ->
                    val dId = item.docId
                    val p = item.pupCode
                    val sender = item.senderName
                    val rec = item.receiver
                    val status = item.status

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, getGlassBorderColor(isDark).copy(alpha = 0.5f)),
                        colors = CardDefaults.cardColors(containerColor = getThemeSurfaceColor(isDark).copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column {
                                Text("PUP: $p", fontWeight = FontWeight.Bold, color = getTextColor(isDark), fontSize = 13.sp)
                                Text("Gửi bởi: $sender", color = getTextColor(isDark).copy(alpha = 0.7f), fontSize = 12.sp)
                                Text("Gửi tới: $rec", color = getTextColor(isDark).copy(alpha = 0.7f), fontSize = 12.sp)
                                Text("Trạng thái: $status", color = if (status == "Pushed") Color.Green else ThemeAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            if (status == "Pending") {
                                var isPushingSupport by remember { mutableStateOf(false) }
                                var selectedDriverForThisCard by remember { mutableStateOf("") }
                                var showDialog by remember { mutableStateOf(false) }

                                if (showDialog) {
                                    var searchQuery by remember { mutableStateOf("") }
                                    AlertDialog(
                                        onDismissRequest = { showDialog = false },
                                        title = { Text("Chọn Rider nhận hỗ trợ", color = ThemePrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                                        text = {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = searchQuery,
                                                    onValueChange = { searchQuery = it },
                                                    placeholder = { Text("Tìm kiếm Rider...", color = getTextColor(isDark).copy(alpha = 0.5f)) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    singleLine = true,
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = getTextColor(isDark),
                                                        unfocusedTextColor = getTextColor(isDark),
                                                        focusedBorderColor = ThemePrimary,
                                                        unfocusedBorderColor = ThemePrimary.copy(alpha = 0.4f),
                                                        unfocusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = 0.1f),
                                                        focusedContainerColor = getThemeSurfaceColor(isDark).copy(alpha = 0.15f)
                                                    )
                                                )

                                                val filteredDrivers = driversList.filter { driver ->
                                                    val name = driver["name"]?.toString() ?: ""
                                                    val code = driver["code"]?.toString() ?: ""
                                                    name.contains(searchQuery, ignoreCase = true) || code.contains(searchQuery, ignoreCase = true)
                                                }

                                                LazyColumn(
                                                    modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    if (filteredDrivers.isEmpty()) {
                                                        item {
                                                            Text("Không tìm thấy Rider nào", color = getTextColor(isDark).copy(alpha = 0.6f), modifier = Modifier.padding(8.dp))
                                                        }
                                                    } else {
                                                        items(filteredDrivers) { driver ->
                                                            val dName = driver["name"]?.toString() ?: ""
                                                            val dCode = driver["code"]?.toString() ?: ""
                                                            Card(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .clickable {
                                                                        selectedDriverForThisCard = "$dName - $dCode"
                                                                        showDialog = false
                                                                    },
                                                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                                            ) {
                                                                Column(modifier = Modifier.padding(12.dp)) {
                                                                    Text("$dName - $dCode", color = getTextColor(isDark), fontWeight = FontWeight.Medium)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        confirmButton = {
                                            TextButton(onClick = { showDialog = false }) {
                                                Text("ĐÓNG", color = ThemePrimary)
                                            }
                                        },
                                        containerColor = getThemeSurfaceColor(isDark)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { showDialog = true },
                                        modifier = Modifier.weight(1f).height(38.dp),
                                        border = BorderStroke(1.dp, ThemePrimary.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (selectedDriverForThisCard.isBlank()) "Chọn Rider" else selectedDriverForThisCard,
                                            color = getTextColor(isDark),
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Premium3DButton(
                                        onClick = {
                                            if (selectedDriverForThisCard.isBlank()) {
                                                Toast.makeText(context, "Vui lòng chọn Rider nhận!", Toast.LENGTH_SHORT).show()
                                                return@Premium3DButton
                                            }
                                            isPushingSupport = true
                                            coroutineScope.launch(Dispatchers.IO) {
                                                try {
                                                    val client = okhttp3.OkHttpClient()
                                                    val timeString = SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault()).format(Date())
                                                    val bodyJson = JSONObject().apply {
                                                        put("action", "admin_chuyen_ho_tro")
                                                        put("riderName", sender)
                                                        put("pupCode", p)
                                                        put("receiverRider", selectedDriverForThisCard)
                                                        put("timestamp", timeString)
                                                        put("device", "Android App")
                                                        put("password", sharedPreferences.getString("sheet_password", "") ?: "")
                                                    }
                                                    val body = bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                                                    val request = okhttp3.Request.Builder()
                                                        .url(webAppUrl)
                                                        .post(body)
                                                        .build()

                                                    client.newCall(request).execute().use { response ->
                                                        val responseBody = response.body?.string()
                                                        withContext(Dispatchers.Main) {
                                                            isPushingSupport = false
                                                            if (response.isSuccessful && responseBody != null) {
                                                                db.collection("support_pushes").document(dId)
                                                                    .update("status", "Pushed")

                                                                db.collection("transfers").document(p).set(mapOf(
                                                                    "pupCode" to p,
                                                                    "senderEmail" to currentUserEmail,
                                                                    "senderName" to "$currentUserName (Admin Support)",
                                                                    "receiverRider" to selectedDriverForThisCard,
                                                                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                                                ))

                                                                Toast.makeText(context, "Hỗ trợ chuyển thành công!", Toast.LENGTH_SHORT).show()
                                                            } else {
                                                                Toast.makeText(context, "Lỗi chuyển: ${response.message}", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    withContext(Dispatchers.Main) {
                                                        isPushingSupport = false
                                                        Toast.makeText(context, "Lỗi mạng: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isPushingSupport,
                                        modifier = Modifier.width(90.dp).height(38.dp),
                                        containerBrush = getButtonGradientBrush(isDark),
                                        shadowColor = ThemePrimary.copy(alpha = 0.4f)
                                    ) {
                                        if (isPushingSupport) {
                                            CircularProgressIndicator(color = getButtonTextColor(), modifier = Modifier.size(16.dp))
                                        } else {
                                            Text("Chuyển", color = getButtonTextColor(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getRemainingTimeText(untilDate: Date?, isLocked: Boolean): String {
    if (!isLocked) return "0s"
    if (untilDate == null) return "Vĩnh viễn"
    val diff = untilDate.time - System.currentTimeMillis()
    if (diff <= 0) return "Hết hạn (đang cập nhật)"
    
    val hours = diff / (3600 * 1000)
    val minutes = (diff % (3600 * 1000)) / (60 * 1000)
    val seconds = (diff % (60 * 1000)) / 1000
    
    return when {
        hours > 24 -> "${hours / 24} ngày ${hours % 24} giờ"
        hours > 0 -> "$hours giờ $minutes phút"
        minutes > 0 -> "$minutes phút $seconds giây"
        else -> "$seconds giây"
    }
}

@Composable
fun PrintLockedScreen(remainingText: String) {
    val isDark = LocalThemeIsDark.current
    val infiniteTransition = rememberInfiniteTransition(label = "airplaneLock")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flightProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(getThemeBgBrush(isDark))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "ĐÃ BỊ KHÓA QUET & IN",
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                color = ThemePrimary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(getThemeSurfaceColor(isDark).copy(alpha = 0.2f))
                    .border(1.dp, getGlassBorderColor(isDark), RoundedCornerShape(16.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    // 1. Robot 1 (Left)
                    drawRoundRect(
                        color = Color(0xFF80DEEA),
                        topLeft = Offset(40.dp.toPx(), h - 90.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(40.dp.toPx(), 50.dp.toPx()),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFF00ACC1),
                        radius = 12.dp.toPx(),
                        center = Offset(60.dp.toPx(), h - 100.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = Offset(60.dp.toPx(), h - 100.dp.toPx())
                    )
                    
                    // 2. Printer (Right)
                    drawRoundRect(
                        color = Color(0xFFB0BEC5),
                        topLeft = Offset(w - 80.dp.toPx(), h - 70.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(50.dp.toPx(), 35.dp.toPx()),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(w - 70.dp.toPx(), h - 85.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(30.dp.toPx(), 20.dp.toPx())
                    )

                    // 3. Robot 2 with Gun (Bottom Right)
                    drawRoundRect(
                        color = Color(0xFFFF8A80),
                        topLeft = Offset(w - 140.dp.toPx(), h - 80.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(35.dp.toPx(), 40.dp.toPx()),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFD32F2F),
                        radius = 10.dp.toPx(),
                        center = Offset(w - 122.dp.toPx(), h - 90.dp.toPx())
                    )
                    drawLine(
                        color = Color.Black,
                        start = Offset(w - 135.dp.toPx(), h - 80.dp.toPx()),
                        end = Offset(w - 155.dp.toPx(), h - 95.dp.toPx()),
                        strokeWidth = 4.dp.toPx()
                    )

                    // 4. Paper Airplane flight & shooting logic
                    val flightStartX = 80.dp.toPx()
                    val flightEndX = w - 80.dp.toPx()
                    val flightY = h - 110.dp.toPx()
                    
                    val isLaserFired = progress in 0.45f..0.60f
                    val isBurnt = progress >= 0.50f
                    
                    if (isLaserFired) {
                        val targetX = flightStartX + (flightEndX - flightStartX) * 0.5f
                        val targetY = flightY - 20.dp.toPx()
                        drawLine(
                            color = Color(0xFFFF1744),
                            start = Offset(w - 155.dp.toPx(), h - 95.dp.toPx()),
                            end = Offset(targetX, targetY),
                            strokeWidth = 3.dp.toPx()
                        )
                    }

                    if (!isBurnt) {
                        val currentX = flightStartX + (flightEndX - flightStartX) * progress
                        val airplanePath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(currentX, flightY - 10.dp.toPx())
                            lineTo(currentX + 15.dp.toPx(), flightY - 15.dp.toPx())
                            lineTo(currentX - 5.dp.toPx(), flightY - 25.dp.toPx())
                            close()
                        }
                        drawPath(airplanePath, color = Color.White)
                    } else {
                        val explosionProgress = (progress - 0.5f) / 0.5f
                        if (explosionProgress < 0.8f) {
                            val hitX = flightStartX + (flightEndX - flightStartX) * 0.5f
                            val fallY = (flightY - 20.dp.toPx()) + 80.dp.toPx() * explosionProgress
                            drawCircle(
                                color = Color(0xFFFF9100),
                                radius = (15.dp.toPx() * (1f - explosionProgress)),
                                center = Offset(hitX + (20.dp.toPx() * explosionProgress), fallY)
                            )
                            drawCircle(
                                color = Color(0xFFFF3D00),
                                radius = (8.dp.toPx() * (1f - explosionProgress)),
                                center = Offset(hitX + (20.dp.toPx() * explosionProgress), fallY)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Bạn đã bị khóa Quét & In vui lòng liên hệ quản lý để mở khóa",
                color = getTextColor(isDark),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Thời gian còn lại: $remainingText",
                color = ThemeAccent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ChuyenPickLockedScreen(
    remainingText: String,
    reports: List<Map<String, Any>>,
    context: android.content.Context
) {
    val isDark = LocalThemeIsDark.current
    val infiniteTransition = rememberInfiniteTransition(label = "hitRobot")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "throwTimeline"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(getThemeBgBrush(isDark))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "ĐÃ BỊ KHÓA CHUYỂN PICK",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = ThemePrimary,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(getThemeSurfaceColor(isDark).copy(alpha = 0.2f))
                    .border(1.dp, getGlassBorderColor(isDark), RoundedCornerShape(16.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    
                    val packageProgress = (animationProgress / 0.4f).coerceIn(0f, 1f)
                    val isHit = animationProgress >= 0.4f
                    val fallProgress = ((animationProgress - 0.4f) / 0.3f).coerceIn(0f, 1f)
                    
                    // Robot 2 (Top Right)
                    drawRoundRect(
                        color = Color(0xFFFFB74D),
                        topLeft = Offset(w - 70.dp.toPx(), 40.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(30.dp.toPx(), 35.dp.toPx()),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFE65100),
                        radius = 8.dp.toPx(),
                        center = Offset(w - 55.dp.toPx(), 30.dp.toPx())
                    )

                    // Robot 1 (Bottom Left)
                    val r1BaseX = 60.dp.toPx()
                    val r1BaseY = h - 65.dp.toPx()
                    
                    val (r1X, r1Y, rotationAngle) = if (isHit) {
                        val yOffset = 25.dp.toPx() * fallProgress
                        Triple(r1BaseX + 15.dp.toPx() * fallProgress, r1BaseY + yOffset, 45f * fallProgress)
                    } else {
                        Triple(r1BaseX, r1BaseY, 0f)
                    }

                    if (isHit && fallProgress > 0.2f) {
                        val starAngle = (animationProgress * 360f) * (Math.PI / 180f)
                        val starRadius = 15.dp.toPx()
                        val starX = r1X + 15.dp.toPx() + starRadius * Math.cos(starAngle).toFloat()
                        val starY = r1Y - 15.dp.toPx() + starRadius * Math.sin(starAngle).toFloat()
                        drawCircle(
                            color = Color.Yellow,
                            radius = 4.dp.toPx(),
                            center = Offset(starX, starY)
                        )
                    }

                    drawCircle(
                        color = Color(0xFF81C784),
                        radius = 10.dp.toPx(),
                        center = Offset(r1X + 15.dp.toPx(), r1Y - 10.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color(0xFF4CAF50),
                        topLeft = Offset(r1X, r1Y),
                        size = androidx.compose.ui.geometry.Size(30.dp.toPx(), 35.dp.toPx()),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )

                    // Flying package
                    if (!isHit) {
                        val startX = w - 80.dp.toPx()
                        val startY = 50.dp.toPx()
                        val endX = r1BaseX + 15.dp.toPx()
                        val endY = r1BaseY - 15.dp.toPx()
                        
                        val currentX = startX + (endX - startX) * packageProgress
                        val currentY = startY + (endY - startY) * packageProgress - (30.dp.toPx() * Math.sin(packageProgress * Math.PI).toFloat())
                        
                        drawRoundRect(
                            color = Color(0xFF8D6E63),
                            topLeft = Offset(currentX, currentY),
                            size = androidx.compose.ui.geometry.Size(14.dp.toPx(), 14.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                    } else {
                        val bounceProgress = ((animationProgress - 0.4f) / 0.4f).coerceIn(0f, 1f)
                        if (bounceProgress < 1f) {
                            val startX = r1BaseX + 15.dp.toPx()
                            val startY = r1BaseY - 15.dp.toPx()
                            val currentX = startX + 40.dp.toPx() * bounceProgress
                            val currentY = startY + (10.dp.toPx() * bounceProgress) - (20.dp.toPx() * Math.sin(bounceProgress * Math.PI).toFloat())
                            drawRoundRect(
                                color = Color(0xFF8D6E63),
                                topLeft = Offset(currentX, currentY),
                                size = androidx.compose.ui.geometry.Size(14.dp.toPx(), 14.dp.toPx()),
                                cornerRadius = CornerRadius(2.dp.toPx())
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Bạn đã bị khóa Chuyển Pick vui lòng liên hệ quản lý để mở khóa",
                color = getTextColor(isDark),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        item {
            Text(
                "Thời gian còn lại: $remainingText",
                color = ThemeAccent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (reports.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Danh sách tố cáo lỗi đẩy sai:",
                    fontWeight = FontWeight.Bold,
                    color = getTextColor(isDark),
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }
            
            items(reports) { r ->
                val reporter = r["reporterName"]?.toString() ?: "Ẩn danh"
                val pup = r["pupCode"]?.toString() ?: "N/A"
                val time = r["timestamp"]?.toString() ?: ""
                val details = "$reporter tố cáo PUP: $pup ($time)"
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = getThemeSurfaceColor(isDark).copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, getGlassBorderColor(isDark)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(details, color = getTextColor(isDark), fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "SAO CHÉP",
                            color = ThemePrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Report Details", details)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Đã sao chép thông tin!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppInfoSettingsSection(isDark: Boolean) {
    var showBugReportDialog by remember { mutableStateOf(false) }
    var showIntroDialog by remember { mutableStateOf(false) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = getThemeSurfaceColor(isDark).copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, getGlassBorderColor(isDark)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Cấu hình & Thông tin ứng dụng", color = ThemePrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))

            // Item 1: Góp ý, báo lỗi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showBugReportDialog = true }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Feedback, contentDescription = "Góp ý", tint = ThemePrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Góp ý, báo lỗi", color = getTextColor(isDark), fontSize = 14.sp)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = getTextColor(isDark).copy(alpha = 0.5f))
            }

            HorizontalDivider(color = getGlassBorderColor(isDark).copy(alpha = 0.5f), thickness = 1.dp)

            // Item 2: Giới thiệu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showIntroDialog = true }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = "Giới thiệu", tint = ThemePrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Giới thiệu", color = getTextColor(isDark), fontSize = 14.sp)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = getTextColor(isDark).copy(alpha = 0.5f))
            }

            HorizontalDivider(color = getGlassBorderColor(isDark).copy(alpha = 0.5f), thickness = 1.dp)

            // Item 3: Phiên bản
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showVersionDialog = true }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NewReleases, contentDescription = "Phiên bản", tint = ThemePrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Phiên bản", color = getTextColor(isDark), fontSize = 14.sp)
                }
                Text("1.0.0", color = getTextColor(isDark).copy(alpha = 0.6f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            HorizontalDivider(color = getGlassBorderColor(isDark).copy(alpha = 0.5f), thickness = 1.dp)

            // Item 4: Chính sách quyền riêng tư và bảo mật
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPrivacyDialog = true }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = "Bảo mật", tint = ThemePrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Chính sách quyền riêng tư và bảo mật", color = getTextColor(isDark), fontSize = 14.sp)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = getTextColor(isDark).copy(alpha = 0.5f))
            }
        }
    }

    // Dialogs
    if (showBugReportDialog) {
        AlertDialog(
            onDismissRequest = { showBugReportDialog = false },
            title = { Text("Góp ý, báo lỗi", color = ThemePrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Mọi góp ý báo lỗi vui lòng liên hệ Leader của bạn", color = getTextColor(isDark)) },
            confirmButton = {
                TextButton(onClick = { showBugReportDialog = false }) {
                    Text("ĐỒNG Ý", color = ThemePrimary)
                }
            },
            containerColor = getThemeSurfaceColor(isDark)
        )
    }

    if (showIntroDialog) {
        AlertDialog(
            onDismissRequest = { showIntroDialog = false },
            title = { Text("Giới thiệu", color = ThemePrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Hỗ Trợ Vận Hành, được thiết kế bởi VTDStadio và dùng để hỗ trợ inbill, chuyển pick nhanh", color = getTextColor(isDark)) },
            confirmButton = {
                TextButton(onClick = { showIntroDialog = false }) {
                    Text("ĐÓNG", color = ThemePrimary)
                }
            },
            containerColor = getThemeSurfaceColor(isDark)
        )
    }

    if (showVersionDialog) {
        AlertDialog(
            onDismissRequest = { showVersionDialog = false },
            title = { Text("Phiên bản", color = ThemePrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Phiên bản hiện tại: 1.0.0", color = getTextColor(isDark)) },
            confirmButton = {
                TextButton(onClick = { showVersionDialog = false }) {
                    Text("ĐÓNG", color = ThemePrimary)
                }
            },
            containerColor = getThemeSurfaceColor(isDark)
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Chính sách quyền riêng tư và bảo mật", color = ThemePrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                    Text("ĐIỀU KHOẢN SỬ DỤNG DỊCH VỤ", fontWeight = FontWeight.Bold, color = getTextColor(isDark))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Chào mừng bạn đến với ứng dụng Hỗ trợ vận hành. Khi sử dụng ứng dụng này, bạn đồng ý tuân thủ các điều khoản dịch vụ của chúng tôi.", color = getTextColor(isDark))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("CAM KẾT BẢO MẬT THÔNG TIN", fontWeight = FontWeight.Bold, color = getTextColor(isDark))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Chúng tôi cam kết bảo mật tuyệt đối mọi thông tin cá nhân, lịch sử chuyển pick và in ấn của bạn. Tất cả dữ liệu được lưu trữ an toàn bằng công nghệ cơ sở dữ liệu đám mây tiên tiến bảo mật cao. Chúng tôi không bao giờ chia sẻ dữ liệu của bạn cho bất kỳ bên thứ ba nào khi chưa được phép.", color = getTextColor(isDark))
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("ĐỒNG Ý", color = ThemePrimary)
                }
            },
            containerColor = getThemeSurfaceColor(isDark)
        )
    }
}

