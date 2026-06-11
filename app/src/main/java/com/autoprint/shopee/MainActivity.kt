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
    var autoSubmitOnScan by remember { mutableStateOf(sharedPreferences.getBoolean("auto_submit_on_scan", true)) }

    // Quản lý hiển thị mật khẩu
    var passwordVisible by remember { mutableStateOf(false) }

    // Giao diện chính
    var activeTab by remember { mutableStateOf(0) } // 0: Nhập/Quét, 1: Cấu hình
    var orderCode by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    // Quét mã camera (Sử dụng overlay để tránh lỗi crash của Dialog Lifecycle)
    var showCameraScanner by remember { mutableStateOf(false) }
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
            Toast.makeText(context, "Ứng dụng cần quyền Camera để quét mã vạch", Toast.LENGTH_LONG).show()
        }
    }

    // Danh sách lịch sử cục bộ của điện thoại này
    val historyList = remember { mutableStateListOf<PrintHistoryItem>() }
    val coroutineScope = rememberCoroutineScope()

    // Hàm gửi mã lên Google Sheets
    fun sendPrintRequest(codeToSend: String) {
        if (codeToSend.isBlank()) {
            statusText = "Lỗi: Mã vận đơn không được để trống!"
            return
        }
        if (apiUrl.isBlank()) {
            statusText = "Lỗi: Vui lòng cấu hình URL API trong mục Cài đặt!"
            return
        }
        if (username.isBlank() || password.isBlank()) {
            statusText = "Lỗi: Vui lòng nhập Tài khoản & Mật khẩu trong mục Cài đặt!"
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
        cleanCodes.forEach { code ->
            val newItem = PrintHistoryItem(code, timeString, "Đang gửi")
            historyList.add(0, newItem)
        }

        // Tạo Mã lô (Batch ID) duy nhất cho phiên gửi này
        val batchId = "BATCH_${username.trim()}_${System.currentTimeMillis()}"

        coroutineScope.launch(Dispatchers.IO) {
            val client = OkHttpClient()
            val json = JSONObject().apply {
                val jsonArray = JSONArray()
                cleanCodes.forEach { jsonArray.put(it) }
                put("codes", jsonArray)
                put("batchId", batchId)
                put("username", username.trim())
                put("password", password.trim())
            }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(apiUrl)
                .post(body)
                .build()

            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                withContext(Dispatchers.Main) {
                    isSending = false
                    if (response.isSuccessful && responseBody != null) {
                        val respJson = JSONObject(responseBody)
                        if (respJson.getString("status") == "success") {
                            statusText = "Gửi in thành công ${cleanCodes.size} mã!"
                            // Cập nhật trạng thái thành công cho tất cả mã vừa gửi
                            cleanCodes.forEach { code ->
                                val index = historyList.indexOfFirst { it.code == code && it.status == "Đang gửi" }
                                if (index != -1) {
                                    historyList[index] = historyList[index].copy(status = "Thành công")
                                }
                            }
                            // Báo âm thanh thành công
                            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100).startTone(ToneGenerator.TONE_PROP_ACK)
                        } else {
                            val msg = respJson.optString("message", "Lỗi xác thực hoặc hệ thống")
                            statusText = "Thất bại: $msg"
                            cleanCodes.forEach { code ->
                                updateHistoryFailed(historyList, code, msg)
                            }
                        }
                    } else {
                        val errorMsg = "Lỗi HTTP ${response.code}"
                        statusText = "Thất bại: $errorMsg"
                        cleanCodes.forEach { code ->
                            updateHistoryFailed(historyList, code, errorMsg)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isSending = false
                    statusText = "Lỗi kết nối: ${e.message}"
                    cleanCodes.forEach { code ->
                        updateHistoryFailed(historyList, code, e.message ?: "Lỗi kết nối")
                    }
                }
            }
        }
    }

    // Giao diện
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFFF5722), // Màu cam thương hiệu
            background = Color(0xFF121212), // Giao diện tối
            surface = Color(0xFF1E1E1E)
        )
    ) {
        // Nếu bật quét mã, hiển thị view quét chiếm toàn màn hình thay vì dialog để tránh lỗi crash
        if (showCameraScanner && hasCameraPermission) {
            CameraScannerView(
                onCodeScanned = { code ->
                    showCameraScanner = false
                    
                    // Nếu đã có mã trước đó, chèn xuống dòng
                    if (orderCode.isNotBlank()) {
                        orderCode = orderCode.trim() + "\n" + code
                    } else {
                        orderCode = code
                    }
                    
                    // Rung phản hồi thành công
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
                    if (vibrator != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(120)
                        }
                    }
                    
                    if (autoSubmitOnScan) {
                        sendPrintRequest(orderCode)
                        orderCode = ""
                    }
                },
                onDismiss = { showCameraScanner = false },
                cameraExecutor = cameraExecutor
            )
        } else {
            // Màn hình ứng dụng chính
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
                        // MÀN HÌNH QUÉT & IN
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Card Trạng thái hiển thị lỗi/thành công
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

                            // Ô nhập mã vận đơn (Hỗ trợ nhiều dòng)
                            OutlinedTextField(
                                value = orderCode,
                                onValueChange = { orderCode = it },
                                label = { Text("Danh sách mã vận đơn (Mỗi mã 1 dòng)") },
                                placeholder = { Text("Nhập hoặc quét mã vạch...\nSPXVN0123...\nSPXVN0456...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 220.dp),
                                singleLine = false, // Hỗ trợ nhiều dòng
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

                            // Hàng các nút điều khiển chính
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Nút kích hoạt quét CameraX
                                Button(
                                    onClick = {
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
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Quét camera")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Quét Mã", color = Color.White)
                                }

                                // Nút bấm gửi yêu cầu in hàng loạt
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
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Gửi In", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Tiêu đề danh sách lịch sử cục bộ
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
                                        modifier = Modifier.clickable { historyList.clear() }
                                    )
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF333333))

                            // Hiển thị danh sách các mã đã in
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
                        // MÀN HÌNH TÀI KHOẢN VÀ CẤU HÌNH API
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Cấu hình tài khoản do Admin cấp", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)

                            // Tài khoản
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

                            // Mật khẩu
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

                            // URL Kết nối Google Sheet
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

                            // Switch cấu hình tự động gửi
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Tự động gửi khi quét xong", color = Color.White)
                                    Text("App tự động gửi in ngay sau khi camera quét thành công mã vạch", fontSize = 11.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = autoSubmitOnScan,
                                    onCheckedChange = { autoSubmitOnScan = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF5722))
                                )
                            }

                            // Nút lưu lại cấu hình
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

fun updateHistoryFailed(historyList: MutableList<PrintHistoryItem>, code: String, error: String) {
    val index = historyList.indexOfFirst { it.code == code && it.status == "Đang gửi" }
    if (index != -1) {
        historyList[index] = historyList[index].copy(status = "Thất bại", errorMessage = error)
    }
}

// Chuyển Dialog thành Composable View toàn màn hình để không bao giờ bị crash View Tree Lifecycle trong Jetpack Compose
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScannerView(
    onCodeScanned: (String) -> Unit,
    onDismiss: () -> Unit,
    cameraExecutor: ExecutorService
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Quét mã vạch đơn hàng", fontSize = 16.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Trở lại", tint = Color.White)
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
            // Hiển thị camera stream
            AndroidView(
                factory = { previewView },
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
                    // Viền chỉ hướng quét
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                            .clip(RoundedCornerShape(16.dp))
                            .align(Alignment.Center)
                    )
                }
            }

            // Khởi chạy cameraX
            DisposableEffect(Unit) {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { code ->
                                onCodeScanned(code)
                            })
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("CameraScannerView", "Không thể bind lifecycle", e)
                    }

                }, ContextCompat.getMainExecutor(context))

                onDispose {
                    try {
                        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                        cameraProvider.unbindAll()
                    } catch (e: Exception) {
                        Log.e("CameraScannerView", "Không thể unbind camera", e)
                    }
                }
            }
        }
    }
}

class BarcodeAnalyzer(private val onBarcodeDetected: (String) -> Unit) : ImageAnalysis.Analyzer {
    
    private val scanner = BarcodeScanning.getClient()

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (rawValue != null && rawValue.isNotBlank()) {
                            onBarcodeDetected(rawValue)
                            break
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e("BarcodeAnalyzer", "Lỗi ML Kit", it)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
