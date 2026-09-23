package com.example.ui.companion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ai.GeminiThinkingService
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.ChapterEntity
import com.example.domain.engine.EpubParser
import com.example.domain.engine.TxtParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiCompanionScreen(
    bookId: String,
    chapterIndex: Int,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var book by remember { mutableStateOf<BookEntity?>(null) }
    var chapter by remember { mutableStateOf<ChapterEntity?>(null) }
    var contextText by remember { mutableStateOf("") }

    var userQuery by remember { mutableStateOf("") }
    var aiResponse by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(bookId) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getInstance(context)
            val b = db.bookDao().getBookById(bookId)
            val chapters = db.chapterDao().getChaptersListForBook(bookId)
            val ch = chapters.getOrNull(chapterIndex) ?: chapters.firstOrNull()

            book = b
            chapter = ch

            if (b != null && ch != null) {
                val f = File(b.filePath)
                contextText = when (b.primaryFormat.lowercase()) {
                    "epub", "zip" -> {
                        val raw = EpubParser.readChapterHtml(f, ch.locator)
                        raw.replace(Regex("<[^>]*>"), " ").take(4000)
                    }
                    "txt" -> TxtParser.readChunkContent(f, ch.locator).take(4000)
                    else -> ch.title ?: ""
                }
            }
        }
    }

    fun requestAiAnalysis(query: String) {
        val b = book ?: return
        val ch = chapter ?: return
        isLoading = true
        errorMessage = null
        aiResponse = null

        coroutineScope.launch {
            val result = GeminiThinkingService.analyzeWithHighThinking(
                bookTitle = b.title,
                chapterTitle = ch.title ?: "Bab $chapterIndex",
                contextText = contextText,
                userQuery = query
            )

            result.onSuccess { text ->
                aiResponse = text
                isLoading = false
            }.onFailure { err ->
                errorMessage = err.message ?: "Gagal mendapatkan analisis"
                isLoading = false
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ai_companion_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "論",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Telaah Sastra",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Analisis Naratif & Budaya",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Book & Chapter context dossier
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = book?.title ?: "Memuat data naskah...",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Konteks Telaah: ${chapter?.title ?: "Bab $chapterIndex"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Inquiry Presets (Restyled as clean editorial inquiry cards)
            item {
                Text(
                    text = "Topik Telaah Narasi:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InquiryCard(
                        title = "Struktur Plot & Subteks",
                        description = "Analisis dinamika alur cerita, pacing, dan foreshadowing di bab ini.",
                        onClick = {
                            userQuery = "Buat ringkasan bab ini secara mendalam beserta analisis dinamika plot."
                            requestAiAnalysis(userQuery)
                        }
                    )

                    InquiryCard(
                        title = "Konteks Budaya & Furigana",
                        description = "Eksplorasi terminologi Jepang, kiasan sastra, dan makna kanji unik.",
                        onClick = {
                            userQuery = "Jelaskan konteks budaya Jepang, makna kanji/furigana yang unik, atau kiasan sastra di bab ini."
                            requestAiAnalysis(userQuery)
                        }
                    )

                    InquiryCard(
                        title = "Psikologi & Dinamika Karakter",
                        description = "Bedah motivasi batin, konflik emosional, dan relasi antar tokoh.",
                        onClick = {
                            userQuery = "Analisis motivasi emosional, konflik batin, dan relasi antar karakter dalam bab ini."
                            requestAiAnalysis(userQuery)
                        }
                    )
                }
            }

            // Custom Question Input
            item {
                Text(
                    text = "Ajukan Pertanyaan Khusus:",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = userQuery,
                        onValueChange = { userQuery = it },
                        placeholder = { Text("Tanyakan aspek sastra atau plot...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { if (userQuery.isNotBlank()) requestAiAnalysis(userQuery) },
                        enabled = !isLoading && userQuery.isNotBlank(),
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (userQuery.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Kirim Pertanyaan",
                            tint = if (userQuery.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Thinking Indicator (Clean, non-distracting)
            if (isLoading) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Sedang Menelaah Narasi...",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Menganalisis teks sastra dengan penalaran mendalam",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Error Message
            if (errorMessage != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Catatan Kesalahan",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Literary Analysis Output (Editorial formatting, clean serif-like presence, high readability)
            if (aiResponse != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(3.dp)
                                ) {
                                    Text(
                                        text = "評",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Catatan Telaah Kritis",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                            Spacer(modifier = Modifier.height(14.dp))

                            SelectionContainer {
                                Text(
                                    text = aiResponse!!,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 15.sp,
                                        lineHeight = 26.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun InquiryCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
