package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.ChatViewModel
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis

import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressDashboardSheet(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val studySessions by viewModel.studySessions.collectAsState()
    val quizPerformances by viewModel.quizPerformances.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DeepAbyss,
        contentColor = LightIvory,
        dragHandle = { BottomSheetDefaults.DragHandle(color = WarmGrey) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp)
        ) {
            Text(
                text = "Progress Dashboard",
                fontSize = 24.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = ScholasticGold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    DashboardSectionTitle(title = "Study Time (Minutes)")
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (studySessions.isNotEmpty()) {
                        val sessionDurations = studySessions.map { it.durationMinutes }
                        val studyModel = entryModelOf(*sessionDurations.toTypedArray())
                        
                        val studySubjectsFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                            val index = value.toInt()
                            if (index >= 0 && index < studySessions.size) {
                                studySessions[index].subject.take(3).uppercase()
                            } else ""
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                            Chart(
                                chart = columnChart(),
                                model = studyModel,
                                startAxis = rememberStartAxis(),
                                bottomAxis = rememberBottomAxis(valueFormatter = studySubjectsFormatter)
                            )
                        }
                    } else {
                        EmptyStateMessage("No study sessions recorded yet.")
                    }
                }

                item {
                    DashboardSectionTitle(title = "Quiz Performance (%)")
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (quizPerformances.isNotEmpty()) {
                        val scores = quizPerformances.map { it.scorePercentage }
                        val quizModel = entryModelOf(*scores.toTypedArray())
                        
                        val quizSubjectsFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                            val index = value.toInt()
                            if (index >= 0 && index < quizPerformances.size) {
                                quizPerformances[index].subject.take(3).uppercase()
                            } else ""
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                            Chart(
                                chart = lineChart(),
                                model = quizModel,
                                startAxis = rememberStartAxis(),
                                bottomAxis = rememberBottomAxis(valueFormatter = quizSubjectsFormatter)
                            )
                        }
                    } else {
                        EmptyStateMessage("No quiz performances recorded yet.")
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = WarmGrey,
        letterSpacing = 1.sp
    )
}

@Composable
fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, color = WarmGrey, fontSize = 14.sp)
    }
}
