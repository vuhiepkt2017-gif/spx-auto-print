package com.VTDStadio.Hotroin

import android.Manifest
import android.content.Context
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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
    Premium3DButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        containerBrush = Brush.verticalGradient(listOf(containerColor.copy(alpha = 0.9f), containerColor)),
        shadowColor = shadowColor,
        content = content
    )
}

fun extractSpxCodes(textContent: String): List<String> {
    val regex = Regex("([A-Za-z0-9-]{8,25})")
    val foundCodes = mutableListOf<String>()
    if (textContent.isBlank()) return foundCodes
    
    val lines = textContent.split("\n")
    for (line in lines) {
        // Remove spaces inside the line (handwritten codes are often split by OCR with spaces)
        val stripped = line.replace("\\s".toRegex(), "")
        val matches = regex.findAll(stripped)
        for (match in matches) {
            var code = match.value.uppercase(Locale.ROOT)
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
            foundCodes.add(code)
        }
    }
    return foundCodes.distinct()
}

class MainActivity : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
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

@Composable
fun AIParticleBackground() {
    val isDark = LocalThemeIsDark.current
    val gridColor = if (isDark) Color(0xFF1B1D44).copy(alpha = 0.4f) else Color(0xFFD2D8EC).copy(alpha = 0.5f)
    val glow1 = if (isDark) Color(0xFFB19FFB).copy(alpha = 0.15f) else Color(0xFFE3DCFF).copy(alpha = 0.6f)
    val glow2 = if (isDark) Color(0xFFFFB8D1).copy(alpha = 0.12f) else Color(0xFFFFE3EE).copy(alpha = 0.5f)
    val glow3 = if (isDark) Color(0xFF80DEEA).copy(alpha = 0.12f) else Color(0xFFE0F7FA).copy(alpha = 0.5f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w == 0f || h == 0f) return@Canvas

        // 1. Draw soft radial glow spots (Gemini/AI style)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glow1, Color.Transparent),
                center = Offset(w * 0.1f, h * 0.1f),
                radius = w * 0.8f
            ),
            radius = w * 0.8f,
            center = Offset(w * 0.1f, h * 0.1f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glow2, Color.Transparent),
                center = Offset(w * 0.9f, h * 0.8f),
                radius = w * 0.7f
            ),
            radius = w * 0.7f,
            center = Offset(w * 0.9f, h * 0.8f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glow3, Color.Transparent),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w * 0.6f
            ),
            radius = w * 0.6f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        // 2. Draw static tech grid overlay
        val stepX = 40.dp.toPx()
        val stepY = 40.dp.toPx()
        
        var x = 0f
        while (x < w) {
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 1.dp.toPx()
            )
            x += stepX
        }
        
        var y = 0f
        while (y < h) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx()
            )
            y += stepY
        }
    }
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
    var isLoggedIn by remember { mutableStateOf(sharedPreferences.getBoolean("is_logged_in", false)) }
    var currentUserEmail by remember { mutableStateOf(sharedPreferences.getString("user_email", "") ?: "") }
    var currentUserName by remember { mutableStateOf(sharedPreferences.getString("user_name", "") ?: "") }
    var currentUserRole by remember { mutableStateOf(sharedPreferences.getString("user_role", "") ?: "") }
    var currentRiderCode by remember { mutableStateOf(sharedPreferences.getString("rider_code", "") ?: "") }

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
                        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        recognizer.process(inputImage)
                            .addOnSuccessListener { visionText ->
                                val textContent = visionText.text
                                val foundCodes = extractSpxCodes(textContent)
                                if (foundCodes.isNotEmpty()) {
                                    val existingList = orderCode.split("\n").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
                                    var added = 0
                                    foundCodes.forEach { code ->
                                        if (!existingList.contains(code)) {
                                            existingList.add(code)
                                            added++
                                        }
                                    }
                                    orderCode = existingList.joinToString("\n")
                                    Toast.makeText(context, "Đã thêm $added mã từ ảnh!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Không tìm thấy mã SPX trong ảnh!", Toast.LENGTH_LONG).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Lỗi nhận dạng: ${e.message}", Toast.LENGTH_SHORT).show()
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
        if (currentUserRole != "Admin") {
            activeTab = 0
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

        val sdf = SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault())
        val timeString = sdf.format(Date())

        val newItems = cleanCodes.map { PrintHistoryItem(it, timeString, "Đang truyền...") }
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
                                        if (cleanCodes.contains(item.code) && item.status.contains("Đang")) {
                                            item.copy(status = "Thành công")
                                        } else {
                                            item
                                        }
                                    })
                                    triggerVibration()
                                } else {
                                    statusText = "Thất bại: $messageVal"
                                    updateHistoryList(historyList.map { item ->
                                        if (cleanCodes.contains(item.code) && item.status.contains("Đang")) {
                                            item.copy(status = "Thất bại", errorMessage = messageVal)
                                        } else {
                                            item
                                        }
                                    })
                                }
                            } catch (e: Exception) {
                                statusText = "Thất bại: Lỗi định dạng phản hồi từ Google Sheets"
                                updateHistoryList(historyList.map { item ->
                                    if (cleanCodes.contains(item.code) && item.status.contains("Đang")) {
                                        item.copy(status = "Thất bại", errorMessage = e.message)
                                    } else {
                                        item
                                    }
                                })
                            }
                        } else {
                            statusText = "Thất bại: Mã phản hồi HTTP ${response.code}"
                            updateHistoryList(historyList.map { item ->
                                if (cleanCodes.contains(item.code) && item.status.contains("Đang")) {
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
                        if (cleanCodes.contains(item.code) && item.status.contains("Đang")) {
                            item.copy(status = "Thất bại", errorMessage = e.message)
                        } else {
                            item
                        }
                    })
                }
            }
        }
    }

    if (!isLoggedIn) {
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(ThemeCyan, shape = CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "AUTO PRINT",
                                    fontWeight = FontWeight.Bold,
                                    color = getTextColor(isDark),
                                    fontSize = 18.sp
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { onThemeToggle(!isDark) }) {
                                Icon(
                                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Chuyển giao diện",
                                    tint = getTextColor(isDark)
                                )
                            }
                            if (currentUserRole != "Admin") {
                                IconButton(onClick = handleSignOut) {
                                    Icon(
                                        imageVector = Icons.Default.Logout,
                                        contentDescription = "Đăng xuất",
                                        tint = ThemePrimary
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = getThemeBgColor(isDark)
                        )
                    )
                },
                bottomBar = {
                    if (currentUserRole == "Admin") {
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
                                        Icons.Default.AdminPanelSettings,
                                        contentDescription = "Quản trị",
                                        tint = if (activeTab == 1) ThemePrimary else getTextColor(isDark).copy(alpha = 0.6f)
                                    )
                                },
                                label = { Text("Quản trị", color = getTextColor(isDark)) }
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

                    if (activeTab == 0 || currentUserRole != "Admin") {
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
                                onValueChange = { orderCode = it },
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
                                    unfocusedBorderColor = getGlassBorderColor(isDark)
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
                                     focusManager.clearFocus()
                                     sendPrintRequest(orderCode)
                                     orderCode = ""
                                 },
                                 enabled = !isSending && orderCode.isNotBlank(),
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .height(64.dp),
                                 containerBrush = Brush.linearGradient(
                                     colors = if (isSending) listOf(Color.Gray, Color.DarkGray)
                                     else listOf(ThemePrimary, ThemeAccent)
                                 ),
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
                                         Text("TRUYỀN IN CLOUD", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = getButtonTextColor())
                                     }
                                 }
                             }

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("LỊCH SỬ GỬI TIN", fontWeight = FontWeight.Bold, color = getTextColor(isDark).copy(alpha = 0.6f), fontSize = 14.sp)
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
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Admin Dashboard
                        // Admin Dashboard
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
                    }
                }
            }
        }
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

    LaunchedEffect(webAppUrl) {
        inputWebAppUrl = webAppUrl
    }

    LaunchedEffect(sheetUsername, sheetPassword) {
        inputSheetUser = sheetUsername
        inputSheetPass = sheetPassword
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Profile & Logout Card
            Card(
                colors = CardDefaults.cardColors(containerColor = getThemeSurfaceColor(isDark).copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, getGlassBorderColor(isDark)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Thông tin đăng nhập", color = ThemePrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Email: $currentUserEmail", color = getTextColor(isDark))
                    Text("Tên: $currentUserName", color = getTextColor(isDark))
                    Text("Vai trò: Quản trị viên (Admin)", color = Color(0xFFFFA000), fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Premium3DButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        containerBrush = Brush.linearGradient(listOf(ThemePrimary.copy(alpha = 0.8f), ThemePrimary)),
                        shadowColor = ThemePrimary.copy(alpha = 0.4f)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Đăng xuất", tint = getButtonTextColor())
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đăng xuất tài khoản", color = getButtonTextColor(), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            // Web App URL Settings
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
                                unfocusedBorderColor = getGlassBorderColor(isDark)
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
            // Google Sheets Credentials Card
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
                        Text("Cấu hình Tài khoản Sheets", color = ThemePrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        IconButton(onClick = { isEditingCreds = !isEditingCreds }) {
                            Icon(
                                imageVector = if (isEditingCreds) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = "Sửa Credentials",
                                tint = if (isEditingCreds) Color.Green else getTextColor(isDark)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isEditingCreds) {
                        OutlinedTextField(
                            value = inputSheetUser,
                            onValueChange = { inputSheetUser = it },
                            label = { Text("Tài khoản (Username)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = getTextColor(isDark),
                                unfocusedTextColor = getTextColor(isDark),
                                focusedBorderColor = ThemePrimary,
                                unfocusedBorderColor = getGlassBorderColor(isDark)
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = inputSheetPass,
                            onValueChange = { inputSheetPass = it },
                            label = { Text("Mật khẩu (Password)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = getTextColor(isDark),
                                unfocusedTextColor = getTextColor(isDark),
                                focusedBorderColor = ThemePrimary,
                                unfocusedBorderColor = getGlassBorderColor(isDark)
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Premium3DButton(
                            onClick = {
                                onUpdateCredentials(inputSheetUser, inputSheetPass)
                                isEditingCreds = false
                            },
                            modifier = Modifier.align(Alignment.End).width(160.dp),
                            containerColor = ThemePrimary,
                            shadowColor = ThemePrimary.copy(alpha = 0.5f)
                        ) {
                            Text("Lưu tài khoản", color = getButtonTextColor(), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("Tài khoản: ${sheetUsername.ifBlank { "(Chưa cấu hình)" }}", color = getTextColor(isDark).copy(alpha = 0.7f))
                        Text("Mật khẩu: ${if (sheetPassword.isBlank()) "(Chưa cấu hình)" else "********"}", color = getTextColor(isDark).copy(alpha = 0.7f))
                    }
                }
            }
        }

        // Section: Code Rider Creator
        item {
            AdminCreateRiderSection(db = db)
        }

        // Section: Registered riders
        item {
            AdminRiderListSection(db = db)
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
                label = { Text("Mã Code Rider (Ví dụ: VTD01)", color = getTextColor(isDark).copy(alpha = 0.6f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = getTextColor(isDark),
                    unfocusedTextColor = getTextColor(isDark),
                    focusedBorderColor = ThemePrimary,
                    unfocusedBorderColor = getGlassBorderColor(isDark)
                )
            )

            OutlinedTextField(
                value = riderName,
                onValueChange = { riderName = it },
                label = { Text("Tên Rider tương ứng", color = getTextColor(isDark).copy(alpha = 0.6f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = getTextColor(isDark),
                    unfocusedTextColor = getTextColor(isDark),
                    focusedBorderColor = ThemePrimary,
                    unfocusedBorderColor = getGlassBorderColor(isDark)
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
                containerColor = ThemePrimary,
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
    var activeRidersList by remember { mutableStateOf(listOf<Map<String, Any>>()) }

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
                        riderCodesList.forEach { data ->
                            val code = data["code"]?.toString() ?: ""
                            val name = data["name"]?.toString() ?: ""
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

        // Active Sign-in Accounts List
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
                                    Text(name, fontWeight = FontWeight.Bold, color = getTextColor(isDark))
                                    Text("Email: $email", fontSize = 12.sp, color = getTextColor(isDark).copy(alpha = 0.7f))
                                    Text("Mã Code: $code", fontSize = 12.sp, color = ThemeCyan, fontWeight = FontWeight.Medium)
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
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AIParticleBackground()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Premium Icon Logo Circle
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(ThemePrimary.copy(alpha = 0.1f))
                    .border(
                        2.dp,
                        Brush.linearGradient(listOf(ThemePrimary, ThemeCyan)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = "Logo",
                    modifier = Modifier.size(45.dp),
                    tint = ThemePrimary
                )
            }

            Text(
                "HỖ TRỢ IN HÀNG LOẠT",
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
                                containerBrush = Brush.linearGradient(listOf(ThemePrimary, ThemeAccent)),
                                shadowColor = ThemeAccent.copy(alpha = 0.5f)
                            ) {
                                Icon(Icons.Default.Login, contentDescription = "Login", tint = getButtonTextColor())
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ĐĂNG NHẬP GOOGLE", fontWeight = FontWeight.Bold, color = getButtonTextColor())
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
                                label = { Text("Mã kích hoạt (Rider Code)", color = getTextColor(isDark).copy(alpha = 0.6f)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = getTextColor(isDark),
                                    unfocusedTextColor = getTextColor(isDark),
                                    focusedBorderColor = ThemePrimary,
                                    unfocusedBorderColor = getGlassBorderColor(isDark)
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
