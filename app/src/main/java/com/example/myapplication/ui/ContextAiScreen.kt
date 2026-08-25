package com.example.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.example.myapplication.actions.ActionResult
import com.example.myapplication.actions.ActionType
import com.example.myapplication.actions.SuggestedAction
import com.example.myapplication.ai.LocalAIState
import com.example.myapplication.classifier.DetectedContentType
import com.example.myapplication.clipboard.ClipboardUiState
import com.example.myapplication.clipboard.ClipboardViewModel
import com.example.myapplication.clipboard.ClipboardViewModelFactory
import com.example.myapplication.ui.theme.MyApplicationTheme

import com.example.myapplication.repository.StudyRepository

@Composable
fun ContextAiRoute(
    onOpenLibrary: () -> Unit,
    repository: StudyRepository,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val application = context.applicationContext as android.app.Application
    val viewModel: ClipboardViewModel = viewModel(
        factory = ClipboardViewModelFactory(application, repository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current

    DisposableEffect(view, viewModel) {
        val listener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            viewModel.onWindowFocusChanged(hasFocus)
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
        viewModel.onWindowFocusChanged(view.hasWindowFocus())
        onDispose {
            if (view.viewTreeObserver.isAlive) {
                view.viewTreeObserver.removeOnWindowFocusChangeListener(listener)
            }
            viewModel.onWindowFocusChanged(false)
        }
    }

    ContextAiScreen(
        uiState = uiState,
        onMonitoringChange = viewModel::setMonitoringEnabled,
        onActionClick = viewModel::performAction,
        onDismissResult = viewModel::clearActionResult,
        onConfirmAiAction = viewModel::confirmAiAction,
        onCancelAiAction = viewModel::cancelAiAction,
        onRegenerate = viewModel::regenerateAction,
        onEdit = viewModel::startEditing,
        onSaveEdit = viewModel::saveEdit,
        onCancelEdit = viewModel::cancelEdit,
        onEditingTextChange = viewModel::updateEditingText,
        onCopyResult = viewModel::copyText,
        onAskFollowUp = viewModel::askFollowUp,
        onSaveStudyItem = viewModel::saveStudyItem,
        onOpenLibrary = onOpenLibrary,
        modifier = modifier
    )
}

@Composable
fun ContextAiScreen(
    uiState: ClipboardUiState,
    onMonitoringChange: (Boolean) -> Unit,
    onActionClick: (SuggestedAction) -> Unit,
    onDismissResult: () -> Unit,
    onConfirmAiAction: () -> Unit,
    onCancelAiAction: () -> Unit,
    onRegenerate: () -> Unit,
    onEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onEditingTextChange: (String) -> Unit,
    onCopyResult: (String) -> Unit,
    onAskFollowUp: (String) -> Unit,
    onSaveStudyItem: (content: String?, type: String?) -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = "ContextAI",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Contextual Clipboard Assistant",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onOpenLibrary) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Study Library")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        MonitoringCard(
            enabled = uiState.monitoringEnabled,
            onEnabledChange = onMonitoringChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        AiStatusCard(state = uiState.aiState)

        Spacer(modifier = Modifier.height(16.dp))

        LatestClipboardCard(
            previewText = uiState.latestClipboard,
            isTruncated = uiState.isTruncated,
            statusMessage = uiState.statusMessage
        )

        Spacer(modifier = Modifier.height(16.dp))

        ContentTypeCard(type = uiState.detectedContentType)

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.actionResult != null || uiState.isGenerating) {
            ActionResultCard(
                result = uiState.actionResult ?: ActionResult("AI Engine", "Processing..."),
                isGenerating = uiState.isGenerating,
                editingText = uiState.editingText,
                followUpResult = uiState.followUpResult,
                onDismiss = onDismissResult,
                onRegenerate = onRegenerate,
                onEdit = onEdit,
                onSaveEdit = onSaveEdit,
                onCancelEdit = onCancelEdit,
                onEditingTextChange = onEditingTextChange,
                onCopy = onCopyResult,
                onAskFollowUp = onAskFollowUp,
                onSave = { onSaveStudyItem(null, null) },
                onSaveFollowUp = { content -> onSaveStudyItem(content, "Follow-up") }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState.pendingAiAction != null) {
            SensitiveWarningCard(
                actionTitle = uiState.pendingAiAction.title,
                onConfirm = onConfirmAiAction,
                onCancel = onCancelAiAction
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Suggested actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = if (uiState.suggestedActions.isEmpty()) {
                "Copy something to see what you can do."
            } else {
                "Choose an action for your clipboard content."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            uiState.suggestedActions.forEach { action ->
                SuggestedActionCard(
                    title = action.title,
                    description = action.description ?: "",
                    icon = action.icon,
                    onClick = { onActionClick(action) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MonitoringCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Clipboard monitoring",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (enabled) "ON" else "OFF",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = "Reads clipboard only while ContextAI is open",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
        }
    }
}

@Composable
private fun LatestClipboardCard(
    previewText: String?,
    isTruncated: Boolean,
    statusMessage: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Latest clipboard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (previewText.isNullOrBlank()) {
                Text(
                    text = statusMessage ?: "Nothing copied yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis
                )
                if (isTruncated || statusMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusMessage ?: "Preview truncated",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentTypeCard(type: DetectedContentType) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Detected:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = type.emoji,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = type.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun AiStatusCard(state: LocalAIState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "AI Engine",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val dotColor = when (state) {
                    LocalAIState.Ready -> MaterialTheme.colorScheme.primary
                    LocalAIState.Generating, LocalAIState.Loading -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.outline
                }
                Surface(
                    modifier = Modifier.size(8.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = dotColor
                ) {}
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (state is LocalAIState.Error) "Error" else state.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state is LocalAIState.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
            if (state is LocalAIState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            if (state == LocalAIState.Ready || state == LocalAIState.Generating) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "AI processing happens on this device.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SensitiveWarningCard(
    actionTitle: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Sensitive content detected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This text may contain sensitive information. Do you want to process it with the on-device AI for '$actionTitle'?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Process Anyway", color = MaterialTheme.colorScheme.onError)
                }
            }
        }
    }
}

@Composable
private fun ActionResultCard(
    result: ActionResult,
    isGenerating: Boolean,
    editingText: String?,
    followUpResult: String?,
    onDismiss: () -> Unit,
    onRegenerate: () -> Unit,
    onEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onEditingTextChange: (String) -> Unit,
    onCopy: (String) -> Unit,
    onAskFollowUp: (String) -> Unit,
    onSave: () -> Unit,
    onSaveFollowUp: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (result.isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isGenerating) "Thinking locally..." else result.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (result.isError) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
                if (!isGenerating) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Dismiss",
                            color = if (result.isError) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (isGenerating) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (result.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else if (editingText != null) {
                OutlinedTextField(
                    value = editingText,
                    onValueChange = onEditingTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancelEdit) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onSaveEdit) {
                        Text("Save")
                    }
                }
            } else {
                Text(
                    text = result.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (result.isError) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )

                if (!result.isError) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (result.title in listOf("Explain", "Summarize", "Simplified", "Study Questions", "Flashcards", "Quiz")) {
                            TextButton(onClick = onSave) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        TextButton(onClick = onRegenerate) {
                            Text("Regenerate")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = onEdit) {
                            Text("Edit")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { onCopy(result.content) }) {
                            Text("Copy")
                        }
                    }

                    if (result.title in listOf("Explain", "Summarize", "Simplified", "Study Questions", "Flashcards", "Quiz", "Answer")) {
                        Spacer(modifier = Modifier.height(16.dp))
                        FollowUpSection(
                            followUpResult = followUpResult,
                            isGenerating = isGenerating,
                            onAsk = onAskFollowUp,
                            onSave = onSaveFollowUp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowUpSection(
    followUpResult: String?,
    isGenerating: Boolean,
    onAsk: (String) -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    
    Column {
        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Text(
            text = "Ask a follow-up:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("e.g. Can you explain the sunlight part again?") },
                textStyle = MaterialTheme.typography.bodyMedium,
                singleLine = true,
                enabled = !isGenerating
            )
            Spacer(modifier = Modifier.width(8.dp))
            androidx.compose.material3.IconButton(
                onClick = { 
                    onAsk(text)
                    text = ""
                },
                enabled = !isGenerating && text.isNotBlank()
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Ask"
                )
            }
        }
        if (followUpResult != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            ) {
                Text(
                    text = followUpResult,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onSave(followUpResult) }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Result", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestedActionCard(
    title: String,
    description: String,
    icon: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContextAiScreenPreview() {
    MyApplicationTheme {
        ContextAiScreen(
            uiState = ClipboardUiState(
                monitoringEnabled = true,
                latestClipboard = "https://www.google.com",
                detectedContentType = DetectedContentType.URL,
                suggestedActions = listOf(
                    SuggestedAction("open", "Open", "Open this link in a browser.", ActionType.OPEN_URL, "🌐"),
                    SuggestedAction("copy", "Copy", "Copy this URL again.", ActionType.COPY, "📋")
                )
            ),
            onMonitoringChange = {},
            onActionClick = {},
            onDismissResult = {},
            onConfirmAiAction = {},
            onCancelAiAction = {},
            onRegenerate = {},
            onEdit = {},
            onSaveEdit = {},
            onCancelEdit = {},
            onEditingTextChange = {},
            onCopyResult = {},
            onAskFollowUp = {},
            onSaveStudyItem = { _, _ -> },
            onOpenLibrary = {}
        )
    }
}
