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

    private val _aiOptions = MutableStateFlow<List<GeminiService.CardDesignOption>>(emptyList())
    val aiOptions: StateFlow<List<GeminiService.CardDesignOption>> = _aiOptions.asStateFlow()

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
        prefs.accountType = "Premium"
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
    fun triggerAICardGeneration(
        name: String,
        companyName: String,
        jobTitle: String,
        phoneNumber: String,
        email: String,
        website: String,
        address: String,
        category: String,
        preferredColor: String,
        preferredStyle: String,
        logoUri: String?,
        photoUri: String?
    ) {
        _aiLoading.value = true
        _aiError.value = null
        _aiSuccess.value = false
        _aiOptions.value = emptyList()

        GeminiService.generateCardLayout(
            name = name,
            companyName = companyName,
            jobTitle = jobTitle,
            phoneNumber = phoneNumber,
            email = email,
            website = website,
            address = address,
            category = category,
            preferredColor = preferredColor,
            preferredStyle = preferredStyle,
            logoUri = logoUri,
            photoUri = photoUri,
            callback = object : GeminiService.AIAssistantCallback {
                override fun onSuccess(options: List<GeminiService.CardDesignOption>) {
                    _aiOptions.value = options
                    _aiSuccess.value = true
                    _aiLoading.value = false
                    // Track generation Count
                    prefs.aiGenerationsCount = prefs.aiGenerationsCount + 1
                }

                override fun onFailure(error: String) {
                    _aiError.value = error
                    _aiLoading.value = false
                }
            }
        )
    }

    fun saveChosenDesignToEditor(
        optionToSave: GeminiService.CardDesignOption,
        name: String,
        companyName: String,
        jobTitle: String,
        phoneNumber: String,
        email: String,
        website: String,
        address: String,
        logoUri: String?,
        photoUri: String?
    ) {
        val fieldsStr = StringBuilder("[")
        optionToSave.visibleFields.forEachIndexed { idx, field ->
            fieldsStr.append("\"").append(field).append("\"")
            if (idx < optionToSave.visibleFields.size - 1) fieldsStr.append(",")
        }
        fieldsStr.append("]")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Incorporate custom images if present
                val elements = mutableListOf<DesignElement>()
                if (!logoUri.isNullOrEmpty()) {
                    elements.add(
                        DesignElement(
                            id = "custom_logo_element",
                            type = "ICON",
                            name = "Uploaded Logo",
                            content = logoUri,
                            x = 25f,
                            y = 100f,
                            scale = 1.0f
                        )
                    )
                }
                if (!photoUri.isNullOrEmpty()) {
                    elements.add(
                        DesignElement(
                            id = "custom_photo_element",
                            type = "STICKER",
                            name = "Profile Photo",
                            content = photoUri,
                            x = 180f,
                            y = 30f,
                            scale = 1.0f
                        )
                    )
                }

                val designElementsJsonStr = if (elements.isNotEmpty()) {
                    val arrStr = StringBuilder("[")
                    elements.forEachIndexed { index, designElement ->
                        arrStr.append("{")
                            .append("\"id\":\"").append(designElement.id).append("\",")
                            .append("\"type\":\"").append(designElement.type).append("\",")
                            .append("\"name\":\"").append(designElement.name).append("\",")
                            .append("\"content\":\"").append(designElement.content).append("\",")
                            .append("\"x\":").append(designElement.x).append(",")
                            .append("\"y\":").append(designElement.y).append(",")
                            .append("\"color\":\"").append(designElement.color).append("\",")
                            .append("\"fontSize\":").append(designElement.fontSize).append(",")
                            .append("\"isBold\":").append(designElement.isBold).append(",")
                            .append("\"isItalic\":").append(designElement.isItalic).append(",")
                            .append("\"isUnderline\":").append(designElement.isUnderline).append(",")
                            .append("\"rotation\":").append(designElement.rotation).append(",")
                            .append("\"scale\":").append(designElement.scale).append(",")
                            .append("\"zIndex\":").append(designElement.zIndex)
                            .append("}")
                        if (index < elements.size - 1) arrStr.append(",")
                    }
                    arrStr.append("]")
                    arrStr.toString()
                } else {
                    "[]"
                }

                val newAICard = UserCard(
                    cardName = "$name\'s AI Custom Card",
                    templateId = "ai_template",
                    themeName = optionToSave.themeName,
                    fullName = name,
                    companyName = companyName,
                    jobTitle = jobTitle,
                    mobileNumber = phoneNumber,
                    email = email,
                    website = website,
                    address = address,
                    backgroundColor = optionToSave.backgroundColor,
                    gradientEndColor = optionToSave.gradientEndColor,
                    qrCodeColor = optionToSave.primaryColor,
                    fontStyle = optionToSave.fontStyle,
                    qrCodeX = optionToSave.qrX,
                    qrCodeY = optionToSave.qrY,
                    qrCodeVisible = optionToSave.qrCodeVisible,
                    visibleFieldsJson = fieldsStr.toString(),
                    designElementsJson = designElementsJsonStr,
                    lastUpdated = System.currentTimeMillis()
                )

                val insertedId = repository.saveCard(newAICard)
                val cardWithId = newAICard.copy(id = insertedId.toInt())
                _editingCard.value = cardWithId
                
                // Set the current editor active card immediately
                _editingCard.value = cardWithId
            } catch (e: Exception) {
                android.util.Log.e("CardViewModel", "Failed saving card option", e)
            }
        }
    }

    fun resetAIStates() {
        _aiSuccess.value = false
        _aiError.value = null
        _aiOptions.value = emptyList()
    }
}
