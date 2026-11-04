package id.co.arista.nativeapp

import android.os.Bundle
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
    var reportsLoading by remember { mutableStateOf(false) }
    var reportRange by remember { mutableStateOf("Harian") }
    var cart by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var pendingOrders by remember { mutableStateOf<List<PendingOrderEntity>>(emptyList()) }
    var editingPendingId by remember { mutableStateOf<String?>(null) }
    var savingPending by remember { mutableStateOf(false) }
    var queuedSalesCount by remember { mutableIntStateOf(0) }
    var syncingSales by remember { mutableStateOf(false) }
    var activeShift by remember { mutableStateOf<ShiftEntity?>(null) }
    var openingCashInput by remember { mutableStateOf("") }
    var closingCashInput by remember { mutableStateOf("") }
    var paymentBusy by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf("Tunai") }
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

    Surface(modifier = Modifier.fillMaxSize(), color = Cream) {
        if (session == null || profile == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 26.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ARISTA", color = Ink, fontSize = 31.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                Text("COFFEE & ROASTERY", color = Muted, fontSize = 11.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(34.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Masuk ke ARISTA", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = email, onValueChange = { email = it; error = ""; recoveryMessage = "" },
                            modifier = Modifier.fillMaxWidth(), label = { Text("Email") },
                            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = password, onValueChange = { password = it; error = "" },
                            modifier = Modifier.fillMaxWidth(), label = { Text("Password") },
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
                        Spacer(Modifier.height(18.dp))
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
                                                activeShift = localDao.getShifts().firstOrNull { it.endedAt == null }
                                                syncPendingSales(api, localDao, loggedIn.accessToken)
                                                queuedSalesCount = localDao.getOutbox().size
                                                reportSales = api.sales(loggedIn.accessToken)
                                                if (userProfile.peran.equals("Pemilik", true)) reportExpenses = api.expenses(loggedIn.accessToken)
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
                                            try { reportSales = api.sales(session!!.accessToken); if (profile?.peran.equals("Pemilik", true)) reportExpenses = api.expenses(session!!.accessToken); error = "" }
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
                Column(Modifier.fillMaxSize().padding(padding).padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ARISTA", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                            Text("COFFEE & ROASTERY", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                        }
                        TextButton(onClick = { session = null; profile = null; password = "" }) {
                            Text("Keluar", color = Terracotta)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("Halo, ${profile!!.nama}", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                    Text("Peran: ${profile!!.peran}", color = Muted, fontSize = 13.sp)
                    Spacer(Modifier.height(18.dp))
                    if (selectedTab == 1) {
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(12.dp)) {
                                if (activeShift == null) {
                                    Text("Shift belum dibuka", color = Ink, fontWeight = FontWeight.SemiBold)
                                    OutlinedTextField(openingCashInput, { openingCashInput = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Modal awal kas (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                                    Button(enabled = openingCashInput.isNotBlank(), onClick = {
                                        val amount = openingCashInput.toDoubleOrNull() ?: 0.0
                                        val shift = ShiftEntity(java.util.UUID.randomUUID().toString(), System.currentTimeMillis(), amount)
                                        scope.launch { localDao.saveShift(shift); activeShift = shift; openingCashInput = ""; closingCashInput = "" }
                                    }, colors = ButtonDefaults.buttonColors(containerColor = Terracotta)) { Text("Buka shift") }
                                } else {
                                    Text("Shift aktif", color = Terracotta, fontWeight = FontWeight.SemiBold)
                                    Text("Modal awal: Rp ${activeShift!!.openingCash.toLong()}", color = Ink, fontSize = 12.sp)
                                    OutlinedTextField(closingCashInput, { closingCashInput = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Uang kas akhir (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                                    Button(enabled = closingCashInput.isNotBlank(), onClick = {
                                        val current = activeShift ?: return@Button
                                        val endAmount = closingCashInput.toDoubleOrNull() ?: 0.0
                                        val closed = current.copy(endedAt = System.currentTimeMillis(), closingCash = endAmount, difference = endAmount - current.openingCash)
                                        scope.launch { localDao.saveShift(closed); activeShift = null; closingCashInput = "" }
                                    }, colors = ButtonDefaults.buttonColors(containerColor = Terracotta)) { Text("Tutup shift") }
                                }
                            }
                        }
                        Text("Pilih menu", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        when {
                            productsLoading -> CircularProgressIndicator(color = Terracotta)
                            products.isEmpty() -> Text("Belum ada menu aktif atau gagal memuat menu.", color = Muted, fontSize = 14.sp)
                            else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                items(products, key = { it.id }) { product ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            if (activeShift == null) error = "Buka kasir dulu untuk mulai menjual."
                                            else { cart = cart + (product.id to ((cart[product.id] ?: 0) + 1)); error = "" }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(product.name, color = Ink, fontWeight = FontWeight.SemiBold)
                                                Text(product.category, color = Muted, fontSize = 12.sp)
                                                Text("Ketuk untuk tambah", color = Muted, fontSize = 11.sp)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Rp ${product.price.toLong()}", color = Terracotta, fontWeight = FontWeight.Bold)
                                                val quantity = cart[product.id] ?: 0
                                                if (quantity > 0) {
                                                    Text("Di keranjang: $quantity", color = Terracotta, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                                    TextButton(onClick = {
                                                        val next = quantity - 1
                                                        cart = if (next <= 0) cart - product.id else cart + (product.id to next)
                                                    }) { Text("Kurangi", color = Muted) }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        val chosen = products.filter { (cart[it.id] ?: 0) > 0 }
                        if (chosen.isNotEmpty()) {
                            val subtotal = chosen.sumOf { it.price * (cart[it.id] ?: 0) }
                            val discountValue = discountInput.toDoubleOrNull() ?: 0.0
                            val discountAmount = if (discountType == "%") (subtotal * discountValue.coerceIn(0.0, 100.0) / 100.0).toLong().toDouble() else discountValue.coerceAtLeast(0.0).coerceAtMost(subtotal)
                            val total = (subtotal - discountAmount).coerceAtLeast(0.0)
                            Spacer(Modifier.height(10.dp))
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.padding(14.dp)) {
                                    Text("Keranjang · ${cart.values.sum()} item", color = Ink, fontWeight = FontWeight.Bold)
                                    chosen.forEach { p ->
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("${p.name} × ${cart[p.id]}", color = Muted, fontSize = 12.sp)
                                            Text("Rp ${(p.price * (cart[p.id] ?: 0)).toLong()}", color = Ink, fontSize = 12.sp)
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text("Total Rp ${total.toLong()}", color = Terracotta, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(customerName, { customerName = it }, Modifier.fillMaxWidth(), label = { Text("Nama customer (kosong = Umum)") }, singleLine = true)
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(discountInput, { discountInput = it.filter { ch -> ch.isDigit() || ch == '.' } }, Modifier.weight(1f), label = { Text("Diskon") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            listOf("Rp", "%").forEach { type ->
                                                if (discountType == type) Button(onClick = { discountType = type }, colors = ButtonDefaults.buttonColors(containerColor = Terracotta)) { Text(type) }
                                                else OutlinedButton(onClick = { discountType = type }) { Text(type) }
                                            }
                                        }
                                    }
                                    Text("Subtotal Rp ${subtotal.toLong()} · Potongan Rp ${discountAmount.toLong()}", color = Muted, fontSize = 12.sp)
                                    Text("Total setelah diskon Rp ${total.toLong()}", color = Terracotta, fontWeight = FontWeight.Bold)
                                    listOf(listOf("Tunai", "QRIS", "Debit/Kredit"), listOf("Transfer", "Piutang")).forEach { methodRow ->
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            methodRow.forEach { method ->
                                            if (paymentMethod == method) Button(onClick = { paymentMethod = method }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Terracotta)) { Text(method) }
                                            else OutlinedButton(onClick = { paymentMethod = method }, modifier = Modifier.weight(1f)) { Text(method, fontSize = 11.sp) }
                                            }
                                        }
                                    }
                                    if (paymentMethod == "Tunai") {
                                        OutlinedTextField(value = cashReceived, onValueChange = { cashReceived = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth(), label = { Text("Uang diterima (Rp)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf(total.toLong(), 20000L, 50000L).distinct().forEach { amount ->
                                                TextButton(onClick = { cashReceived = amount.toString() }) { Text(if (amount == total.toLong()) "Uang pas" else "Rp ${amount}") }
                                            }
                                        }
                                        val received = cashReceived.toLongOrNull() ?: 0L
                                        Text("Kembalian Rp ${(received - total.toLong()).coerceAtLeast(0L)}", color = Muted, fontSize = 13.sp)
                                    }
                                    OutlinedButton(enabled = !savingPending && activeShift != null && customerName.isNotBlank(), modifier = Modifier.fillMaxWidth(), onClick = {
                                        savingPending = true
                                        scope.launch {
                                            try {
                                                val rows = org.json.JSONArray()
                                                chosen.forEach { p -> rows.put(org.json.JSONObject().put("id", p.id).put("qty", cart[p.id] ?: 0)) }
                                                if (customerName.isBlank()) { error = "Isi nama customer dulu untuk menyimpan ORDERAN."; return@launch }
                                                localDao.savePendingOrder(PendingOrderEntity(editingPendingId ?: java.util.UUID.randomUUID().toString(), customerName.trim(), rows.toString(), subtotal, System.currentTimeMillis()))
                                                pendingOrders = localDao.getPendingOrders(); editingPendingId = null; cart = emptyMap(); cashReceived = ""; customerName = ""; discountInput = ""; error = ""
                                            } catch (e: Exception) { error = e.message ?: "Pesanan gagal disimpan." }
                                            finally { savingPending = false }
                                        }
                                    }) { Text(if (savingPending) "Menyimpan pesanan…" else "Simpan pesanan") }
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
                                                        rows.put(org.json.JSONObject().put("id", p.id).put("name", p.name)
                                                            .put("cat", p.category).put("price", p.price).put("cost", p.cost)
                                                            .put("qty", cart[p.id] ?: 0))
                                                    }
                                                    val now = java.time.LocalDateTime.now()
                                                    val sale = org.json.JSONObject()
                                                        .put("id", java.util.UUID.randomUUID().toString())
                                                        .put("sale_date", now.toLocalDate().toString())
                                                        .put("sale_time", now.toLocalTime().toString().take(5))
                                                        .put("items", rows).put("total", total)
                                                        .put("method", paymentMethod).put("paid", if (paymentMethod == "Piutang") 0L else if (paymentMethod == "Tunai") (cashReceived.toLongOrNull() ?: 0L) else total.toLong()).put("change_amount", if (paymentMethod == "Tunai") ((cashReceived.toLongOrNull() ?: 0L) - total.toLong()).coerceAtLeast(0L) else 0L)
                                                        .put("cashier_name", profile?.nama ?: "")
                                                        .put("customer", customerName.trim().ifBlank { "Umum" }).put("diskon", discountAmount.toLong())
                                                    val online = hasValidatedInternet(context)
                                                    var queuedAfterNetworkError = false
                                                    if (online) {
                                                        try {
                                                            try {
                                                                api.saveSale(active.accessToken, sale)
                                                            } catch (e: AristaHttpException) {
                                                                if (e.statusCode != 401) throw e
                                                                val refreshed = api.refreshSession(active.refreshToken)
                                                                session = refreshed
                                                                api.saveSale(refreshed.accessToken, sale)
                                                            }
                                                        } catch (e: AristaHttpException) {
                                                            throw e
                                                        } catch (e: java.io.IOException) {
                                                            // Keep the same sale UUID: sync checks the server before retrying,
                                                            // preventing a duplicate if the server committed but the reply was lost.
                                                            localDao.enqueue(SyncOutboxEntity(sale.getString("id"), sale.toString(), System.currentTimeMillis()))
                                                            queuedSalesCount = localDao.getOutbox().size
                                                            queuedAfterNetworkError = true
                                                        }
                                                    } else {
                                                        localDao.enqueue(SyncOutboxEntity(sale.getString("id"), sale.toString(), System.currentTimeMillis()))
                                                        queuedSalesCount = localDao.getOutbox().size
                                                    }
                                                    cart = emptyMap()
                                                    editingPendingId?.let { localDao.deletePendingOrder(it) }
                                                    editingPendingId = null
                                                    pendingOrders = localDao.getPendingOrders()
                                                    cashReceived = ""; customerName = ""; discountInput = ""; discountType = "Rp"
                                                    if (online && !queuedAfterNetworkError) {
                                                        try {
                                                            products = api.products(active.accessToken)
                                                            reportSales = api.sales(active.accessToken)
                                                        } catch (_: Exception) {
                                                            // The sale is already confirmed; a refresh failure must not invite duplicate payment.
                                                        }
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
                                    if (error.isNotBlank()) Text(error, color = Color(0xFFB3261E), fontSize = 12.sp)
                                }
                            }
                        }
                        if (queuedSalesCount > 0) {
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
                        Spacer(Modifier.height(12.dp))
                        Text("ORDERAN", color = Ink, fontWeight = FontWeight.SemiBold)
                        Text("Pesanan yang belum dibayar. Stok belum berkurang sampai pembayaran selesai.", color = Muted, fontSize = 11.sp)
                        if (pendingOrders.isEmpty()) {
                            Text("Belum ada pesanan tertunda.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp))
                        } else {
                            LazyColumn(Modifier.height(180.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(pendingOrders, key = { it.id }) { order ->
                                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(order.customer, color = Ink, fontWeight = FontWeight.SemiBold)
                                                Text("${org.json.JSONArray(order.itemsJson).length()} jenis menu · Rp ${order.total.toLong()}", color = Muted, fontSize = 11.sp)
                                            }
                                            TextButton(onClick = {
                                                val ids = org.json.JSONArray(order.itemsJson)
                                                val restored = mutableMapOf<String, Int>()
                                                for (i in 0 until ids.length()) { val row = ids.getJSONObject(i); restored[row.optString("id")] = row.optInt("qty") }
                                                cart = restored
                                                customerName = order.customer
                                                editingPendingId = order.id
                                            }) { Text("Buka & bayar", color = Terracotta) }
                                            TextButton(onClick = { scope.launch { localDao.deletePendingOrder(order.id); pendingOrders = localDao.getPendingOrders() } }) { Text("Hapus", color = Color(0xFFB3261E)) }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (selectedTab == 2) {
                        val owner = profile?.peran.equals("Pemilik", true)
                        val todayDate = java.time.LocalDate.now()
                        val periodStart = when (reportRange) {
                            "Mingguan" -> todayDate.with(java.time.DayOfWeek.MONDAY)
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
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Laporan penjualan", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = {
                                reportsLoading = true
                                scope.launch {
                                    try { reportSales = api.sales(session!!.accessToken); error = "" }
                                    catch (e: Exception) { error = e.message ?: "Gagal memuat laporan." }
                                    finally { reportsLoading = false }
                                }
                            }) { Text("Muat ulang", color = Terracotta) }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Harian", "Mingguan", "Bulanan").forEach { range ->
                                if (reportRange == range) Button(onClick = { reportRange = range }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Terracotta)) { Text(range, fontSize = 11.sp) }
                                else OutlinedButton(onClick = { reportRange = range }, modifier = Modifier.weight(1f)) { Text(range, fontSize = 11.sp) }
                            }
                        }
                        if (reportsLoading) CircularProgressIndicator(color = Terracotta)
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
                             if (owner) {
                                 Text("Catat pengeluaran", color = Ink, fontWeight = FontWeight.SemiBold)
                                 Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                     Column(Modifier.padding(12.dp)) {
                                         OutlinedTextField(expenseName, { expenseName = it }, Modifier.fillMaxWidth(), label = { Text("Nama pengeluaran") }, singleLine = true)
                                         Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                             OutlinedTextField(expenseQty, { expenseQty = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("Jumlah") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                                             OutlinedTextField(expensePrice, { expensePrice = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("Harga satuan") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                                         }
                                         OutlinedTextField(expenseCategory, { expenseCategory = it }, Modifier.fillMaxWidth(), label = { Text("Kategori (opsional)") }, singleLine = true)
                                         OutlinedTextField(expenseNote, { expenseNote = it }, Modifier.fillMaxWidth(), label = { Text("Catatan (opsional)") }, singleLine = true)
                                         val qtyValue = expenseQty.toDoubleOrNull() ?: 0.0
                                         val priceValue = expensePrice.toDoubleOrNull() ?: 0.0
                                         Text("Total: Rp ${(qtyValue * priceValue).toLong()}", color = Terracotta, fontWeight = FontWeight.SemiBold)
                                         Button(enabled = !managementBusy && expenseName.isNotBlank() && qtyValue > 0 && priceValue > 0, onClick = {
                                             val token = session?.accessToken ?: return@Button
                                             managementBusy = true
                                             scope.launch {
                                                 try {
                                                     val q = expenseQty.toDouble()
                                                     val p = expensePrice.toDouble()
                                                     val payload = org.json.JSONObject().put("tanggal", java.time.LocalDate.now().toString()).put("nama", expenseName.trim()).put("kategori", expenseCategory.trim().ifBlank { org.json.JSONObject.NULL }).put("qty", q).put("harga", p).put("total", q * p).put("catatan", expenseNote.trim().ifBlank { org.json.JSONObject.NULL })
                                                     api.createExpense(token, payload)
                                                     reportExpenses = api.expenses(token)
                                                     expenseName = ""; expenseCategory = ""; expenseQty = "1"; expensePrice = ""; expenseNote = ""; error = ""
                                                 } catch (e: Exception) { error = e.message ?: "Pengeluaran gagal disimpan." }
                                                 finally { managementBusy = false }
                                             }
                                         }, colors = ButtonDefaults.buttonColors(containerColor = Terracotta)) { Text("Simpan pengeluaran") }
                                     }
                                 }
                             }
Text("Transaksi terbaru", color = Ink, fontWeight = FontWeight.SemiBold)
                            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(periodSales.filterNot { it.optBoolean("batal") }.take(50)) { sale ->
                                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                        Column(Modifier.padding(12.dp)) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(sale.optString("customer", "Umum"), color = Ink, fontWeight = FontWeight.SemiBold)
                                                Text("Rp ${sale.optDouble("total").toLong()}", color = Terracotta, fontWeight = FontWeight.Bold)
                                            }
                                            Text("${sale.optString("sale_date")} · ${sale.optString("sale_time")} · ${sale.optString("method")}", color = Muted, fontSize = 12.sp)
                                            if (sale.optBoolean("batal")) Text("DIBATALKAN", color = Color(0xFFB3261E), fontSize = 11.sp)
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
                                Column(Modifier.padding(14.dp)) {
                                    Text("Omzet hari ini", color = Muted, fontSize = 11.sp)
                                    Text("Rp ${todayTotal.toLong()}", color = Terracotta, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.padding(14.dp)) {
                                    Text("Transaksi", color = Muted, fontSize = 11.sp)
                                    Text("${todaySales.size}", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(14.dp)) {
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
                                    OutlinedTextField(productName, { productName = it }, Modifier.fillMaxWidth(), label = { Text("Nama menu") }, singleLine = true)
                                    OutlinedTextField(productCategory, { productCategory = it }, Modifier.fillMaxWidth(), label = { Text("Kategori") }, singleLine = true)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(productPrice, { productPrice = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("Harga jual") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                                        OutlinedTextField(productCost, { productCost = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("Modal") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                                    }
                                    OutlinedTextField(productStock, { productStock = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Stok") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
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
                                    OutlinedTextField(staffName, { staffName = it }, Modifier.fillMaxWidth(), label = { Text("Nama kasir") }, singleLine = true)
                                    OutlinedTextField(staffEmail, { staffEmail = it }, Modifier.fillMaxWidth(), label = { Text("Email kasir") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                                    OutlinedTextField(staffPassword, { staffPassword = it }, Modifier.fillMaxWidth(), label = { Text("Password awal (min. 6 karakter)") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
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
