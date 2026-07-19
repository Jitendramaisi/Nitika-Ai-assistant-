package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.Part
import com.example.data.api.ResponseSchema
import com.example.data.api.RetrofitClient
import com.example.data.database.ChatDao
import com.example.data.database.MessageEntity
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
data class QuizItem(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

class ChatRepository(private val chatDao: ChatDao) {

    val allMessages: Flow<List<MessageEntity>> = chatDao.getAllMessagesFlow()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        
    private val quizListType = Types.newParameterizedType(List::class.java, QuizItem::class.java)
    private val quizListAdapter = moshi.adapter<List<QuizItem>>(quizListType)

    private val systemInstruction = Content(
        parts = listOf(
            Part(
                text = "You are Nitika, an exceptional, highly knowledgeable, friendly, and supportive virtual Professor. " +
                        "You specialize in explaining complex topics across all academic fields clearly, writing engaging quizzes " +
                        "to test understanding, and helping students learn with professional yet encouraging academic guidance. " +
                        "Keep your explanations structured, utilizing markdown (like bullet points, bold text, headers) for readability. " +
                        "Never output raw JSON in standard chats. Address the user politely, as a dedicated professor guiding a brilliant student.\n\n" +
                        com.example.data.api.KnowledgeBase.TRUSTED_SOURCES_CONTEXT
            )
        )
    )

    suspend fun sendMessage(userText: String, previousMessages: List<MessageEntity>) = withContext(Dispatchers.IO) {
        // 1. Save user's message
        val userMessage = MessageEntity(
            sender = "user",
            text = userText,
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(userMessage)

        // 2. Prepare conversation history for Gemini context
        val apiContents = mutableListOf<Content>()
        
        // Take last 10 messages for context to keep within limits
        previousMessages.takeLast(10).forEach { msg ->
            if (!msg.isQuiz) { // Chat only
                apiContents.add(
                    Content(
                        role = if (msg.sender == "user") "user" else "model",
                        parts = listOf(Part(text = msg.text))
                    )
                )
            }
        }
        
        // Append current message
        apiContents.add(
            Content(
                role = "user",
                parts = listOf(Part(text = userText))
            )
        )

        // 3. Make Gemini API Call
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                val errorMsg = MessageEntity(
                    sender = "professor",
                    text = "Hello! I am Professor Nitika. It seems my API key is not configured yet. Please configure the GEMINI_API_KEY in the Secrets Panel of Google AI Studio so we can begin our lectures!",
                    timestamp = System.currentTimeMillis()
                )
                chatDao.insertMessage(errorMsg)
                return@withContext
            }

            val request = GenerateContentRequest(
                contents = apiContents,
                systemInstruction = systemInstruction,
                generationConfig = GenerationConfig(
                    temperature = 0.7f
                )
            )

            val response = RetrofitClient.service.streamGenerateContent(
                model = "gemini-3.5-flash",
                apiKey = apiKey,
                request = request
            )

            var messageId: Long = -1
            var accumulatedText = ""

            val reader = com.squareup.moshi.JsonReader.of(response.source())
            val responseAdapter = moshi.adapter(com.example.data.api.GenerateContentResponse::class.java)

            reader.beginArray()
            while (reader.hasNext()) {
                val chunk = responseAdapter.fromJson(reader)
                val chunkText = chunk?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (chunkText != null) {
                    accumulatedText += chunkText
                    if (messageId == -1L) {
                        val initialMsg = MessageEntity(
                            sender = "professor",
                            text = accumulatedText,
                            timestamp = System.currentTimeMillis()
                        )
                        messageId = chatDao.insertMessage(initialMsg)
                    } else {
                        val updatedMsg = MessageEntity(
                            id = messageId.toInt(),
                            sender = "professor",
                            text = accumulatedText,
                            timestamp = System.currentTimeMillis()
                        )
                        chatDao.updateMessage(updatedMsg)
                    }
                }
            }
            reader.endArray()

            if (messageId == -1L) {
                // In case it didn't stream any text
                val professorMessage = MessageEntity(
                    sender = "professor",
                    text = "I apologize, student, but I was unable to compile a response. Could you rephrase your query?",
                    timestamp = System.currentTimeMillis()
                )
                chatDao.insertMessage(professorMessage)
            }

        } catch (e: Exception) {
            val errMessage = MessageEntity(
                sender = "professor",
                text = "Class has been temporarily interrupted due to a connection issue: ${e.localizedMessage}. Please ensure your network is stable and retry.",
                timestamp = System.currentTimeMillis()
            )
            chatDao.insertMessage(errMessage)
        }
    }

    suspend fun generateQuiz(topic: String) = withContext(Dispatchers.IO) {
        // 1. Add system quiz user trigger notification to history
        val userTriggerMsg = MessageEntity(
            sender = "user",
            text = "Professor, please test me on: $topic",
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(userTriggerMsg)

        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                val errorMsg = MessageEntity(
                    sender = "professor",
                    text = "I cannot generate a quiz because my credentials are missing. Please add the GEMINI_API_KEY to the Secrets panel in AI Studio.",
                    timestamp = System.currentTimeMillis()
                )
                chatDao.insertMessage(errorMsg)
                return@withContext
            }

            // Define Schema for structured quiz response
            val quizSchema = ResponseSchema(
                type = "ARRAY",
                items = ResponseSchema(
                    type = "OBJECT",
                    properties = mapOf(
                        "question" to ResponseSchema(type = "STRING", description = "The question text"),
                        "options" to ResponseSchema(type = "ARRAY", items = ResponseSchema(type = "STRING")),
                        "correctIndex" to ResponseSchema(type = "INTEGER", description = "The 0-based index of the correct answer (0 to 3)"),
                        "explanation" to ResponseSchema(type = "STRING", description = "A detailed explanation of why the correct option is right")
                    ),
                    required = listOf("question", "options", "correctIndex", "explanation")
                )
            )

            val prompt = "Create a high-quality 3-question multiple choice academic quiz testing knowledge about: $topic. Each question must have exactly 4 choices."
            
            val request = GenerateContentRequest(
                contents = listOf(
                    Content(parts = listOf(Part(text = prompt)))
                ),
                systemInstruction = systemInstruction,
                generationConfig = GenerationConfig(
                    temperature = 0.5f,
                    responseMimeType = "application/json",
                    responseSchema = quizSchema
                )
            )

            val response = RetrofitClient.service.generateContent(
                model = "gemini-3.5-flash",
                apiKey = apiKey,
                request = request
            )

            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                val quizzes = quizListAdapter.fromJson(jsonText)
                if (quizzes != null && quizzes.isNotEmpty()) {
                    // Let professor introduce the quiz
                    val introMessage = MessageEntity(
                        sender = "professor",
                        text = "Excellent! Let us test your knowledge on **$topic**. I have prepared a tailored 3-question assessment for you. Good luck, student!",
                        timestamp = System.currentTimeMillis()
                    )
                    chatDao.insertMessage(introMessage)

                    // Insert quiz items
                    quizzes.forEach { quiz ->
                        val optionsJson = moshi.adapter<List<String>>(
                            Types.newParameterizedType(List::class.java, String::class.java)
                        ).toJson(quiz.options)

                        val quizMessage = MessageEntity(
                            sender = "professor",
                            text = "Quiz: ${quiz.question}",
                            isQuiz = true,
                            quizQuestion = quiz.question,
                            quizOptionsJson = optionsJson,
                            quizCorrectIndex = quiz.correctIndex,
                            quizExplanation = quiz.explanation,
                            quizSelectedAnswer = -1,
                            timestamp = System.currentTimeMillis()
                        )
                        chatDao.insertMessage(quizMessage)
                    }
                } else {
                    throw Exception("Could not compile quiz items from the generated response.")
                }
            } else {
                throw Exception("Response returned empty.")
            }

        } catch (e: Exception) {
            val errMessage = MessageEntity(
                sender = "professor",
                text = "I encountered an error trying to write your quiz: ${e.localizedMessage}. Let us retry or test another subject!",
                timestamp = System.currentTimeMillis()
            )
            chatDao.insertMessage(errMessage)
        }
    }

    suspend fun updateMessage(message: MessageEntity) = withContext(Dispatchers.IO) {
        chatDao.updateMessage(message)
    }

    suspend fun clearChat() = withContext(Dispatchers.IO) {
        chatDao.clearHistory()
    }

    suspend fun summarizeTranscript(transcript: String) = withContext(Dispatchers.IO) {
        val userTriggerMsg = MessageEntity(
            sender = "user",
            text = "Professor, please summarize this transcript for me.",
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(userTriggerMsg)

        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                val errorMsg = MessageEntity(
                    sender = "professor",
                    text = "I cannot summarize this because my credentials are missing. Please add the GEMINI_API_KEY to the Secrets panel in AI Studio.",
                    timestamp = System.currentTimeMillis()
                )
                chatDao.insertMessage(errorMsg)
                return@withContext
            }

            val prompt = "Please summarize the following lecture transcript into concise bullet points and highlight the key takeaways:\n\n$transcript"
            
            val request = GenerateContentRequest(
                contents = listOf(
                    Content(parts = listOf(Part(text = prompt)))
                ),
                systemInstruction = systemInstruction,
                generationConfig = GenerationConfig(
                    temperature = 0.5f
                )
            )

            val response = RetrofitClient.service.streamGenerateContent(
                model = "gemini-3.5-flash",
                apiKey = apiKey,
                request = request
            )

            var messageId: Long = -1
            var accumulatedText = ""

            val reader = com.squareup.moshi.JsonReader.of(response.source())
            val responseAdapter = moshi.adapter(com.example.data.api.GenerateContentResponse::class.java)

            reader.beginArray()
            while (reader.hasNext()) {
                val chunk = responseAdapter.fromJson(reader)
                val chunkText = chunk?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (chunkText != null) {
                    accumulatedText += chunkText
                    if (messageId == -1L) {
                        val initialMsg = MessageEntity(
                            sender = "professor",
                            text = "Here is the summary of your transcript:\n\n$accumulatedText",
                            timestamp = System.currentTimeMillis()
                        )
                        messageId = chatDao.insertMessage(initialMsg)
                    } else {
                        val updatedMsg = MessageEntity(
                            id = messageId.toInt(),
                            sender = "professor",
                            text = "Here is the summary of your transcript:\n\n$accumulatedText",
                            timestamp = System.currentTimeMillis()
                        )
                        chatDao.updateMessage(updatedMsg)
                    }
                }
            }
            reader.endArray()

            if (messageId == -1L) {
                // In case it didn't stream any text
                val professorMessage = MessageEntity(
                    sender = "professor",
                    text = "I apologize, student, but I was unable to compile a summary.",
                    timestamp = System.currentTimeMillis()
                )
                chatDao.insertMessage(professorMessage)
            }

        } catch (e: Exception) {
            val errMessage = MessageEntity(
                sender = "professor",
                text = "I encountered an error trying to summarize your transcript: ${e.localizedMessage}",
                timestamp = System.currentTimeMillis()
            )
            chatDao.insertMessage(errMessage)
        }
    }
}
