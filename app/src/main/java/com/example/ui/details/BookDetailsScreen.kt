package com.example.ui.details

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MetadataItem
import com.example.data.model.MetadataStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailsScreen(
    bookId: String,
    viewModel: BookDetailsViewModel,
    onBackClick: () -> Unit,
    onReadClick: (String, Int) -> Unit,
    onAiCompanionClick: (String, Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRawJsonDialog by remember { mutableStateOf(false) }

    LaunchedEffect(bookId) {
        viewModel.loadBook(bookId)
    }

    val book = uiState.book

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("book_details_screen"),
        topBar = {
            TopAppBar(
                title = { Text("Detail Light Novel", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (book != null) {
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (book.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorit",
                                tint = if (book.isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Hapus Buku")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (book != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val currentCh = uiState.progress?.currentChapterIndex ?: 0
                                onAiCompanionClick(book.id, currentCh)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("btn_ai_companion"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(Icons.Outlined.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Telaah Sastra", color = MaterialTheme.colorScheme.onSurface)
                        }

                        Button(
                            onClick = {
                                val currentCh = uiState.progress?.currentChapterIndex ?: 0
                                onReadClick(book.id, currentCh)
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(52.dp)
                                .testTag("btn_start_reading"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            val isReading = (uiState.progress?.progressPercentage ?: 0f) > 0f
                            Text(if (isReading) "Lanjutkan Baca" else "Mulai Membaca")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading || book == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header: Cover + Primary Title & Info
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Native resolution cover card with authentic book spine depth
                        Surface(
                            modifier = Modifier
                                .size(width = 130.dp, height = 185.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shadowElevation = 4.dp
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (!book.coverPath.isNullOrBlank()) {
                                    AsyncImage(
                                        model = book.coverPath,
                                        contentDescription = book.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "文庫",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = book.title,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // Book Spine Depth fold
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(10.dp)
                                        .align(Alignment.CenterStart)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent)
                                            )
                                        )
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = book.primaryFormat.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (!book.author.isNullOrBlank()) {
                                Text(
                                    text = "Penulis: ${book.author}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!book.illustrator.isNullOrBlank()) {
                                Text(
                                    text = "Ilustrator: ${book.illustrator}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!book.seriesName.isNullOrBlank()) {
                                Text(
                                    text = "Seri: ${book.seriesName} ${if (book.seriesIndex != null) "Vol. ${book.seriesIndex}" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Reading Progress bar
                item {
                    val progressVal = uiState.progress?.progressPercentage ?: 0f
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Progres Membaca",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${progressVal.toInt()}%",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { (progressVal / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }

                // Synopsis / Description
                if (!book.description.isNullOrBlank()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Sinopsis",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = book.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Metadata Section Header & Basic/Advanced Toggle (DET-2)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.isAdvancedMetadata) "Metadata Mendalam (Advanced)" else "Metadata Standar (Basic)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (uiState.isAdvancedMetadata) "Mode Lanjutan" else "Mode Dasar",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = uiState.isAdvancedMetadata,
                                onCheckedChange = { viewModel.toggleAdvancedMetadata() },
                                modifier = Modifier.testTag("toggle_advanced_metadata")
                            )
                        }
                    }
                }

                // Metadata Items List with Visual Status Separation (MD-3, MD-4)
                items(uiState.metadataList) { item ->
                    MetadataRow(item = item, isAdvanced = uiState.isAdvancedMetadata)
                }

                // Raw JSON Inspector button (Section 8.1 Rule 3)
                item {
                    OutlinedButton(
                        onClick = { showRawJsonDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buka Raw Payload Metadata (JSON Mentah)")
                    }
                }

                // Chapter Index
                item {
                    Text(
                        text = "Daftar Bab & Struktur (${uiState.chapters.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                items(uiState.chapters) { chapter ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onReadClick(book.id, chapter.indexInBook)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.progress?.currentChapterIndex == chapter.indexInBook)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${chapter.indexInBook + 1}.",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = chapter.title ?: "Bab ${chapter.indexInBook + 1}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                if (!chapter.contentPreview.isNullOrBlank()) {
                                    Text(
                                        text = chapter.contentPreview,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(70.dp))
                }
            }
        }
    }

    // Delete Confirmation Dialog (IMP-6: Remove vs Delete permanently)
    if (showDeleteDialog && book != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Buku dari Koleksi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pilih metode penghapusan untuk '${book.title}':")
                    Text(
                        "• Hapus dari Perpustakaan: Menghapus dari indeks Yomu tanpa menyentuh file asli di penyimpanan.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "• Hapus File Permanen: Menghapus file fisik beserta seluruh data pembacaan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteBookPermanently { onBackClick() }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus File Permanen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.removeBookFromLibrary { onBackClick() }
                    }
                ) {
                    Text("Hapus dari Perpustakaan")
                }
            }
        )
    }

    // Raw JSON Inspector Dialog
    if (showRawJsonDialog) {
        AlertDialog(
            onDismissRequest = { showRawJsonDialog = false },
            title = { Text("Raw Metadata Payload (JSON)") },
            text = {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(10.dp)) {
                        item {
                            Text(
                                text = uiState.rawJsonFormatted,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRawJsonDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
fun MetadataRow(item: MetadataItem, isAdvanced: Boolean) {
    if (!isAdvanced && item.status == MetadataStatus.CUSTOM_UNKNOWN) {
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.key,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.value ?: "Tidak Tersedia di file",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.value != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
                )
                if (isAdvanced && item.namespace.isNotEmpty()) {
                    Text(
                        text = "Sumber: ${item.namespace}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Visual Status Badge (MD-4)
            val badgeColor = when (item.status) {
                MetadataStatus.PARSED -> Color(0xFF2E7D32) // Green: Terbaca
                MetadataStatus.NOT_AVAILABLE -> Color(0xFF757575) // Gray: Tidak Tersedia
                MetadataStatus.CUSTOM_UNKNOWN -> Color(0xFFF57C00) // Amber: Tidak Dikenal
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = badgeColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = item.status.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
