package com.example.ui.library

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entity.BookEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onBookClick: (String) -> Unit,
    onImportClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("library_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Yomu",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "読む",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                },
                actions = {
                    if (uiState.totalBooks > 0) {
                        IconButton(
                            onClick = { viewModel.toggleViewMode() },
                            modifier = Modifier.testTag("toggle_view_mode")
                        ) {
                            Icon(
                                imageVector = if (uiState.isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                contentDescription = "Ganti Tampilan Grid/List"
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.testTag("sort_button")
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "Urutkan Buku")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option.displayName,
                                                fontWeight = if (uiState.sortOption == option) FontWeight.Bold else FontWeight.Normal,
                                                color = if (uiState.sortOption == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        leadingIcon = {
                                            if (uiState.sortOption == option) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                        },
                                        onClick = {
                                            viewModel.onSortOptionChanged(option)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(imageVector = Icons.Outlined.Settings, contentDescription = "Pengaturan")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (uiState.totalBooks > 0) {
                ExtendedFloatingActionButton(
                    onClick = onImportClick,
                    icon = { Icon(Icons.Default.Add, contentDescription = "Import Buku") },
                    text = { Text("Import Naskah") },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("fab_import_book"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        if (uiState.totalBooks == 0) {
            // Clean, distraction-free empty state for empty library
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                EmptyLibraryView(onImportClick = onImportClick)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Editorial Header (pure literary typography and clean stats)
                LibraryHeader(
                    totalBooks = uiState.totalBooks,
                    readingCount = uiState.readingCount,
                    finishedCount = uiState.finishedCount
                )

                // Search Bar with clean integrated styling
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("search_input"),
                    placeholder = { Text("Cari judul, penulis, seri, atau kanji...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus Pencarian")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Filter Chips Row
                FilterChipsRow(
                    selectedStatus = uiState.selectedStatus,
                    selectedFormat = uiState.selectedFormat,
                    favoritesOnly = uiState.favoritesOnly,
                    onStatusSelected = { viewModel.onStatusFilterChanged(it) },
                    onFormatSelected = { viewModel.onFormatFilterChanged(it) },
                    onToggleFavorites = { viewModel.toggleFavoritesOnly() }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Main Book List / Grid
                if (uiState.filteredBooks.isEmpty()) {
                    NoSearchResultView(
                        query = uiState.searchQuery,
                        onResetFilters = { viewModel.clearAllFilters() }
                    )
                } else {
                    AnimatedContent(
                        targetState = uiState.isGridView,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                        },
                        label = "view_mode_transition"
                    ) { isGrid ->
                        if (isGrid) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 145.dp),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 84.dp, top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(uiState.filteredBooks, key = { it.id }) { book ->
                                    BookGridItem(
                                        book = book,
                                        onClick = { onBookClick(book.id) },
                                        onToggleFavorite = { viewModel.toggleFavorite(book) }
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 84.dp, top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(uiState.filteredBooks, key = { it.id }) { book ->
                                    BookListItem(
                                        book = book,
                                        onClick = { onBookClick(book.id) },
                                        onToggleFavorite = { viewModel.toggleFavorite(book) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryHeader(
    totalBooks: Int,
    readingCount: Int,
    finishedCount: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "読書録",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Koleksi Naskah",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$totalBooks Judul Terkatalog",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$readingCount",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Dibaca",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$finishedCount",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "Selesai",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun FilterChipsRow(
    selectedStatus: String,
    selectedFormat: String,
    favoritesOnly: Boolean,
    onStatusSelected: (String) -> Unit,
    onFormatSelected: (String) -> Unit,
    onToggleFavorites: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = favoritesOnly,
            onClick = onToggleFavorites,
            label = { Text("Favorit") },
            leadingIcon = {
                Icon(
                    imageVector = if (favoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (favoritesOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        // Status filter
        var statusExpanded by remember { mutableStateOf(false) }
        Box {
            FilterChip(
                selected = selectedStatus != "ALL",
                onClick = { statusExpanded = true },
                label = {
                    Text(
                        when (selectedStatus) {
                            "reading" -> "Sedang Dibaca"
                            "finished" -> "Selesai"
                            "unread" -> "Belum Dibaca"
                            else -> "Status"
                        }
                    )
                },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
            )
            DropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                DropdownMenuItem(text = { Text("Semua Status") }, onClick = { onStatusSelected("ALL"); statusExpanded = false })
                DropdownMenuItem(text = { Text("Sedang Dibaca") }, onClick = { onStatusSelected("reading"); statusExpanded = false })
                DropdownMenuItem(text = { Text("Belum Dibaca") }, onClick = { onStatusSelected("unread"); statusExpanded = false })
                DropdownMenuItem(text = { Text("Selesai") }, onClick = { onStatusSelected("finished"); statusExpanded = false })
            }
        }

        // Format filter
        var formatExpanded by remember { mutableStateOf(false) }
        Box {
            FilterChip(
                selected = selectedFormat != "ALL",
                onClick = { formatExpanded = true },
                label = { Text(if (selectedFormat == "ALL") "Format" else selectedFormat.uppercase()) },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
            )
            DropdownMenu(expanded = formatExpanded, onDismissRequest = { formatExpanded = false }) {
                DropdownMenuItem(text = { Text("Semua Format") }, onClick = { onFormatSelected("ALL"); formatExpanded = false })
                DropdownMenuItem(text = { Text("EPUB") }, onClick = { onFormatSelected("epub"); formatExpanded = false })
                DropdownMenuItem(text = { Text("PDF") }, onClick = { onFormatSelected("pdf"); formatExpanded = false })
                DropdownMenuItem(text = { Text("CBZ") }, onClick = { onFormatSelected("cbz"); formatExpanded = false })
                DropdownMenuItem(text = { Text("TXT") }, onClick = { onFormatSelected("txt"); formatExpanded = false })
            }
        }
    }
}

@Composable
fun BookGridItem(
    book: BookEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag("book_card_${book.id}"),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(205.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (!book.coverPath.isNullOrBlank()) {
                    val context = LocalContext.current
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(book.coverPath)
                            .crossfade(true)
                            .build(),
                        contentDescription = book.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Refined literary typography fallback
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "文庫",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Light
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = book.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Authentic Physical Book Spine Shadow (Inner edge fold)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(10.dp)
                        .align(Alignment.CenterStart)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.38f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Refined Format badge
                Surface(
                    shape = RoundedCornerShape(bottomEnd = 4.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = book.primaryFormat.uppercase(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Favorite icon button
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                ) {
                    Icon(
                        imageVector = if (book.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorit",
                        tint = if (book.isFavorite) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                if (!book.seriesName.isNullOrBlank()) {
                    Text(
                        text = "${book.seriesName} ${if (book.seriesIndex != null) "Vol. ${book.seriesIndex}" else ""}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!book.author.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun BookListItem(
    book: BookEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag("book_list_item_${book.id}"),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 60.dp, height = 85.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (!book.coverPath.isNullOrBlank()) {
                    val context = LocalContext.current
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(book.coverPath)
                            .crossfade(true)
                            .build(),
                        contentDescription = book.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().padding(4.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "文庫",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Spine crease
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(6.dp)
                        .align(Alignment.CenterStart)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = book.primaryFormat.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    if (!book.seriesName.isNullOrBlank()) {
                        Text(
                            text = "${book.seriesName} ${if (book.seriesIndex != null) "Vol. ${book.seriesIndex}" else ""}",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = book.author ?: "Penulis Tidak Diketahui",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (book.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorit",
                    tint = if (book.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyLibraryView(onImportClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "書庫",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 26.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Perpustakaan Pribadi",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Koleksi bacaan Anda masih kosong. Masukkan naskah dalam format EPUB, PDF, CBZ, atau TXT dari perangkat Anda untuk mulai membaca.",
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onImportClick,
            modifier = Modifier
                .height(48.dp)
                .testTag("empty_state_import_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Import Naskah Sekarang", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Didukung: EPUB (Ruby Furigana) • CBZ • PDF • TXT",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun NoSearchResultView(
    query: String,
    onResetFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(52.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Naskah Tidak Ditemukan",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (query.isNotBlank())
                "Tidak ada buku yang sesuai dengan kata kunci \"$query\" atau filter aktif."
            else
                "Tidak ada buku yang sesuai dengan filter yang dipilih.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedButton(
            onClick = onResetFilters,
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset Pencarian & Filter")
        }
    }
}
