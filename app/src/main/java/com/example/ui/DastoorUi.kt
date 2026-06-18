package com.example.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.*
import kotlinx.coroutines.launch

// Color tokens for Highlights as requested (#Colors: Yellow, Green, Blue, Red)
val HIGHLIGHT_COLORS = listOf(
    Pair("زرد", "#FFF176"),
    Pair("سبز", "#81C784"),
    Pair("آبی", "#64B5F6"),
    Pair("قرمز", "#E57373")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DastoorApp(viewModel: DastoorViewModel) {
    val layoutDirection = LocalLayoutDirection.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Observe our ViewModel state
    val activeScreen by viewModel.activeScreen.collectAsStateWithLifecycle()
    val settings by viewModel.userSettingsState.collectAsStateWithLifecycle()

    // Force RTL layout direction globally for Farsi user experience
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (settings.themeMode == "DARK") Color(0xFF1E1E24) else MaterialTheme.colorScheme.background
        ) {
            when (val screen = activeScreen) {
                is Screen.PinLock -> PinLockScreen(viewModel)
                is Screen.Dashboard -> DashboardScreen(viewModel)
                is Screen.PDFViewer -> PDFViewerScreen(viewModel, screen.documentId)
                is Screen.CategoryManager -> CategoryManagerScreen(viewModel)
                is Screen.Settings -> SettingsScreen(viewModel)
                else -> DashboardScreen(viewModel)
            }
        }
    }
}

@Composable
fun PinLockScreen(viewModel: DastoorViewModel) {
    var codeText by remember { mutableStateOf("") }
    val settings by viewModel.userSettingsState.collectAsStateWithLifecycle()
    var isFailed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = "قفل",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "ورود امن به سیستم دستوریار",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "لطفاً رمز عبور ۴ رقمی خود را وارد کنید",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Custom Code Pins representation
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            repeat(4) { index ->
                val active = index < codeText.length
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                        )
                )
            }
        }

        if (isFailed) {
            Text(
                text = "رمز وارد شده اشتباه است. مجدداً تلاش کنید.",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Farsi Soft Grid Keyboard
        val keys = listOf("۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹", "پاک کردن", "۰", "حذف")
        
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            for (row in 0 until 4) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (col in 0 until 3) {
                        val keyIndex = row * 3 + col
                        if (keyIndex < keys.size) {
                            val key = keys[keyIndex]
                            OutlinedButton(
                                onClick = {
                                    isFailed = false
                                    if (key == "پاک کردن") {
                                        codeText = ""
                                    } else if (key == "حذف") {
                                        if (codeText.isNotEmpty()) {
                                            codeText = codeText.dropLast(1)
                                        }
                                    } else {
                                        if (codeText.length < 4) {
                                            // Mapping Persian digits to English
                                            val englishChar = when(key) {
                                                "۱" -> "1"
                                                "۲" -> "2"
                                                "۳" -> "3"
                                                "۴" -> "4"
                                                "۵" -> "5"
                                                "۶" -> "6"
                                                "۷" -> "7"
                                                "۸" -> "8"
                                                "۹" -> "9"
                                                "۰" -> "0"
                                                else -> key
                                            }
                                            codeText += englishChar
                                            if (codeText.length == 4) {
                                                val success = viewModel.tryPinCode(codeText)
                                                if (!success) {
                                                    codeText = ""
                                                    isFailed = true
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.2f)
                                    .testTag("pin_keyboard_$keyIndex")
                            ) {
                                Text(text = key, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DastoorViewModel) {
    val context = LocalContext.current
    val docs by viewModel.uiDocumentsState.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val tags by viewModel.allTags.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    val notes by viewModel.allNotes.collectAsStateWithLifecycle()
    val bookmarks by viewModel.allBookmarks.collectAsStateWithLifecycle()
    val history by viewModel.readingHistoryState.collectAsStateWithLifecycle()

    var showAddDocDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = "قفسه",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "دستوریار مهندسی",
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.CategoryManager) },
                        modifier = Modifier.testTag("category_mgr_nav")
                    ) {
                        Icon(Icons.Filled.List, "مدیریت دسته‌ها")
                    }
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Settings) },
                        modifier = Modifier.testTag("settings_nav")
                    ) {
                        Icon(Icons.Filled.Settings, "تنظیمات")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDocDialog = true },
                icon = { Icon(Icons.Filled.Add, "سند جدید") },
                text = { Text("افزودن سند فنی") },
                modifier = Modifier.testTag("add_doc_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Creative visual banner showing dynamic statistics
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                // Background Image
                Image(
                    painter = painterResource(id = R.drawable.tech_docs_banner),
                    contentDescription = "بنر داکیومنت‌ها",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Color overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )
                // Stats HUD overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "مرجع آنلاین آیین‌نامه‌ها و استانداردهای ایران",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "📚 کل فایل‌ها: ${docs.size} سند",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "🔖 یادداشت‌ها: ${notes.size}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "★ نشان‌شده: ${bookmarks.size}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Rapid Modern Search Bar
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_bar_input"),
                placeholder = { Text("جستجو در عنوان، موضوع، برچسب یا داخل PDF...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "جستجو") },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "پاک کردن")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Category Horizontal filter pills
            Text(
                text = "دسته‌بندی‌های تخصصی",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp)
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { viewModel.selectedCategoryId.value = null },
                        label = { Text("همه مستندات") }
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat.id,
                        onClick = { viewModel.selectedCategoryId.value = cat.id },
                        label = { Text(cat.name) }
                    )
                }
            }

            // Tag quick filtration row
            if (tags.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        InputChip(
                            selected = selectedTag == null,
                            onClick = { viewModel.selectedTag.value = null },
                            label = { Text("همه برچسب‌ها") },
                            trailingIcon = { if (selectedTag == null) Icon(Icons.Filled.Check, null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                    items(tags) { tag ->
                        InputChip(
                            selected = selectedTag == tag.name,
                            onClick = { viewModel.selectedTag.value = tag.name },
                            label = { Text("#${tag.name}") }
                        )
                    }
                }
            }

            // Document Lists
            Text(
                text = "مستندات و مقررات فنی مهندسی",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
            )

            if (docs.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "خالی",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "هیچ مستند مهندسی یافت نشد.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            } else {
                for (doc in docs) {
                    DocumentCard(
                        doc = doc,
                        categoryName = categories.find { it.id == doc.categoryId }?.name ?: "دسته‌بندی نشده",
                        onOpen = { viewModel.navigateTo(Screen.PDFViewer(doc.id)) },
                        onFavoriteToggle = { viewModel.toggleFavorite(doc) },
                        onDelete = { viewModel.deleteDocument(doc) }
                    )
                }
            }

            // Reading History HUD
            if (history.isNotEmpty()) {
                Text(
                    text = "آخرین فایل‌های مطالعه شده",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp)
                )
                
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val recentDocs = history.take(5)
                    items(recentDocs) { hist ->
                        // find matching document
                        val linkedDoc = docs.find { it.id == hist.documentId }
                        if (linkedDoc != null) {
                            Card(
                                onClick = { viewModel.navigateTo(Screen.PDFViewer(linkedDoc.id)) },
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(110.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = linkedDoc.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "صفحه ${hist.lastPage}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${(hist.progress * 100).toInt()}% پیشرفت",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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

    if (showAddDocDialog) {
        AddDocumentDialog(
            categories = categories,
            onDismiss = { showAddDocDialog = false },
            onAdd = { title, desc, catId, contentText, tagsString, pages, size ->
                viewModel.addDocument(title, desc, catId, contentText, tagsString, pages, size)
                showAddDocDialog = false
                Toast.makeText(context, "سند فنی جدید ذخیره شد", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun DocumentCard(
    doc: Document,
    categoryName: String,
    onOpen: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onOpen() }
            .testTag("doc_card_${doc.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Category Chip Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = categoryName,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "نشان کردن",
                            tint = if (doc.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("delete_doc_btn_${doc.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "حذف فایل",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = doc.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = doc.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info badges
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "📄 ${doc.totalPages} صفحه",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "💾 ${doc.fileSize}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "📅 ${doc.dateAdded}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                // Tags flow
                if (doc.tags.isNotEmpty()) {
                    val tagList = doc.tags.split(",")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (t in tagList.take(2)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "#$t",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDocumentDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onAdd: (String, String, Int, String, String, Int, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(if (categories.isNotEmpty()) categories.first().id else 0) }
    var contentText by remember { mutableStateOf("") }
    var tagsString by remember { mutableStateOf("") }
    var totalPages by remember { mutableStateOf("10") }
    var sizeText by remember { mutableStateOf("۱.۴ مگابایت") }

    var expandedCategoryMenu by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "افزودن سند یا آیین‌نامه جدید",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان فایل") },
                    modifier = Modifier.fillMaxWidth().testTag("add_doc_title_field"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("توضیحات کوتاه") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Selection Box dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedCategoryMenu,
                    onExpandedChange = { expandedCategoryMenu = !expandedCategoryMenu }
                ) {
                    val currentCategoryName = categories.find { it.id == selectedCategoryId }?.name ?: "انتخاب دسته‌بندی"
                    OutlinedTextField(
                        value = currentCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("دسته‌بندی تخصصی") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoryMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategoryMenu,
                        onDismissRequest = { expandedCategoryMenu = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    expandedCategoryMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = contentText,
                    onValueChange = { contentText = it },
                    label = { Text("محتوا یا پاراگراف‌های اساسی سند (جهت جستجو و هوش مصنوعی)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 10
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = tagsString,
                    onValueChange = { tagsString = it },
                    label = { Text("برچسب‌ها (با کاما جدا کنید، مثل: آیین‌نامه,عمران)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = totalPages,
                        onValueChange = { totalPages = it },
                        label = { Text("تعداد صفحات") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sizeText,
                        onValueChange = { sizeText = it },
                        label = { Text("حجم فایل (مثال: ۲.۵ مگ)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("انصراف")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotEmpty() && selectedCategoryId > 0) {
                                onAdd(
                                    title,
                                    desc,
                                    selectedCategoryId,
                                    contentText,
                                    tagsString,
                                    totalPages.toIntOrNull() ?: 10,
                                    sizeText
                                )
                            }
                        },
                        modifier = Modifier.testTag("submit_doc_button")
                    ) {
                        Text("ذخیره سند")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFViewerScreen(viewModel: DastoorViewModel, documentId: Int) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val doc by viewModel.currentDocument.collectAsStateWithLifecycle()
    val activePage by viewModel.currentDocumentPage.collectAsStateWithLifecycle()
    val rotation by viewModel.studyRotation.collectAsStateWithLifecycle()
    val nightMode by viewModel.studyNightMode.collectAsStateWithLifecycle()
    val notes by viewModel.allNotes.collectAsStateWithLifecycle()
    val bookmarks by viewModel.allBookmarks.collectAsStateWithLifecycle()

    var activeTabInLeftSidebar by remember { mutableStateOf(0) } // 0: Bookmarks, 1: Page Notes, 2: AI Assistant, 3: Highlighter
    var showSidebar by remember { mutableStateOf(false) }

    // Dialog state
    var showBookmarkDialog by remember { mutableStateOf(false) }
    var currentNoteTextState by remember { mutableStateOf("") }

    // Fetch active note for active page when page changes
    LaunchedEffect(activePage, doc) {
        doc?.let { d ->
            val pageNote = notes.find { it.documentId == d.id && it.pageNumber == activePage }
            currentNoteTextState = pageNote?.text ?: ""
        }
    }

    if (doc == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = doc!!.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                        Icon(Icons.Filled.ArrowBack, "برگشت")
                    }
                },
                actions = {
                    IconButton(onClick = { showSidebar = !showSidebar }) {
                        Icon(Icons.Filled.Menu, "کنترل‌ها و دستیار")
                    }
                    IconButton(onClick = { viewModel.rotateDocument() }) {
                        Icon(Icons.Filled.Refresh, "چرخش")
                    }
                    IconButton(onClick = { viewModel.toggleStudyNightMode() }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "تم"
                        )
                    }
                    IconButton(onClick = { showBookmarkDialog = true }) {
                        Icon(Icons.Filled.Star, "نشان‌گذاری صفحه")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (nightMode) Color(0xFF1E1E24) else MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Technical Reader Canvas
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (nightMode) Color(0xFF121214) else Color(0xFFF1F2F6))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // PDF Viewer Controls Bar
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (nightMode) Color(0xFF2E2E36) else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.prevPage() }) {
                            Icon(Icons.Filled.ArrowForward, "صفحه قبلی")
                        }
                        
                        Text(
                            text = "صفحه $activePage از ${doc!!.totalPages}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (nightMode) Color.White else Color.Black
                        )
                        
                        IconButton(onClick = { viewModel.nextPage() }) {
                            Icon(Icons.Filled.ArrowBack, "صفحه بعدی")
                        }
                    }
                }

                // Render page body content (Highly functional with zoom & rotate!)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (nightMode) Color(0xFF2E2E36) else Color.White)
                        .border(1.dp, if (nightMode) Color.DarkGray else Color.LightGray, RoundedCornerShape(12.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .rotate(rotation),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "فهرست مطالب و آیین‌نامه تخصصی:",
                            color = if (nightMode) Color.LightGray else Color.DarkGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Render standard PDF contents parsed dynamically or simulated paragraphs
                        val contentParagraphs = doc!!.contentText.split("\n\n")
                        val currentParagraph = contentParagraphs.getOrElse(activePage % contentParagraphs.size) { doc!!.contentText }
                        
                        Text(
                            text = currentParagraph,
                            fontSize = 16.sp,
                            lineHeight = 28.sp,
                            color = if (nightMode) Color.White else Color.Black
                        )
                    }
                }

                // In-reader fast notes container
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (nightMode) Color(0xFF2E2E36) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "📝 یادداشت کوتاه همین صفحه (ذخیره همزمان):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (nightMode) Color.LightGray else Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = currentNoteTextState,
                            onValueChange = {
                                currentNoteTextState = it
                                viewModel.saveNoteForActivePage(it)
                            },
                            placeholder = { Text("مثال: الزامات آرماتوبندی در اینجا رعایت شود") },
                            modifier = Modifier.fillMaxWidth().testTag("page_note_input"),
                            maxLines = 2,
                            textStyle = TextStyle(fontSize = 13.sp)
                        )
                    }
                }
            }

            // Collapsible Navigation and Intelligent Assistant Sidebar Interface
            AnimatedVisibility(
                visible = showSidebar,
                enter = slideInHorizontally(initialOffsetX = { it }),
                exit = slideOutHorizontally(targetOffsetX = { it })
            ) {
                Card(
                    shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                    modifier = Modifier
                        .width(310.dp)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp)
                    ) {
                        // Header showing sidebar options
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ابزارها و هوش مصنوعی",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { showSidebar = false }) {
                                Icon(Icons.Filled.Close, "بستن پنل")
                            }
                        }

                        // Tab toggles for sidebar
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            IconButton(
                                onClick = { activeTabInLeftSidebar = 0 },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (activeTabInLeftSidebar == 0) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) {
                                Icon(Icons.Filled.Star, "بوکمارک‌ها", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { activeTabInLeftSidebar = 1 },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (activeTabInLeftSidebar == 1) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) {
                                Icon(Icons.Filled.Edit, "یادداشت‌ها", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { activeTabInLeftSidebar = 2 },
                                modifier = Modifier.testTag("ai_panel_tab_btn"),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (activeTabInLeftSidebar == 2) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) {
                                Icon(Icons.Filled.Face, "هوش مصنوعی", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { activeTabInLeftSidebar = 3 },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (activeTabInLeftSidebar == 3) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) {
                                Icon(Icons.Filled.Edit, "هایلایتر", modifier = Modifier.size(18.dp))
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Sidebar Active Content Switcher
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            when (activeTabInLeftSidebar) {
                                0 -> BookmarksSidebarTab(viewModel, documentId)
                                1 -> NotesSidebarTab(viewModel, documentId)
                                2 -> AiAssistantSidebarTab(viewModel)
                                3 -> HighlighterSidebarTab(viewModel)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBookmarkDialog) {
        var bookmarkTitle by remember { mutableStateOf("بوکمارک صفحه $activePage") }
        var bookmarkNote by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showBookmarkDialog = false }) {
            Card(
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "ثبت بوکمارک صفحه $activePage",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = bookmarkTitle,
                        onValueChange = { bookmarkTitle = it },
                        label = { Text("نام دلخواه برای بوکمارک") },
                        modifier = Modifier.fillMaxWidth().testTag("bookmark_title_field")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = bookmarkNote,
                        onValueChange = { bookmarkNote = it },
                        label = { Text("توضیح یا نکته کوتاه") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showBookmarkDialog = false }) {
                            Text("انصراف")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.addBookmark(bookmarkTitle, bookmarkNote)
                                showBookmarkDialog = false
                                Toast.makeText(context, "بوکمارک ذخیره شد", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("submit_bookmark_btn")
                        ) {
                            Text("ذخیره")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookmarksSidebarTab(viewModel: DastoorViewModel, documentId: Int) {
    val bList by viewModel.allBookmarks.collectAsStateWithLifecycle()
    val docBookmarks = bList.filter { it.documentId == documentId }

    if (docBookmarks.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Star, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("هیچ بوکمارکی ثبت نشده است.", fontSize = 12.sp, color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(docBookmarks) { b ->
                Card(
                    onClick = { viewModel.goToPage(b.pageNumber) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(b.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            IconButton(
                                onClick = { viewModel.deleteBookmark(b) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                        if (b.note.isNotEmpty()) {
                            Text(b.note, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                        }
                        Text(
                            text = "صفحه ${b.pageNumber} • ${b.dateAdded}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotesSidebarTab(viewModel: DastoorViewModel, documentId: Int) {
    val nList by viewModel.allNotes.collectAsStateWithLifecycle()
    val docNotes = nList.filter { it.documentId == documentId }

    if (docNotes.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Edit, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("هیچ یادداشتی در صفحات ثبت نشده.", fontSize = 12.sp, color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(docNotes) { n ->
                Card(
                    onClick = { viewModel.goToPage(n.pageNumber) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(n.text, fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "صفحه ${n.pageNumber} • ثبت: ${n.dateAdded}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiAssistantSidebarTab(viewModel: DastoorViewModel) {
    val response by viewModel.aiResponse.collectAsStateWithLifecycle()
    val loading by viewModel.aiLoading.collectAsStateWithLifecycle()
    var questionText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // AI command buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.requestAiSummary() },
                modifier = Modifier.weight(1f).testTag("ai_summarize_btn"),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("خلاصه هوشمند", fontSize = 10.sp)
            }
            Button(
                onClick = { viewModel.requestAiChecklist() },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("چک‌لیست فنی", fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Question fields
        OutlinedTextField(
            value = questionText,
            onValueChange = { questionText = it },
            placeholder = { Text("از متن آیین‌نامه سوال بپرسید...") },
            modifier = Modifier.fillMaxWidth().testTag("ai_question_input"),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { 
                    viewModel.askAiQuestion(questionText)
                    questionText = ""
                }) {
                    Icon(Icons.Filled.Send, "ارسال")
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Response screen
        Card(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(12.dp)
            ) {
                if (loading) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("در حال پردازش هوش مصنوعی...", fontSize = 11.sp, color = Color.Gray)
                    }
                } else if (response.isNotEmpty()) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "پاسخ دستیار فنی:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = response,
                            fontSize = 13.sp,
                            lineHeight = 22.sp
                        )
                    }
                } else {
                    Text(
                        text = "یکی از گزینه‌های بالا را انتخاب کرده یا سوال از روی متن بپرسید تا هوش مصنوعی بر اساس مستندات پاسخ دهد.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun HighlighterSidebarTab(viewModel: DastoorViewModel) {
    var rawScannerText by remember { mutableStateOf("") }
    var cleanedOcrText by remember { mutableStateOf("") }
    var ocrLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        // Quick Highlight panel
        Text(
            text = "رنگ‌های هایلایت موجود:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (h in HIGHLIGHT_COLORS) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(android.graphics.Color.parseColor(h.second)))
                        .clickable {
                            viewModel.saveHighlight("هایلایت شده توسط کاربر در صفحه", h.second)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(h.first, fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        // OCR Engine block
        Text(
            text = "موتور OCR هوشمند (متون اسکن‌شده)",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "جهت استخراج متن خالص فارسی از تصویر اسکن شده، متن خام OCR را اینجا وارد کنید تا تصفیه و خوانا شود.",
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        OutlinedTextField(
            value = rawScannerText,
            onValueChange = { rawScannerText = it },
            placeholder = { Text("متن اسکن شده حاصل از دوربین یا فایل...") },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (rawScannerText.trim().isNotEmpty()) {
                    ocrLoading = true
                    viewModel.runSmartOcr(rawScannerText) { result ->
                        cleanedOcrText = result
                        ocrLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (ocrLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
            } else {
                Text("تصفیه و تشخیص OCR فارسی")
            }
        }

        if (cleanedOcrText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("متن تصفیه شده نهایی:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(cleanedOcrText, fontSize = 12.sp, lineHeight = 20.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerScreen(viewModel: DastoorViewModel) {
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    var newCatName by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مدیریت دسته‌بندی‌های کارهای فنی") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                        Icon(Icons.Filled.ArrowBack, "برگشت")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "دسته‌بندی‌های موجود",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Input to add new Category
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    placeholder = { Text("نام دسته جدید (به عنوان مثال: ممیزی گاز)") },
                    modifier = Modifier.weight(1f).testTag("new_cat_name_field"),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (newCatName.trim().isNotEmpty()) {
                            viewModel.addCategory(newCatName.trim())
                            newCatName = ""
                            Toast.makeText(context, "دسته‌بندی جدید ثبت شد", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("submit_new_cat_btn")
                ) {
                    Text("افزودن")
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cat.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            
                            if (cat.isSystem) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("پیش‌فرض سیستم", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            } else {
                                IconButton(onClick = { viewModel.deleteCategory(cat) }) {
                                    Icon(Icons.Filled.Delete, "حذف", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: DastoorViewModel) {
    val settings by viewModel.userSettingsState.collectAsStateWithLifecycle()
    var pinValue by remember { mutableStateOf(settings.securePin) }
    var backupJsonText by remember { mutableStateOf("") }
    var importJsonText by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تنظیمات و ابزارها") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                        Icon(Icons.Filled.ArrowBack, "برگشت")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme setting card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("تم رنگی برنامه", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("LIGHT" to "روشن", "DARK" to "تاریک").forEach { (mode, label) ->
                            val selected = settings.themeMode == mode
                            Button(
                                onClick = { viewModel.updateTheme(mode) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }

            // PIN Lock code secure settings
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("امنیت و رمز عبور ورود", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Text("جهت محافظت از یادداشت‌ها و مستندات، یک رمز ۴ رقمی تعیین کنید.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = pinValue,
                            onValueChange = { if (it.length <= 4) pinValue = it },
                            placeholder = { Text("مثال: ۱۲۳۴") },
                            modifier = Modifier.weight(1f).testTag("settings_pin_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.updatePinLock(pinValue)
                                Toast.makeText(context, "تغییرات قفل امنیتی ذخیره شد", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("save_pin_btn")
                        ) {
                            Text("بروزرسانی قفل")
                        }
                    }
                }
            }

            // Backup & Restore
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("سازماندهی پشتیبان‌گیری و بازیابی داده‌ها (Backup/Restore)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Text("تمامی بوکمارک‌ها، یادداشتها و اطلاعات کاربران در قالب یک فایل متنی JSON پشتیبان‌گیری و بازیابی شود.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            backupJsonText = viewModel.exportBackupJson()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("export_backup_btn")
                    ) {
                        Icon(Icons.Filled.Share, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ایجاد نسخه پشتیبان JSON")
                    }

                    if (backupJsonText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = backupJsonText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("کد نسخه پشتیبان (کپی کنید)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("کد پشتیبان JSON کپی‌شده قبلی را اینجا جایگذاری کنید...") },
                        modifier = Modifier.fillMaxWidth().height(80.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val ok = viewModel.importBackupJson(importJsonText)
                            if (ok) {
                                Toast.makeText(context, "بازیابی داده‌ها با موفقیت انجام شد", Toast.LENGTH_SHORT).show()
                                importJsonText = ""
                            } else {
                                Toast.makeText(context, "خطا در فرمت کدهای ورودی!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Filled.Refresh, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("بازیابی نسخه پشتیبان")
                    }
                }
            }
        }
    }
}
