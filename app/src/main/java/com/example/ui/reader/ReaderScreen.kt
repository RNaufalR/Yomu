package com.example.ui.reader

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReaderTheme
import com.example.data.model.ReadingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: String,
    initialChapterIndex: Int,
    viewModel: ReaderViewModel,
    onBackClick: () -> Unit,
    onAiCompanionClick: (String, Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(bookId) {
        viewModel.initReader(bookId, initialChapterIndex)
    }

    val theme = uiState.preferences.theme
    val bgColor = Color(theme.backgroundColor)
    val textColor = Color(theme.textColor)
    val surfaceColor = Color(theme.surfaceColor)
    val accentColor = Color(theme.accentColor)

    val listState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .testTag("reader_screen")
    ) {
        // Main Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { viewModel.toggleControlsVisibility() }
                    )
                }
        ) {
            if (uiState.isLoadingContent) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else if (uiState.book?.primaryFormat?.lowercase() == "pdf") {
                // PDF Viewer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.pdfCurrentPageBitmap != null) {
                        Image(
                            bitmap = uiState.pdfCurrentPageBitmap!!.asImageBitmap(),
                            contentDescription = "Halaman PDF",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("Gagal merender halaman PDF", color = textColor)
                    }
                }
            } else if (uiState.book?.primaryFormat?.lowercase() == "cbz") {
                // CBZ Comic / Illustration Viewer
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.cbzCurrentPageBitmap != null) {
                        Image(
                            bitmap = uiState.cbzCurrentPageBitmap!!.asImageBitmap(),
                            contentDescription = "Halaman CBZ",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("Gagal memuat gambar ilustrasi", color = textColor)
                    }
                }
            } else {
                // Text / EPUB Reader
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = uiState.preferences.marginPaddingDp.dp),
                    contentPadding = PaddingValues(top = 72.dp, bottom = 96.dp)
                ) {
                    // Chapter Title Header
                    item(key = "chapter_title") {
                        Text(
                            text = uiState.currentChapterTitle,
                            color = textColor,
                            fontSize = (uiState.preferences.fontSizeSp * 1.35f).sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp, top = 10.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Paragraphs with Ruby Furigana using stable keys
                    itemsIndexed(
                        items = uiState.paragraphs,
                        key = { index, _ -> "para_${uiState.currentChapterIndex}_$index" }
                    ) { _, paragraphSegments ->
                        RubyParagraphView(
                            segments = paragraphSegments,
                            preferences = uiState.preferences,
                            textColor = textColor
                        )
                    }

                        // Next Chapter Nav Button at bottom of text
                        item {
                            Spacer(modifier = Modifier.height(30.dp))
                            if (uiState.currentChapterIndex < uiState.chapters.size - 1) {
                                Button(
                                    onClick = { viewModel.nextChapter() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                ) {
                                    Text("Lanjut ke Bab Berikutnya")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                                }
                            } else {
                                Text(
                                    text = "— Akhir Naskah —",
                                    color = textColor.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
            }
        }

        // Top Control Bar
        AnimatedVisibility(
            visible = uiState.isControlsVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = surfaceColor.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("reader_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = textColor)
                    }

                    Text(
                        text = uiState.currentChapterTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = { viewModel.toggleSearch() }) {
                        Icon(Icons.Default.Search, contentDescription = "Cari di Buku", tint = textColor)
                    }

                    IconButton(onClick = { viewModel.addBookmark() }) {
                        Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Tambah Pembatas", tint = textColor)
                    }

                    IconButton(
                        onClick = { onAiCompanionClick(bookId, uiState.currentChapterIndex) },
                        modifier = Modifier.testTag("reader_ai_btn")
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = "AI Companion", tint = accentColor)
                    }

                    IconButton(onClick = { viewModel.toggleSettingsSheet() }) {
                        Icon(Icons.Outlined.Tune, contentDescription = "Tipografi & Tema", tint = textColor)
                    }
                }
            }
        }

        // Bottom Navigation Bar
        AnimatedVisibility(
            visible = uiState.isControlsVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = surfaceColor.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // Chapter Scrubber / Progress
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Bab ${uiState.currentChapterIndex + 1} / ${uiState.chapters.size}",
                            color = textColor.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "${(((uiState.currentChapterIndex + 1).toFloat() / uiState.chapters.size.coerceAtLeast(1).toFloat()) * 100).toInt()}%",
                            color = textColor.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Scrubber Slider
                    Slider(
                        value = uiState.currentChapterIndex.toFloat(),
                        onValueChange = { newIdx ->
                            viewModel.loadChapterContent(newIdx.toInt().coerceIn(0, uiState.chapters.size - 1))
                        },
                        valueRange = 0f..(uiState.chapters.size - 1).coerceAtLeast(1).toFloat(),
                        steps = (uiState.chapters.size - 2).coerceAtLeast(0),
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = textColor.copy(alpha = 0.2f)
                        )
                    )

                    // Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.prevChapter() },
                            enabled = uiState.currentChapterIndex > 0
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Bab Sebelumnya", tint = textColor)
                        }

                        TextButton(onClick = { viewModel.toggleToc() }) {
                            Icon(Icons.Default.Menu, contentDescription = null, tint = textColor)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Daftar Isi (TOC)", color = textColor)
                        }

                        IconButton(
                            onClick = { viewModel.nextChapter() },
                            enabled = uiState.currentChapterIndex < uiState.chapters.size - 1
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Bab Berikutnya", tint = textColor)
                        }
                    }
                }
            }
        }

        // In-Book Search Overlay
        if (uiState.isSearchOpen) {
            Surface(
                color = surfaceColor,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.performSearch(it) },
                            placeholder = { Text("Cari kata di dalam novel...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup Pencarian", tint = textColor)
                        }
                    }

                    if (uiState.searchResults.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ditemukan ${uiState.searchResults.size} kecocokan",
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor
                            )
                            Row {
                                IconButton(
                                    onClick = {
                                        val prev = (uiState.currentSearchIndex - 1 + uiState.searchResults.size) % uiState.searchResults.size
                                        viewModel.jumpToSearchResult(prev)
                                    }
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Sebelumnya", tint = textColor)
                                }
                                IconButton(
                                    onClick = {
                                        val next = (uiState.currentSearchIndex + 1) % uiState.searchResults.size
                                        viewModel.jumpToSearchResult(next)
                                    }
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Berikutnya", tint = textColor)
                                }
                            }
                        }

                        // Current snippet preview
                        val currentMatch = uiState.searchResults.getOrNull(uiState.currentSearchIndex)
                        if (currentMatch != null) {
                            Text(
                                text = currentMatch.second,
                                style = MaterialTheme.typography.bodySmall,
                                color = accentColor,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // TOC Drawer / Dialog
        if (uiState.isTocOpen) {
            AlertDialog(
                onDismissRequest = { viewModel.toggleToc() },
                title = { Text("Daftar Isi & Bab") },
                text = {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        items(uiState.chapters) { ch ->
                            val isCurrent = ch.indexInBook == uiState.currentChapterIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrent) accentColor.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable {
                                        viewModel.loadChapterContent(ch.indexInBook)
                                        viewModel.toggleToc()
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${ch.indexInBook + 1}.",
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) accentColor else textColor,
                                    modifier = Modifier.width(32.dp)
                                )
                                Text(
                                    text = ch.title ?: "Bab ${ch.indexInBook + 1}",
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) accentColor else textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.toggleToc() }) { Text("Tutup") }
                }
            )
        }

        // Reader Typography & Theme BottomSheet
        if (uiState.isSettingsSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.toggleSettingsSheet() },
                containerColor = surfaceColor
            ) {
                ReaderSettingsSheetContent(
                    preferences = uiState.preferences,
                    textColor = textColor,
                    onUpdatePreferences = { viewModel.updatePreferences(it) }
                )
            }
        }
    }
}

@Composable
fun ReaderSettingsSheetContent(
    preferences: com.example.data.model.ReaderPreferences,
    textColor: Color,
    onUpdatePreferences: ((com.example.data.model.ReaderPreferences) -> com.example.data.model.ReaderPreferences) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Kustomisasi Tampilan Baca",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = textColor
        )

        // Theme Chooser
        Text("Tema Warna", style = MaterialTheme.typography.labelMedium, color = textColor)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ReaderTheme.values().forEach { th ->
                val isSelected = preferences.theme == th
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(th.backgroundColor))
                        .clickable { onUpdatePreferences { it.copy(theme = th) } }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = th.displayName,
                            tint = Color(th.textColor)
                        )
                    }
                }
            }
        }

        // Font Size Adjuster
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ukuran Huruf (${preferences.fontSizeSp.toInt()} sp)", color = textColor)
            Row {
                IconButton(
                    onClick = {
                        onUpdatePreferences { it.copy(fontSizeSp = (it.fontSizeSp - 1f).coerceAtLeast(12f)) }
                    }
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Perkecil", tint = textColor)
                }
                IconButton(
                    onClick = {
                        onUpdatePreferences { it.copy(fontSizeSp = (it.fontSizeSp + 1f).coerceAtMost(32f)) }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Perbesar", tint = textColor)
                }
            }
        }

        // Furigana Ruby toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Tampilkan Furigana (Ruby Text)", color = textColor, fontWeight = FontWeight.SemiBold)
                Text("Anotasi cara baca kanji di atas teks Jepang", style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.7f))
            }
            Switch(
                checked = preferences.showFurigana,
                onCheckedChange = { newVal -> onUpdatePreferences { it.copy(showFurigana = newVal) } }
            )
        }

        // Font Family Selector
        Text("Jenis Huruf (Font Family)", style = MaterialTheme.typography.labelMedium, color = textColor)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Serif", "Sans-Serif", "Monospace").forEach { family ->
                FilterChip(
                    selected = preferences.fontFamily == family,
                    onClick = { onUpdatePreferences { it.copy(fontFamily = family) } },
                    label = { Text(family) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}
