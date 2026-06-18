package com.example.data

import kotlinx.coroutines.flow.Flow

class DastoorRepository(private val db: DastoorDatabase) {

    // --- Documents ---
    val allDocuments: Flow<List<Document>> = db.documentDao().getAllDocuments()
    val favoriteDocuments: Flow<List<Document>> = db.documentDao().getFavoriteDocuments()
    
    fun getDocumentById(id: Int): Flow<Document?> = db.documentDao().getDocumentById(id)
    suspend fun getDocumentByIdSync(id: Int): Document? = db.documentDao().getDocumentByIdSync(id)
    
    fun getDocumentsByCategory(categoryId: Int): Flow<List<Document>> = 
        db.documentDao().getDocumentsByCategory(categoryId)
        
    fun searchDocuments(query: String): Flow<List<Document>> = 
        db.documentDao().searchDocuments(query)

    suspend fun insertDocument(document: Document): Long = db.documentDao().insertDocument(document)
    suspend fun updateDocument(document: Document) = db.documentDao().updateDocument(document)
    suspend fun deleteDocument(document: Document) = db.documentDao().deleteDocument(document)

    // --- Categories ---
    val allCategories: Flow<List<Category>> = db.categoryDao().getAllCategories()
    suspend fun insertCategory(category: Category): Long = db.categoryDao().insertCategory(category)
    suspend fun deleteCategory(category: Category) = db.categoryDao().deleteCategory(category)

    // --- Tags ---
    val allTags: Flow<List<Tag>> = db.tagDao().getAllTags()
    suspend fun insertTag(tag: Tag): Long = db.tagDao().insertTag(tag)
    suspend fun deleteTag(tag: Tag) = db.tagDao().deleteTag(tag)

    // --- Bookmarks ---
    val allBookmarks: Flow<List<Bookmark>> = db.bookmarkDao().getAllBookmarks()
    fun getBookmarksForDocument(documentId: Int): Flow<List<Bookmark>> = 
        db.bookmarkDao().getBookmarksForDocument(documentId)
    suspend fun insertBookmark(bookmark: Bookmark): Long = db.bookmarkDao().insertBookmark(bookmark)
    suspend fun deleteBookmark(bookmark: Bookmark) = db.bookmarkDao().deleteBookmark(bookmark)

    // --- Notes ---
    val allNotes: Flow<List<Note>> = db.noteDao().getAllNotes()
    fun getNotesForDocument(documentId: Int): Flow<List<Note>> = 
        db.noteDao().getNotesForDocument(documentId)
    suspend fun getNoteByPage(documentId: Int, page: Int): Note? = 
        db.noteDao().getNoteByPage(documentId, page)
    suspend fun insertNote(note: Note): Long = db.noteDao().insertNote(note)
    suspend fun deleteNote(note: Note) = db.noteDao().deleteNote(note)

    // --- Highlights ---
    fun getHighlightsForDocument(documentId: Int): Flow<List<Highlight>> = 
        db.highlightDao().getHighlightsForDocument(documentId)
    suspend fun insertHighlight(highlight: Highlight): Long = db.highlightDao().insertHighlight(highlight)
    suspend fun deleteHighlight(highlight: Highlight) = db.highlightDao().deleteHighlight(highlight)

    // --- Reading History ---
    val readingHistory: Flow<List<ReadingHistory>> = db.readingHistoryDao().getReadingHistory()
    val totalDuration: Flow<Long?> = db.readingHistoryDao().getTotalDuration()
    suspend fun insertReadingHistory(history: ReadingHistory): Long = db.readingHistoryDao().insertReadingHistory(history)
    suspend fun clearHistory() = db.readingHistoryDao().clearHistory()

    // --- User Settings ---
    val userSettings: Flow<UserSettings?> = db.userSettingsDao().getUserSettings()
    suspend fun saveUserSettings(settings: UserSettings) = db.userSettingsDao().saveUserSettings(settings)
}
