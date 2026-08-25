package com.example.myapplication.clipboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.actions.ActionHandler
import com.example.myapplication.actions.ActionResult
import com.example.myapplication.actions.ActionSuggestionEngine
import com.example.myapplication.actions.SuggestedAction
import com.example.myapplication.ai.LocalAIEngine
import com.example.myapplication.ai.LocalAIState
import com.example.myapplication.ai.DefaultLocalAIEngine
import com.example.myapplication.ai.LocalModelManager
import com.example.myapplication.ai.SensitiveContentFilter
import com.example.myapplication.classifier.ContentClassifier
import com.example.myapplication.classifier.DetectedContentType
import com.example.myapplication.classifier.RuleBasedContentClassifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.example.myapplication.repository.StudyRepository
import com.example.myapplication.data.StudyItem

class ClipboardViewModel(
    application: Application,
    private val studyRepository: StudyRepository
) : AndroidViewModel(application) {
    private val classifier: ContentClassifier = RuleBasedContentClassifier()
    private val suggestionEngine: ActionSuggestionEngine = ActionSuggestionEngine()
    private val aiEngine: LocalAIEngine = DefaultLocalAIEngine(
        application.applicationContext,
        LocalModelManager(application.applicationContext)
    )
    private val monitor = ClipboardMonitor(application.applicationContext)
    private val actionHandler = ActionHandler(application.applicationContext, aiEngine)

    private val _uiState = MutableStateFlow(ClipboardUiState())
    val uiState: StateFlow<ClipboardUiState> = _uiState.asStateFlow()

    init {
        monitor.setCallback { result ->
            viewModelScope.launch {
                _uiState.update { current -> current.applyResult(result, classifier, suggestionEngine) }
            }
        }
        
        viewModelScope.launch {
            aiEngine.state.collectLatest { aiState ->
                _uiState.update { it.copy(aiState = aiState) }
            }
        }
    }

    fun setMonitoringEnabled(enabled: Boolean) {
        _uiState.update { it.copy(monitoringEnabled = enabled) }
        monitor.setMonitoringEnabled(enabled)
    }

    fun onWindowFocusChanged(hasFocus: Boolean) {
        monitor.setHasWindowFocus(hasFocus)
    }

    fun performAction(action: SuggestedAction) {
        val text = uiState.value.latestClipboard ?: return
        
        // AI actions require explicit confirmation if sensitive
        if (isAiAction(action) && SensitiveContentFilter.containsSensitiveInfo(text)) {
            _uiState.update { it.copy(pendingAiAction = action) }
            return
        }
        
        executeAction(action, text)
    }

    fun confirmAiAction() {
        val action = uiState.value.pendingAiAction ?: return
        val text = uiState.value.latestClipboard ?: return
        _uiState.update { it.copy(pendingAiAction = null) }
        executeAction(action, text)
    }

    fun cancelAiAction() {
        _uiState.update { it.copy(pendingAiAction = null) }
    }

    private fun executeAction(action: SuggestedAction, text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, lastAction = action, actionResult = null, editingText = null, followUpResult = null) }
            val result = actionHandler.handleAction(action, text)
            _uiState.update { it.copy(actionResult = result, isGenerating = false) }
        }
    }

    fun askFollowUp(question: String) {
        val originalText = uiState.value.latestClipboard ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            val answer = aiEngine.answerFollowUp(originalText, question)
            _uiState.update { it.copy(followUpResult = answer, isGenerating = false) }
        }
    }

    fun regenerateAction() {
        val action = uiState.value.lastAction ?: return
        val text = uiState.value.latestClipboard ?: return
        executeAction(action, text)
    }

    fun startEditing() {
        val currentText = uiState.value.actionResult?.content ?: return
        _uiState.update { it.copy(editingText = currentText) }
    }

    fun updateEditingText(newText: String) {
        _uiState.update { it.copy(editingText = newText) }
    }

    fun saveEdit() {
        val editedText = uiState.value.editingText ?: return
        _uiState.update { current ->
            current.copy(
                actionResult = current.actionResult?.copy(content = editedText),
                editingText = null
            )
        }
    }

    fun cancelEdit() {
        _uiState.update { it.copy(editingText = null) }
    }

    fun copyText(text: String) {
        viewModelScope.launch {
            actionHandler.handleAction(
                SuggestedAction("copy", "Copy", null, com.example.myapplication.actions.ActionType.COPY, "📋"),
                text
            )
        }
    }

    private fun isAiAction(action: SuggestedAction): Boolean {
        return action.type !in listOf(
            com.example.myapplication.actions.ActionType.COPY,
            com.example.myapplication.actions.ActionType.OPEN_URL,
            com.example.myapplication.actions.ActionType.CALL,
            com.example.myapplication.actions.ActionType.CALCULATE,
            com.example.myapplication.actions.ActionType.SHOW_STEPS,
            com.example.myapplication.actions.ActionType.COMPOSE_EMAIL
        )
    }

    fun clearActionResult() {
        _uiState.update { it.copy(actionResult = null) }
    }

    fun saveStudyItem(overrideContent: String? = null, overrideType: String? = null) {
        val state = uiState.value
        val result = state.actionResult
        val content = overrideContent ?: result?.content ?: return
        val type = overrideType ?: result?.title ?: "Study Note"
        val sourceText = state.latestClipboard ?: ""
        
        viewModelScope.launch {
            val title = generateTitle(sourceText, type)
            val item = StudyItem(
                title = title,
                type = type,
                sourceText = sourceText,
                generatedContent = content,
                quizScore = null
            )
            studyRepository.insertItem(item)
            _uiState.update { it.copy(statusMessage = "Saved to library!") }
        }
    }

    private fun generateTitle(sourceText: String, resultType: String): String {
        val baseTitle = if (sourceText.length <= 40) {
            sourceText.trim().removeSuffix("?")
        } else {
            sourceText.take(37).trim() + "..."
        }
        
        return when (resultType) {
            "Flashcards" -> "$baseTitle — Flashcards"
            "Quiz" -> "$baseTitle — Quiz"
            "Study Questions" -> "$baseTitle — Questions"
            else -> baseTitle
        }
    }

    override fun onCleared() {
        monitor.release()
        aiEngine.release()
        super.onCleared()
    }
}

data class ClipboardUiState(
    val monitoringEnabled: Boolean = false,
    val latestClipboard: String? = null,
    val isTruncated: Boolean = false,
    val statusMessage: String? = null,
    val detectedContentType: DetectedContentType = DetectedContentType.UNKNOWN,
    val suggestedActions: List<SuggestedAction> = emptyList(),
    val actionResult: ActionResult? = null,
    val aiState: LocalAIState = LocalAIState.Unavailable,
    val pendingAiAction: SuggestedAction? = null,
    val isGenerating: Boolean = false,
    val lastAction: SuggestedAction? = null,
    val editingText: String? = null,
    val followUpResult: String? = null
)

private fun ClipboardUiState.applyResult(
    result: ClipboardReadResult,
    classifier: ContentClassifier,
    suggestionEngine: ActionSuggestionEngine
): ClipboardUiState {
    return when (result) {
        is ClipboardReadResult.Text -> {
            val type = classifier.classify(result.preview.stripPreviewEllipsis(result.truncated))
            copy(
                latestClipboard = result.preview,
                isTruncated = result.truncated,
                statusMessage = if (result.truncated) {
                    "Showing the first ${ClipboardMonitor.PREVIEW_CHAR_LIMIT} characters"
                } else {
                    null
                },
                detectedContentType = type,
                suggestedActions = suggestionEngine.getSuggestions(type, result.preview.stripPreviewEllipsis(result.truncated)),
                actionResult = null // Clear previous result on new clipboard content
            )
        }
        ClipboardReadResult.Empty -> copy(
            latestClipboard = null,
            isTruncated = false,
            statusMessage = "Clipboard is empty",
            detectedContentType = DetectedContentType.UNKNOWN,
            suggestedActions = emptyList()
        )
        ClipboardReadResult.Unavailable -> {
            if (latestClipboard != null) {
                this
            } else {
                copy(
                    statusMessage = "Clipboard is unavailable. Copy text while ContextAI is open, or return to this app after copying.",
                    detectedContentType = DetectedContentType.UNKNOWN,
                    suggestedActions = emptyList()
                )
            }
        }
        ClipboardReadResult.Sensitive -> copy(
            latestClipboard = null,
            isTruncated = false,
            statusMessage = "Sensitive clipboard content is hidden",
            detectedContentType = DetectedContentType.UNKNOWN,
            suggestedActions = emptyList()
        )
    }
}

private fun String.stripPreviewEllipsis(truncated: Boolean): String {
    return if (truncated && endsWith("…")) dropLast(1) else this
}
