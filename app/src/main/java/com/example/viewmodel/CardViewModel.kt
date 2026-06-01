package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.repository.CardRepository
import com.example.utils.GeminiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class CardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CardRepository
    val prefs = PreferencesManager(application)

    // Reactive list of saved cards
    val savedCards: StateFlow<List<UserCard>>

    // Current card being modified inside the editor
    private val _editingCard = MutableStateFlow<UserCard?>(null)
    val editingCard: StateFlow<UserCard?> = _editingCard.asStateFlow()

    // Undo / Redo Stacks
    private val undoStack = java.util.Stack<UserCard>()
    private val redoStack = java.util.Stack<UserCard>()

    // AI Generation States
    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    private val _aiSuccess = MutableStateFlow(false)
    val aiSuccess: StateFlow<Boolean> = _aiSuccess.asStateFlow()

    // Preferences-based states for reactive UI
    private val _isUserPremium = MutableStateFlow(prefs.accountType == "Premium")
    val isUserPremium: StateFlow<Boolean> = _isUserPremium.asStateFlow()
    
    private val _activeTheme = MutableStateFlow(prefs.themeMode)
    val activeTheme: StateFlow<String> = _activeTheme.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = CardRepository(database.cardDao())
        
        savedCards = repository.allCards
            .flowOn(Dispatchers.IO)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    // AUTH CONTEXTS
    fun loginMock(name: String, email: String) {
        prefs.isLoggedIn = true
        prefs.userName = name
        prefs.userEmail = email
        prefs.accountType = "Free"
        prefs.subscriptionPlan = "None"
        _isUserPremium.value = false
    }

    fun loginGoogle(name: String, email: String, photoUrl: String?) {
        prefs.isLoggedIn = true
        prefs.userName = name
        prefs.userEmail = email
        if (photoUrl != null) {
            prefs.userPhoto = photoUrl
        }
        prefs.accountType = "Free"
        prefs.subscriptionPlan = "None"
        _isUserPremium.value = false
    }

    fun loginApple(name: String, email: String, photoUrl: String? = null) {
        prefs.isLoggedIn = true
        prefs.userName = name
        prefs.userEmail = email
        prefs.userPhoto = photoUrl ?: "apple_avatar"
        prefs.accountType = "Free"
        prefs.subscriptionPlan = "None"
        _isUserPremium.value = false
    }

    fun loginGuest() {
        prefs.isLoggedIn = true
        prefs.userName = "Guest Builder"
        prefs.userEmail = "guest@pillaiplay.com"
        prefs.accountType = "Free"
        prefs.subscriptionPlan = "None"
        _isUserPremium.value = false
    }

    fun logout() {
        prefs.clearAuth()
        _isUserPremium.value = false
    }

    fun upgradeSubscription(plan: String) {
        prefs.subscriptionPlan = plan
        _isUserPremium.value = true
    }

    fun downgradeToFree() {
        prefs.subscriptionPlan = "None"
        prefs.accountType = "Free"
        _isUserPremium.value = false
    }

    fun changeTheme(mode: String) {
        prefs.themeMode = mode
        _activeTheme.value = mode
    }

    // EDITOR ENGINE & HISTORY CONTROLS
    fun selectCardForEditing(card: UserCard) {
        _editingCard.value = card
        undoStack.clear()
        redoStack.clear()
    }

    fun createNewCardProject(name: String, templateId: String = "vibe_modern_gold") {
        viewModelScope.launch(Dispatchers.IO) {
            val newProject = UserCard(
                cardName = name,
                templateId = templateId,
                isPremium = templateId.contains("premium"),
                lastUpdated = System.currentTimeMillis()
            )
            val generatedId = repository.saveCard(newProject)
            val insertedProject = newProject.copy(id = generatedId.toInt())
            _editingCard.value = insertedProject
            undoStack.clear()
            redoStack.clear()
        }
    }

    // Call when editing elements to push current state into undo stack
    fun applyCardEdit(modified: UserCard) {
        val current = _editingCard.value ?: return
        
        // Push current onto undo stack before modifying
        undoStack.push(current.copy())
        redoStack.clear() // Clear redo since we entered a new manual brush
        
        _editingCard.value = modified
        
        // Non-blocking auto-save to Database
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCard(modified.copy(lastUpdated = System.currentTimeMillis()))
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val current = _editingCard.value ?: return
            redoStack.push(current.copy()) // Push current to redo
            
            val previousState = undoStack.pop()
            _editingCard.value = previousState
            
            viewModelScope.launch(Dispatchers.IO) {
                repository.updateCard(previousState.copy(lastUpdated = System.currentTimeMillis()))
            }
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val current = _editingCard.value ?: return
            undoStack.push(current.copy()) // Push current to undo
            
            val nextState = redoStack.pop()
            _editingCard.value = nextState
            
            viewModelScope.launch(Dispatchers.IO) {
                repository.updateCard(nextState.copy(lastUpdated = System.currentTimeMillis()))
            }
        }
    }

    fun hasUndo(): Boolean = undoStack.isNotEmpty()
    fun hasRedo(): Boolean = redoStack.isNotEmpty()

    // CARD CRUD WRAPPERS
    fun updateEditingCardName(newName: String) {
        val current = _editingCard.value ?: return
        applyCardEdit(current.copy(cardName = newName))
    }

    fun duplicateCard(card: UserCard) {
        viewModelScope.launch(Dispatchers.IO) {
            val cloned = card.copy(
                id = 0, // Reset PrimaryKey to autoGenerate
                cardName = "${card.cardName} (Copy)",
                lastUpdated = System.currentTimeMillis()
            )
            repository.saveCard(cloned)
        }
    }

    fun deleteCard(card: UserCard) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCard(card)
            if (_editingCard.value?.id == card.id) {
                _editingCard.value = null
            }
        }
    }

    // AI CARD GENERATION CLIENT
    fun triggerAICardGeneration(name: String, businessType: String, companyName: String) {
        _aiLoading.value = true
        _aiError.value = null
        _aiSuccess.value = false

        GeminiService.generateCardLayout(
            name = name,
            businessType = businessType,
            companyName = companyName,
            callback = object : GeminiService.AIAssistantCallback {
                override fun onSuccess(
                    themeName: String,
                    backgroundColor: String,
                    gradientEndColor: String,
                    primaryColor: String,
                    fontStyle: String,
                    qrX: Float,
                    qrY: Float,
                    visibleFields: List<String>
                ) {
                    val fieldsStr = StringBuilder("[")
                    visibleFields.forEachIndexed { idx, field ->
                        fieldsStr.append("\"").append(field).append("\"")
                        if (idx < visibleFields.size - 1) fieldsStr.append(",")
                    }
                    fieldsStr.append("]")

                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val newAICard = UserCard(
                                cardName = "$name\'s $businessType Card",
                                templateId = "ai_template",
                                themeName = themeName,
                                fullName = name,
                                companyName = companyName,
                                jobTitle = "$businessType Professional",
                                backgroundColor = backgroundColor,
                                gradientEndColor = gradientEndColor,
                                qrCodeColor = primaryColor,
                                fontStyle = fontStyle,
                                qrCodeX = qrX,
                                qrCodeY = qrY,
                                visibleFieldsJson = fieldsStr.toString(),
                                lastUpdated = System.currentTimeMillis()
                            )
                            val insertedId = repository.saveCard(newAICard)
                            _editingCard.value = newAICard.copy(id = insertedId.toInt())
                            
                            _aiSuccess.value = true
                            _aiLoading.value = false
                        } catch (e: Exception) {
                            _aiError.value = "DB Save failed: ${e.message}"
                            _aiLoading.value = false
                        }
                    }
                }

                override fun onFailure(error: String) {
                    _aiError.value = error
                    _aiLoading.value = false
                }
            }
        )
    }

    fun resetAIStates() {
        _aiSuccess.value = false
        _aiError.value = null
    }
}
