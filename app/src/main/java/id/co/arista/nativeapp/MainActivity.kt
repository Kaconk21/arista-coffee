package id.co.arista.nativeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
    var cart by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var pendingOrders by remember { mutableStateOf<List<PendingOrderEntity>>(emptyList()) }
    var editingPendingId by remember { mutableStateOf<String?>(null) }
    var savingPending by remember { mutableStateOf(false) }
    var paymentBusy by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf("Tunai") }
    var cashReceived by remember { mutableStateOf("") }
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
    var manageAccounts by remember { mutableStateOf(false) }

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
                            value = email, onValueChange = { email = it; error = "" },
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
                                                reportSales = api.sales(loggedIn.accessToken)
                                                pendingOrders = localDao.getPendingOrders()
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
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("Gunakan akun ARISTA yang sudah terdaftar.", color = Muted, fontSize = 12.sp)
            }
        } else {
            val labels = listOf("Beranda", "Kasir", "Laporan", "Kelola")
            val icons = listOf(Icons.Default.Coffee, Icons.Default.PointOfSale, Icons.Default.Assessment, Icons.Default.Settings)
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
                        Text("Pilih menu", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        when {
                            productsLoading -> CircularProgressIndicator(color = Terracotta)
                            products.isEmpty() -> Text("Belum ada menu aktif atau gagal memuat menu.", color = Muted, fontSize = 14.sp)
                            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                items(products, key = { it.id }) { product ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
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
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Rp ${product.price.toLong()}", color = Terracotta, fontWeight = FontWeight.Bold)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    OutlinedButton(onClick = {
                                                        val next = (cart[product.id] ?: 0) - 1
                                                        cart = if (next <= 0) cart - product.id else cart + (product.id to next)
                                                    }, enabled = (cart[product.id] ?: 0) > 0) { Text("−") }
                                                    Text("  ${cart[product.id] ?: 0}  ", color = Ink)
                                                    OutlinedButton(onClick = {
                                                        cart = cart + (product.id to ((cart[product.id] ?: 0) + 1))
                                                    }) { Text("+") }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        val chosen = products.filter { (cart[it.id] ?: 0) > 0 }
                        if (chosen.isNotEmpty()) {
                            val total = chosen.sumOf { it.price * (cart[it.id] ?: 0) }
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
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("Tunai", "QRIS").forEach { method ->
                                            if (paymentMethod == method) Button(onClick = { paymentMethod = method }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Terracotta)) { Text(method) }
                                            else OutlinedButton(onClick = { paymentMethod = method }, modifier = Modifier.weight(1f)) { Text(method) }
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
                                    OutlinedButton(enabled = !savingPending, modifier = Modifier.fillMaxWidth(), onClick = {
                                        savingPending = true
                                        scope.launch {
                                            try {
                                                val rows = org.json.JSONArray()
                                                chosen.forEach { p -> rows.put(org.json.JSONObject().put("id", p.id).put("qty", cart[p.id] ?: 0)) }
                                                localDao.savePendingOrder(PendingOrderEntity(editingPendingId ?: java.util.UUID.randomUUID().toString(), "Umum", rows.toString(), total, System.currentTimeMillis()))
                                                pendingOrders = localDao.getPendingOrders(); editingPendingId = null; cart = emptyMap(); cashReceived = ""; error = ""
                                            } catch (e: Exception) { error = e.message ?: "Pesanan gagal disimpan." }
                                            finally { savingPending = false }
                                        }
                                    }) { Text(if (savingPending) "Menyimpan pesanan…" else "Simpan pesanan") }
                                    Button(
                                        enabled = !paymentBusy && (paymentMethod != "Tunai" || (cashReceived.toLongOrNull() ?: 0L) >= total.toLong()),
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
                                                        .put("method", paymentMethod).put("paid", if (paymentMethod == "Tunai") (cashReceived.toLongOrNull() ?: 0L) else total).put("change_amount", if (paymentMethod == "Tunai") ((cashReceived.toLongOrNull() ?: 0L) - total.toLong()).coerceAtLeast(0L) else 0L)
                                                        .put("cashier_name", profile?.nama ?: "")
                                                        .put("customer", "Umum").put("diskon", 0)
                                                    api.saveSale(active.accessToken, sale)
                                                    cart = emptyMap()
                                                    editingPendingId?.let { localDao.deletePendingOrder(it) }
                                                    editingPendingId = null
                                                    pendingOrders = localDao.getPendingOrders()
                                                    cashReceived = ""
                                                    products = api.products(active.accessToken)
                                                    error = ""
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
                        if (pendingOrders.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text("Pesanan tersimpan di perangkat", color = Ink, fontWeight = FontWeight.SemiBold)
                            LazyColumn(Modifier.height(150.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(pendingOrders, key = { it.id }) { order ->
                                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(order.customer, color = Ink, fontWeight = FontWeight.SemiBold)
                                                Text("Rp ${order.total.toLong()}", color = Muted, fontSize = 11.sp)
                                            }
                                            TextButton(onClick = {
                                                val ids = org.json.JSONArray(order.itemsJson)
                                                val restored = mutableMapOf<String, Int>()
                                                for (i in 0 until ids.length()) { val row = ids.getJSONObject(i); restored[row.optString("id")] = row.optInt("qty") }
                                                cart = restored
                                                editingPendingId = order.id
                                            }) { Text("Buka", color = Terracotta) }
                                            TextButton(onClick = { scope.launch { localDao.deletePendingOrder(order.id); pendingOrders = localDao.getPendingOrders() } }) { Text("Hapus", color = Color(0xFFB3261E)) }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (selectedTab == 2) {
                        val owner = profile?.peran.equals("Pemilik", true)
                        val omzet = (0 until reportSales.length()).sumOf { i ->
                            val row = reportSales.optJSONObject(i) ?: org.json.JSONObject()
                            if (row.optBoolean("batal")) 0.0 else row.optDouble("total")
                        }
                        val modal = (0 until reportSales.length()).sumOf { i ->
                            val sale = reportSales.optJSONObject(i) ?: org.json.JSONObject()
                            if (sale.optBoolean("batal")) 0.0 else {
                                val rows = sale.optJSONArray("items") ?: org.json.JSONArray()
                                (0 until rows.length()).sumOf { k ->
                                    val item = rows.optJSONObject(k) ?: org.json.JSONObject()
                                    item.optDouble("cost") * item.optDouble("qty")
                                }
                            }
                        }
                        val expenseTotal = (0 until reportExpenses.length()).sumOf { i -> reportExpenses.optJSONObject(i)?.optDouble("total") ?: 0.0 }
                        val jumlah = (0 until reportSales.length()).count { i ->
                            !(reportSales.optJSONObject(i)?.optBoolean("batal") ?: false)
                        }
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
                                items((0 until minOf(reportSales.length(), 50)).map { reportSales.getJSONObject(it) }) { sale ->
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
                    } else {
                        val owner = profile?.peran.equals("Pemilik", ignoreCase = true)
                        if (owner) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { manageAccounts = false }, colors = ButtonDefaults.buttonColors(containerColor = if (!manageAccounts) Terracotta else Color.LightGray), modifier = Modifier.weight(1f)) { Text("Menu") }
                                Button(onClick = { manageAccounts = true }, colors = ButtonDefaults.buttonColors(containerColor = if (manageAccounts) Terracotta else Color.LightGray), modifier = Modifier.weight(1f)) { Text("Akun kasir") }
                            }
                        }
                        if (!manageAccounts || !owner) {
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
                        if (manageAccounts && owner) {
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
                    }
                }
            }
        }
    }
}
