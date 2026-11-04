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
    var cart by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
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
                                onClick = { selectedTab = index },
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
                                                OutlinedButton(onClick = {
                                                    cart = cart + (product.id to ((cart[product.id] ?: 0) + 1))
                                                }) { Text("Tambah") }
                                            }
                                        }
                                    }
                                }
                            }
                        }
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
