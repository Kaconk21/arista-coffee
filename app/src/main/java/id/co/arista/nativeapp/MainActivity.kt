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
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var session by remember { mutableStateOf<AristaSession?>(null) }
    var profile by remember { mutableStateOf<AristaProfile?>(null) }
    var products by remember { mutableStateOf<List<AristaProduct>>(emptyList()) }
    var productsLoading by remember { mutableStateOf(false) }
    var reportSales by remember { mutableStateOf(org.json.JSONArray()) }
    var reportsLoading by remember { mutableStateOf(false) }
    var cart by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var paymentBusy by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf("Tunai") }
    var cashReceived by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

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
                                            try { products = api.products(loggedIn.accessToken) } finally { productsLoading = false }
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
                                    if (index == 2 && session != null) {
                                        reportsLoading = true
                                        scope.launch {
                                            try { reportSales = api.sales(session!!.accessToken); error = "" }
                                            catch (e: Exception) { error = e.message ?: "Laporan gagal dimuat." }
                                            finally { reportsLoading = false }
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
                    } else if (selectedTab == 2) {
                        val omzet = (0 until reportSales.length()).sumOf { i ->
                            val row = reportSales.optJSONObject(i) ?: org.json.JSONObject()
                            if (row.optBoolean("batal")) 0.0 else row.optDouble("total")
                        }
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
                                    Text("Rp \${omzet.toLong()}", color = Terracotta, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                                    Text("Jumlah transaksi: \${jumlah}", color = Ink)
                                    Text("Data maksimal 200 transaksi terakhir", color = Muted, fontSize = 11.sp)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Transaksi terbaru", color = Ink, fontWeight = FontWeight.SemiBold)
                            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items((0 until minOf(reportSales.length(), 50)).map { reportSales.getJSONObject(it) }) { sale ->
                                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                        Column(Modifier.padding(12.dp)) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(sale.optString("customer", "Umum"), color = Ink, fontWeight = FontWeight.SemiBold)
                                                Text("Rp \${sale.optDouble("total").toLong()}", color = Terracotta, fontWeight = FontWeight.Bold)
                                            }
                                            Text("\${sale.optString("sale_date")} · \${sale.optString("sale_time")} · \${sale.optString("method")}", color = Muted, fontSize = 12.sp)
                                            if (sale.optBoolean("batal")) Text("DIBATALKAN", color = Color(0xFFB3261E), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                        if (error.isNotBlank()) Text(error, color = Color(0xFFB3261E), fontSize = 12.sp)
                    } else {
                        val descriptions = listOf(
                            "Ringkasan usaha ARISTA akan ditampilkan di sini.",
                            "",
                            "Modul laporan penjualan, laba, dan pengeluaran sedang dibangun.",
                            "Pengelolaan menu, akun kasir, dan pengaturan sedang dibangun."
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(Modifier.padding(18.dp)) {
                                Text(labels[selectedTab], color = Terracotta, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(8.dp))
                                Text(descriptions[selectedTab], color = Muted, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
