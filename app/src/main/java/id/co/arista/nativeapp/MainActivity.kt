package id.co.arista.nativeapp

import android.os.Bundle
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val Cream = Color(0xFFF7F1E8)
private val Terracotta = Color(0xFFB5583A)
private val Ink = Color(0xFF2B211A)
private val Muted = Color(0xFF7A6B5D)

private fun saleTimestamp(sale: org.json.JSONObject): Long {
    val date = sale.optString("sale_date")
    val time = sale.optString("sale_time").ifBlank { "00:00:00" }
    val patterns = listOf("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd'T'HH:mm:ss")
    for (pattern in patterns) {
        try {
            val parser = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
            parser.isLenient = false
            return parser.parse("$date $time")?.time ?: continue
        } catch (_: Exception) { }
    }
    return 0L
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { AristaApp() } }
    }
}

@Composable
private fun AristaApp() {
    val api = remember { AristaApi() }
    val context = LocalContext.current
    val localDb = remember(context) { Room.databaseBuilder(context.applicationContext, AristaDatabase::class.java, "arista-local.db").build() }
    val localDao = remember(localDb) { localDb.localDao() }
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var recoveryBusy by remember { mutableStateOf(false) }
    var recoveryMessage by remember { mutableStateOf("") }
    var session by remember { mutableStateOf<AristaSession?>(null) }
    var profile by remember { mutableStateOf<AristaProfile?>(null) }
    var products by remember { mutableStateOf<List<AristaProduct>>(emptyList()) }
    var productsLoading by remember { mutableStateOf(false) }
    var reportSales by remember { mutableStateOf(org.json.JSONArray()) }
    var reportExpenses by remember { mutableStateOf(org.json.JSONArray()) }
    var expenseName by remember { mutableStateOf("") }
    var expenseCategory by remember { mutableStateOf("") }
    var expenseQty by remember { mutableStateOf("1") }
    var expensePrice by remember { mutableStateOf("") }
    var expenseNote by remember { mutableStateOf("") }
    var expenseDate by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    var reportsLoading by remember { mutableStateOf(false) }
    var reportRange by remember { mutableStateOf("Harian") }
    var reportTab by remember { mutableStateOf("Penjualan") }
    var showCancelledSales by remember { mutableStateOf(false) }
    var receivableSales by remember { mutableStateOf(org.json.JSONArray()) }
    var receivablePayments by remember { mutableStateOf(org.json.JSONArray()) }
    var receivablesLoading by remember { mutableStateOf(false) }
    var receivableCustomer by remember { mutableStateOf<String?>(null) }
    var receivableAmount by remember { mutableStateOf("") }
    var receivableMethod by remember { mutableStateOf("Tunai") }
    var cart by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var cashierSection by remember { mutableStateOf("Menu") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var pendingOrders by remember { mutableStateOf<List<PendingOrderEntity>>(emptyList()) }
    var editingPendingId by remember { mutableStateOf<String?>(null) }
    var savingPending by remember { mutableStateOf(false) }
    var queuedSalesCount by remember { mutableIntStateOf(0) }
    var syncingSales by remember { mutableStateOf(false) }
    var activeShift by remember { mutableStateOf<ShiftEntity?>(null) }
    var shiftHistory by remember { mutableStateOf<List<ShiftEntity>>(emptyList()) }
    var openingCashInput by remember { mutableStateOf("") }
    var closingCashInput by remember { mutableStateOf("") }
    var paymentBusy by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf("Tunai") }
    var lastReceipt by remember { mutableStateOf<org.json.JSONObject?>(null) }
    var cashReceived by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var discountInput by remember { mutableStateOf("") }
    var discountType by remember { mutableStateOf("Rp") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var productName by remember { mutableStateOf("") }
    var productCategory by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productCost by remember { mutableStateOf("") }
    var productStock by remember { mutableStateOf("0") }
    var editingProductId by remember { mutableStateOf<String?>(null) }
    var managementBusy by remember { mutableStateOf(false) }
    var staffRows by remember { mutableStateOf(org.json.JSONArray()) }
    var staffName by remember { mutableStateOf("") }
    var staffEmail by remember { mutableStateOf("") }
    var staffPassword by remember { mutableStateOf("") }
    var manageTab by remember { mutableStateOf("Menu") }
    var backupContent by remember { mutableStateOf("") }
    var pendingRestore by remember { mutableStateOf<org.json.JSONObject?>(null) }
    val createBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null && backupContent.isNotBlank()) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(backupContent.toByteArray(Charsets.UTF_8)) }
                error = "Backup berhasil disimpan."
            } catch (e: Exception) { error = "Gagal menyimpan backup: ${e.message ?: "kesalahan file"}" }
        }
    }
    val restoreBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: throw IllegalStateException("File tidak dapat dibaca.")
                val parsed = org.json.JSONObject(raw)
                if (parsed.optString("app") != "arista-native" || parsed.optInt("version") != 1 ||
                    !parsed.has("pendingOrders") || !parsed.has("syncOutbox") || !parsed.has("shifts")) {
                    throw IllegalArgumentException("Format backup ARISTA tidak sesuai.")
                }
                pendingRestore = parsed
                error = ""
            } catch (e: Exception) { error = e.message ?: "File backup tidak valid." }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Cream) {
        if (session == null || profile == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ARISTA", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                Text("COFFEE & ROASTERY", color = Muted, fontSize = 11.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Masuk ke ARISTA", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = email, onValueChange = { email = it; error = ""; recoveryMessage = "" },
                            modifier = Modifier.fillMaxWidth().height(54.dp), label = { Text("Email") },
                            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password, onValueChange = { password = it; error = "" },
                            modifier = Modifier.fillMaxWidth().height(54.dp), label = { Text("Password") },
                            singleLine = true, visualTransformation = PasswordVisualTransformation()
                        )
                        if (error.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(error, color = Color(0xFFB3261E), fontSize = 13.sp)
                        }
                        if (recoveryMessage.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(recoveryMessage, color = Muted, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    error = "Masukkan email dan password."
                                } else {
                                    busy = true
                                    error = ""
                                    scope.launch {
                                        try {
                                            val loggedIn = api.login(email, password)
                                            val userProfile = api.profile(loggedIn.accessToken, loggedIn.userId)
                                            session = loggedIn
                                            profile = userProfile
                                            productsLoading = true
                                            try {
                                                products = api.products(loggedIn.accessToken)
                                                pendingOrders = localDao.getPendingOrders()
                                                shiftHistory = localDao.getShifts()
                                                activeShift = shiftHistory.firstOrNull { it.endedAt == null }
                                                syncPendingSales(api, localDao, loggedIn.accessToken)
                                                queuedSalesCount = localDao.getOutbox().size
                                                reportSales = api.sales(loggedIn.accessToken)
                                                if (userProfile.peran.equals("Pemilik", true)) { val d = java.time.LocalDate.now(); reportExpenses = api.expensesForRange(loggedIn.accessToken, d.toString(), d.toString()) }
                                            } finally { productsLoading = false }
                                        } catch (e: Exception) {
                                            error = e.message ?: "Tidak dapat masuk. Periksa koneksi."
                                        } finally { busy = false }
                                    }
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (busy) CircularProgressIndicator(color = Color.White, modifier = Modifier.height(20.dp))
                            else Text("MASUK", fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = {
                                if (email.isBlank()) {
                                    recoveryMessage = "Masukkan email akun terlebih dahulu."
                                } else {
                                    recoveryBusy = true
                                    recoveryMessage = ""
                                    scope.launch {
                                        try {
                                            api.requestPasswordRecovery(email)
                                            recoveryMessage = "Jika email terdaftar, tautan pemulihan telah dikirim. Periksa kotak masuk dan spam."
                                        } catch (e: Exception) {
                                            recoveryMessage = e.message ?: "Tautan pemulihan gagal dikirim."
                                        } finally { recoveryBusy = false }
                                    }
                                }
                            },
                            enabled = !recoveryBusy && !busy,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            if (recoveryBusy) CircularProgressIndicator(modifier = Modifier.height(16.dp), strokeWidth = 2.dp)
                            else Text("Lupa kata sandi?")
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("Gunakan akun ARISTA yang sudah terdaftar.", color = Muted, fontSize = 12.sp)
            }
        } else {
            val isOwner = profile?.peran.equals("Pemilik", true)
            val labels = if (isOwner) listOf("Beranda", "Kasir", "Laporan", "Kelola", "Profil")
                else listOf("Beranda", "Kasir", "Laporan", "Profil")
            val icons = if (isOwner) listOf(Icons.Default.Coffee, Icons.Default.PointOfSale, Icons.Default.Assessment, Icons.Default.Settings, Icons.Default.Person)
                else listOf(Icons.Default.Coffee, Icons.Default.PointOfSale, Icons.Default.Assessment, Icons.Default.Person)
            Scaffold(
                containerColor = Cream,
                bottomBar = {
                    NavigationBar(containerColor = Color.White) {
                        labels.forEachIndexed { index, label ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = {
                                    selectedTab = index
                                    if ((index == 0 || index == 2) && session != null) {
                                        reportsLoading = true
                                        scope.launch {
                                            try { reportSales = api.sales(session!!.accessToken); if (profile?.peran.equals("Pemilik", true)) { val d = java.time.LocalDate.now(); reportExpenses = api.expensesForRange(session!!.accessToken, d.toString(), d.toString()) }; error = "" }
                                            catch (e: Exception) { error = e.message ?: "Data gagal dimuat." }
                                            finally { reportsLoading = false }
                                        }
                                    }
                                    if (index == 3 && session != null && profile?.peran == "Pemilik") {
                                        managementBusy = true
                                        scope.launch {
                                            try { staffRows = org.json.JSONArray(api.cashierAction(session!!.accessToken, org.json.JSONObject().put("aksi", "daftar"))); error = "" }
                                            catch (e: Exception) { error = e.message ?: "Daftar akun gagal dimuat." }
                                            finally { managementBusy = false }
                                        }
                                    }
                                },
                                icon = { Icon(icons[index], contentDescription = label) },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ARISTA", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                            Text("COFFEE & ROASTERY", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                        }
                        TextButton(onClick = { session = null; profile = null; password = "" }) {
                            Text("Keluar", color = Terracotta)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Halo, ${profile!!.nama}", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("Peran: ${profile!!.peran}", color = Muted, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    if (selectedTab == 1 && cashierSection != "Struk") {
                        val shiftSales = (0 until reportSales.length()).mapNotNull { reportSales.optJSONObject(it) }
                            .filter { sale ->
                                if (sale.optBoolean("batal")) false
                                else saleTimestamp(sale) >= (activeShift?.startedAt ?: Long.MAX_VALUE)
                            }
                        val shiftCashSales = shiftSales.filter { it.optString("method") == "Tunai" }.sumOf { it.optDouble("total") }
                        val shiftAllSales = shiftSales.sumOf { it.optDouble("total") }
                        val expectedCash = (activeShift?.openingCash ?: 0.0) + shiftCashSales
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(12.dp)) {
                                if (activeShift == null) {
                                    Text("Shift belum dibuka", color = Ink, fontWeight = FontWeight.SemiBold)
                                    OutlinedTextField(openingCashInput, { openingCashInput = it.filter(Char::isDigit) }, Modifier.fillMaxWidth().height(54.dp), label = { Text("Modal awal kas (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                                    Button(enabled = openingCashInput.isNotBlank(), onClick = {
                                        val amount = openingCashInput.toDoubleOrNull() ?: 0.0
                                        val shift = ShiftEntity(java.util.UUID.randomUUID().toString(), System.currentTimeMillis(), amount)
                                        scope.launch { localDao.saveShift(shift); activeShift = shift; shiftHistory = localDao.getShifts(); openingCashInput = ""; closingCashInput = "" }
                                    }, colors = ButtonDefaults.buttonColors(containerColor = Terracotta)) { Text("Buka shift") }
                                } else {
                                    Text("Shift aktif", color = Terracotta, fontWeight = FontWeight.SemiBold)
                                    Text("Sejak " + java.text.SimpleDateFormat("HH:mm", java.util.Locale("id", "ID")).format(java.util.Date(activeShift!!.startedAt)), color = Muted, fontSize = 11.sp)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Uang awal", color = Muted, fontSize = 12.sp)
                                        Text("Rp ${activeShift!!.openingCash.toLong()}", color = Ink, fontSize = 12.sp)
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Penjualan tunai", color = Muted, fontSize = 12.sp)
                                        Text("Rp ${shiftCashSales.toLong()}", color = Ink, fontSize = 12.sp)
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Semua penjualan (${shiftSales.size} transaksi)", color = Muted, fontSize = 12.sp)
                                        Text("Rp ${shiftAllSales.toLong()}", color = Ink, fontSize = 12.sp)
                                    }
                                    HorizontalDivider(color = Cream, modifier = Modifier.padding(vertical = 7.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Kas seharusnya di laci", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        Text("Rp ${expectedCash.toLong()}", color = Terracotta, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    OutlinedTextField(closingCashInput, { closingCashInput = it.filter(Char::isDigit) }, Modifier.fillMaxWidth().height(54.dp), label = { Text("Uang yang dihitung sekarang (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                                    if (closingCashInput.isNotBlank()) {
                                        val difference = (closingCashInput.toDoubleOrNull() ?: 0.0) - expectedCash
                                        Text(
                                            when { difference == 0.0 -> "Selisih: Pas"; difference > 0 -> "Selisih: Lebih Rp ${difference.toLong()}"; else -> "Selisih: Kurang Rp ${(-difference).toLong()}" },
                                            color = if (difference < 0) Color(0xFFB3261E) else Terracotta,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Button(enabled = closingCashInput.isNotBlank(), onClick = {
                                        val current = activeShift ?: return@Button
                                        val endAmount = closingCashInput.toDoubleOrNull() ?: 0.0
                                        val closed = current.copy(endedAt = System.currentTimeMillis(), closingCash = endAmount, difference = endAmount - expectedCash)
                                        scope.launch { localDao.saveShift(closed); activeShift = null; shiftHistory = localDao.getShifts(); closingCashInput = "" }
                                    }, colors = ButtonDefaults.buttonColors(containerColor = Terracotta)) { Text("Tutup kasir") }
                                }
                            }
                        }
                        if (shiftHistory.isNotEmpty()) {
                            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Riwayat shift kasir", color = Ink, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(6.dp))
                                    LazyColumn(Modifier.heightIn(max = 190.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(shiftHistory, key = { it.id }) { shift ->
                                            val startText = java.text.SimpleDateFormat("dd/MM/yy HH:mm", java.util.Locale("id", "ID")).format(java.util.Date(shift.startedAt))
                                            val endText = shift.endedAt?.let { java.text.SimpleDateFormat("dd/MM/yy HH:mm", java.util.Locale("id", "ID")).format(java.util.Date(it)) } ?: "Masih aktif"
                                            Column(Modifier.fillMaxWidth().background(Cream, RoundedCornerShape(8.dp)).padding(9.dp)) {
                                                Text("Mulai: $startText · $endText", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                Text("Modal awal Rp ${shift.openingCash.toLong()}" + (shift.closingCash?.let { " · Kas akhir Rp ${it.toLong()}" } ?: ""), color = Muted, fontSize = 11.sp)
                                                shift.difference?.let { Text("Selisih kas: Rp ${it.toLong()}", color = if (it < 0) Color(0xFFB3261E) else Terracotta, fontSize = 11.sp) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (cashierSection != "Struk") Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf("Menu", "Keranjang", "ORDERAN", "SHIFT").forEach { section ->
                                val label = when (section) { "Keranjang" -> "Keranjang (${cart.values.sum()})"; "ORDERAN" -> "ORDERAN (${pendingOrders.size})"; else -> section }
                                if (cashierSection == section) Button(onClick = { cashierSection = section }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp), colors = ButtonDefaults.buttonColors(containerColor = Terracotta)) { Text(label, fontSize = 10.sp) }
                                else OutlinedButton(onClick = { cashierSection = section }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)) { Text(label, fontSize = 10.sp) }
                            }
                        }
                        if (cashierSection == "SHIFT") {
                            Text("SHIFT KASIR", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            Text("Ringkasan dan riwayat buka/tutup kasir", color = Muted, fontSize = 11.sp)
                            if (shiftHistory.isEmpty()) Text("Belum ada riwayat tutup kasir.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                            else shiftHistory.forEach { oldShift ->
                                val startText = java.text.SimpleDateFormat("dd/MM/yy HH:mm", java.util.Locale("id", "ID")).format(java.util.Date(oldShift.startedAt))
                                val endText = oldShift.endedAt?.let { java.text.SimpleDateFormat("HH:mm", java.util.Locale("id", "ID")).format(java.util.Date(it)) } ?: "Masih aktif"
                                Card(Modifier.fillMaxWidth().padding(bottom = 7.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                    Column(Modifier.padding(10.dp)) {
                                        Text(startText + " – " + endText, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Uang awal", color = Muted, fontSize = 11.sp); Text("Rp ${oldShift.openingCash.toLong()}", color = Ink, fontSize = 11.sp) }
                                        oldShift.closingCash?.let { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Uang dihitung", color = Muted, fontSize = 11.sp); Text("Rp ${it.toLong()}", color = Ink, fontSize = 11.sp) } }
                                        oldShift.difference?.let { diff -> Text(if (diff == 0.0) "Selisih: Pas" else if (diff > 0) "Selisih: Lebih Rp ${diff.toLong()}" else "Selisih: Kurang Rp ${(-diff).toLong()}", color = if (diff < 0) Color(0xFFB3261E) else Terracotta, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                                    }
                                }
                            }
                        }
                        if (cashierSection == "Menu") {
                            val categories = listOf("Semua") + products.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
                            val visibleProducts = if (selectedCategory == "Semua") products else products.filter { it.category == selectedCategory }
                            Text("Menu", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                categories.forEach { category ->
                                    val active = selectedCategory == category
                                    if (active) {
                                        Button(
                                            onClick = { selectedCategory = category },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
                                        ) { Text(category, fontSize = 12.sp, maxLines = 1) }
                                    } else {
                                        OutlinedButton(
                                            onClick = { selectedCategory = category },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                                        ) { Text(category, fontSize = 12.sp, maxLines = 1) }
                                    }
                                }
                            }
                            when {
                                productsLoading -> CircularProgressIndicator(color = Terracotta)
                                products.isEmpty() -> Text("Belum ada menu aktif atau gagal memuat menu.", color = Muted, fontSize = 14.sp)
                                else -> LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    gridItems(visibleProducts, key = { it.id }) { product ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White)
                                        ) {
                                            Column(
                                                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                                horizontalAlignment = Alignment.Start
                                            ) {
                                                Text(product.name, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                                Text("Rp ${product.price.toLong()}", color = Muted, fontSize = 12.sp)
                                                Button(
                                                    onClick = {
                                                        if (activeShift == null) error = "Buka kasir dulu untuk mulai menjual."
                                                        else { cart = cart + (product.id to ((cart[product.id] ?: 0) + 1)); error = "" }
                                                    },
                                                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
                                                ) { Text("+ Tambah", fontSize = 12.sp) }
                                                val quantity = cart[product.id] ?: 0
                                                if (quantity > 0) {
                                                    Text("Di keranjang: $quantity", color = Terracotta, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (cashierSection == "Keranjang") {
                            val chosen = products.filter { (cart[it.id] ?: 0) > 0 }
                            if (chosen.isEmpty()) {
                                Text("Keranjang kosong.", color = Muted, fontSize = 12.sp)
                                OutlinedButton(onClick = { cashierSection = "Menu" }) { Text("Pilih menu") }
                            } else {
                                val subtotal = chosen.sumOf { it.price * (cart[it.id] ?: 0) }
                                Card(Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp)) {
                                        Text("Keranjang · ${cart.values.sum()} item", color = Ink, fontWeight = FontWeight.Bold)
                                        chosen.forEach { p ->
                                            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Column(Modifier.weight(1f)) {
                                                    Text(p.name, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                    Text("Rp ${p.price.toLong()} × ${cart[p.id]}", color = Muted, fontSize = 11.sp)
                                                }
                                                Text("Rp ${(p.price * (cart[p.id] ?: 0)).toLong()}", color = Ink, fontSize = 12.sp)
                                                TextButton(onClick = { cart = if ((cart[p.id] ?: 0) <= 1) cart - p.id else cart + (p.id to ((cart[p.id] ?: 0) - 1)) }) { Text("−") }
                                                Text("${cart[p.id] ?: 0}", color = Ink, fontSize = 12.sp)
                                                TextButton(onClick = { cart = cart + (p.id to ((cart[p.id] ?: 0) + 1)) }) { Text("+") }
                                            }
                                            HorizontalDivider(color = Cream)
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedTextField(customerName, { customerName = it; error = "" }, Modifier.fillMaxWidth().height(54.dp), label = { Text("Nama customer") }, singleLine = true)
                                        Spacer(Modifier.height(8.dp))
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Subtotal", color = Muted)
                                            Text("Rp ${subtotal.toLong()}", color = Ink, fontWeight = FontWeight.SemiBold)
                                        }
                                        Spacer(Modifier.height(12.dp))
                                        OutlinedButton(enabled = !savingPending && activeShift != null, modifier = Modifier.fillMaxWidth(), onClick = {
                                            savingPending = true
                                            scope.launch {
                                                try {
                                                    if (customerName.isBlank()) {
                                                        error = "Isi nama customer dulu untuk pesanan tertunda."
                                                        savingPending = false
                                                        return@launch
                                                    }
                                                    val rows = org.json.JSONArray()
                                                    chosen.forEach { p ->
                                                        rows.put(org.json.JSONObject().put("id", p.id).put("name", p.name).put("price", p.price).put("cost", p.cost).put("qty", cart[p.id] ?: 0))
                                                    }
                                                    localDao.savePendingOrder(PendingOrderEntity(editingPendingId ?: java.util.UUID.randomUUID().toString(), customerName.trim(), rows.toString(), subtotal, System.currentTimeMillis()))
                                                    pendingOrders = localDao.getPendingOrders(); editingPendingId = null; cart = emptyMap(); cashReceived = ""; customerName = ""; discountInput = ""; cashierSection = "ORDERAN"; error = ""
                                                } catch (e: Exception) { error = e.message ?: "Pesanan gagal disimpan." }
                                                finally { savingPending = false }
                                            }
                                        }) { Text(if (savingPending) "Menyimpan pesanan…" else "Simpan pesanan") }
                                        Button(enabled = activeShift != null, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Terracotta), onClick = { cashierSection = "Pembayaran" }) {
                                            Text("Lanjut ke pembayaran")
                                        }
                                        if (error.isNotBlank()) Text(error, color = Color(0xFFB3261E), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        if (cashierSection == "Pembayaran") {
                            val chosen = products.filter { (cart[it.id] ?: 0) > 0 }
                            if (chosen.isEmpty()) {
                                Text("Keranjang kosong. Pilih menu terlebih dahulu.", color = Muted, fontSize = 12.sp)
                                OutlinedButton(onClick = { cashierSection = "Menu" }) { Text("Pilih menu") }
                            } else {
                                val subtotal = chosen.sumOf { it.price * (cart[it.id] ?: 0) }
                                val discountValue = discountInput.toDoubleOrNull() ?: 0.0
                                val discountAmount = if (discountType == "%") (subtotal * discountValue.coerceIn(0.0, 100.0) / 100.0).toLong().toDouble() else discountValue.coerceAtLeast(0.0).coerceAtMost(subtotal)
                                val total = (subtotal - discountAmount).coerceAtLeast(0.0)
                                Card(Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp)) {
                                        Text("Pembayaran", color = Ink, fontWeight = FontWeight.Bold)
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Subtotal", color = Muted, fontSize = 12.sp)
                                            Text("Rp ${subtotal.toLong()}", color = Ink, fontSize = 12.sp)
                                        }
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(discountInput, { discountInput = it.filter { ch -> ch.isDigit() || ch == '.' } }, Modifier.weight(1f).height(54.dp), label = { Text("Diskon") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                listOf("Rp", "%").forEach { type ->
                                                    if (discountType == type) Button(onClick = { discountType = type }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp), colors = ButtonDefaults.buttonColors(containerColor = Terracotta)) { Text(type) }
                                                    else OutlinedButton(onClick = { discountType = type }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) { Text(type) }
                                                }
                                            }
                                        }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Potongan", color = Muted, fontSize = 12.sp)
                                            Text("-Rp ${discountAmount.toLong()}", color = Muted, fontSize = 12.sp)
                                        }
                                        Text("TOTAL Rp ${total.toLong()}", color = Terracotta, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Spacer(Modifier.height(10.dp))
                                        listOf(listOf("Tunai", "QRIS", "Debit/Kredit"), listOf("Transfer", "Piutang")).forEach { methodRow ->
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                methodRow.forEach { method ->
                                                    if (paymentMethod == method) Button(onClick = { paymentMethod = method }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 2.dp, vertical = 7.dp), colors = ButtonDefaults.buttonColors(containerColor = Terracotta)) { Text(method, fontSize = 10.sp) }
                                                    else OutlinedButton(onClick = { paymentMethod = method }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 2.dp, vertical = 7.dp)) { Text(method, fontSize = 10.sp) }
                                                }
                                            }
                                        }
                                        if (paymentMethod == "Tunai") {
                                            OutlinedTextField(value = cashReceived, onValueChange = { cashReceived = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth().height(54.dp), label = { Text("Uang diterima (Rp)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                listOf(total.toLong(), 20000L, 50000L).distinct().forEach { amount ->
                                                    TextButton(onClick = { cashReceived = amount.toString() }) { Text(if (amount == total.toLong()) "Uang pas" else "Rp $amount", fontSize = 11.sp) }
                                                }
                                            }
                                            val received = cashReceived.toLongOrNull() ?: 0L
                                            Text("Kembalian Rp ${(received - total.toLong()).coerceAtLeast(0L)}", color = Muted, fontSize = 12.sp)
                                        } else if (paymentMethod == "Piutang") {
                                            Text("Dicatat sebagai piutang untuk ${customerName.trim().ifBlank { "customer tanpa nama" }}.", color = Muted, fontSize = 12.sp)
                                        } else {
                                            Text("Pembayaran ${paymentMethod} dicatat manual.", color = Muted, fontSize = 12.sp)
                                        }
                                        Button(
                                            enabled = !paymentBusy && activeShift != null && (if (paymentMethod == "Piutang") customerName.isNotBlank() else paymentMethod != "Tunai" || (cashReceived.toLongOrNull() ?: 0L) >= total.toLong()),
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                                            onClick = {
                                                val active = session ?: return@Button
                                                paymentBusy = true
                                                scope.launch {
                                                    try {
                                                        val rows = org.json.JSONArray()
                                                        chosen.forEach { p ->
                                                            rows.put(org.json.JSONObject().put("id", p.id).put("name", p.name).put("cat", p.category).put("price", p.price).put("cost", p.cost).put("qty", cart[p.id] ?: 0))
                                                        }
                                                        val now = java.time.LocalDateTime.now()
                                                        val sale = org.json.JSONObject()
                                                            .put("id", java.util.UUID.randomUUID().toString())
                                                            .put("sale_date", now.toLocalDate().toString())
                                                            .put("sale_time", now.toLocalTime().toString().take(5))
                                                            .put("items", rows).put("total", total)
                                                            .put("method", paymentMethod)
                                                            .put("paid", if (paymentMethod == "Piutang") 0L else if (paymentMethod == "Tunai") (cashReceived.toLongOrNull() ?: 0L) else total.toLong())
                                                            .put("change_amount", if (paymentMethod == "Tunai") ((cashReceived.toLongOrNull() ?: 0L) - total.toLong()).coerceAtLeast(0L) else 0L)
                                                            .put("cashier_name", profile?.nama ?: "")
                                                            .put("customer", customerName.trim().ifBlank { "Umum" })
                                                            .put("diskon", discountAmount.toLong())
                                                        val online = hasValidatedInternet(context)
                                                        var queuedAfterNetworkError = false
                                                        if (online) {
                                                            try {
                                                                try { api.saveSale(active.accessToken, sale) }
                                                                catch (e: AristaHttpException) {
                                                                    if (e.statusCode != 401) throw e
                                                                    val refreshed = api.refreshSession(active.refreshToken)
                                                                    session = refreshed
                                                                    api.saveSale(refreshed.accessToken, sale)
                                                                }
                                                            } catch (e: AristaHttpException) { throw e }
                                                            catch (e: java.io.IOException) {
                                                                localDao.enqueue(SyncOutboxEntity(sale.getString("id"), sale.toString(), System.currentTimeMillis()))
                                                                queuedSalesCount = localDao.getOutbox().size
                                                                queuedAfterNetworkError = true
                                                            }
                                                        } else {
                                                            localDao.enqueue(SyncOutboxEntity(sale.getString("id"), sale.toString(), System.currentTimeMillis()))
                                                            queuedSalesCount = localDao.getOutbox().size
                                                        }
                                                        lastReceipt = sale
                                                        cart = emptyMap()
                                                        editingPendingId?.let { localDao.deletePendingOrder(it) }
                                                        editingPendingId = null
                                                        pendingOrders = localDao.getPendingOrders()
                                                        cashierSection = "Struk"
                                                        cashReceived = ""; customerName = ""; discountInput = ""; discountType = "Rp"
                                                        if (online && !queuedAfterNetworkError) {
                                                            try { products = api.products(active.accessToken); reportSales = api.sales(active.accessToken) } catch (_: Exception) { }
                                                            error = ""
                                                        } else {
                                                            error = "Transaksi disimpan di perangkat. Akan disinkronkan saat internet tersedia."
                                                        }
                                                    } catch (e: Exception) {
                                                        error = e.message ?: "Transaksi gagal disimpan."
                                                    } finally { paymentBusy = false }
                                                }
                                            }
                                        ) {
                                            if (paymentBusy) CircularProgressIndicator(color = Color.White, modifier = Modifier.height(18.dp))
                                            else Text("BAYAR ${paymentMethod.uppercase()}")
                                        }
                                        OutlinedButton(enabled = !paymentBusy, modifier = Modifier.fillMaxWidth(), onClick = { cashierSection = "Keranjang" }) { Text("Kembali ke keranjang") }
                                        if (error.isNotBlank()) Text(error, color = Color(0xFFB3261E), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        if (cashierSection == "Struk") {
                            val receipt = lastReceipt
                            if (receipt != null) {
                                val receiptItems = receipt.optJSONArray("items") ?: org.json.JSONArray()
                                val receiptSubtotal = (0 until receiptItems.length()).sumOf { i ->
                                    val item = receiptItems.optJSONObject(i) ?: org.json.JSONObject()
                                    item.optDouble("price") * item.optInt("qty", 1)
                                }
                                val receiptTotal = receipt.optDouble("total")
                                val receiptDiscount = receipt.optDouble("diskon")
                                Card(Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("ARISTA", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                                        Text("COFFEE & ROASTERY", color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
                                        Spacer(Modifier.height(12.dp))
                                        HorizontalDivider(color = Cream)
                                        Text("STRUK PEMBAYARAN", color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 10.dp))
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Tanggal", color = Muted, fontSize = 12.sp)
                                            Text(receipt.optString("sale_date"), color = Ink, fontSize = 12.sp)
                                        }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Jam", color = Muted, fontSize = 12.sp)
                                            Text(receipt.optString("sale_time"), color = Ink, fontSize = 12.sp)
                                        }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Customer", color = Muted, fontSize = 12.sp)
                                            Text(receipt.optString("customer", "Umum"), color = Ink, fontSize = 12.sp)
                                        }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Kasir", color = Muted, fontSize = 12.sp)
                                            Text(receipt.optString("cashier_name"), color = Ink, fontSize = 12.sp)
                                        }
                                        HorizontalDivider(color = Cream)
                                        for (i in 0 until receiptItems.length()) {
                                            val item = receiptItems.optJSONObject(i) ?: org.json.JSONObject()
                                            val qty = item.optInt("qty", 1)
                                            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text(item.optString("name"), color = Ink, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                                    Text("Rp ${(item.optDouble("price") * qty).toLong()}", color = Ink, fontSize = 12.sp)
                                                }
                                                Text("${qty} × Rp ${item.optDouble("price").toLong()}", color = Muted, fontSize = 10.sp)
                                            }
                                        }
                                        HorizontalDivider(color = Cream)
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Subtotal", color = Muted, fontSize = 12.sp)
                                            Text("Rp ${receiptSubtotal.toLong()}", color = Ink, fontSize = 12.sp)
                                        }
                                        if (receiptDiscount > 0) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Diskon", color = Muted, fontSize = 12.sp)
                                            Text("-Rp ${receiptDiscount.toLong()}", color = Ink, fontSize = 12.sp)
                                        }
                                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("TOTAL", color = Ink, fontWeight = FontWeight.Bold)
                                            Text("Rp ${receiptTotal.toLong()}", color = Terracotta, fontWeight = FontWeight.Bold)
                                        }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Metode", color = Muted, fontSize = 12.sp)
                                            Text(receipt.optString("method"), color = Ink, fontSize = 12.sp)
                                        }
                                        if (receipt.optString("method") == "Tunai") {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Diterima", color = Muted, fontSize = 12.sp)
                                                Text("Rp ${receipt.optDouble("paid").toLong()}", color = Ink, fontSize = 12.sp)
                                            }
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Kembalian", color = Muted, fontSize = 12.sp)
                                                Text("Rp ${receipt.optDouble("change_amount").toLong()}", color = Ink, fontSize = 12.sp)
                                            }
                                        }
                                        Spacer(Modifier.height(12.dp))
                                        Text("Terima kasih", color = Terracotta, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            Button(
                                onClick = { lastReceipt = null; cashierSection = "Menu"; error = "" },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
                            ) { Text("Selesai") }
                            if (error.isNotBlank()) Text(error, color = Muted, fontSize = 11.sp)
                        }
                        if (queuedSalesCount > 0 && cashierSection != "Struk") {
                            Spacer(Modifier.height(10.dp))
                            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("$queuedSalesCount transaksi menunggu sinkronisasi", color = Ink, fontWeight = FontWeight.SemiBold)
                                    OutlinedButton(enabled = !syncingSales && hasValidatedInternet(context), modifier = Modifier.fillMaxWidth(), onClick = {
                                        val token = session?.accessToken ?: return@OutlinedButton
                                        syncingSales = true
                                        scope.launch {
                                            try {
                                                syncPendingSales(api, localDao, token)
                                                queuedSalesCount = localDao.getOutbox().size
                                                products = api.products(token)
                                                reportSales = api.sales(token)
                                                error = if (queuedSalesCount == 0) "Semua transaksi offline berhasil disinkronkan." else "Masih ada transaksi yang belum berhasil dikirim."
                                            } catch (e: Exception) { error = e.message ?: "Sinkronisasi gagal." }
                                            finally { syncingSales = false }
                                        }
                                    }) { Text(if (syncingSales) "Menyinkronkan…" else "Sinkronkan sekarang") }
                                }
                            }
                        }
                        if (cashierSection == "ORDERAN") {
                        Spacer(Modifier.height(12.dp))
                        Text("ORDERAN", color = Ink, fontWeight = FontWeight.SemiBold)
                        Text("Pesanan yang belum dibayar. Stok belum berkurang sampai pembayaran selesai.", color = Muted, fontSize = 11.sp)
                        if (pendingOrders.isEmpty()) {
                            Text("Belum ada pesanan tertunda.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp))
                        } else {
                            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(pendingOrders, key = { it.id }) { order ->
                                    val savedItems = org.json.JSONArray(order.itemsJson)
                                    val itemSummary = (0 until savedItems.length()).mapNotNull { i -> savedItems.optJSONObject(i)?.let { row -> "${row.optString("name", "Menu")} × ${row.optInt("qty", 1)}" } }.joinToString(", ")
                                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(order.customer, color = Ink, fontWeight = FontWeight.SemiBold)
                                                Text(itemSummary.ifBlank { "${savedItems.length()} jenis menu" } + " · Rp ${order.total.toLong()}", color = Muted, fontSize = 11.sp)
                                            }
                                            TextButton(onClick = {
                                                val ids = org.json.JSONArray(order.itemsJson)
                                                val restored = mutableMapOf<String, Int>()
                                                for (i in 0 until ids.length()) { val row = ids.getJSONObject(i); restored[row.optString("id")] = row.optInt("qty") }
                                                cart = restored
                                                customerName = order.customer
                                                editingPendingId = order.id
                                                cashierSection = "Keranjang"
                                            }) { Text("Buka & bayar", color = Terracotta) }
                                            TextButton(onClick = { scope.launch { localDao.deletePendingOrder(order.id); pendingOrders = localDao.getPendingOrders() } }) { Text("Hapus", color = Color(0xFFB3261E)) }
                                        }
                                    }
                                }
                            }
                        }
                        }
                    } else if (selectedTab == 2) {
                        val owner = profile?.peran.equals("Pemilik", true)
                        val todayDate = java.time.LocalDate.now()
                        val periodStart = when (reportRange) {
                            "Mingguan" -> todayDate.minusDays(6)
                            "Bulanan" -> todayDate.withDayOfMonth(1)
                            else -> todayDate
                        }
                        val periodSales = (0 until reportSales.length()).mapNotNull { reportSales.optJSONObject(it) }.filter { sale ->
                            val date = try { java.time.LocalDate.parse(sale.optString("sale_date")) } catch (_: Exception) { null }
                            date != null && !date.isBefore(periodStart) && !date.isAfter(todayDate)
                        }
                        val periodExpenses = (0 until reportExpenses.length()).mapNotNull { reportExpenses.optJSONObject(it) }.filter { expense ->
                            val date = try { java.time.LocalDate.parse(expense.optString("tanggal")) } catch (_: Exception) { null }
                            date != null && !date.isBefore(periodStart) && !date.isAfter(todayDate)
                        }
                        val omzet = periodSales.sumOf { sale ->
                            if (sale.optBoolean("batal")) 0.0 else sale.optDouble("total")
                        }
                        val modal = periodSales.sumOf { sale ->
                            if (sale.optBoolean("batal")) 0.0 else {
                                val rows = sale.optJSONArray("items") ?: org.json.JSONArray()
                                (0 until rows.length()).sumOf { k ->
                                    val item = rows.optJSONObject(k) ?: org.json.JSONObject()
                                    item.optDouble("cost") * item.optDouble("qty")
                                }
                            }
                        }
                        val expenseTotal = periodExpenses.sumOf { it.optDouble("total") }
                        val jumlah = periodSales.count { !it.optBoolean("batal") }
                        val cancelledSales = periodSales.filter { it.optBoolean("batal") }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Laporan penjualan", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = {
                                reportsLoading = true
                                scope.launch {
                                    try { reportSales = api.salesForRange(session!!.accessToken, periodStart.toString(), todayDate.toString()); if (owner) reportExpenses = api.expensesForRange(session!!.accessToken, periodStart.toString(), todayDate.toString()); error = "" }
                                    catch (e: Exception) { error = e.message ?: "Gagal memuat laporan." }
                                    finally { reportsLoading = false }
                                }
                            }) { Text("Muat ulang", color = Terracotta) }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Harian", "Mingguan", "Bulanan").forEach { range ->
                                if (reportRange == range) Button(onClick = {
                                    reportRange = range
                                    val endDate = java.time.LocalDate.now()
                                    val startDate = when (range) { "Mingguan" -> endDate.minusDays(6); "Bulanan" -> endDate.withDayOfMonth(1); else -> endDate }
                                    val token = session?.accessToken
                                    if (token != null) { reportsLoading = true; scope.launch { try { reportSales = api.salesForRange(token, startDate.toString(), endDate.toString()); if (owner) reportExpenses = api.expensesForRange(token, startDate.toString(), endDate.toString()); error = "" } catch (e: Exception) { error = e.message ?: "Gagal memuat laporan." } finally { reportsLoading = false } } }
                                }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Terracotta)) { Text(range, fontSize = 11.sp) }
                                else OutlinedButton(onClick = {
                                    reportRange = range
                                    val endDate = java.time.LocalDate.now()
                                    val startDate = when (range) { "Mingguan" -> endDate.minusDays(6); "Bulanan" -> endDate.withDayOfMonth(1); else -> endDate }
                                    val token = session?.accessToken
                                    if (token != null) { reportsLoading = true; scope.launch { try { reportSales = api.salesForRange(token, startDate.toString(), endDate.toString()); if (owner) reportExpenses = api.expensesForRange(token, startDate.toString(), endDate.toString()); error = "" } catch (e: Exception) { error = e.message ?: "Gagal memuat laporan." } finally { reportsLoading = false } } }
                                }, modifier = Modifier.weight(1f)) { Text(range, fontSize = 11.sp) }
                            }
                        }
                        if (owner) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                listOf("Penjualan", "Pengeluaran", "Piutang").forEach { tab ->
                                    if (reportTab == tab) Button(onClick = {
                                        reportTab = tab
                                        if (tab == "Piutang" && receivableSales.length() == 0 && !receivablesLoading) {
                                            val token = session?.accessToken
                                            if (token != null) {
                                                receivablesLoading = true
                                                scope.launch {
                                                    try {
                                                        receivableSales = api.receivableSales(token)
                                                        receivablePayments = api.receivablePayments(token)
                                                        error = ""
                                                    } catch (e: Exception) { error = e.message ?: "Piutang gagal dimuat." }
                                                    finally { receivablesLoading = false }
                                                }
                                            }
                                        }
                                    }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp), colors = ButtonDefaults.buttonColors(containerColor = Terracotta)) { Text(tab, fontSize = 10.sp) }
                                    else OutlinedButton(onClick = { reportTab = tab }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)) { Text(tab, fontSize = 10.sp) }
                                }
                            }
                        }
                        if (reportTab == "Piutang" && owner) {
                            if (receivablesLoading) CircularProgressIndicator(color = Terracotta)
                            else {
                                val unpaid = (0 until receivableSales.length()).mapNotNull { receivableSales.optJSONObject(it) }.filterNot { it.optBoolean("batal") }.map { sale ->
                                    val paid = (0 until receivablePayments.length()).sumOf { i ->
                                        val payment = receivablePayments.optJSONObject(i) ?: org.json.JSONObject()
                                        if (payment.optString("sale_id") == sale.optString("id")) payment.optDouble("nominal") else 0.0
                                    }
                                    org.json.JSONObject(sale.toString()).put("dibayar", paid).put("sisa", (sale.optDouble("total") - paid).coerceAtLeast(0.0))
                                }.filter { it.optDouble("sisa") > 0.0 }
                                val customers = unpaid.map { it.optString("customer").ifBlank { "Umum" } }.distinct().sorted()
                                val totalDue = unpaid.sumOf { it.optDouble("sisa") }
                                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text("Total piutang belum lunas", color = Muted, fontSize = 12.sp)
                                        Text("Rp ${totalDue.toLong()}", color = Terracotta, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                        Text("${unpaid.size} nota · ${customers.size} customer", color = Muted, fontSize = 11.sp)
                                    }
                                }
                                if (receivableCustomer == null) {
                                    Text("Daftar customer", color = Ink, fontWeight = FontWeight.SemiBold)
                                    if (customers.isEmpty()) Text("Tidak ada piutang yang belum lunas.", color = Muted, fontSize = 12.sp)
                                    customers.forEach { customer ->
                                        val due = unpaid.filter { it.optString("customer").ifBlank { "Umum" } == customer }.sumOf { it.optDouble("sisa") }
                                        Card(Modifier.fillMaxWidth().clickable { receivableCustomer = customer }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(customer, color = Ink, fontWeight = FontWeight.SemiBold)
                                                Text("Rp ${due.toLong()}", color = Terracotta, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                } else {
                                    TextButton(onClick = { receivableCustomer = null; receivableAmount = "" }) { Text("← Semua customer", color = Terracotta) }
                                    val customerNotes = unpaid.filter { it.optString("customer").ifBlank { "Umum" } == receivableCustomer }
                                    val customerDue = customerNotes.sumOf { it.optDouble("sisa") }
                                    Text(receivableCustomer ?: "", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text("Sisa tagihan Rp ${customerDue.toLong()}", color = Terracotta, fontWeight = FontWeight.SemiBold)
                                    OutlinedTextField(receivableAmount, { receivableAmount = it.filter(Char::isDigit) }, Modifier.fillMaxWidth().height(54.dp), label = { Text("Nominal pembayaran (Rp)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("Tunai", "QRIS", "Debit/Kredit", "Transfer").forEach { method ->
                                            if (receivableMethod == method) Button(onClick = { receivableMethod = method }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 2.dp, vertical = 7.dp), colors = ButtonDefaults.buttonColors(containerColor = Terracotta)) { Text(method, fontSize = 9.sp) }
                                            else OutlinedButton(onClick = { receivableMethod = method }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 2.dp, vertical = 7.dp)) { Text(method, fontSize = 9.sp) }
                                        }
                                    }
                                    Button(enabled = !paymentBusy && (receivableAmount.toDoubleOrNull() ?: 0.0) > 0, onClick = {
                                        val token = session?.accessToken ?: return@Button
                                        val amount = receivableAmount.toDoubleOrNull() ?: 0.0
                                        paymentBusy = true
                                        scope.launch {
                                            try {
                                                api.payReceivableCustomer(token, receivableCustomer ?: "", amount, receivableMethod)
                                                receivableSales = api.receivableSales(token)
                                                receivablePayments = api.receivablePayments(token)
                                                receivableAmount = ""
                                                error = ""
                                            } catch (e: Exception) { error = e.message ?: "Pembayaran piutang gagal." }
                                            finally { paymentBusy = false }
                                        }
                                    }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Terracotta)) { Text("Bayar sekaligus") }
                                    Text("Nota belum lunas", color = Ink, fontWeight = FontWeight.SemiBold)
                                    customerNotes.forEach { note ->
                                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                            Column(Modifier.padding(12.dp)) {
                                                Text(note.optString("sale_date"), color = Ink, fontWeight = FontWeight.SemiBold)
                                                Text("Total Rp ${note.optDouble("total").toLong()} · Sudah dibayar Rp ${note.optDouble("dibayar").toLong()}", color = Muted, fontSize = 11.sp)
                                                Text("Sisa Rp ${note.optDouble("sisa").toLong()}", color = Terracotta, fontWeight = FontWeight.Bold)
                                                TextButton(enabled = !paymentBusy, onClick = {
                                                    val token = session?.accessToken ?: return@TextButton
                                                    val amount = (receivableAmount.toDoubleOrNull() ?: 0.0).let { if (it > 0) it.coerceAtMost(note.optDouble("sisa")) else note.optDouble("sisa") }
                                                    paymentBusy = true
                                                    scope.launch {
                                                        try {
                                                            api.payReceivableNote(token, note.optString("id"), amount, receivableMethod)
                                                            receivableSales = api.receivableSales(token)
                                                            receivablePayments = api.receivablePayments(token)
                                                            receivableAmount = ""
                                                            error = ""
                                                        } catch (e: Exception) { error = e.message ?: "Pembayaran nota gagal." }
                                                        finally { paymentBusy = false }
                                                    }
                                                }) { Text("Bayar nota ini", color = Terracotta) }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (reportTab == "Pengeluaran" && owner) {
                            val expenseSum = periodExpenses.sumOf { it.optDouble("total") }
                            Text("Laporan pengeluaran", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Total pengeluaran: Rp ${expenseSum.toLong()}", color = Terracotta, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(expenseName, { expenseName = it }, Modifier.fillMaxWidth().height(54.dp), label = { Text("Nama pengeluaran") }, singleLine = true)
                                    OutlinedTextField(expenseCategory, { expenseCategory = it }, Modifier.fillMaxWidth().height(54.dp), label = { Text("Kategori (opsional)") }, singleLine = true)
                                    OutlinedTextField(expenseDate, { expenseDate = it }, Modifier.fillMaxWidth().height(54.dp), label = { Text("Tanggal (YYYY-MM-DD)") }, singleLine = true)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(expenseQty, { expenseQty = it.filter(Char::isDigit) }, Modifier.weight(1f).height(54.dp), label = { Text("Jumlah") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                                        OutlinedTextField(expensePrice, { expensePrice = it.filter(Char::isDigit) }, Modifier.weight(1f).height(54.dp), label = { Text("Harga satuan") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                                    }
                                    OutlinedTextField(expenseNote, { expenseNote = it }, Modifier.fillMaxWidth().height(54.dp), label = { Text("Catatan (opsional)") }, singleLine = true)
                                    val qVal = expenseQty.toDoubleOrNull() ?: 0.0
                                    val pVal = expensePrice.toDoubleOrNull() ?: 0.0
                                    Text("Total: Rp ${(qVal * pVal).toLong()}", color = Muted)
                                    Button(enabled = !managementBusy && expenseName.isNotBlank() && qVal > 0 && pVal > 0 && runCatching { java.time.LocalDate.parse(expenseDate) }.isSuccess, onClick = {
                                        val token = session?.accessToken ?: return@Button
                                        managementBusy = true
                                        scope.launch {
                                            try {
                                                val payload = org.json.JSONObject().put("tanggal", expenseDate).put("nama", expenseName.trim()).put("kategori", expenseCategory.trim().ifBlank { org.json.JSONObject.NULL }).put("qty", qVal).put("harga", pVal).put("total", qVal * pVal).put("catatan", expenseNote.trim().ifBlank { org.json.JSONObject.NULL })
                                                api.createExpense(token, payload)
                                                val endDate = java.time.LocalDate.now()
                                                val startDate = when (reportRange) { "Mingguan" -> endDate.minusDays(6); "Bulanan" -> endDate.withDayOfMonth(1); else -> endDate }
                                                reportExpenses = api.expensesForRange(token, startDate.toString(), endDate.toString())
                                                expenseName = ""; expenseCategory = ""; expenseQty = "1"; expensePrice = ""; expenseNote = ""; expenseDate = java.time.LocalDate.now().toString(); error = ""
                                            } catch (e: Exception) { error = e.message ?: "Pengeluaran gagal disimpan." }
                                            finally { managementBusy = false }
                                        }
                                    }, colors = ButtonDefaults.buttonColors(containerColor = Terracotta)) { Text("Simpan pengeluaran") }
                                }
                            }
                            Text("Riwayat pengeluaran", color = Ink, fontWeight = FontWeight.SemiBold)
                            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                items(periodExpenses, key = { it.optString("id") }) { expense ->
                                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(expense.optString("nama"), color = Ink, fontWeight = FontWeight.SemiBold)
                                                Text("${expense.optString("tanggal")} · ${expense.optString("kategori", "-")} · ${expense.optDouble("qty")} × Rp ${expense.optDouble("harga").toLong()}", color = Muted, fontSize = 11.sp)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Rp ${expense.optDouble("total").toLong()}", color = Terracotta, fontWeight = FontWeight.Bold)
                                                TextButton(enabled = !managementBusy, onClick = {
                                                    val token = session?.accessToken ?: return@TextButton
                                                    managementBusy = true
                                                    scope.launch {
                                                        try { api.deleteExpense(token, expense.optString("id")); val endDate = java.time.LocalDate.now(); val startDate = when (reportRange) { "Mingguan" -> endDate.minusDays(6); "Bulanan" -> endDate.withDayOfMonth(1); else -> endDate }; reportExpenses = api.expensesForRange(token, startDate.toString(), endDate.toString()); error = "" }
                                                        catch (e: Exception) { error = e.message ?: "Pengeluaran gagal dihapus." }
                                                        finally { managementBusy = false }
                                                    }
                                                }) { Text("Hapus", color = Color(0xFFB3261E), fontSize = 11.sp) }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (reportsLoading) CircularProgressIndicator(color = Terracotta)
                        else {
                            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Omzet transaksi aktif", color = Muted, fontSize = 12.sp)
                                    Text("Rp ${omzet.toLong()}", color = Terracotta, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                                    Text("Jumlah transaksi: ${jumlah}", color = Ink)
                                     if (owner) {
                                         Text("Estimasi modal: Rp ${modal.toLong()}", color = Muted, fontSize = 12.sp)
                                         Text("Laba kotor: Rp ${(omzet - modal).toLong()}", color = Terracotta, fontWeight = FontWeight.SemiBold)
                                         Text("Pengeluaran tercatat: Rp ${expenseTotal.toLong()}", color = Muted, fontSize = 12.sp)
                                         Text("Sisa setelah pengeluaran: Rp ${(omzet - modal - expenseTotal).toLong()}", color = Ink, fontWeight = FontWeight.SemiBold)
                                     }
                                    Text("Data maksimal 200 transaksi terakhir", color = Muted, fontSize = 11.sp)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
Text("Transaksi terbaru", color = Ink, fontWeight = FontWeight.SemiBold)
                            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(periodSales.filterNot { it.optBoolean("batal") }.take(50)) { sale ->
                                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                        Column(Modifier.padding(12.dp)) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(sale.optString("customer", "Umum"), color = Ink, fontWeight = FontWeight.SemiBold)
                                                Text("Rp \${sale.optDouble("total").toLong()}", color = Terracotta, fontWeight = FontWeight.Bold)
                                            }
                                            Text("\${sale.optString("sale_date")} · \${sale.optString("sale_time")} · \${sale.optString("method")}", color = Muted, fontSize = 12.sp)
                                            val saleItems = sale.optJSONArray("items") ?: org.json.JSONArray()
                                            if (saleItems.length() > 0) {
                                                Text((0 until saleItems.length()).mapNotNull { saleItems.optJSONObject(it) }.joinToString(", ") { "\${it.optString("name")} x\${it.optInt("qty", 1)}" }, color = Muted, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                                if (cancelledSales.isNotEmpty()) {
                                    item {
                                        TextButton(onClick = { showCancelledSales = !showCancelledSales }) {
                                            Text(if (showCancelledSales) "Sembunyikan transaksi dibatalkan (\${cancelledSales.size})" else "Lihat transaksi dibatalkan (\${cancelledSales.size})", color = Terracotta)
                                        }
                                    }
                                    if (showCancelledSales) {
                                        items(cancelledSales.take(50)) { sale ->
                                            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0ED))) {
                                                Column(Modifier.padding(12.dp)) {
                                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Text(sale.optString("customer", "Umum"), color = Ink, fontWeight = FontWeight.SemiBold)
                                                        Text("DIBATALKAN", color = Color(0xFFB3261E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Text("\${sale.optString("sale_date")} · \${sale.optString("sale_time")} · \${sale.optString("method")}", color = Muted, fontSize = 11.sp)
                                                    Text("Rp \${sale.optDouble("total").toLong()}", color = Muted, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (error.isNotBlank()) Text(error, color = Color(0xFFB3261E), fontSize = 12.sp)
                    } else if (selectedTab == 0) {
                        val activeSales = (0 until reportSales.length()).mapNotNull { reportSales.optJSONObject(it) }.filterNot { it.optBoolean("batal") }
                        val today = java.time.LocalDate.now().toString()
                        val todaySales = activeSales.filter { it.optString("sale_date") == today }
                        val todayTotal = todaySales.sumOf { it.optDouble("total") }
                        Text("Ringkasan usaha", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Omzet hari ini", color = Muted, fontSize = 11.sp)
                                    Text("Rp ${todayTotal.toLong()}", color = Terracotta, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Transaksi", color = Muted, fontSize = 11.sp)
                                    Text("${todaySales.size}", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Card(
                            Modifier.fillMaxWidth().clickable { selectedTab = 1; cashierSection = "SHIFT" },
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(if (activeShift == null) "Kasir belum dibuka" else "Kasir sedang buka", color = Ink, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        if (activeShift == null) "Ketuk untuk membuka kasir dan isi uang awal" else "Sejak " + java.text.SimpleDateFormat("HH:mm", java.util.Locale("id", "ID")).format(java.util.Date(activeShift!!.startedAt)) + " · ketuk untuk mengelola shift",
                                        color = Muted, fontSize = 11.sp
                                    )
                                }
                                Text(if (activeShift == null) "›" else "BUKA", color = Terracotta, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { selectedTab = 1; cashierSection = "ORDERAN" },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("ORDERAN · " + pendingOrders.size, color = Terracotta, fontSize = 12.sp) }
                        Spacer(Modifier.height(10.dp))
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Menu aktif", color = Muted, fontSize = 12.sp)
                                Text("${products.size} menu", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text("Data transaksi dimuat dari Supabase", color = Muted, fontSize = 11.sp)
                                TextButton(onClick = {
                                    reportsLoading = true
                                    scope.launch {
                                        try { reportSales = api.sales(session!!.accessToken); products = api.products(session!!.accessToken); error = "" }
                                        catch (e: Exception) { error = e.message ?: "Gagal memuat ringkasan." }
                                        finally { reportsLoading = false }
                                    }
                                }) { Text("Perbarui ringkasan", color = Terracotta) }
                            }
                        }
                        Text("Transaksi terbaru", color = Ink, fontWeight = FontWeight.SemiBold)
                        if (reportsLoading) CircularProgressIndicator(color = Terracotta)
                        else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            items(todaySales.take(10)) { sale ->
                                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(Modifier.weight(1f)) {
                                            Text(sale.optString("customer", "Umum"), color = Ink, fontWeight = FontWeight.SemiBold)
                                            Text("${sale.optString("sale_time")} · ${sale.optString("method")}", color = Muted, fontSize = 11.sp)
                                        }
                                        Text("Rp ${sale.optDouble("total").toLong()}", color = Terracotta, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        if (error.isNotBlank()) Text(error, color = Color(0xFFB3261E), fontSize = 12.sp)
                    } else if (profile?.peran.equals("Pemilik", ignoreCase = true) && selectedTab == 3) {
                        val owner = true
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Backup dan pemulihan", color = Ink, fontWeight = FontWeight.SemiBold)
                                Text("Simpan data lokal aplikasi. Snapshot menu dan transaksi disertakan sebagai arsip; pemulihan tidak mengubah data cloud.", color = Muted, fontSize = 11.sp)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(enabled = !managementBusy, onClick = {
                                        managementBusy = true
                                        scope.launch {
                                            try {
                                                val pending = localDao.getPendingOrders()
                                                val outbox = localDao.getOutbox()
                                                val shifts = localDao.getShifts()
                                                val root = org.json.JSONObject()
                                                    .put("app", "arista-native")
                                                    .put("version", 1)
                                                    .put("createdAt", java.time.Instant.now().toString())
                                                    .put("productsSnapshot", org.json.JSONArray().apply { products.forEach { p -> put(org.json.JSONObject().put("id", p.id).put("name", p.name).put("category", p.category).put("price", p.price).put("cost", p.cost).put("stock", p.stock)) } })
                                                    .put("salesSnapshot", org.json.JSONArray(reportSales.toString()))
                                                    .put("pendingOrders", org.json.JSONArray().apply { pending.forEach { p -> put(org.json.JSONObject().put("id", p.id).put("customer", p.customer).put("itemsJson", p.itemsJson).put("total", p.total).put("updatedAt", p.updatedAt)) } })
                                                    .put("syncOutbox", org.json.JSONArray().apply { outbox.forEach { q -> put(org.json.JSONObject().put("id", q.id).put("payloadJson", q.payloadJson).put("createdAt", q.createdAt).put("retryCount", q.retryCount).put("lastError", q.lastError)) } })
                                                    .put("shifts", org.json.JSONArray().apply { shifts.forEach { sh -> put(org.json.JSONObject().put("id", sh.id).put("startedAt", sh.startedAt).put("openingCash", sh.openingCash).put("endedAt", sh.endedAt ?: org.json.JSONObject.NULL).put("closingCash", sh.closingCash ?: org.json.JSONObject.NULL).put("difference", sh.difference ?: org.json.JSONObject.NULL)) } })
                                                backupContent = root.toString(2)
                                                createBackupLauncher.launch("arista-backup-${java.time.LocalDate.now()}.json")
                                                error = ""
                                            } catch (e: Exception) { error = e.message ?: "Backup gagal dibuat." }
                                            finally { managementBusy = false }
                                        }
                                    }, colors = ButtonDefaults.buttonColors(containerColor = Terracotta)) { Text("Unduh backup") }
                                    OutlinedButton(enabled = !managementBusy, onClick = { restoreBackupLauncher.launch(arrayOf("application/json", "text/json")) }) { Text("Pulihkan backup") }
                                }
                            }
                        }
                        if (owner) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                listOf("Menu", "Kategori", "Harga", "Akun kasir").forEach { tab ->
                                    if (manageTab == tab) Button(onClick = { manageTab = tab }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 3.dp, vertical = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = Terracotta)) { Text(tab, fontSize = 10.sp) }
                                    else OutlinedButton(onClick = { manageTab = tab }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 3.dp, vertical = 8.dp)) { Text(tab, fontSize = 10.sp) }
                                }
                            }
                        }
                        if (manageTab == "Menu" || !owner) {
                        Text("Daftar menu", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        if (owner) {
                            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(if (editingProductId == null) "Tambah menu" else "Ubah menu", color = Terracotta, fontWeight = FontWeight.SemiBold)
                                    OutlinedTextField(productName, { productName = it }, Modifier.fillMaxWidth().height(54.dp), label = { Text("Nama menu") }, singleLine = true)
                                    OutlinedTextField(productCategory, { productCategory = it }, Modifier.fillMaxWidth().height(54.dp), label = { Text("Kategori") }, singleLine = true)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(productPrice, { productPrice = it.filter(Char::isDigit) }, Modifier.weight(1f).height(54.dp), label = { Text("Harga jual") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                                        OutlinedTextField(productCost, { productCost = it.filter(Char::isDigit) }, Modifier.weight(1f).height(54.dp), label = { Text("Modal") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                                    }
                                    OutlinedTextField(productStock, { productStock = it.filter(Char::isDigit) }, Modifier.fillMaxWidth().height(54.dp), label = { Text("Stok") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(enabled = !managementBusy && productName.isNotBlank() && (productPrice.toDoubleOrNull() ?: 0.0) > 0, onClick = {
                                            val token = session?.accessToken ?: return@Button
                                            managementBusy = true
                                            scope.launch {
                                                try {
                                                    val item = org.json.JSONObject().put("name", productName.trim()).put("cat", productCategory.trim()).put("price", productPrice.toDouble()).put("cost", productCost.toDoubleOrNull() ?: 0.0).put("stock", productStock.toIntOrNull() ?: 0)
                                                    val id = editingProductId
                                                    if (id == null) api.addProduct(token, item.put("id", java.util.UUID.randomUUID().toString()).put("deleted", false))
                                                    else api.updateProduct(token, id, item)
                                                    products = api.products(token)
                                                    productName = ""; productCategory = ""; productPrice = ""; productCost = ""; productStock = "0"; editingProductId = null; error = ""
                                                } catch (e: Exception) { error = e.message ?: "Menu gagal disimpan." }
                                                finally { managementBusy = false }
                                            }
                                        }, colors = ButtonDefaults.buttonColors(containerColor = Terracotta)) { Text(if (managementBusy) "Menyimpan…" else "Simpan menu") }
                                        if (editingProductId != null) OutlinedButton(onClick = { editingProductId = null; productName = ""; productCategory = ""; productPrice = ""; productCost = ""; productStock = "0" }) { Text("Batal") }
                                    }
                                }
                            }
                        } else Text("Akun Kasir hanya dapat melihat daftar menu.", color = Muted, fontSize = 12.sp)
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            items(products, key = { it.id }) { p ->
                                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(p.name, color = Ink, fontWeight = FontWeight.SemiBold)
                                            Text("${p.category} · Rp ${p.price.toLong()} · Stok ${p.stock}", color = Muted, fontSize = 11.sp)
                                        }
                                        if (owner) {
                                            TextButton(onClick = { editingProductId = p.id; productName = p.name; productCategory = p.category; productPrice = p.price.toLong().toString(); productCost = p.cost.toLong().toString(); productStock = p.stock.toString() }) { Text("Ubah", color = Terracotta) }
                                            TextButton(enabled = !managementBusy, onClick = {
                                                val token = session?.accessToken ?: return@TextButton
                                                managementBusy = true
                                                scope.launch {
                                                    try { api.deleteProduct(token, p.id); products = api.products(token); error = "" }
                                                    catch (e: Exception) { error = e.message ?: "Menu gagal dihapus." }
                                                    finally { managementBusy = false }
                                                }
                                            }) { Text("Hapus", color = Color(0xFFB3261E)) }
                                        }
                                    }
                                }
                            }
                        }
                        }
                        if (manageTab == "Kategori" && owner) {
                            Text("Kategori menu", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            val categoryCounts = products.groupingBy { it.category.ifBlank { "Tanpa kategori" } }.eachCount().toList().sortedBy { it.first.lowercase() }
                            if (categoryCounts.isEmpty()) Text("Belum ada kategori.", color = Muted, fontSize = 12.sp)
                            else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                items(categoryCounts) { entry ->
                                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(entry.first, color = Ink, fontWeight = FontWeight.SemiBold)
                                            Text("${entry.second} menu", color = Muted, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                            Text("Kategori diatur melalui kolom Kategori saat menambah atau mengubah menu.", color = Muted, fontSize = 11.sp)
                        } else if (manageTab == "Harga" && owner) {
                            Text("Harga & margin", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                items(products, key = { it.id }) { product ->
                                    val marginPercent = if (product.price > 0) ((product.price - product.cost) / product.price * 100).toInt() else 0
                                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(product.name, color = Ink, fontWeight = FontWeight.SemiBold)
                                                Text("Jual Rp ${product.price.toLong()} · Modal Rp ${product.cost.toLong()}", color = Muted, fontSize = 11.sp)
                                                Text("Margin ${marginPercent}%", color = Terracotta, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            TextButton(onClick = { manageTab = "Menu"; editingProductId = product.id; productName = product.name; productCategory = product.category; productPrice = product.price.toLong().toString(); productCost = product.cost.toLong().toString(); productStock = product.stock.toString() }) { Text("Ubah", color = Terracotta) }
                                        }
                                    }
                                }
                            }
                        }
                        if (manageTab == "Akun kasir" && owner) {
                            Spacer(Modifier.height(12.dp))
                            Text("Kelola akun kasir", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Tambah akun kasir", color = Terracotta, fontWeight = FontWeight.SemiBold)
                                    OutlinedTextField(staffName, { staffName = it }, Modifier.fillMaxWidth().height(54.dp), label = { Text("Nama kasir") }, singleLine = true)
                                    OutlinedTextField(staffEmail, { staffEmail = it }, Modifier.fillMaxWidth().height(54.dp), label = { Text("Email kasir") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                                    OutlinedTextField(staffPassword, { staffPassword = it }, Modifier.fillMaxWidth().height(54.dp), label = { Text("Password awal (min. 6 karakter)") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                                    Button(enabled = !managementBusy && staffName.isNotBlank() && staffEmail.contains("@") && staffPassword.length >= 6, onClick = {
                                        val token = session?.accessToken ?: return@Button
                                        managementBusy = true
                                        scope.launch {
                                            try {
                                                val payload = org.json.JSONObject().put("aksi", "tambah").put("nama", staffName.trim()).put("email", staffEmail.trim()).put("password", staffPassword)
                                                api.cashierAction(token, payload)
                                                staffName = ""; staffEmail = ""; staffPassword = ""
                                                staffRows = org.json.JSONArray(api.cashierAction(token, org.json.JSONObject().put("aksi", "daftar")))
                                                error = ""
                                            } catch (e: Exception) { error = e.message ?: "Akun kasir gagal dibuat." }
                                            finally { managementBusy = false }
                                        }
                                    }, colors = ButtonDefaults.buttonColors(containerColor = Terracotta)) { Text("Buat akun kasir") }
                                }
                            }
                            Text("Pengguna terdaftar", color = Ink, fontWeight = FontWeight.SemiBold)
                            if (managementBusy) CircularProgressIndicator(color = Terracotta)
                            else LazyColumn(Modifier.height(180.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items((0 until staffRows.length()).map { staffRows.getJSONObject(it) }) { staff ->
                                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(staff.optString("nama"), color = Ink, fontWeight = FontWeight.SemiBold)
                                                Text("${staff.optString("email")} · ${staff.optString("peran")}", color = Muted, fontSize = 11.sp)
                                            }
                                            if (staff.optString("peran") == "Kasir") TextButton(onClick = {
                                                val token = session?.accessToken ?: return@TextButton
                                                managementBusy = true
                                                scope.launch {
                                                    try {
                                                        api.cashierAction(token, org.json.JSONObject().put("aksi", "hapus").put("id", staff.optString("id")))
                                                        staffRows = org.json.JSONArray(api.cashierAction(token, org.json.JSONObject().put("aksi", "daftar")))
                                                        error = ""
                                                    } catch (e: Exception) { error = e.message ?: "Akun gagal dihapus." }
                                                    finally { managementBusy = false }
                                                }
                                            }) { Text("Hapus", color = Color(0xFFB3261E)) }
                                        }
                                    }
                                }
                            }
                        }
                        if (error.isNotBlank()) Text(error, color = Color(0xFFB3261E), fontSize = 12.sp)
                    } else {
                        Text("Profil", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(16.dp)) {
                                Text(profile!!.nama, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                Text(profile!!.peran, color = Muted, fontSize = 13.sp)
                                Spacer(Modifier.height(12.dp))
                                Text("Status koneksi: " + if (hasValidatedInternet(context)) "Online" else "Offline", color = Muted, fontSize = 12.sp)
                                Text("Transaksi menunggu sinkronisasi: $queuedSalesCount", color = Muted, fontSize = 12.sp)
                            }
                        }
                        Button(onClick = { session = null; profile = null; password = ""; selectedTab = 0 }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Terracotta)) {
                            Text("Keluar dari akun")
                        }
                    }
                }
            }
        }
    }
    pendingRestore?.let { backup ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Pulihkan backup lokal?") },
            text = { Text("Data order tertunda, antrean sinkronisasi, dan riwayat shift di perangkat ini akan diganti dengan isi backup. Menu dan transaksi di cloud tidak diubah.") },
            confirmButton = {
                TextButton(onClick = {
                    val data = pendingRestore ?: return@TextButton
                    pendingRestore = null
                    scope.launch {
                        try {
                            localDao.clearPendingOrders()
                            localDao.clearOutbox()
                            localDao.clearShifts()
                            val pending = data.getJSONArray("pendingOrders")
                            for (i in 0 until pending.length()) {
                                val o = pending.getJSONObject(i)
                                localDao.savePendingOrder(PendingOrderEntity(o.getString("id"), o.optString("customer"), o.getString("itemsJson"), o.optDouble("total"), o.optLong("updatedAt")))
                            }
                            val queue = data.getJSONArray("syncOutbox")
                            for (i in 0 until queue.length()) {
                                val o = queue.getJSONObject(i)
                                localDao.enqueue(SyncOutboxEntity(o.getString("id"), o.getString("payloadJson"), o.optLong("createdAt"), o.optInt("retryCount"), if (o.isNull("lastError")) null else o.optString("lastError")))
                            }
                            val shifts = data.getJSONArray("shifts")
                            for (i in 0 until shifts.length()) {
                                val o = shifts.getJSONObject(i)
                                localDao.saveShift(ShiftEntity(o.getString("id"), o.optLong("startedAt"), o.optDouble("openingCash"), if (o.isNull("endedAt")) null else o.optLong("endedAt"), if (o.isNull("closingCash")) null else o.optDouble("closingCash"), if (o.isNull("difference")) null else o.optDouble("difference")))
                            }
                            pendingOrders = localDao.getPendingOrders()
                            shiftHistory = localDao.getShifts()
                            activeShift = shiftHistory.firstOrNull { it.endedAt == null }
                            queuedSalesCount = localDao.getOutbox().size
                            error = "Backup lokal berhasil dipulihkan."
                        } catch (e: Exception) { error = e.message ?: "Pemulihan backup gagal." }
                    }
                }) { Text("Pulihkan") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Batal") } }
        )
    }
}

private suspend fun syncPendingSales(api: AristaApi, dao: AristaLocalDao, token: String) {
    val queued = dao.getOutbox()
    for (item in queued) {
        try {
            if (!api.saleExists(token, item.id)) {
                api.saveSale(token, org.json.JSONObject(item.payloadJson))
            }
            dao.removeFromOutbox(item.id)
        } catch (e: Exception) {
            dao.markRetry(item.id, e.message ?: "Sinkronisasi gagal")
            break
        }
    }
}
private fun hasValidatedInternet(context: Context): Boolean {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
