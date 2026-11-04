package id.co.arista.nativeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Cream = Color(0xFFF7F1E8)
private val Terracotta = Color(0xFFB5583A)
private val Ink = Color(0xFF2B211A)
private val Muted = Color(0xFF7A6B5D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AristaApp()
            }
        }
    }
}

@Composable
private fun AristaApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Beranda", "Kasir", "Laporan", "Kelola")
    val icons = listOf(Icons.Default.Coffee, Icons.Default.PointOfSale, Icons.Default.Assessment, Icons.Default.Settings)

    Scaffold(
        containerColor = Cream,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                tabs.forEachIndexed { index, label ->
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
        Surface(
            modifier = Modifier.fillMaxSize().padding(padding),
            color = Cream
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Text("ARISTA", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Text("COFFEE & ROASTERY", color = Muted, fontSize = 11.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(22.dp))
                when (selectedTab) {
                    0 -> PlaceholderPage("Beranda", "Ringkasan usaha ARISTA akan dibangun di sini.")
                    1 -> PlaceholderPage("Kasir", "Alur menu, keranjang, pembayaran, dan order akan dibangun di sini.")
                    2 -> PlaceholderPage("Laporan", "Laporan penjualan dan pengeluaran akan dibangun di sini.")
                    else -> PlaceholderPage("Kelola", "Menu, pengguna, profil usaha, dan pengaturan akan dibangun di sini.")
                }
            }
        }
    }
}

@Composable
private fun PlaceholderPage(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = Terracotta, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(description, color = Muted, fontSize = 14.sp)
        }
    }
}
