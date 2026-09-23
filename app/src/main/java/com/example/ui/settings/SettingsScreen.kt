package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var furiganaDefault by remember { mutableStateOf(true) }
    var amoledDefault by remember { mutableStateOf(false) }
    var cacheClearedMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan Yomu", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Typography & Reading Defaults
            item {
                Text(
                    text = "Preferensi Baca & Tipografi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Anotasi Furigana Aktif", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Secara default render tag <ruby> dengan huruf furigana di atas kanji",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = furiganaDefault,
                                onCheckedChange = { furiganaDefault = it }
                            )
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Mode True Black (AMOLED)", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Latar belakang hitam pekat 0x000000 untuk efisiensi daya layar OLED",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = amoledDefault,
                                onCheckedChange = { amoledDefault = it }
                            )
                        }
                    }
                }
            }

            // AI Reading Companion Info
            item {
                Text(
                    text = "AI Companion & Integrasi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Model AI: Gemini 3.1 Pro Preview", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Konfigurasi penalaran: Thinking Level HIGH. Memberikan analisis plot sastra dan pemahaman konteks Light Novel yang mendalam.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Kunci API: Diambil secara aman melalui BuildConfig (Secrets panel AI Studio).",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Storage & Maintenance
            item {
                Text(
                    text = "Penyimpanan & Cache",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Bersihkan Cache Sampul & Gambar Sementara",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Menghapus thumbnail cache tanpa mempengaruhi file naskah asli atau progres membaca.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                try {
                                    val cacheDir = context.cacheDir
                                    cacheDir.listFiles()?.forEach { if (it.isFile) it.delete() }
                                    cacheClearedMessage = "Cache sampul berhasil dibersihkan."
                                } catch (e: Exception) {
                                    cacheClearedMessage = "Gagal: ${e.message}"
                                }
                            }
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bersihkan Cache")
                        }

                        if (cacheClearedMessage != null) {
                            Text(
                                text = cacheClearedMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // About Yomu
            item {
                Text(
                    text = "Tentang Yomu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Yomu (読む) — Versi 1.0.0", fontWeight = FontWeight.Bold)
                        Text(
                            "Multi-format Light Novel & Ebook Reader dengan tipografi Furigana/Ruby, metadata Dublin Core/Calibre mendalam, dan AI Companion bertenaga Gemini 3.1 Pro.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Format Didukung: EPUB2/3 (Reflowable), PDF (Fixed-Layout), CBZ (Comic Archive), TXT (Shift-JIS/UTF-8), ZIP.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
