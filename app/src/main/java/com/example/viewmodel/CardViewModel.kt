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
    private val _canUndo = androidx.compose.runtime.mutableStateOf(false)
    private val _canRedo = androidx.compose.runtime.mutableStateOf(false)
    private var lastEditTime = 0L

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
    private val _isUserPremium = MutableStateFlow(true)
    val isUserPremium: StateFlow<Boolean> = _isUserPremium.asStateFlow()
    
    private val _activeTheme = MutableStateFlow(prefs.themeMode)
    val activeTheme: StateFlow<String> = _activeTheme.asStateFlow()

    // Dynamic Templates Management States
    private val _favoriteTemplatesList = MutableStateFlow<Set<String>>(emptySet())
    val favoriteTemplatesList: StateFlow<Set<String>> = _favoriteTemplatesList.asStateFlow()

    private val _recentTemplatesList = MutableStateFlow<List<String>>(emptyList())
    val recentTemplatesList: StateFlow<List<String>> = _recentTemplatesList.asStateFlow()

    private val _customTemplatesList = MutableStateFlow<List<UserCard>>(emptyList())
    val customTemplatesList: StateFlow<List<UserCard>> = _customTemplatesList.asStateFlow()

    // Auto-Save Configuration & States (Persists current design draft periodically)
    private var autoSaveJob: kotlinx.coroutines.Job? = null
    private val _isAutoSaveEnabled = MutableStateFlow(true)
    val isAutoSaveEnabled: StateFlow<Boolean> = _isAutoSaveEnabled.asStateFlow()

    private val _autoSaveIntervalSeconds = MutableStateFlow(10) // default 10s
    val autoSaveIntervalSeconds: StateFlow<Int> = _autoSaveIntervalSeconds.asStateFlow()

    private val _autoSaveStatus = MutableStateFlow<String>("Active")
    val autoSaveStatus: StateFlow<String> = _autoSaveStatus.asStateFlow()

    private var lastAutoSavedCardHash: Int = 0

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

        // Load templates preferences
        _favoriteTemplatesList.value = prefs.favoriteTemplates.split(";").filter { it.isNotEmpty() }.toSet()
        _recentTemplatesList.value = prefs.recentTemplates.split(";").filter { it.isNotEmpty() }
        _customTemplatesList.value = loadCustomTemplatesFromPrefs()

        startAutoSaveLoop()
    }

    // AUTH CONTEXTS
    fun loginMock(name: String, email: String) {
        prefs.isLoggedIn = true
        prefs.userName = name
        prefs.userEmail = email
        prefs.accountType = "Premium"
        prefs.subscriptionPlan = "Lifetime"
        _isUserPremium.value = true
    }

    fun loginGoogle(name: String, email: String, photoUrl: String?) {
        prefs.isLoggedIn = true
        prefs.userName = name
        prefs.userEmail = email
        if (photoUrl != null) {
            prefs.userPhoto = photoUrl
        }
        prefs.accountType = "Premium"
        prefs.subscriptionPlan = "Lifetime"
        _isUserPremium.value = true
    }

    fun loginApple(name: String, email: String, photoUrl: String? = null) {
        prefs.isLoggedIn = true
        prefs.userName = name
        prefs.userEmail = email
        prefs.userPhoto = photoUrl ?: "apple_avatar"
        prefs.accountType = "Premium"
        prefs.subscriptionPlan = "Lifetime"
        _isUserPremium.value = true
    }

    fun loginGuest() {
        prefs.isLoggedIn = true
        prefs.userName = "Guest Builder"
        prefs.userEmail = "guest@pillaiplay.com"
        prefs.accountType = "Premium"
        prefs.subscriptionPlan = "Lifetime"
        _isUserPremium.value = true
    }

    fun logout() {
        prefs.clearAuth()
        _isUserPremium.value = true
    }

    fun upgradeSubscription(plan: String) {
        prefs.subscriptionPlan = plan
        prefs.accountType = "Premium"
        _isUserPremium.value = true
    }

    fun downgradeToFree() {
        prefs.subscriptionPlan = "Lifetime"
        prefs.accountType = "Premium"
        _isUserPremium.value = true
    }

    fun changeTheme(mode: String) {
        prefs.themeMode = mode
        _activeTheme.value = mode
    }

    // EDITOR ENGINE & HISTORY CONTROLS
    private fun updateStackStates() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun toggleAutoSave(enabled: Boolean) {
        _isAutoSaveEnabled.value = enabled
        if (enabled) {
            startAutoSaveLoop()
        } else {
            autoSaveJob?.cancel()
            _autoSaveStatus.value = "Paused"
        }
    }

    fun updateAutoSaveInterval(seconds: Int) {
        _autoSaveIntervalSeconds.value = seconds
        startAutoSaveLoop()
    }

    fun startAutoSaveLoop() {
        autoSaveJob?.cancel()
        if (!_isAutoSaveEnabled.value) {
            _autoSaveStatus.value = "Disabled"
            return
        }

        autoSaveJob = viewModelScope.launch(Dispatchers.IO) {
            // Wait briefly on launch
            kotlinx.coroutines.delay(2000L)
            while (true) {
                val intervalMs = _autoSaveIntervalSeconds.value * 1000L
                kotlinx.coroutines.delay(intervalMs)
                
                val currentCard = _editingCard.value
                if (currentCard != null) {
                    val currentHash = currentCard.hashCode()
                    if (currentHash != lastAutoSavedCardHash) {
                        _autoSaveStatus.value = "Saving..."
                        try {
                            repository.updateCard(currentCard.copy(lastUpdated = System.currentTimeMillis()))
                            lastAutoSavedCardHash = currentHash
                            val timeStr = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date())
                            _autoSaveStatus.value = "Saved at $timeStr"
                        } catch (e: java.lang.Exception) {
                            _autoSaveStatus.value = "Error saving"
                        }
                    }
                }
            }
        }
    }

    fun commitUndoBoundary() {
        lastEditTime = 0L
    }

    fun selectCardForEditing(card: UserCard) {
        _editingCard.value = card
        lastAutoSavedCardHash = card.hashCode()
        _autoSaveStatus.value = "Active"
        undoStack.clear()
        redoStack.clear()
        commitUndoBoundary()
        updateStackStates()
    }

    fun createNewCardProject(name: String, templateId: String = "vibe_modern_gold") {
        viewModelScope.launch(Dispatchers.IO) {
            // Track in Recent templates
            addRecentTemplate(templateId)

            val customPreset = _customTemplatesList.value.find { it.templateId == templateId }
            val newProject = if (customPreset != null) {
                // Return a copy of the custom template with isPremium, name updated and id set appropriately
                customPreset.copy(
                    id = 0,
                    cardName = name,
                    lastUpdated = System.currentTimeMillis()
                )
            } else {
                val preset = com.example.ui.cardTemplates.find { it.id == templateId }
                if (preset != null) {
                    val category = preset.category
                    val bgType = if (preset.bgStart == preset.bgEnd) "SOLID" else "GRADIENT"

                    // Setup coordinates based on layout category
                    when (category) {
                        "Luxury" -> {
                            UserCard(
                                cardName = name,
                                templateId = templateId,
                                themeName = preset.name,
                                isPremium = preset.isPremium,
                                backgroundColor = preset.bgStart,
                                gradientEndColor = preset.bgEnd,
                                backgroundType = bgType,
                                qrCodeColor = preset.primaryColor,
                                fontStyle = preset.fontStyle,
                                borderStyle = preset.borderStyle,
                                lastUpdated = System.currentTimeMillis(),
                                // Layout positions
                                qrCodeX = 275f, qrCodeY = 65f, qrCodeSize = 75f,
                                fullNameX = 25f, fullNameY = 30f, fullNameSize = 18f,
                                jobTitleX = 25f, jobTitleY = 55f, jobTitleSize = 10f,
                                companyNameX = 25f, companyNameY = 80f, companyNameSize = 11f,
                                mobileNumberX = 25f, mobileNumberY = 120f, mobileNumberSize = 8.5f,
                                emailX = 25f, emailY = 140f, emailSize = 8.5f,
                                websiteX = 25f, websiteY = 160f, websiteSize = 8.5f,
                                addressX = 25f, addressY = 180f, addressSize = 8.5f
                            )
                        }
                        "Technology" -> {
                            UserCard(
                                cardName = name,
                                templateId = templateId,
                                themeName = preset.name,
                                isPremium = preset.isPremium,
                                backgroundColor = preset.bgStart,
                                gradientEndColor = preset.bgEnd,
                                backgroundType = bgType,
                                qrCodeColor = preset.primaryColor,
                                fontStyle = preset.fontStyle,
                                borderStyle = preset.borderStyle,
                                lastUpdated = System.currentTimeMillis(),
                                // Layout positions
                                qrCodeX = 30f, qrCodeY = 65f, qrCodeSize = 80f,
                                fullNameX = 140f, fullNameY = 30f, fullNameSize = 18f,
                                jobTitleX = 140f, jobTitleY = 55f, jobTitleSize = 10f,
                                companyNameX = 140f, companyNameY = 75f, companyNameSize = 10f,
                                mobileNumberX = 140f, mobileNumberY = 110f, mobileNumberSize = 8.5f,
                                emailX = 140f, emailY = 130f, emailSize = 8.5f,
                                websiteX = 140f, websiteY = 150f, websiteSize = 8.5f,
                                addressX = 140f, addressY = 170f, addressSize = 8.5f
                            )
                        }
                        "Modern Minimalist" -> {
                            UserCard(
                                cardName = name,
                                templateId = templateId,
                                themeName = preset.name,
                                isPremium = preset.isPremium,
                                backgroundColor = preset.bgStart,
                                gradientEndColor = preset.bgEnd,
                                backgroundType = bgType,
                                qrCodeColor = preset.primaryColor,
                                fontStyle = preset.fontStyle,
                                borderStyle = preset.borderStyle,
                                lastUpdated = System.currentTimeMillis(),
                                // Layout positions
                                qrCodeX = 295f, qrCodeY = 15f, qrCodeSize = 50f,
                                fullNameX = 135f, fullNameY = 30f, fullNameSize = 22f,
                                jobTitleX = 145f, jobTitleY = 60f, jobTitleSize = 10f,
                                companyNameX = 135f, companyNameY = 80f, companyNameSize = 11f,
                                mobileNumberX = 20f, mobileNumberY = 185f, mobileNumberSize = 8f,
                                emailX = 145f, emailY = 185f, emailSize = 8f,
                                websiteX = 265f, websiteY = 185f, websiteSize = 8f,
                                addressX = 20f, addressY = 205f, addressSize = 8f
                            )
                        }
                        "Creative" -> {
                            UserCard(
                                cardName = name,
                                templateId = templateId,
                                themeName = preset.name,
                                isPremium = preset.isPremium,
                                backgroundColor = preset.bgStart,
                                gradientEndColor = preset.bgEnd,
                                backgroundType = bgType,
                                qrCodeColor = preset.primaryColor,
                                fontStyle = preset.fontStyle,
                                borderStyle = preset.borderStyle,
                                lastUpdated = System.currentTimeMillis(),
                                // Layout positions
                                qrCodeX = 270f, qrCodeY = 65f, qrCodeSize = 80f,
                                fullNameX = 25f, fullNameY = 40f, fullNameSize = 19f,
                                jobTitleX = 25f, jobTitleY = 65f, jobTitleSize = 10f,
                                companyNameX = 25f, companyNameY = 90f, companyNameSize = 12f,
                                mobileNumberX = 25f, mobileNumberY = 125f, mobileNumberSize = 8.5f,
                                emailX = 125f, emailY = 125f, emailSize = 8.5f,
                                websiteX = 25f, websiteY = 155f, websiteSize = 8.5f,
                                addressX = 125f, addressY = 155f, addressSize = 8.5f
                            )
                        }
                        else -> {
                            UserCard(
                                cardName = name,
                                templateId = templateId,
                                themeName = preset.name,
                                isPremium = preset.isPremium,
                                backgroundColor = preset.bgStart,
                                gradientEndColor = preset.bgEnd,
                                backgroundType = bgType,
                                qrCodeColor = preset.primaryColor,
                                fontStyle = preset.fontStyle,
                                borderStyle = preset.borderStyle,
                                lastUpdated = System.currentTimeMillis()
                            )
                        }
                    }
                } else {
                    UserCard(
                        cardName = name,
                        templateId = templateId,
                        isPremium = templateId.contains("premium"),
                        lastUpdated = System.currentTimeMillis()
                    )
                }
            }
            val generatedId = repository.saveCard(newProject)
            val insertedProject = newProject.copy(id = generatedId.toInt())
            _editingCard.value = insertedProject
            lastAutoSavedCardHash = insertedProject.hashCode()
            _autoSaveStatus.value = "Active"
            undoStack.clear()
            redoStack.clear()
            commitUndoBoundary()
            updateStackStates()
        }
    }

    // Call when editing elements to push current state into undo stack
    fun applyCardEdit(modified: UserCard) {
        val current = _editingCard.value ?: return
        
        val now = System.currentTimeMillis()
        // Coalesce changes that occur within 1.2 seconds of each other (e.g., continuous drags, slide inputs, rapid typing)
        if (now - lastEditTime < 1200L && undoStack.isNotEmpty()) {
            // Coalesce: keep the previous boundary state; do not push the intermediate state
        } else {
            if (undoStack.size >= 100) {
                undoStack.removeAt(0)
            }
            undoStack.push(current.copy())
        }
        
        redoStack.clear() // Clear redo since we entered a new manual edit
        _editingCard.value = modified
        lastEditTime = now
        updateStackStates()
        
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
            commitUndoBoundary()
            updateStackStates()
            
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
            commitUndoBoundary()
            updateStackStates()
            
            viewModelScope.launch(Dispatchers.IO) {
                repository.updateCard(nextState.copy(lastUpdated = System.currentTimeMillis()))
            }
        }
    }

    fun hasUndo(): Boolean = _canUndo.value
    fun hasRedo(): Boolean = _canRedo.value

    // CARD CRUD WRAPPERS
    fun updateEditingCardName(newName: String) {
        val current = _editingCard.value ?: return
        applyCardEdit(current.copy(cardName = newName))
    }

    fun updateCardInDatabase(card: UserCard) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCard(card)
        }
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
        brandDescription: String,
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
            brandDescription = brandDescription,
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

    // --- DYNAMIC TEMPLATES METHODS ---
    fun toggleFavoriteTemplate(templateId: String) {
        val current = _favoriteTemplatesList.value.toMutableSet()
        if (current.contains(templateId)) {
            current.remove(templateId)
        } else {
            current.add(templateId)
        }
        _favoriteTemplatesList.value = current
        prefs.favoriteTemplates = current.joinToString(";")
    }

    fun addRecentTemplate(templateId: String) {
        val current = _recentTemplatesList.value.toMutableList()
        current.remove(templateId)
        current.add(0, templateId)
        val trimmed = current.take(10) // Keep last 10
        _recentTemplatesList.value = trimmed
        prefs.recentTemplates = trimmed.joinToString(";")
    }

    private fun loadCustomTemplatesFromPrefs(): List<UserCard> {
        val jsonStr = prefs.customTemplatesJson
        if (jsonStr.isEmpty() || jsonStr == "[]") return emptyList()
        val list = mutableListOf<UserCard>()
        try {
            val array = org.json.JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(JSONObjectToUserCard(obj))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveAsMyTemplate(card: UserCard, customName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val customTemplateId = "custom_${System.currentTimeMillis()}"
            val newTemplate = card.copy(
                id = 0, // template ID placeholder
                cardName = customName,
                templateId = customTemplateId,
                themeName = customName,
                lastUpdated = System.currentTimeMillis()
            )
            val currentTemplates = _customTemplatesList.value.toMutableList()
            currentTemplates.add(0, newTemplate)
            _customTemplatesList.value = currentTemplates
            saveCustomTemplatesToPrefs(currentTemplates)
        }
    }

    fun deleteCustomTemplate(templateId: String) {
        val currentTemplates = _customTemplatesList.value.toMutableList()
        currentTemplates.removeAll { it.templateId == templateId }
        _customTemplatesList.value = currentTemplates
        saveCustomTemplatesToPrefs(currentTemplates)
    }

    private fun saveCustomTemplatesToPrefs(list: List<UserCard>) {
        try {
            val array = org.json.JSONArray()
            list.forEach { card ->
                array.put(card.toJSONObject())
            }
            prefs.customTemplatesJson = array.toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun UserCard.toJSONObject(): org.json.JSONObject {
        val obj = org.json.JSONObject()
        obj.put("id", id)
        obj.put("cardName", cardName)
        obj.put("templateId", templateId)
        obj.put("themeName", themeName)
        obj.put("fullName", fullName)
        obj.put("jobTitle", jobTitle)
        obj.put("companyName", companyName)
        obj.put("mobileNumber", mobileNumber)
        obj.put("whatsAppNumber", whatsAppNumber)
        obj.put("email", email)
        obj.put("website", website)
        obj.put("address", address)
        obj.put("backgroundColor", backgroundColor)
        obj.put("gradientEndColor", gradientEndColor)
        obj.put("backgroundType", backgroundType)
        obj.put("backgroundImage", backgroundImage)
        obj.put("fontStyle", fontStyle)
        obj.put("borderStyle", borderStyle)
        obj.put("qrCodeColor", qrCodeColor)
        obj.put("qrCodeX", qrCodeX.toDouble())
        obj.put("qrCodeY", qrCodeY.toDouble())
        obj.put("qrCodeSize", qrCodeSize.toDouble())
        obj.put("qrCodeVisible", qrCodeVisible)
        obj.put("qrCodeType", qrCodeType)
        obj.put("qrCodeData", qrCodeData)
        // Coords
        obj.put("fullNameX", fullNameX.toDouble())
        obj.put("fullNameY", fullNameY.toDouble())
        obj.put("fullNameScale", fullNameScale.toDouble())
        obj.put("fullNameSize", fullNameSize.toDouble())
        obj.put("jobTitleX", jobTitleX.toDouble())
        obj.put("jobTitleY", jobTitleY.toDouble())
        obj.put("jobTitleScale", jobTitleScale.toDouble())
        obj.put("jobTitleSize", jobTitleSize.toDouble())
        obj.put("companyNameX", companyNameX.toDouble())
        obj.put("companyNameY", companyNameY.toDouble())
        obj.put("companyNameScale", companyNameScale.toDouble())
        obj.put("companyNameSize", companyNameSize.toDouble())
        obj.put("mobileNumberX", mobileNumberX.toDouble())
        obj.put("mobileNumberY", mobileNumberY.toDouble())
        obj.put("mobileNumberScale", mobileNumberScale.toDouble())
        obj.put("mobileNumberSize", mobileNumberSize.toDouble())
        obj.put("emailX", emailX.toDouble())
        obj.put("emailY", emailY.toDouble())
        obj.put("emailScale", emailScale.toDouble())
        obj.put("emailSize", emailSize.toDouble())
        obj.put("websiteX", websiteX.toDouble())
        obj.put("websiteY", websiteY.toDouble())
        obj.put("websiteScale", websiteScale.toDouble())
        obj.put("websiteSize", websiteSize.toDouble())
        obj.put("addressX", addressX.toDouble())
        obj.put("addressY", addressY.toDouble())
        obj.put("addressScale", addressScale.toDouble())
        obj.put("addressSize", addressSize.toDouble())
        obj.put("visibleFieldsJson", visibleFieldsJson)
        obj.put("designElementsJson", designElementsJson)
        return obj
    }

    private fun JSONObjectToUserCard(obj: org.json.JSONObject): UserCard {
        return UserCard(
            id = obj.optInt("id", 0),
            cardName = obj.optString("cardName", ""),
            templateId = obj.optString("templateId", "vibe_modern_gold"),
            themeName = obj.optString("themeName", ""),
            fullName = obj.optString("fullName", "Pillai Play"),
            jobTitle = obj.optString("jobTitle", "Chief Executive Officer"),
            companyName = obj.optString("companyName", "Pillai\'Play Entertainment"),
            mobileNumber = obj.optString("mobileNumber", "+91 98765 43210"),
            email = obj.optString("email", "hello@pillaiplay.com"),
            website = obj.optString("website", "www.pillaiplay.com"),
            address = obj.optString("address", "Navi Mumbai, Maharashtra, India"),
            backgroundColor = obj.optString("backgroundColor", "#10121A"),
            gradientEndColor = obj.optString("gradientEndColor", "#1D2130"),
            backgroundType = obj.optString("backgroundType", "GRADIENT"),
            backgroundImage = obj.optString("backgroundImage", "gradient_golden"),
            fontStyle = obj.optString("fontStyle", "Space Grotesk"),
            borderStyle = obj.optString("borderStyle", "MINIMAL_GOLD"),
            qrCodeColor = obj.optString("qrCodeColor", "#D4AF37"),
            qrCodeX = obj.optDouble("qrCodeX", 240.0).toFloat(),
            qrCodeY = obj.optDouble("qrCodeY", 110.0).toFloat(),
            qrCodeSize = obj.optDouble("qrCodeSize", 80.0).toFloat(),
            qrCodeVisible = obj.optBoolean("qrCodeVisible", true),
            qrCodeType = obj.optString("qrCodeType", "WEBSITE"),
            qrCodeData = obj.optString("qrCodeData", "https://pillaiplay.com"),
            fullNameX = obj.optDouble("fullNameX", 25.0).toFloat(),
            fullNameY = obj.optDouble("fullNameY", 25.0).toFloat(),
            fullNameScale = obj.optDouble("fullNameScale", 1.0).toFloat(),
            fullNameSize = obj.optDouble("fullNameSize", 20.0).toFloat(),
            jobTitleX = obj.optDouble("jobTitleX", 25.0).toFloat(),
            jobTitleY = obj.optDouble("jobTitleY", 55.0).toFloat(),
            jobTitleScale = obj.optDouble("jobTitleScale", 1.0).toFloat(),
            jobTitleSize = obj.optDouble("jobTitleSize", 10.0).toFloat(),
            companyNameX = obj.optDouble("companyNameX", 25.0).toFloat(),
            companyNameY = obj.optDouble("companyNameY", 80.0).toFloat(),
            companyNameScale = obj.optDouble("companyNameScale", 1.0).toFloat(),
            companyNameSize = obj.optDouble("companyNameSize", 12.0).toFloat(),
            mobileNumberX = obj.optDouble("mobileNumberX", 25.0).toFloat(),
            mobileNumberY = obj.optDouble("mobileNumberY", 120.0).toFloat(),
            mobileNumberScale = obj.optDouble("mobileNumberScale", 1.0).toFloat(),
            mobileNumberSize = obj.optDouble("mobileNumberSize", 9.0).toFloat(),
            emailX = obj.optDouble("emailX", 25.0).toFloat(),
            emailY = obj.optDouble("emailY", 140.0).toFloat(),
            emailScale = obj.optDouble("emailScale", 1.0).toFloat(),
            emailSize = obj.optDouble("emailSize", 9.0).toFloat(),
            websiteX = obj.optDouble("websiteX", 25.0).toFloat(),
            websiteY = obj.optDouble("websiteY", 160.0).toFloat(),
            websiteScale = obj.optDouble("websiteScale", 1.0).toFloat(),
            websiteSize = obj.optDouble("websiteSize", 9.0).toFloat(),
            addressX = obj.optDouble("addressX", 25.0).toFloat(),
            addressY = obj.optDouble("addressY", 180.0).toFloat(),
            addressScale = obj.optDouble("addressScale", 1.0).toFloat(),
            addressSize = obj.optDouble("addressSize", 9.0).toFloat(),
            visibleFieldsJson = obj.optString("visibleFieldsJson", "[]"),
            designElementsJson = obj.optString("designElementsJson", "[]")
        )
    }
}
