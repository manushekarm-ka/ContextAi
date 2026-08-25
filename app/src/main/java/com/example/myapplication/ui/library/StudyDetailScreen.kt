package com.example.myapplication.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.StudyItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyDetailScreen(
    item: StudyItem,
    onBack: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onQuizScoreUpdate: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.type) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onFavoriteToggle) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Source Text:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = item.sourceText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Generated Content:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            StudyContentDisplay(item, onQuizScoreUpdate)
        }
    }
}

@Composable
private fun StudyContentDisplay(item: StudyItem, onQuizScoreUpdate: (String) -> Unit) {
    when (item.type) {
        "Flashcards" -> FlashcardsDisplay(item.generatedContent)
        "Quiz" -> QuizDisplay(item.generatedContent, item.quizScore, onQuizScoreUpdate)
        else -> Text(
            text = item.generatedContent,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun FlashcardsDisplay(content: String) {
    val cards = content.split("CARD").filter { it.isNotBlank() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cards.forEachIndexed { index, cardText ->
            val lines = cardText.trim().lines()
            val question = lines.find { it.startsWith("Q:") }?.removePrefix("Q:")?.trim() ?: ""
            val answer = lines.find { it.startsWith("A:") }?.removePrefix("A:")?.trim() ?: ""
            
            if (question.isNotBlank()) {
                var revealed by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                Card(
                    onClick = { revealed = !revealed },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Card ${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = question,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (revealed) {
                            androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                text = answer,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            Text(
                                text = "Tap to reveal answer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizDisplay(content: String, score: String?, onScoreUpdate: (String) -> Unit) {
    val quizData = remember(content) { parseQuiz(content) }
    var currentScore by remember { mutableIntStateOf(0) }
    var questionsAnswered by remember { mutableIntStateOf(0) }

    val onAnswer = { isCorrect: Boolean ->
        if (questionsAnswered < quizData.size) {
            if (isCorrect) currentScore++
            questionsAnswered++
            if (questionsAnswered == quizData.size) {
                onScoreUpdate("$currentScore/${quizData.size}")
            }
        }
    }

    if (score != null && questionsAnswered == 0) {
        Text(
            text = "Previous Score: $score",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (questionsAnswered == quizData.size && quizData.isNotEmpty()) {
        Text(
            text = "Your Score: $currentScore / ${quizData.size}",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Button(
            onClick = { 
                currentScore = 0
                questionsAnswered = 0
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Retake Quiz")
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        quizData.forEachIndexed { index, data ->
            QuizQuestionCard(
                index = index,
                data = data,
                onAnswerSelected = onAnswer
            )
        }
    }
}

private data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctLetter: String
)

private fun parseQuiz(content: String): List<QuizQuestion> {
    return content.split(Regex("Q\\d+:")).filter { it.isNotBlank() }.map { qText ->
        val lines = qText.trim().lines()
        val question = lines.getOrNull(0)?.trim() ?: ""
        val options = lines.filter { it.matches(Regex("^[A-D]\\).*")) }
        val correctLine = lines.find { it.startsWith("Correct:", ignoreCase = true) }
        val correctLetter = correctLine?.substringAfter(":")?.trim()?.take(1) ?: ""
        QuizQuestion(question, options, correctLetter)
    }.filter { it.question.isNotBlank() }
}

@Composable
private fun QuizQuestionCard(
    index: Int,
    data: QuizQuestion,
    onAnswerSelected: (Boolean) -> Unit
) {
    var selectedOption by remember { mutableStateOf<String?>(null) }
    val isCorrect = selectedOption?.startsWith(data.correctLetter) == true
    val isAnswered = selectedOption != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !isAnswered -> MaterialTheme.colorScheme.surfaceVariant
                isCorrect -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Question ${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = data.question,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            data.options.forEach { option ->
                val letter = option.take(1)
                val isThisSelected = selectedOption == option
                
                Surface(
                    onClick = {
                        if (!isAnswered) {
                            selectedOption = option
                            onAnswerSelected(letter == data.correctLetter)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isThisSelected) {
                        if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = option,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isThisSelected) {
                            if (isCorrect) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (isAnswered && !isCorrect) {
                Text(
                    text = "Correct answer: ${data.correctLetter}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
