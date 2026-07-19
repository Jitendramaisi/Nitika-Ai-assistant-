package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.MessageEntity
import androidx.compose.foundation.BorderStroke
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OnGold
import com.example.ui.theme.ScholasticGold
import com.example.ui.theme.DeepAbyss
import com.example.ui.theme.OxfordBlue
import com.example.ui.theme.CardSlate
import com.example.ui.theme.SageGreen
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.LightIvory
import com.example.ui.theme.WarmGrey
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.ChatViewModelFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.launch

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    private val viewModel: ChatViewModel by viewModels {
        ChatViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                dynamicColor = false
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    NitikaAppScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun NitikaAppScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    var inputVal by remember { mutableStateOf("") }
    var showLibrary by remember { mutableStateOf(false) }
    var showTranscriptDialog by remember { mutableStateOf(false) }
    var showDashboard by remember { mutableStateOf(false) }
    var textToExport by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null && textToExport != null) {
            viewModel.exportPdf(context, uri, textToExport!!)
            textToExport = null
        }
    }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0)
            if (!spokenText.isNullOrBlank()) {
                viewModel.addResource(
                    title = "Voice Note",
                    url = "Local Transcription",
                    category = "Note",
                    description = spokenText
                )
                viewModel.sendMessage("I just recorded a voice note: \"$spokenText\"")
            }
        }
    }
    
    // Auto scroll to bottom when messages list changes
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val recommendedTopics = listOf(
        AcademicTopic("🍅 Pomodoro", "Pomodoro time management technique", "Study Skills"),
        AcademicTopic("🗣️ Feynman", "Feynman technique for learning", "Study Skills"),
        AcademicTopic("⚖️ Integrity", "Importance of academic integrity", "Ethics"),
        AcademicTopic("🔬 Consensus", "Scientific consensus explained", "Science"),
        AcademicTopic("🖥️ Recursion", "Recursion in programming", "Computer Science"),
        AcademicTopic("🌌 Quantum physics", "Schrodinger's Cat simply explained", "Physics"),
        AcademicTopic("📜 Silk Road", "Brief overview of the ancient Silk Road", "History")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Academic Top Bar Header
        AcademicHeader(
            isDarkTheme = isDarkTheme,
            onToggleTheme = { viewModel.toggleTheme() },
            onClearChat = { viewModel.clearChat() },
            onOpenLibrary = { showLibrary = true },
            onOpenDashboard = { showDashboard = true }
        )

        // Main Chat Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                // Empty loading or welcome state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ScholasticGold)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(messages) { message ->
                        if (message.isQuiz) {
                            QuizCardItem(
                                message = message,
                                onSelectOption = { selectedIndex ->
                                    viewModel.submitQuizAnswer(message, selectedIndex)
                                }
                            )
                        } else {
                            ChatBubbleItem(
                                message = message,
                                onExportPdf = { 
                                    textToExport = message.text
                                    exportPdfLauncher.launch("Lecture_Summary.pdf") 
                                }
                            )
                        }
                    }

                    if (isGenerating) {
                        item {
                            ProfessorThinkingIndicator()
                        }
                    }
                }
            }
        }

        // Horizontal Slider of Predefined Academic Subjects
        AnimatedVisibility(
            visible = !isGenerating,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Recommended Studies",
                    fontSize = 12.sp,
                    color = ScholasticGold,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
                    fontFamily = FontFamily.Serif
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recommendedTopics.forEach { topic ->
                        var showTopicDialog by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardSlate)
                                .border(1.dp, ScholasticGold.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .clickable { showTopicDialog = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column {
                                Text(
                                    text = topic.label,
                                    fontSize = 13.sp,
                                    color = LightIvory,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = topic.category,
                                    fontSize = 10.sp,
                                    color = WarmGrey
                                )
                            }
                        }

                        if (showTopicDialog) {
                            AlertDialog(
                                onDismissRequest = { showTopicDialog = false },
                                containerColor = CardSlate,
                                titleContentColor = ScholasticGold,
                                textContentColor = LightIvory,
                                title = {
                                    Text(
                                        text = "Explore ${topic.label}",
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                text = {
                                    Text(
                                        text = "Choose how you want to learn with Professor Nitika today regarding ${topic.label}.",
                                        fontSize = 14.sp
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        colors = ButtonDefaults.buttonColors(containerColor = ScholasticGold, contentColor = OnGold),
                                        onClick = {
                                            showTopicDialog = false
                                            viewModel.sendMessage(topic.query)
                                        }
                                    ) {
                                        Text("Ask Lecture")
                                    }
                                },
                                dismissButton = {
                                    FilledTonalButton(
                                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = OxfordBlue, contentColor = LightIvory),
                                        onClick = {
                                            showTopicDialog = false
                                            viewModel.generateQuiz(topic.label)
                                        }
                                    ) {
                                        Text("Take Quiz")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Bottom Input Console
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSlate)
                .navigationBarsPadding()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Voice Note Button
                IconButton(
                    onClick = {
                        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak now to record a voice note...")
                        }
                        try {
                            speechRecognizerLauncher.launch(intent)
                        } catch (e: Exception) {
                            // Intent not available
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(OxfordBlue)
                        .border(1.dp, ScholasticGold.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Record Voice Note",
                        tint = ScholasticGold
                    )
                }

                // Transcript Summary Button
                IconButton(
                    onClick = { showTranscriptDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(OxfordBlue)
                        .border(1.dp, ScholasticGold.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Paste Transcript",
                        tint = ScholasticGold
                    )
                }

                // Outlined Input Console Field
                OutlinedTextField(
                    value = inputVal,
                    onValueChange = { inputVal = it },
                    placeholder = {
                        Text(
                            "Consult Nitika or enter a topic...",
                            color = WarmGrey,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LightIvory,
                        unfocusedTextColor = LightIvory,
                        focusedBorderColor = ScholasticGold,
                        unfocusedBorderColor = WarmGrey.copy(alpha = 0.5f),
                        focusedContainerColor = DeepAbyss,
                        unfocusedContainerColor = DeepAbyss
                    ),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputVal.isNotBlank() && !isGenerating) {
                                viewModel.sendMessage(inputVal)
                                inputVal = ""
                                keyboardController?.hide()
                            }
                        }
                    )
                )

                // Ask Button
                IconButton(
                    onClick = {
                        if (inputVal.isNotBlank() && !isGenerating) {
                            viewModel.sendMessage(inputVal)
                            inputVal = ""
                            keyboardController?.hide()
                        }
                    },
                    enabled = inputVal.isNotBlank() && !isGenerating,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputVal.isNotBlank() && !isGenerating) ScholasticGold else CardSlate.copy(alpha = 0.5f)
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Ask",
                        tint = if (inputVal.isNotBlank() && !isGenerating) OnGold else WarmGrey
                    )
                }

                // Quiz Generator Action Button
                IconButton(
                    onClick = {
                        val topic = if (inputVal.isNotBlank()) inputVal else "General Academics"
                        viewModel.generateQuiz(topic)
                        inputVal = ""
                        keyboardController?.hide()
                    },
                    enabled = !isGenerating,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(OxfordBlue)
                        .border(1.dp, ScholasticGold.copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Generate Quiz",
                        tint = ScholasticGold
                    )
                }
            }
        }
    }

    if (showLibrary) {
        ResourceLibrarySheet(
            viewModel = viewModel,
            onDismiss = { showLibrary = false }
        )
    }

    if (showDashboard) {
        ProgressDashboardSheet(
            viewModel = viewModel,
            onDismiss = { showDashboard = false }
        )
    }

    if (showTranscriptDialog) {
        var transcriptText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showTranscriptDialog = false },
            containerColor = CardSlate,
            titleContentColor = ScholasticGold,
            textContentColor = LightIvory,
            title = {
                Text(
                    text = "Summarize Transcript",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Paste a lecture transcript below. Professor Nitika will generate concise bullet points and key takeaways.",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = transcriptText,
                        onValueChange = { transcriptText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LightIvory,
                            unfocusedTextColor = LightIvory,
                            focusedBorderColor = ScholasticGold,
                            unfocusedBorderColor = WarmGrey
                        ),
                        placeholder = { Text("Paste transcript here...", color = WarmGrey) }
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = ScholasticGold, contentColor = OnGold),
                    onClick = {
                        if (transcriptText.isNotBlank()) {
                            viewModel.summarizeTranscript(transcriptText)
                            showTranscriptDialog = false
                        }
                    },
                    enabled = transcriptText.isNotBlank() && !isGenerating
                ) {
                    Text("Summarize")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTranscriptDialog = false }) {
                    Text("Cancel", color = LightIvory)
                }
            }
        )
    }
}

data class AcademicTopic(val label: String, val query: String, val category: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicHeader(
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    onClearChat: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenDashboard: () -> Unit = {}
) {
    var showConfirmClear by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardSlate,
        tonalElevation = 4.dp,
        border = BorderStroke(width = 0.5.dp, color = ScholasticGold.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Glowing Custom Vector Professor Initials Badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(OxfordBlue, DeepAbyss)
                        )
                    )
                    .border(2.dp, ScholasticGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "N",
                    fontSize = 22.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = ScholasticGold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Nitika AI",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = ScholasticGold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SageGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Academic Advisor",
                        fontSize = 11.sp,
                        color = WarmGrey
                    )
                }
            }

            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(OxfordBlue.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Theme",
                    tint = LightIvory
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onOpenDashboard,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(OxfordBlue.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.InsertChart,
                    contentDescription = "Dashboard",
                    tint = LightIvory
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onOpenLibrary,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(OxfordBlue.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.CollectionsBookmark,
                    contentDescription = "Library",
                    tint = LightIvory
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { showConfirmClear = true },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DeepAbyss.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Study Session",
                    tint = LightIvory
                )
            }
        }
    }

    if (showConfirmClear) {
        AlertDialog(
            onDismissRequest = { showConfirmClear = false },
            containerColor = CardSlate,
            titleContentColor = ScholasticGold,
            textContentColor = LightIvory,
            title = {
                Text(
                    text = "Reset Study Session?",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will erase your entire chat and quiz log history with Professor Nitika. Do you wish to proceed?",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmClear = false
                        onClearChat()
                    }
                ) {
                    Text("Reset", color = CrimsonRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClear = false }) {
                    Text("Cancel", color = LightIvory)
                }
            }
        )
    }
}

@Composable
fun ChatBubbleItem(
    message: MessageEntity,
    onExportPdf: () -> Unit = {}
) {
    val isUser = message.sender == "user"
    val bubbleColor = if (isUser) OxfordBlue else CardSlate
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val isSummary = message.text.startsWith("Here is the summary of your transcript")

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            if (!isUser) {
                // Compact avatar badge beside model response
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp, end = 8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(OxfordBlue)
                        .border(1.dp, ScholasticGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "N",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = ScholasticGold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(bubbleColor)
                    .border(
                        width = 1.dp,
                        color = if (isUser) ScholasticGold.copy(alpha = 0.15f) else ScholasticGold.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .padding(14.dp)
            ) {
                Column {
                    // Styled academic reply
                    Text(
                        text = message.text,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = LightIvory,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    if (isSummary) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onExportPdf,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ScholasticGold
                            ),
                            border = BorderStroke(1.dp, ScholasticGold.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Export PDF", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Summary to PDF", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuizCardItem(
    message: MessageEntity,
    onSelectOption: (Int) -> Unit
) {
    val options = remember(message.quizOptionsJson) {
        try {
            val type = Types.newParameterizedType(List::class.java, String::class.java)
            Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
                .adapter<List<String>>(type)
                .fromJson(message.quizOptionsJson ?: "[]") ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    val isAnswered = message.quizSelectedAnswer != -1

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, ScholasticGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Card Title Label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Quiz,
                    contentDescription = "Quiz Alert",
                    tint = ScholasticGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ACADEMIC TEST",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScholasticGold,
                    letterSpacing = 1.sp
                )
            }

            // The Question
            Text(
                text = message.quizQuestion ?: "",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = LightIvory,
                lineHeight = 22.sp,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 4 Options
            options.forEachIndexed { index, option ->
                val isSelected = message.quizSelectedAnswer == index
                val isCorrect = message.quizCorrectIndex == index

                val optionBgColor by animateColorAsState(
                    targetValue = when {
                        !isAnswered -> DeepAbyss
                        isSelected && isCorrect -> SageGreen.copy(alpha = 0.2f)
                        isSelected && !isCorrect -> CrimsonRed.copy(alpha = 0.2f)
                        isCorrect -> SageGreen.copy(alpha = 0.2f)
                        else -> DeepAbyss
                    }
                )

                val optionBorderColor by animateColorAsState(
                    targetValue = when {
                        !isAnswered -> WarmGrey.copy(alpha = 0.3f)
                        isSelected && isCorrect -> SageGreen
                        isSelected && !isCorrect -> CrimsonRed
                        isCorrect -> SageGreen
                        else -> WarmGrey.copy(alpha = 0.2f)
                    }
                )

                val optionTextColor = when {
                    !isAnswered -> LightIvory
                    isCorrect -> SageGreen
                    isSelected && !isCorrect -> CrimsonRed
                    else -> WarmGrey
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(optionBgColor)
                        .border(1.dp, optionBorderColor, RoundedCornerShape(12.dp))
                        .clickable(enabled = !isAnswered) {
                            onSelectOption(index)
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${'A' + index}.  $option",
                            fontSize = 14.sp,
                            fontWeight = if (isSelected || (isAnswered && isCorrect)) FontWeight.Bold else FontWeight.Normal,
                            color = optionTextColor,
                            modifier = Modifier.weight(1f)
                        )

                        if (isAnswered) {
                            Icon(
                                imageVector = when {
                                    isCorrect -> Icons.Default.CheckCircle
                                    isSelected && !isCorrect -> Icons.Default.Cancel
                                    else -> Icons.Default.RadioButtonUnchecked
                                },
                                contentDescription = "Verification status",
                                tint = when {
                                    isCorrect -> SageGreen
                                    isSelected && !isCorrect -> CrimsonRed
                                    else -> WarmGrey.copy(alpha = 0.4f)
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Professor Explanation block
            AnimatedVisibility(visible = isAnswered) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(OxfordBlue.copy(alpha = 0.4f))
                        .border(1.dp, ScholasticGold.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Explanation",
                            tint = ScholasticGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Professor's Feedback",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ScholasticGold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = message.quizExplanation ?: "",
                        fontSize = 13.sp,
                        color = LightIvory,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ProfessorThinkingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(OxfordBlue)
                .border(1.dp, ScholasticGold, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "N",
                fontSize = 13.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = ScholasticGold
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSlate),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Professor Nitika is formulating lecture...",
                fontSize = 13.sp,
                fontFamily = FontFamily.Serif,
                color = ScholasticGold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}
