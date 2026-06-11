package com.autoprint.shopee

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("ShopeeAutoPrintPrefs", Context.MODE_PRIVATE)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            ShopeeAutoPrintApp(sharedPreferences, cameraExecutor)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

// Model cho lịch sử quét/gửi
data class PrintHistoryItem(
    val code: String,
    val timestamp: String,
    val status: String, // "Thành công", "Đang gửi", "Thất bại"
    val errorMessage: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopeeAutoPrintApp(sharedPreferences: SharedPreferences, cameraExecutor: ExecutorService) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Cấu hình lưu trữ tài khoản do admin cấp
    var username by remember { mutableStateOf(sharedPreferences.getString("username", "") ?: "") }
    var password by remember { mutableStateOf(sharedPreferences.getString("password", "") ?: "") }
    var apiUrl by remember { mutableStateOf(sharedPreferences.getString("api_url", "") ?: "") }
    var autoSubmitOnScan by remember { mutableStateOf(sharedPreferences.getBoolean("auto_submit_on_scan", false)) } // Mặc định tắt tự động gửi để kiểm tra danh sách trước

    // Quản lý hiển thị mật khẩu
    var passwordVisible by remember { mutableStateOf(false) }

    // Giao diện chính
    var activeTab by remember { mutableStateOf(0) } // 0: Nhập/Quét, 1: Cấu hình
    var orderCode by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    // Quét mã camera
    var showCameraScanner by remember { mutableStateOf(false) }
    var isOcrMode by remember { mutableStateOf(false) } // False: Quét mã vạch tự động, True: Chụp ảnh quét chữ OCR
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission = isGranted
        if (isGranted) {
            showCameraScanner = true
        } else {
            Toast.makeText(context, "Ứng dụng cần quyền Camera để chụp ảnh/quét mã", Toast.LENGTH_LONG).show()
        }
    }

    // Danh sách lịch sử cục bộ của điện thoại này
    var historyList by remember { mutableStateOf(listOf<PrintHistoryItem>()) }
    val coroutineScope = rememberCoroutineScope()

    // Khởi tạo ToneGenerator một lần duy nhất để tránh cạn kiệt tài nguyên audio track gây crash native
    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (e: Exception) {
            Log.e("ShopeeAutoPrint", "Không thể khởi tạo ToneGenerator", e)
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                toneGenerator?.release()
            } catch (e: Exception) {
                Log.e("ShopeeAutoPrint", "Lỗi giải phóng ToneGenerator", e)
            }
        }
    }

    // Hàm phát âm thanh thành công (Được bọc try-catch chống crash hoàn toàn)
    fun playSuccessBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK)
        } catch (e: Exception) {
            Log.e("ShopeeAutoPrint", "Lỗi phát âm thanh beep", e)
        }
    }

    // Hàm rung phản hồi (Được bọc try-catch chống crash hoàn toàn)
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

    // Hàm gửi mã lên Google Sheets (Bọc toàn bộ trong try-catch chống crash)
    fun sendPrintRequest(codeToSend: String) {
        try {
            if (codeToSend.isBlank()) {
                statusText = "Lỗi: Mã vận đơn không được để trống!"
                return
            }
            if (apiUrl.isBlank() || !apiUrl.trim().startsWith("http")) {
                statusText = "Lỗi: Vui lòng nhập URL API hợp lệ (bắt đầu bằng http)!"
                return
            }
            if (username.isBlank() || password.isBlank()) {
                statusText = "Lỗi: Vui lòng cấu hình tài khoản và mật khẩu trước!"
                return
            }

            // Tách các mã vận đơn bằng dấu xuống dòng (hỗ trợ in hàng loạt)
            val rawCodes = codeToSend.split("\n")
            val cleanCodes = rawCodes.map { it.trim().uppercase(Locale.ROOT) }.filter { it.isNotBlank() }

            if (cleanCodes.isEmpty()) {
                statusText = "Lỗi: Không tìm thấy mã vận đơn hợp lệ!"
                return
            }

            isSending = true
            statusText = "Đang gửi ${cleanCodes.size} mã..."

            val sdf = SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault())
            val timeString = sdf.format(Date())

            // Thêm tất cả mã vào lịch sử hiển thị
            val newItems = cleanCodes.map { PrintHistoryItem(it, timeString, "Đang gửi") }
            historyList = newItems + historyList

            // Tạo Mã lô (Batch ID) duy nhất cho phiên gửi này
            val batchId = "BATCH_${username.trim()}_${System.currentTimeMillis()}"

            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val bodyJson = JSONObject().apply {
                        val jsonArray = JSONArray()
                        cleanCodes.forEach { jsonArray.put(it) }
                        put("codes", jsonArray)
                        put("batchId", batchId)
                        put("username", username.trim())
                        put("password", password.trim())
                    }

                    val body = bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request = Request.Builder()
                        .url(apiUrl.trim())
                        .post(body)
                        .build()

                    client.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string()
                        withContext(Dispatchers.Main) {
                            isSending = false
                            if (response.isSuccessful && responseBody != null) {
                                try {
                                    val respJson = JSONObject(responseBody)
                                    if (respJson.getString("status") == "success") {
                                        statusText = "Gửi in thành công ${cleanCodes.size} mã!"
                                        historyList = historyList.map { item ->
                                            if (cleanCodes.contains(item.code) && item.status == "Đang gửi") {
                                                item.copy(status = "Thành công")
                                            } else {
                                                item
                                            }
                                        }
                                        playSuccessBeep()
                                        triggerVibration()
                                    } else {
                                        val msg = respJson.optString("message", "Lỗi từ máy chủ")
                                        statusText = "Thất bại: $msg"
                                        historyList = historyList.map { item ->
                                            if (cleanCodes.contains(item.code) && item.status == "Đang gửi") {
                                                item.copy(status = "Thất bại", errorMessage = msg)
                                            } else {
                                                item
                                            }
                                        }
                                    }
                                } catch (jsonEx: Exception) {
                                    val errMsg = "JSON lỗi: ${jsonEx.message}"
                                    statusText = "Lỗi đọc dữ liệu máy chủ"
                                    historyList = historyList.map { item ->
                                        if (cleanCodes.contains(item.code) && item.status == "Đang gửi") {
                                            item.copy(status = "Thất bại", errorMessage = errMsg)
                                        } else {
                                            item
                                        }
                                    }
                                }
                            } else {
                                val errorMsg = "Lỗi kết nối máy chủ (HTTP ${response.code})"
                                statusText = "Thất bại: $errorMsg"
                                historyList = historyList.map { item ->
                                    if (cleanCodes.contains(item.code) && item.status == "Đang gửi") {
                                        item.copy(status = "Thất bại", errorMessage = errorMsg)
                                    } else {
                                        item
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    val errMsg = e.message ?: "Lỗi kết nối"
                    withContext(Dispatchers.Main) {
                        isSending = false
                        statusText = "Lỗi kết nối: $errMsg"
                        historyList = historyList.map { item ->
                            if (cleanCodes.contains(item.code) && item.status == "Đang gửi") {
                                item.copy(status = "Thất bại", errorMessage = errMsg)
                            } else {
                                item
                            }
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            statusText = "Lỗi ứng dụng: ${ex.message}"
            Log.e("ShopeeAutoPrint", "Gửi in lỗi", ex)
        }
    }

    // Giao diện chính của ứng dụng
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFFF5722), // Màu cam
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E)
        )
    ) {
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
                            playSuccessBeep()
                        }
                    } else {
                        orderCode = code
                        triggerVibration()
                        playSuccessBeep()
                    }
                    
                    if (autoSubmitOnScan) {
                        showCameraScanner = false
                        sendPrintRequest(orderCode)
                        orderCode = ""
                    }
                },
                onOcrBulkCodes = { codes ->
                    // Thêm danh sách mã quét được từ OCR vào ô nhập liệu
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
                        playSuccessBeep()
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
                            Text(
                                "HỖ TRỢ IN",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF5722),
                                fontSize = 20.sp
                            )
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color(0xFF121212)
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(containerColor = Color(0xFF121212)) {
                        NavigationBarItem(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            icon = { Icon(Icons.Default.Print, contentDescription = "Quét & In") },
                            label = { Text("Quét & In") }
                        )
                        NavigationBarItem(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            icon = { Icon(Icons.Default.Lock, contentDescription = "Tài khoản") },
                            label = { Text("Tài khoản") }
                        )
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color(0xFF121212))
                ) {
                    if (activeTab == 0) {
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
                                label = { Text("Danh sách mã vận đơn (Mỗi mã 1 dòng)") },
                                placeholder = { Text("Nhập hoặc quét mã vạch...\nSPXVN0123...\nSPXVN0456...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 220.dp),
                                singleLine = false,
                                maxLines = 15,
                                trailingIcon = {
                                    if (orderCode.isNotEmpty()) {
                                        IconButton(onClick = { orderCode = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Xóa tất cả")
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFF5722),
                                    focusedLabelColor = Color(0xFFFF5722)
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        isOcrMode = false
                                        if (hasCameraPermission) {
                                            showCameraScanner = true
                                        } else {
                                            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Quét vạch")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Quét Vạch", color = Color.White, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        isOcrMode = true
                                        if (hasCameraPermission) {
                                            showCameraScanner = true
                                        } else {
                                            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = "Chụp quét chữ")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Quét Chữ (OCR)", color = Color.White, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        sendPrintRequest(orderCode)
                                        orderCode = ""
                                    },
                                    enabled = !isSending && orderCode.isNotBlank(),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (isSending) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    } else {
                                        Icon(Icons.Default.Print, contentDescription = "Gửi in")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Gửi In", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Lịch sử gửi (Điện thoại này)", fontWeight = FontWeight.Bold, color = Color.Gray)
                                if (historyList.isNotEmpty()) {
                                    Text(
                                        "Xóa hết",
                                        color = Color(0xFFFF5722),
                                        fontSize = 13.sp,
                                modifier = Modifier.clickable { historyList = emptyList() }
                                    )
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF333333))

                            if (historyList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Chưa có lịch sử gửi mã", color = Color.DarkGray)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(historyList) { item ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(item.code, fontWeight = FontWeight.Bold, color = Color.White)
                                                    Text(item.timestamp, fontSize = 12.sp, color = Color.Gray)
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    val statusColor = when (item.status) {
                                                        "Thành công" -> Color(0xFF4CAF50)
                                                        "Đang gửi" -> Color(0xFFFF9800)
                                                        else -> Color(0xFFF44336)
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(statusColor, shape = RoundedCornerShape(4.dp))
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
                        // MÀN HÌNH TÀI KHOẢN
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Cấu hình tài khoản do Admin cấp", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)

                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Tên đăng nhập") },
                                placeholder = { Text("Nhập tài khoản đăng nhập...") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFF5722),
                                    focusedLabelColor = Color(0xFFFF5722)
                                )
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Mật khẩu") },
                                placeholder = { Text("Nhập mật khẩu...") },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(icon, contentDescription = "Hiện/Ẩn mật khẩu")
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFF5722),
                                    focusedLabelColor = Color(0xFFFF5722)
                                )
                            )

                            OutlinedTextField(
                                value = apiUrl,
                                onValueChange = { apiUrl = it },
                                label = { Text("Đường dẫn Google Sheets API (Web App URL)") },
                                placeholder = { Text("https://script.google.com/macros/s/...") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFF5722),
                                    focusedLabelColor = Color(0xFFFF5722)
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Tự động gửi khi quét xong", color = Color.White)
                                    Text("App tự động gửi in ngay sau khi quét thành công", fontSize = 11.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = autoSubmitOnScan,
                                    onCheckedChange = { autoSubmitOnScan = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF5722))
                                )
                            }

                            Button(
                                onClick = {
                                    sharedPreferences.edit().apply {
                                        putString("username", username.trim())
                                        putString("password", password.trim())
                                        putString("api_url", apiUrl.trim())
                                        putBoolean("auto_submit_on_scan", autoSubmitOnScan)
                                        apply()
                                    }
                                    Toast.makeText(context, "Đã lưu thông tin tài khoản!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = "Lưu")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Lưu thông tin", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Chuyển Dialog thành Composable View toàn màn hình kết hợp quét OCR văn bản trực tiếp
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
    
    // Khởi tạo ImageCapture cho chế độ Chụp Ảnh quét chữ OCR
    val imageCapture = remember { ImageCapture.Builder().build() }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        if (isOcrMode) "Chụp ảnh quét chữ OCR" else "Quét mã vạch tự động", 
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
                            tint = Color(0xFFFF5722)
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
            // Hiển thị camera stream trực tiếp khởi tạo an toàn
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        previewViewReference = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Vẽ lớp phủ hướng dẫn quét
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
                        .align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                            .clip(RoundedCornerShape(16.dp))
                            .align(Alignment.Center)
                    )
                }
            }

            // Giao diện điều khiển phía dưới
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isOcrMode) {
                    // Nút chụp ảnh OCR ở chế độ quét chữ
                    if (isProcessingOcr) {
                        CircularProgressIndicator(color = Color(0xFFFF5722))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Đang xử lý ảnh & nhận dạng chữ...", color = Color.White, fontSize = 13.sp)
                    } else {
                        Button(
                            onClick = {
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
                                                            val regex = Regex("(SPXVN[0-9]+|VN[0-9]+)", RegexOption.IGNORE_CASE)
                                                            val matches = regex.findAll(textContent)
                                                            val foundCodes = matches.map { it.value.toUpperCase(Locale.ROOT) }.toList()
                                                            
                                                            if (foundCodes.isNotEmpty()) {
                                                                onOcrBulkCodes(foundCodes)
                                                            } else {
                                                                Toast.makeText(context, "Không tìm thấy mã SPX nào trong ảnh!", Toast.LENGTH_LONG).show()
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
                                                Log.e("ShopeeAutoPrint", "OCR error", ex)
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            isProcessingOcr = false
                                            Toast.makeText(context, "Lỗi chụp ảnh: ${exception.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            },
                            modifier = Modifier
                                .size(75.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(Color(0xFFFF5722), shape = CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Bấm nút cam để chụp ảnh nhãn và trích xuất chữ",
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

            // Khởi chạy cameraX khi view được gắn thành công
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
                            // Chế độ chụp ảnh quét OCR: Bind Preview và ImageCapture
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } else {
                            // Chế độ quét mã vạch: Bind Preview và ImageAnalysis
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
                        Log.e("CameraScannerView", "Lỗi liên kết CameraX", e)
                    }

                }, ContextCompat.getMainExecutor(context))
            }
            
            // Hủy đăng ký camera khi đóng màn hình
            DisposableEffect(Unit) {
                onDispose {
                    try {
                        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                        cameraProvider.unbindAll()
                    } catch (e: Exception) {
                        Log.e("CameraScannerView", "Không thể hủy bind camera", e)
                    }
                }
            }
        }
    }
}

// Analyzer chuyên dụng chỉ dành cho chế độ quét mã vạch tự động
class ScanAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {
    
    private val barcodeScanner = BarcodeScanning.getClient()

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
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
                Log.e("ScanAnalyzer", "Mã vạch quét thất bại", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}

// Cập nhật lịch sử khi gửi thất bại (Đã chuyển sang map trực tiếp, giữ lại hàm trống để tránh lỗi tham chiếu nếu có)
fun updateHistoryFailed(historyList: MutableList<PrintHistoryItem>, code: String, error: String) {}
