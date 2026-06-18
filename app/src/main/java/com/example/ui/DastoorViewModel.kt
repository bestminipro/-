package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.api.GeminiAssistant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

sealed class Screen {
    object Welcome : Screen()
    object PinLock : Screen()
    object Dashboard : Screen()
    data class PDFViewer(val documentId: Int) : Screen()
    object CategoryManager : Screen()
    object Settings : Screen()
}

class DastoorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DastoorDatabase.getDatabase(application, viewModelScope)
    val repository = DastoorRepository(db)

    // UI Navigation State
    private val _activeScreen = MutableStateFlow<Screen>(Screen.Dashboard)
    val activeScreen: StateFlow<Screen> = _activeScreen

    // Search & Filtering State
    val searchQuery = MutableStateFlow("")
    val selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedTag = MutableStateFlow<String?>(null)

    // Reactive database flows
    val allCategories = repository.allCategories.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val allTags = repository.allTags.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val allBookmarks = repository.allBookmarks.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val allNotes = repository.allNotes.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val readingHistoryState = repository.readingHistory.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val userSettingsState = repository.userSettings
        .filterNotNull()
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings()
        )

    // Main document list filtered by query, category, and tag
    val uiDocumentsState: StateFlow<List<Document>> = combine(
        repository.allDocuments,
        searchQuery,
        selectedCategoryId,
        selectedTag
    ) { docs, query, catId, tag ->
        var filtered = docs
        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true) ||
                it.contentText.contains(query, ignoreCase = true) ||
                it.tags.contains(query, ignoreCase = true)
            }
        }
        if (catId != null) {
            filtered = filtered.filter { it.categoryId == catId }
        }
        if (tag != null) {
            filtered = filtered.filter { it.tags.contains(tag, ignoreCase = true) }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Study Document State
    private val _currentDocument = MutableStateFlow<Document?>(null)
    val currentDocument: StateFlow<Document?> = _currentDocument

    private val _currentDocumentPage = MutableStateFlow(1)
    val currentDocumentPage: StateFlow<Int> = _currentDocumentPage

    private val _studyRotation = MutableStateFlow(0f) // Rotation angle: 0, 90, 180, 270
    val studyRotation: StateFlow<Float> = _studyRotation

    private val _studyNightMode = MutableStateFlow(false)
    val studyNightMode: StateFlow<Boolean> = _studyNightMode

    // Secure Pass State
    val securePinPassed = MutableStateFlow(false)

    // AI Assistant State
    private val _aiResponse = MutableStateFlow("")
    val aiResponse: StateFlow<String> = _aiResponse

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading

    // Temporary Study timer
    private var studyStartTime = 0L

    init {
        // Initial setup check if App Secure PIN is active on start
        viewModelScope.launch {
            repository.userSettings.collectLatest { settings ->
                if (settings != null && settings.securePin.isNotEmpty() && !securePinPassed.value) {
                    _activeScreen.value = Screen.PinLock
                }
            }
        }
    }

    fun navigateTo(screen: Screen) {
        // Finish active timer if moving away from PDFViewer
        if (_activeScreen.value is Screen.PDFViewer && screen !is Screen.PDFViewer) {
            saveStudySessionProgress()
        }
        if (screen is Screen.PDFViewer) {
            studyStartTime = System.currentTimeMillis()
            loadDocumentForStudy(screen.documentId)
        }
        _activeScreen.value = screen
    }

    private fun loadDocumentForStudy(id: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.getDocumentById(id).collectLatest { doc ->
            _currentDocument.value = doc
            // Fetch last reading progress to resume study session
            doc?.let { d ->
                val history = readingHistoryState.value.find { it.documentId == d.id }
                _currentDocumentPage.value = history?.lastPage ?: 1
            }
        }
    }

    fun nextPage() {
        _currentDocument.value?.let { doc ->
            if (_currentDocumentPage.value < doc.totalPages) {
                _currentDocumentPage.value += 1
            }
        }
    }

    fun prevPage() {
        if (_currentDocumentPage.value > 1) {
            _currentDocumentPage.value -= 1
        }
    }

    fun goToPage(page: Int) {
        _currentDocument.value?.let { doc ->
            if (page in 1..doc.totalPages) {
                _currentDocumentPage.value = page
            }
        }
    }

    fun rotateDocument() {
        _studyRotation.value = (_studyRotation.value + 90f) % 360f
    }

    fun toggleStudyNightMode() {
        _studyNightMode.value = !_studyNightMode.value
    }

    private fun saveStudySessionProgress() {
        val doc = _currentDocument.value ?: return
        val duration = (System.currentTimeMillis() - studyStartTime) / 1000
        viewModelScope.launch(Dispatchers.IO) {
            val progress = _currentDocumentPage.value.toFloat() / doc.totalPages
            val history = ReadingHistory(
                documentId = doc.id,
                lastPage = _currentDocumentPage.value,
                progress = progress,
                lastReadTime = System.currentTimeMillis(),
                durationSeconds = if (duration > 0) duration else 10
            )
            repository.insertReadingHistory(history)
        }
    }

    // --- Document operations ---
    fun addDocument(title: String, description: String, categoryId: Int, contentText: String, tags: String, pages: Int, size: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newDoc = Document(
                title = title,
                description = description,
                categoryId = categoryId,
                filePath = "user_uploaded_${System.currentTimeMillis()}.pdf",
                fileSize = size,
                totalPages = pages,
                contentText = contentText,
                tags = tags
            )
            repository.insertDocument(newDoc)
        }
    }

    fun deleteDocument(document: Document) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDocument(document)
            if (_currentDocument.value?.id == document.id) {
                _currentDocument.value = null
            }
        }
    }

    fun updateDocument(document: Document) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateDocument(document)
        }
    }

    fun toggleFavorite(document: Document) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateDocument(document.copy(isFavorite = !document.isFavorite))
        }
    }

    // --- Bookmarks operations ---
    fun addBookmark(title: String, note: String) {
        val doc = _currentDocument.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val b = Bookmark(
                documentId = doc.id,
                pageNumber = _currentDocumentPage.value,
                title = title,
                note = note
            )
            repository.insertBookmark(b)
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBookmark(bookmark)
        }
    }

    // --- Notes operations ---
    fun saveNoteForActivePage(text: String) {
        val doc = _currentDocument.value ?: return
        val page = _currentDocumentPage.value
        viewModelScope.launch(Dispatchers.IO) {
            // Check if there is already a note for this page
            val existing = repository.getNoteByPage(doc.id, page)
            if (existing != null) {
                if (text.isEmpty()) {
                    repository.deleteNote(existing)
                } else {
                    repository.insertNote(existing.copy(text = text))
                }
            } else if (text.isNotEmpty()) {
                val n = Note(
                    documentId = doc.id,
                    pageNumber = page,
                    text = text
                )
                repository.insertNote(n)
            }
        }
    }

    // --- Highlights operations ---
    fun saveHighlight(text: String, colorHex: String) {
        val doc = _currentDocument.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val h = Highlight(
                documentId = doc.id,
                pageNumber = _currentDocumentPage.value,
                highlightedText = text,
                colorHex = colorHex
            )
            repository.insertHighlight(h)
        }
    }

    fun deleteHighlight(highlight: Highlight) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteHighlight(highlight)
        }
    }

    // --- Category operations ---
    fun addCategory(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCategory(Category(name = name, isSystem = false))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCategory(category)
        }
    }

    // --- Tag operations ---
    fun addTag(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTag(Tag(name = name))
        }
    }

    // --- User settings ---
    fun updateTheme(themeMode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val s = userSettingsState.value.copy(themeMode = themeMode)
            repository.saveUserSettings(s)
        }
    }

    fun updateFontSize(fontSize: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val s = userSettingsState.value.copy(fontSize = fontSize)
            repository.saveUserSettings(s)
        }
    }

    fun updatePinLock(pin: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val s = userSettingsState.value.copy(securePin = pin)
            repository.saveUserSettings(s)
            if (pin.isEmpty()) {
                securePinPassed.value = true
            }
        }
    }

    fun tryPinCode(pin: String): Boolean {
        return if (userSettingsState.value.securePin == pin) {
            securePinPassed.value = true
            navigateTo(Screen.Dashboard)
            true
        } else {
            false
        }
    }

    // --- AI Assistant Actions ---
    fun requestAiSummary() {
        val doc = _currentDocument.value ?: return
        val textContent = doc.contentText
        _aiResponse.value = ""
        _aiLoading.value = true
        viewModelScope.launch {
            val resp = GeminiAssistant.summarizeDocument(doc.title, textContent)
            _aiResponse.value = resp
            _aiLoading.value = false
        }
    }

    fun requestAiChecklist() {
        val doc = _currentDocument.value ?: return
        val textContent = doc.contentText
        _aiResponse.value = ""
        _aiLoading.value = true
        viewModelScope.launch {
            val resp = GeminiAssistant.generateChecklist(doc.title, textContent)
            _aiResponse.value = resp
            _aiLoading.value = false
        }
    }

    fun askAiQuestion(question: String) {
        if (question.isEmpty()) return
        val doc = _currentDocument.value ?: return
        val textContent = doc.contentText
        _aiResponse.value = ""
        _aiLoading.value = true
        viewModelScope.launch {
            val resp = GeminiAssistant.askDocumentQuestion(doc.title, textContent, question)
            _aiResponse.value = resp
            _aiLoading.value = false
        }
    }

    fun runSmartOcr(rawText: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val clean = GeminiAssistant.performOcrOnText(rawText)
            onResult(clean)
        }
    }

    // --- Backup & Restore (JSON Export) ---
    fun exportBackupJson(): String {
        return try {
            val rootObj = JSONObject()
            
            // Bookmarks
            val bArr = JSONArray()
            for (b in allBookmarks.value) {
                bArr.put(JSONObject().apply {
                    put("documentId", b.documentId)
                    put("pageNumber", b.pageNumber)
                    put("title", b.title)
                    put("note", b.note)
                    put("dateAdded", b.dateAdded)
                })
            }
            rootObj.put("bookmarks", bArr)

            // Notes
            val nArr = JSONArray()
            for (n in allNotes.value) {
                nArr.put(JSONObject().apply {
                    put("documentId", n.documentId)
                    put("pageNumber", n.pageNumber)
                    put("text", n.text)
                    put("dateAdded", n.dateAdded)
                })
            }
            rootObj.put("notes", nArr)

            // Settings
            val settings = userSettingsState.value
            rootObj.put("settings", JSONObject().apply {
                put("themeMode", settings.themeMode)
                put("fontSize", settings.fontSize)
                put("securePin", settings.securePin)
            })

            rootObj.toString(4) // Beautifully formatted
        } catch (e: Exception) {
            Log.e("Backup", "Error exporting: ${e.message}")
            ""
        }
    }

    fun importBackupJson(jsonString: String): Boolean {
        if (jsonString.trim().isEmpty()) return false
        return try {
            val root = JSONObject(jsonString)
            
            viewModelScope.launch(Dispatchers.IO) {
                // Restore Settings
                if (root.has("settings")) {
                    val sObj = root.getJSONObject("settings")
                    val currentSettings = userSettingsState.value.copy(
                        themeMode = sObj.optString("themeMode", "SYSTEM"),
                        fontSize = sObj.optString("fontSize", "MEDIUM"),
                        securePin = sObj.optString("securePin", "")
                    )
                    repository.saveUserSettings(currentSettings)
                }

                // Restore Bookmarks
                if (root.has("bookmarks")) {
                    val bArr = root.getJSONArray("bookmarks")
                    for (i in 0 until bArr.length()) {
                        val bObj = bArr.getJSONObject(i)
                        repository.insertBookmark(Bookmark(
                            documentId = bObj.optInt("documentId", 1),
                            pageNumber = bObj.optInt("pageNumber", 1),
                            title = bObj.optString("title", ""),
                            note = bObj.optString("note", ""),
                            dateAdded = bObj.optString("dateAdded", "")
                        ))
                    }
                }

                // Restore Notes
                if (root.has("notes")) {
                    val nArr = root.getJSONArray("notes")
                    for (i in 0 until nArr.length()) {
                        val nObj = nArr.getJSONObject(i)
                        repository.insertNote(Note(
                            documentId = nObj.optInt("documentId", 1),
                            pageNumber = nObj.optInt("pageNumber", 1),
                            text = nObj.optString("text", ""),
                            dateAdded = nObj.optString("dateAdded", "")
                        ))
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e("Backup", "Error importing: ${e.message}")
            false
        }
    }
}
