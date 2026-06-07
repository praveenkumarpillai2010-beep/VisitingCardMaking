package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.repository.CardRepository
import com.example.utils.GeminiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    fun forceTogglePremiumStatus() {
        _isUserPremium.value = !_isUserPremium.value
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

    fun createNewCardProject(
        name: String,
        templateId: String = "blank",
        cardShape: String = "RECTANGLE",
        bgColor: String = "#FFFFFF",
        bgType: String = "SOLID",
        gradientEndColor: String = "#FFFFFF"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val newProject = UserCard(
                cardName = name,
                templateId = templateId,
                themeName = "Blank Slate",
                isPremium = false,
                backgroundColor = bgColor,
                gradientEndColor = gradientEndColor,
                backgroundType = bgType,
                visibleFieldsJson = "[]",
                qrCodeVisible = false,
                cardShape = cardShape,
                lastUpdated = System.currentTimeMillis()
            )
            val generatedId = repository.saveCard(newProject)
            val insertedProject = newProject.copy(id = generatedId.toInt())
            _editingCard.value = insertedProject
            lastAutoSavedCardHash = insertedProject.hashCode()
            _autoSaveStatus.value = "Active"
            undoStack.clear()
            redoStack.clear()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                selectCardForEditing(insertedProject)
            }
        }
    }

    fun createNewCardProjectDirectly(card: UserCard) {
        viewModelScope.launch(Dispatchers.IO) {
            val generatedId = repository.saveCard(card)
            val insertedProject = card.copy(id = generatedId.toInt())
            _editingCard.value = insertedProject
            lastAutoSavedCardHash = insertedProject.hashCode()
            _autoSaveStatus.value = "Active"
            undoStack.clear()
            redoStack.clear()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                selectCardForEditing(insertedProject)
            }
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

                // Dynamic Coordinate resolver based on generated layout arrangement and coordinates
                val layout = optionToSave.layoutArrangement
                
                // Defaults by layout style
                val defaultFullNameX: Float
                val defaultFullNameY: Float
                val defaultFullNameSize: Float
                val defaultJobTitleX: Float
                val defaultJobTitleY: Float
                val defaultJobTitleSize: Float
                val defaultCompanyNameX: Float
                val defaultCompanyNameY: Float
                val defaultCompanyNameSize: Float
                val defaultMobileX: Float
                val defaultMobileY: Float
                val defaultEmailX: Float
                val defaultEmailY: Float
                val defaultWebsiteX: Float
                val defaultWebsiteY: Float
                val defaultAddressX: Float
                val defaultAddressY: Float
                val defaultQrX: Float
                val defaultQrY: Float

                when (layout) {
                    "CENTER_MINIMALIST" -> {
                        defaultFullNameX = 90f; defaultFullNameY = 65f; defaultFullNameSize = 20f
                        defaultJobTitleX = 110f; defaultJobTitleY = 90f; defaultJobTitleSize = 10f
                        defaultCompanyNameX = 105f; defaultCompanyNameY = 35f; defaultCompanyNameSize = 12f
                        defaultMobileX = 35f; defaultMobileY = 185f
                        defaultEmailX = 130f; defaultEmailY = 185f
                        defaultWebsiteX = 225f; defaultWebsiteY = 185f
                        defaultAddressX = 105f; defaultAddressY = 205f
                        defaultQrX = 140f; defaultQrY = 110f
                    }
                    "MODERN_SPLIT" -> {
                        defaultFullNameX = 160f; defaultFullNameY = 50f; defaultFullNameSize = 19f
                        defaultJobTitleX = 160f; defaultJobTitleY = 72f; defaultJobTitleSize = 10f
                        defaultCompanyNameX = 160f; defaultCompanyNameY = 25f; defaultCompanyNameSize = 12f
                        defaultMobileX = 160f; defaultMobileY = 115f
                        defaultEmailX = 160f; defaultEmailY = 135f
                        defaultWebsiteX = 160f; defaultWebsiteY = 155f
                        defaultAddressX = 160f; defaultAddressY = 175f
                        defaultQrX = 35f; defaultQrY = 65f
                    }
                    "HORIZONTAL_DENSITY" -> {
                        defaultFullNameX = 20f; defaultFullNameY = 25f; defaultFullNameSize = 19f
                        defaultJobTitleX = 20f; defaultJobTitleY = 105f; defaultJobTitleSize = 10f
                        defaultCompanyNameX = 200f; defaultCompanyNameY = 25f; defaultCompanyNameSize = 13f
                        defaultMobileX = 20f; defaultMobileY = 130f
                        defaultEmailX = 200f; defaultEmailY = 130f
                        defaultWebsiteX = 20f; defaultWebsiteY = 155f
                        defaultAddressX = 200f; defaultAddressY = 155f
                        defaultQrX = 260f; defaultQrY = 50f
                    }
                    else -> { // "CLASSIC_REAR_QR" or default
                        defaultFullNameX = 20f; defaultFullNameY = 25f; defaultFullNameSize = 19f
                        defaultJobTitleX = 20f; defaultJobTitleY = 50f; defaultJobTitleSize = 10f
                        defaultCompanyNameX = 20f; defaultCompanyNameY = 75f; defaultCompanyNameSize = 12f
                        defaultMobileX = 20f; defaultMobileY = 115f
                        defaultEmailX = 20f; defaultEmailY = 135f
                        defaultWebsiteX = 20f; defaultWebsiteY = 155f
                        defaultAddressX = 20f; defaultAddressY = 175f
                        defaultQrX = 260f; defaultQrY = 70f
                    }
                }

                val finalFullNameX = optionToSave.fullNameX ?: defaultFullNameX
                val finalFullNameY = optionToSave.fullNameY ?: defaultFullNameY
                val finalFullNameSize = optionToSave.fullNameSize ?: defaultFullNameSize
                val finalJobTitleX = optionToSave.jobTitleX ?: defaultJobTitleX
                val finalJobTitleY = optionToSave.jobTitleY ?: defaultJobTitleY
                val finalJobTitleSize = optionToSave.jobTitleSize ?: defaultJobTitleSize
                val finalCompanyNameX = optionToSave.companyNameX ?: defaultCompanyNameX
                val finalCompanyNameY = optionToSave.companyNameY ?: defaultCompanyNameY
                val finalCompanyNameSize = optionToSave.companyNameSize ?: defaultCompanyNameSize
                val finalMobileX = optionToSave.mobileX ?: defaultMobileX
                val finalMobileY = optionToSave.mobileY ?: defaultMobileY
                val finalEmailX = optionToSave.emailX ?: defaultEmailX
                val finalEmailY = optionToSave.emailY ?: defaultEmailY
                val finalWebsiteX = optionToSave.websiteX ?: defaultWebsiteX
                val finalWebsiteY = optionToSave.websiteY ?: defaultWebsiteY
                val finalAddressX = optionToSave.addressX ?: defaultAddressX
                val finalAddressY = optionToSave.addressY ?: defaultAddressY
                val finalQrX = optionToSave.qrX ?: defaultQrX
                val finalQrY = optionToSave.qrY ?: defaultQrY

                val newAICard = UserCard(
                    cardName = "$name's AI Custom Card",
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
                    qrCodeX = finalQrX,
                    qrCodeY = finalQrY,
                    qrCodeVisible = optionToSave.qrCodeVisible,
                    fullNameX = finalFullNameX,
                    fullNameY = finalFullNameY,
                    fullNameSize = finalFullNameSize,
                    jobTitleX = finalJobTitleX,
                    jobTitleY = finalJobTitleY,
                    jobTitleSize = finalJobTitleSize,
                    companyNameX = finalCompanyNameX,
                    companyNameY = finalCompanyNameY,
                    companyNameSize = finalCompanyNameSize,
                    mobileNumberX = finalMobileX,
                    mobileNumberY = finalMobileY,
                    emailX = finalEmailX,
                    emailY = finalEmailY,
                    websiteX = finalWebsiteX,
                    websiteY = finalWebsiteY,
                    addressX = finalAddressX,
                    addressY = finalAddressY,
                    visibleFieldsJson = fieldsStr.toString(),
                    designElementsJson = designElementsJsonStr,
                    cardShape = optionToSave.cardShape,
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

    // --- BUSINESS CARD COMMUNITY STATES & HELPERS ---
    private val _communityIsPublic = MutableStateFlow(true)
    val communityIsPublic: StateFlow<Boolean> = _communityIsPublic.asStateFlow()

    private val _communitySearchQuery = MutableStateFlow("")
    val communitySearchQuery: StateFlow<String> = _communitySearchQuery.asStateFlow()

    private val _communitySelectedCategory = MutableStateFlow<String?>(null)
    val communitySelectedCategory: StateFlow<String?> = _communitySelectedCategory.asStateFlow()

    private val _templatesSelectedCategory = MutableStateFlow<String>("All")
    val templatesSelectedCategory: StateFlow<String> = _templatesSelectedCategory.asStateFlow()

    fun selectTemplatesCategory(cat: String) {
        _templatesSelectedCategory.value = cat
    }

    private val _communitySelectedDirectory = MutableStateFlow<String?>(null)
    val communitySelectedDirectory: StateFlow<String?> = _communitySelectedDirectory.asStateFlow()

    private val _communitySavedCards = MutableStateFlow<Set<String>>(emptySet())
    val communitySavedCards: StateFlow<Set<String>> = _communitySavedCards.asStateFlow()

    private val _communityConnections = MutableStateFlow<Set<String>>(emptySet())
    val communityConnections: StateFlow<Set<String>> = _communityConnections.asStateFlow()

    private val _communitySentRequests = MutableStateFlow<Set<String>>(emptySet())
    val communitySentRequests: StateFlow<Set<String>> = _communitySentRequests.asStateFlow()

    private val _communityIncomingRequests = MutableStateFlow<List<String>>(listOf("elena_rostova", "rohan_mehta"))
    val communityIncomingRequests: StateFlow<List<String>> = _communityIncomingRequests.asStateFlow()

    private val _communityBlocked = MutableStateFlow<Set<String>>(emptySet())
    val communityBlocked: StateFlow<Set<String>> = _communityBlocked.asStateFlow()

    private val _communityMuted = MutableStateFlow<Set<String>>(emptySet())
    val communityMuted: StateFlow<Set<String>> = _communityMuted.asStateFlow()

    private val _communityReported = MutableStateFlow<Set<String>>(emptySet())
    val communityReported: StateFlow<Set<String>> = _communityReported.asStateFlow()

    private val _communityChats = MutableStateFlow<Map<String, List<CommunityMessage>>>(emptyMap())
    val communityChats: StateFlow<Map<String, List<CommunityMessage>>> = _communityChats.asStateFlow()

    private val _communityNotifications = MutableStateFlow<List<CommunityNotification>>(listOf(
        CommunityNotification("n1", "Welcome to Professional Community!", "Discover thousands of local professionals and request connections to grow your network.", System.currentTimeMillis() - 3600000, false, "REQUEST"),
        CommunityNotification("n2", "Card Viewed", "Vikram Malhotra (Principal Software Architect) viewed your business card.", System.currentTimeMillis() - 172800000, true, "VIEW"),
        CommunityNotification("n3", "Card Saved", "Sarah Jenkins (Creative Director) saved your business card to her Favorites.", System.currentTimeMillis() - 259200000, true, "SAVE")
    ))
    val communityNotifications: StateFlow<List<CommunityNotification>> = _communityNotifications.asStateFlow()

    private val _communityProfileName = MutableStateFlow(prefs.userName)
    val communityProfileName: StateFlow<String> = _communityProfileName.asStateFlow()

    private val _communityProfileProfession = MutableStateFlow("Lead Developer")
    val communityProfileProfession: StateFlow<String> = _communityProfileProfession.asStateFlow()

    private val _communityProfileCompany = MutableStateFlow("Pillai Play Enterprise")
    val communityProfileCompany: StateFlow<String> = _communityProfileCompany.asStateFlow()

    private val _communityProfileLocation = MutableStateFlow("Navi Mumbai, India")
    val communityProfileLocation: StateFlow<String> = _communityProfileLocation.asStateFlow()

    private val _communityProfileWebsite = MutableStateFlow("www.pillaiplay.com")
    val communityProfileWebsite: StateFlow<String> = _communityProfileWebsite.asStateFlow()

    private val _communityProfileFacebook = MutableStateFlow("pillai_play")
    val communityProfileFacebook: StateFlow<String> = _communityProfileFacebook.asStateFlow()

    private val _communityProfileInstagram = MutableStateFlow("pillai_play")
    val communityProfileInstagram: StateFlow<String> = _communityProfileInstagram.asStateFlow()

    private val _communityProfileLinkedin = MutableStateFlow("pillai-play")
    val communityProfileLinkedin: StateFlow<String> = _communityProfileLinkedin.asStateFlow()

    private val _communityMessagePrivacy = MutableStateFlow("Everyone") // Connections Only, No One, Everyone
    val communityMessagePrivacy: StateFlow<String> = _communityMessagePrivacy.asStateFlow()

    private val _communityProfileVisibilitySetting = MutableStateFlow(true)
    val communityProfileVisibilitySetting: StateFlow<Boolean> = _communityProfileVisibilitySetting.asStateFlow()

    private val _selectedMyFeaturedCardId = MutableStateFlow<Int?>(null)
    val selectedMyFeaturedCardId: StateFlow<Int?> = _selectedMyFeaturedCardId.asStateFlow()

    fun setCommunitySearch(query: String) {
        _communitySearchQuery.value = query
    }

    fun selectCommunityCategory(cat: String?) {
        _communitySelectedCategory.value = cat
    }

    fun selectCommunityDirectory(dir: String?) {
        _communitySelectedDirectory.value = dir
    }

    fun changeProfilePrivacy(isPublic: Boolean) {
        _communityIsPublic.value = isPublic
        addCommunityNotification("Settings Updated", "Your profile visibility was updated to ${if (isPublic) "Public" else "Private"}.", "REQUEST")
    }

    fun changeProfileSettings(name: String, profession: String, company: String, location: String, website: String, facebook: String, instagram: String, linkedin: String) {
        _communityProfileName.value = name
        _communityProfileProfession.value = profession
        _communityProfileCompany.value = company
        _communityProfileLocation.value = location
        _communityProfileWebsite.value = website
        _communityProfileFacebook.value = facebook
        _communityProfileInstagram.value = instagram
        _communityProfileLinkedin.value = linkedin
        addCommunityNotification("Profile Updated", "You updated your professional profile successfully.", "REQUEST")
    }

    fun setMyFeaturedCardId(id: Int?) {
        _selectedMyFeaturedCardId.value = id
    }

    fun changeMessagePrivacy(privacy: String) {
        _communityMessagePrivacy.value = privacy
    }

    fun toggleProfileVisibilitySetting(visible: Boolean) {
        _communityProfileVisibilitySetting.value = visible
    }

    fun saveCommunityCard(professionalId: String) {
        val current = _communitySavedCards.value.toMutableSet()
        if (current.contains(professionalId)) {
            current.remove(professionalId)
            addCommunityNotification("Card Removed", "You removed card of professional from your favorites list.", "SAVE")
        } else {
            current.add(professionalId)
            addCommunityNotification("Card Saved", "You saved card of professional to your ⭐ Favorite Cards Section.", "SAVE")
        }
        _communitySavedCards.value = current
    }

    fun toggleBlockProfessional(profId: String) {
        val current = _communityBlocked.value.toMutableSet()
        if (current.contains(profId)) {
            current.remove(profId)
        } else {
            current.add(profId)
        }
        _communityBlocked.value = current
    }

    fun toggleMuteProfessional(profId: String) {
        val current = _communityMuted.value.toMutableSet()
        if (current.contains(profId)) {
            current.remove(profId)
        } else {
            current.add(profId)
        }
        _communityMuted.value = current
    }

    fun reportProfessional(profId: String, reason: String) {
        val current = _communityReported.value.toMutableSet()
        current.add(profId)
        _communityReported.value = current
        addCommunityNotification("Report Submitted", "You successfully flagged this user. Our team will review this profile shortly.", "REQUEST")
    }

    fun sendConnectionRequest(profId: String) {
        val current = _communitySentRequests.value.toMutableSet()
        current.add(profId)
        _communitySentRequests.value = current
        addCommunityNotification("Connection Sent", "Your professional connection request was dispatched successfully.", "REQUEST")
        
        // Auto-accept request in 5 seconds to feel fully interactive!
        viewModelScope.launch {
            delay(5000)
            acceptSentRequest(profId)
        }
    }

    private fun acceptSentRequest(profId: String) {
        val currentSent = _communitySentRequests.value.toMutableSet()
        if (currentSent.contains(profId)) {
            currentSent.remove(profId)
            _communitySentRequests.value = currentSent
            
            val connections = _communityConnections.value.toMutableSet()
            connections.add(profId)
            _communityConnections.value = connections
            
            addCommunityNotification("Connection Accepted!", "Congratulations! You are now connected with a professional.", "REQUEST")
        }
    }

    fun acceptIncomingRequest(profId: String) {
        val currentIncoming = _communityIncomingRequests.value.toMutableList()
        if (currentIncoming.contains(profId)) {
            currentIncoming.remove(profId)
            _communityIncomingRequests.value = currentIncoming
            
            val connections = _communityConnections.value.toMutableSet()
            connections.add(profId)
            _communityConnections.value = connections
            
            addCommunityNotification("New Connection Built", "You accepted the incoming request and expanded your network.", "REQUEST")
        }
    }

    fun declineIncomingRequest(profId: String) {
        val currentIncoming = _communityIncomingRequests.value.toMutableList()
        currentIncoming.remove(profId)
        _communityIncomingRequests.value = currentIncoming
    }

    fun sendCommunityMessage(profId: String, text: String, imageUrl: String? = null, cardId: Int? = null) {
        val conversation = (_communityChats.value[profId] ?: emptyList()).toMutableList()
        val newMessage = CommunityMessage(
            id = "msg_${System.currentTimeMillis()}",
            senderId = "self",
            text = text,
            timestamp = System.currentTimeMillis(),
            imageUrl = imageUrl,
            visitingCardId = cardId,
            isRead = true
        )
        conversation.add(newMessage)
        
        val updatedChats = _communityChats.value.toMutableMap()
        updatedChats[profId] = conversation
        _communityChats.value = updatedChats

        // Handle simulated professional auto-reply!
        viewModelScope.launch {
            delay(1500)
            receiveCommunityMessageSimulated(profId, text)
        }
    }

    private fun receiveCommunityMessageSimulated(profId: String, userText: String) {
        if (_communityBlocked.value.contains(profId)) return // Blocked users can't reply

        val conversation = (_communityChats.value[profId] ?: emptyList()).toMutableList()
        
        val replies = listOf(
            "That's fantastic! I'd love to follow up and explore how we can collaborate. Let me check my calendar for next week.",
            "Thanks for sharing your brilliant visiting card with me. Feel free to connect or email anytime!",
            "Thank you! Your card layout is pristine and very modern. How did you create it?",
            "Hello! Let's schedule a Zoom call to discuss this further. Looking forward to it!",
            "Great to meet you online. Let's make sure we share our networks when we connect."
        )
        val selectedReplyText = replies.random()

        val replyMsg = CommunityMessage(
            id = "msg_${System.currentTimeMillis()}",
            senderId = profId,
            text = selectedReplyText,
            timestamp = System.currentTimeMillis()
        )
        conversation.add(replyMsg)
        
        val updatedChats = _communityChats.value.toMutableMap()
        updatedChats[profId] = conversation
        _communityChats.value = updatedChats

        addCommunityNotification("New Chat Message", "You received a new message from a professional in the community.", "MESSAGE")
    }

    fun addCommunityNotification(title: String, content: String, type: String) {
        val current = _communityNotifications.value.toMutableList()
        current.add(0, CommunityNotification(
            id = "notif_${System.currentTimeMillis()}",
            title = title,
            content = content,
            timestamp = System.currentTimeMillis(),
            eventType = type
        ))
        _communityNotifications.value = current
    }

    fun clearAllNotifications() {
        _communityNotifications.value = emptyList()
    }

    // --- ENRICHED CREATOR PROFILE & CARD SHARING COMMUNITY ---
    private val _myProfile = MutableStateFlow(
        CreatorProfile(
            id = "self",
            username = "pillai_playmaker",
            name = "Pillai Play",
            bio = "Lead UI architect and designer passionate about elegant Material 3 layouts, rich vector graphics, and business card designs since 2024.",
            companyName = "Pillai Play Enterprise",
            phone = "+91 98765 43210",
            email = "acclead@gmail.com",
            website = "www.pillaiplay.com",
            instagram = "pillaiplus",
            facebook = "pillai.play",
            linkedin = "pillai-playmaker",
            location = "Navi Mumbai, India",
            profilePhoto = "",
            coverBanner = "#004B49",
            isVerified = true,
            isPremium = true,
            isPublic = true,
            followersCount = 1420,
            followingCount = 280,
            likesReceivedCount = 8900,
            totalDesignsCount = 15
        )
    )
    val myProfile: StateFlow<CreatorProfile> = _myProfile.asStateFlow()

    private val _communitySharedCards = MutableStateFlow<List<CommunitySharedCard>>(
        listOf(
            CommunitySharedCard(
                id = "sc_1",
                title = "Cyber Slate Developer Card",
                description = "High-end neon themed cyber futuristic developer card. Uses sleek modern styling with custom code indicators.",
                category = "Technology",
                frontCard = UserCard(
                    id = 10001,
                    cardName = "Cyber Slate",
                    fullName = "Alex Rivera",
                    jobTitle = "Developer Evangelist",
                    companyName = "Cyberdyne Systems",
                    mobileNumber = "+1 (555) 019-2831",
                    email = "alex@cyberdyne.io",
                    website = "cyberdyne.io/alex",
                    address = "Bandra West, Mumbai",
                    backgroundColor = "#0A0F1D",
                    gradientEndColor = "#0E1C38",
                    qrCodeColor = "#00FFCC",
                    fontStyle = "Tech Clean",
                    borderStyle = "CYBER_SLATE"
                ),
                creatorId = "alex_rivera",
                creatorName = "Alex Rivera",
                creatorUsername = "cyber_alex",
                creatorAvatarColor = "#0288D1",
                isVerifiedCreator = true,
                likesCount = 245,
                downloadsCount = 182,
                viewsCount = 1205
            ),
            CommunitySharedCard(
                id = "sc_2",
                title = "Minimal Executive Gold",
                description = "Extremely professional card designed for CEOs, managers, and executives. Highlighted by gold borders and serif fonts.",
                category = "Premium",
                frontCard = UserCard(
                    id = 10002,
                    cardName = "Executive Gold",
                    fullName = "Elena Rostova",
                    jobTitle = "Managing Director",
                    companyName = "North Star Capital",
                    mobileNumber = "+44 20 7946 0192",
                    email = "e.rostova@northstar.com",
                    website = "northstar.com",
                    address = "Colaba, Mumbai",
                    backgroundColor = "#111111",
                    gradientEndColor = "#1C1C1C",
                    qrCodeColor = "#D4AF37",
                    fontStyle = "Elegant Serif",
                    borderStyle = "MINIMAL_GOLD"
                ),
                creatorId = "elena_rostova",
                creatorName = "Elena Rostova",
                creatorUsername = "elena_invest",
                creatorAvatarColor = "#C2185B",
                isPremiumCreator = true,
                likesCount = 512,
                downloadsCount = 425,
                viewsCount = 2410
            ),
            CommunitySharedCard(
                id = "sc_3",
                title = "Creative Burst Portfolio",
                description = "Perfect card for UX designers, artists, and agency creatives. Generous usage of gradients and custom circular shields.",
                category = "Creative",
                frontCard = UserCard(
                    id = 10003,
                    cardName = "Creative Burst",
                    fullName = "Sarah Jenkins",
                    jobTitle = "Creative Director",
                    companyName = "PixelCraft Studio",
                    mobileNumber = "+91 98980 12345",
                    email = "hello@sarahj.design",
                    website = "sarahj.design",
                    address = "Indiranagar, Bengaluru",
                    backgroundColor = "#1C0407",
                    gradientEndColor = "#2D0A14",
                    qrCodeColor = "#FF3366",
                    fontStyle = "Space Grotesk",
                    borderStyle = "MODERN_DOUBLE"
                ),
                creatorId = "sarah_jenkins",
                creatorName = "Sarah Jenkins",
                creatorUsername = "sarah_pixel",
                creatorAvatarColor = "#E040FB",
                isVerifiedCreator = true,
                likesCount = 189,
                downloadsCount = 135,
                viewsCount = 920
            ),
            CommunitySharedCard(
                id = "sc_4",
                title = "Smart Realtor Spotlight",
                description = "Modern and inviting business card layout for real estate agents, architects, and house designers.",
                category = "Real Estate",
                frontCard = UserCard(
                    id = 10004,
                    cardName = "Realty Pro",
                    fullName = "Rajesh Patel",
                    jobTitle = "Chief Broker",
                    companyName = "Patel & Sons Real Estate",
                    mobileNumber = "+91 88877 66554",
                    email = "rajesh@patelrealty.com",
                    website = "patelrealty.com",
                    address = "Andheri, Mumbai",
                    backgroundColor = "#141518",
                    gradientEndColor = "#221C16",
                    qrCodeColor = "#CBB26A",
                    fontStyle = "Elegant Serif",
                    borderStyle = "MODERN_DOUBLE"
                ),
                creatorId = "rajesh_patel",
                creatorName = "Rajesh Patel",
                creatorUsername = "realty_rajesh",
                creatorAvatarColor = "#F57C00",
                likesCount = 89,
                downloadsCount = 64,
                viewsCount = 310
            )
        )
    )
    val communitySharedCards: StateFlow<List<CommunitySharedCard>> = _communitySharedCards.asStateFlow()

    private val _likedSharedCardIds = MutableStateFlow<Set<String>>(emptySet())
    val likedSharedCardIds: StateFlow<Set<String>> = _likedSharedCardIds.asStateFlow()

    private val _favoriteSharedCardIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteSharedCardIds: StateFlow<Set<String>> = _favoriteSharedCardIds.asStateFlow()

    private val _downloadedSharedCardIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadedSharedCardIds: StateFlow<Set<String>> = _downloadedSharedCardIds.asStateFlow()

    private val _followedCreatorIds = MutableStateFlow<Set<String>>(emptySet())
    val followedCreatorIds: StateFlow<Set<String>> = _followedCreatorIds.asStateFlow()

    private val _reportedCardIds = MutableStateFlow<Set<String>>(emptySet())
    val reportedCardIds: StateFlow<Set<String>> = _reportedCardIds.asStateFlow()

    fun likeSharedCard(cardId: String) {
        val current = _likedSharedCardIds.value.toMutableSet()
        val cards = _communitySharedCards.value.map {
            if (it.id == cardId) {
                if (current.contains(cardId)) {
                    current.remove(cardId)
                    it.copy(likesCount = maxOf(0, it.likesCount - 1))
                } else {
                    current.add(cardId)
                    it.copy(likesCount = it.likesCount + 1)
                }
            } else {
                it
            }
        }
        _likedSharedCardIds.value = current
        _communitySharedCards.value = cards
        addCommunityNotification("Card Liked", "Your preference toggled regarding card design: $cardId.", "LIKE")
    }

    fun favoriteSharedCard(cardId: String) {
        val current = _favoriteSharedCardIds.value.toMutableSet()
        if (current.contains(cardId)) {
            current.remove(cardId)
        } else {
            current.add(cardId)
        }
        _favoriteSharedCardIds.value = current
        addCommunityNotification("Saved to Favorites", "Design $cardId saved into your favorite layouts successfully.", "SAVE")
    }

    fun downloadSharedCard(cardId: String) {
        val current = _downloadedSharedCardIds.value.toMutableSet()
        current.add(cardId)
        _downloadedSharedCardIds.value = current
        val cards = _communitySharedCards.value.map {
            if (it.id == cardId) {
                it.copy(downloadsCount = it.downloadsCount + 1)
            } else {
                it
            }
        }
        _communitySharedCards.value = cards
        addCommunityNotification("Design Saved offline", "You downloaded a local template package from the Card Community.", "SAVE")
    }

    fun followCreator(creatorId: String) {
        val current = _followedCreatorIds.value.toMutableSet()
        val profile = _myProfile.value
        if (current.contains(creatorId)) {
            current.remove(creatorId)
            _myProfile.value = profile.copy(followingCount = maxOf(0, profile.followingCount - 1))
        } else {
            current.add(creatorId)
            _myProfile.value = profile.copy(followingCount = profile.followingCount + 1)
        }
        _followedCreatorIds.value = current
        addCommunityNotification("Network Modified", "You changed following status of creator $creatorId.", "FOLLOW")
    }

    fun uploadCustomCardToCommunity(
        title: String,
        description: String,
        category: String,
        frontCard: UserCard,
        backCard: UserCard?
    ) {
        val profile = _myProfile.value
        val newCard = CommunitySharedCard(
            id = "sc_${System.currentTimeMillis()}",
            title = title,
            description = description,
            category = category,
            frontCard = frontCard,
            backCard = backCard,
            creatorId = profile.id,
            creatorName = profile.name,
            creatorUsername = profile.username,
            creatorAvatarColor = "#004B49",
            isVerifiedCreator = profile.isVerified,
            isPremiumCreator = profile.isPremium,
            likesCount = 0,
            downloadsCount = 0,
            viewsCount = 1,
            createdTime = System.currentTimeMillis()
        )
        val currentList = _communitySharedCards.value.toMutableList()
        currentList.add(0, newCard)
        _communitySharedCards.value = currentList
        
        // update designs count
        _myProfile.value = profile.copy(totalDesignsCount = profile.totalDesignsCount + 1)
        addCommunityNotification("Card Published", "Congratulations! Your $title layout was published live inside the Card Community.", "REQUEST")
    }

    fun reportSharedCard(cardId: String, reason: String) {
        val current = _reportedCardIds.value.toMutableSet()
        current.add(cardId)
        _reportedCardIds.value = current
        addCommunityNotification("Card Flagged", "You successfully reported design $cardId for safety review.", "REQUEST")
    }

    // Admin action to delete card
    fun adminDeleteSharedCard(cardId: String) {
        val current = _communitySharedCards.value.filter { it.id != cardId }
        _communitySharedCards.value = current
        addCommunityNotification("Admin Moderation", "You deleted design $cardId as a moderator.", "REQUEST")
    }

    fun saveFullCreatorProfile(
        username: String,
        name: String,
        bio: String,
        companyName: String,
        phone: String,
        email: String,
        location: String,
        website: String,
        instagram: String,
        facebook: String,
        linkedin: String,
        coverBanner: String,
        profilePhoto: String,
        isPublic: Boolean
    ) {
        _myProfile.value = _myProfile.value.copy(
            username = username,
            name = name,
            bio = bio,
            companyName = companyName,
            phone = phone,
            email = email,
            location = location,
            website = website,
            instagram = instagram,
            facebook = facebook,
            linkedin = linkedin,
            coverBanner = coverBanner,
            profilePhoto = profilePhoto,
            isPublic = isPublic
        )
        
        // Sync older simple properties
        _communityProfileName.value = name
        _communityProfileProfession.value = bio
        _communityProfileCompany.value = companyName
        _communityProfileLocation.value = location
        _communityProfileWebsite.value = website
        _communityProfileFacebook.value = facebook
        _communityProfileInstagram.value = instagram
        _communityProfileLinkedin.value = linkedin
        _communityIsPublic.value = isPublic
        addCommunityNotification("Spotlight Updated", "Your modern social creator profile was updated successfully.", "REQUEST")
    }
}
