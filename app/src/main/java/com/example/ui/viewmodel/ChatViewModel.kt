package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.ChatDatabase
import com.example.data.database.MessageEntity
import com.example.data.database.ResourceEntity
import com.example.data.repository.ChatRepository
import com.example.data.repository.ResourceRepository
import com.example.data.repository.TrackingRepository
import com.example.data.database.QuizPerformanceEntity
import com.example.data.database.StudySessionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import android.content.Context
import android.net.Uri
import android.graphics.pdf.PdfDocument
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.text.Layout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ChatDatabase.getDatabase(application)
    private val repository = ChatRepository(database.chatDao())
    private val resourceRepository = ResourceRepository(database.resourceDao())
    private val trackingRepository = TrackingRepository(database.trackingDao())

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    // Observe all messages from DB
    val messages: StateFlow<List<MessageEntity>> = repository.allMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val resources: StateFlow<List<ResourceEntity>> = resourceRepository.allResources
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val studySessions: StateFlow<List<StudySessionEntity>> = trackingRepository.allStudySessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val quizPerformances: StateFlow<List<QuizPerformanceEntity>> = trackingRepository.allQuizPerformance
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Pre-populate with a friendly welcome message if database is empty
        viewModelScope.launch {
            repository.allMessages.collect { list ->
                if (list.isEmpty()) {
                    val welcomeMsg = MessageEntity(
                        sender = "professor",
                        text = "Greetings, student! I am Professor Nitika, your dedicated academic assistant. " +
                                "I am here to help you study complex subjects, explain difficult theories, or guide you through interactive quizzes.\n\n" +
                                "What subject or topic would you like to explore today? Feel free to ask a question, select a recommended subject below, or trigger a custom quiz!",
                        timestamp = System.currentTimeMillis()
                    )
                    database.chatDao().insertMessage(welcomeMsg)
                }
            }
        }
        
        viewModelScope.launch {
            resourceRepository.allResources.collect { list ->
                if (list.isEmpty()) {
                    resourceRepository.addResource(
                        title = "Kali Linux & Python Install Guide",
                        url = "Local Note",
                        category = "Cheatsheet",
                        description = "Kali Linux WSL Install: wsl --install -d kali-linux\n\nPython Install (Debian/Kali): sudo apt update && sudo apt install python3 python3-pip"
                    )
                }
            }
        }
        
        // Pre-populate mock tracking data for dashboard visualization if empty
        viewModelScope.launch {
            trackingRepository.allStudySessions.collect { list ->
                if (list.isEmpty()) {
                    trackingRepository.addStudySession("Physics", 45)
                    trackingRepository.addStudySession("Chemistry", 30)
                    trackingRepository.addStudySession("Biology", 60)
                    trackingRepository.addStudySession("Math", 120)
                }
            }
        }
        
        viewModelScope.launch {
            trackingRepository.allQuizPerformance.collect { list ->
                if (list.isEmpty()) {
                    trackingRepository.addQuizPerformance("Physics", 75f)
                    trackingRepository.addQuizPerformance("Chemistry", 85f)
                    trackingRepository.addQuizPerformance("Biology", 90f)
                    trackingRepository.addQuizPerformance("Math", 65f)
                    trackingRepository.addQuizPerformance("Physics", 80f)
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                repository.sendMessage(text, messages.value)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun generateQuiz(topic: String) {
        if (topic.isBlank()) return
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                repository.generateQuiz(topic)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun submitQuizAnswer(message: MessageEntity, selectedIndex: Int) {
        viewModelScope.launch {
            val updatedMsg = message.copy(quizSelectedAnswer = selectedIndex)
            repository.updateMessage(updatedMsg)
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChat()
        }
    }

    fun summarizeTranscript(transcript: String) {
        if (transcript.isBlank()) return
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                repository.summarizeTranscript(transcript)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun addResource(title: String, url: String, category: String, description: String) {
        viewModelScope.launch {
            resourceRepository.addResource(title, url, category, description)
        }
    }

    fun deleteResource(id: Int) {
        viewModelScope.launch {
            resourceRepository.deleteResource(id)
        }
    }

    fun exportPdf(context: Context, uri: Uri, text: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val document = PdfDocument()
                    val textPaint = TextPaint().apply {
                        isAntiAlias = true
                        textSize = 14f
                        color = android.graphics.Color.BLACK
                        typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                    }

                    val pageWidth = 595
                    val pageHeight = 842
                    val margin = 50
                    val textWidth = pageWidth - 2 * margin
                    val maxTextHeight = pageHeight - 2 * margin
                    
                    val formattedText = text.replace("Here is the summary of your transcript:\n\n", "")

                    val staticLayout = StaticLayout.Builder.obtain(formattedText, 0, formattedText.length, textPaint, textWidth)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(1.0f, 1.0f)
                        .setIncludePad(false)
                        .build()

                    var currentLine = 0
                    val totalLines = staticLayout.lineCount
                    var pageNumber = 1

                    while (currentLine < totalLines) {
                        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        val page = document.startPage(pageInfo)
                        val canvas = page.canvas

                        val startY = staticLayout.getLineTop(currentLine)
                        var endLine = currentLine
                        while (endLine < totalLines && staticLayout.getLineBottom(endLine) - startY < maxTextHeight) {
                            endLine++
                        }

                        canvas.save()
                        canvas.translate(margin.toFloat(), margin.toFloat() - startY.toFloat())
                        canvas.clipRect(0, startY, textWidth, startY + maxTextHeight)
                        staticLayout.draw(canvas)
                        canvas.restore()

                        document.finishPage(page)
                        currentLine = endLine
                        pageNumber++
                    }

                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        document.writeTo(outputStream)
                    }
                    document.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

class ChatViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
