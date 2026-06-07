package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.DesignElement
import com.example.data.UserCard
import com.example.utils.PDFExporter
import com.example.utils.QRCodeComposable
import com.example.utils.QRGenerator
import com.example.viewmodel.CardViewModel
import org.json.JSONArray
import java.io.InputStream
import java.util.*

// Helper local metadata class to represent dynamic custom elements
data class ParsedDesignElement(
    val id: String,
    val type: String,
    val content: String,
    val x: Float,
    val y: Float,
    val colorHex: String,
    val fontSize: Float,
    val isBold: Boolean,
    val rotation: Float,
    val scale: Float,
    val zIndex: Int
)

@Composable
fun EditorScreens(
    viewModel: CardViewModel,
    onNavigate: (ActiveScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val card by viewModel.editingCard.collectAsState()
    val isPremium by viewModel.isUserPremium.collectAsState()

    if (card == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active card project loaded", color = Color.Gray)
        }
        return
    }

    val activeCard = card!!

    val cWValue = when (activeCard.cardShape) {
        "SQUARE", "CIRCLE" -> 320f
        "VERTICAL" -> 240f
        "FOLDED" -> 400f
        else -> 400f
    }
    val cHValue = when (activeCard.cardShape) {
        "SQUARE", "CIRCLE" -> 320f
        "VERTICAL" -> 400f
        "FOLDED" -> 480f
        else -> 240f
    }

    // Canva Redesign - States
    var activeToolCategory by remember { mutableStateOf<String?>(null) } // "TEXT", "LOGO", "IMAGE", "SHAPE", "QR", "COLOR", "BACKGROUND", "LAYERS" or null
    var selectedElementKey by remember { mutableStateOf("") }
    // Initialize zoom factor to -1f representing automatic "Fit to Screen" mode
    var editorZoomFactor by remember { mutableStateOf(-1.0f) }

    // Dialog trigger states
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf(activeCard.cardName) }
    var showExportSheet by remember { mutableStateOf(false) }
    var isFullscreenMode by remember { mutableStateOf(false) }
    var isBackSide by remember { mutableStateOf(false) }

    // Helper visibility layers getter
    val visibleFields = try {
        val list = mutableListOf<String>()
        val array = JSONArray(activeCard.visibleFieldsJson)
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        list
    } catch (e: Exception) {
        listOf("fullName", "jobTitle", "companyName", "mobileNumber", "email", "website")
    }

    // Parse design elements cleanly outside composed drawing loops
    val customLayersList = remember(activeCard.designElementsJson) {
        val list = mutableListOf<Pair<String, String>>()
        try {
            val array = JSONArray(activeCard.designElementsJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(Pair(obj.getString("id"), obj.getString("type")))
            }
        } catch (ignored: Exception) {}
        list
    }

    // Build lists of all layers for selection row shortcut
    val nativeLayerTitles = mapOf(
        "fullName" to "Name Title",
        "jobTitle" to "Job Title",
        "companyName" to "Company Name",
        "mobileNumber" to "Direct Phone",
        "email" to "Email Mailbox",
        "website" to "Website Address",
        "address" to "Location Map",
        "qrCode" to "Primary QR Code"
    )

    // Layout Root
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07080C))
    ) {
        // 1. Compact Top Header (Height strictly 56dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFF0E111A))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = { onNavigate(ActiveScreen.DASHBOARD) },
                    modifier = Modifier.size(36.dp).background(Color(0xFF1B2230), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier.clickable {
                        renameInput = activeCard.cardName
                        showRenameDialog = true
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = activeCard.cardName,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.widthIn(max = 140.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename",
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFFD4AF37)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Interactive Autobsave indication
                val autoSaveEnabled by viewModel.isAutoSaveEnabled.collectAsState()
                val autoSaveStatus by viewModel.autoSaveStatus.collectAsState()
                Box(
                    modifier = Modifier
                        .background(Color(0xFF1D212E), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (autoSaveEnabled) Color(0xFF00FFCC) else Color.Red, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (autoSaveStatus == "Saving...") "Saving" else "Saved",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Save Template Action
                Button(
                    onClick = { viewModel.applyCardEdit(activeCard.copy(lastUpdated = System.currentTimeMillis())) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Export Button
                IconButton(
                    onClick = { showExportSheet = true },
                    modifier = Modifier.size(36.dp).background(Color(0xFF00FFCC), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Black
                    )
                }
            }
        }

        // 2. Main Work Area (Canvas takes 70% or full height depending on collapsed dock)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(if (activeToolCategory != null) 0.7f else 1f)
                .background(Color(0xFF08090D))
        ) {
            val fitZoomFactor = remember(maxWidth, maxHeight) {
                val padWidth = (maxWidth - 40.dp).coerceAtLeast(10.dp)
                val padHeight = (maxHeight - 40.dp).coerceAtLeast(10.dp)
                val horizontalZoom = padWidth.value / cWValue
                val verticalZoom = padHeight.value / cHValue
                minOf(horizontalZoom, verticalZoom).coerceIn(0.2f, 2.5f)
            }

            val currentZoom = if (editorZoomFactor == -1.0f) fitZoomFactor else editorZoomFactor
            val isScrollEnabled = editorZoomFactor != -1.0f

            // Centered Design Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isScrollEnabled) {
                            Modifier
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(24.dp)
                        .width((cWValue * currentZoom).dp)
                        .height((cHValue * currentZoom).dp)
                        .shadow(16.dp, RoundedCornerShape(8.dp))
                        .background(Color.Transparent, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(cWValue.dp)
                            .height(cHValue.dp)
                            .scale(currentZoom)
                    ) {
                        WYSIWYGCardCanvas(
                            card = activeCard,
                            selectedKey = selectedElementKey,
                            onSelectKey = { selectedElementKey = it },
                            viewModel = viewModel,
                            isBackSide = isBackSide
                        )
                    }
                }
            }

            // Elegant Front & Back side visual flip controller
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .background(Color(0xFF0E111A).copy(0.85f), RoundedCornerShape(20.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clickable { isBackSide = false }
                        .background(if (!isBackSide) Color(0xFF2196F3) else Color.Transparent, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("Front View", color = if (!isBackSide) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .clickable { isBackSide = true }
                        .background(if (isBackSide) Color(0xFF2196F3) else Color.Transparent, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("Back View", color = if (isBackSide) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 3. Zoom Controls Floating Action Button Stack (No permanent sidebar!)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FloatingActionButton(
                        onClick = { editorZoomFactor = (currentZoom + 0.15f).coerceIn(0.2f, 2.5f) },
                        containerColor = Color(0xCC181C26),
                        contentColor = Color(0xFF00FFCC),
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(16.dp))
                    }
                    FloatingActionButton(
                        onClick = { editorZoomFactor = (currentZoom - 0.15f).coerceIn(0.2f, 2.5f) },
                        containerColor = Color(0xCC181C26),
                        contentColor = Color(0xFF00FFCC),
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape
                    ) {
                        Text("-", color = Color(0xFF00FFCC), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    if (editorZoomFactor != -1.0f) {
                        FloatingActionButton(
                            onClick = { editorZoomFactor = -1.0f },
                            containerColor = Color(0xCC181C26),
                            contentColor = Color.White,
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Fit to Screen", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // 4. Advanced Property Adjustments Bar (Shown when any element is active)
        if (selectedElementKey.isNotEmpty()) {
            val isLocked = activeCard.lockedElements.split(",").contains(selectedElementKey)
            
            // Get current scale & rotation values
            val scaleVal = getElementScale(activeCard, selectedElementKey)
            val rotationVal = getElementRotation(activeCard, selectedElementKey)

            Surface(
                color = Color(0xFF121520),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                border = BorderStroke(1.dp, Color(0xFF1F2436).copy(0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF00FFCC), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Selected: ${nativeLayerTitles[selectedElementKey] ?: selectedElementKey}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Lock / Unlock Toggle
                            IconButton(
                                onClick = {
                                    val currentLocked = activeCard.lockedElements.split(",").filter { it.isNotEmpty() }.toMutableList()
                                    if (currentLocked.contains(selectedElementKey)) {
                                        currentLocked.remove(selectedElementKey)
                                    } else {
                                        currentLocked.add(selectedElementKey)
                                    }
                                    viewModel.applyCardEdit(activeCard.copy(lockedElements = currentLocked.joinToString(",")))
                                },
                                modifier = Modifier.size(28.dp).background(Color(0xFF1B2030), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Lock State",
                                    tint = if (isLocked) Color(0xFFFF5252) else Color.Gray.copy(alpha = 0.4f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Delete/Hide Object
                            IconButton(
                                onClick = {
                                    if (selectedElementKey == "qrCode") {
                                        // Toggle visibility of QR
                                        viewModel.applyCardEdit(activeCard.copy(qrCodeVisible = !activeCard.qrCodeVisible))
                                    } else if (nativeLayerTitles.containsKey(selectedElementKey)) {
                                        // Filter visible native list
                                        val newList = visibleFields.toMutableList()
                                        if (newList.contains(selectedElementKey)) {
                                            newList.remove(selectedElementKey)
                                        } else {
                                            newList.add(selectedElementKey)
                                        }
                                        val arr = JSONArray()
                                        newList.forEach { arr.put(it) }
                                        viewModel.applyCardEdit(activeCard.copy(visibleFieldsJson = arr.toString()))
                                    } else {
                                        // Custom design element delete
                                        try {
                                            val array = JSONArray(activeCard.designElementsJson)
                                            val newArray = JSONArray()
                                            for (i in 0 until array.length()) {
                                                val obj = array.getJSONObject(i)
                                                if (obj.getString("id") != selectedElementKey) {
                                                    newArray.put(obj)
                                                }
                                            }
                                            viewModel.applyCardEdit(activeCard.copy(designElementsJson = newArray.toString()))
                                            selectedElementKey = "fullName"
                                        } catch (e: Exception) {}
                                    }
                                },
                                modifier = Modifier.size(28.dp).background(Color(0xFFFF5252).copy(0.15f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete or Hide element",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    if (!isLocked) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Size:", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.width(36.dp))
                            Slider(
                                value = scaleVal,
                                onValueChange = { s ->
                                    updateElementSpatial(activeCard, selectedElementKey, null, null, null, s, null, null, viewModel)
                                },
                                valueRange = 0.40f..2.50f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00FFCC),
                                    activeTrackColor = Color(0xFF00FFCC)
                                ),
                                modifier = Modifier.weight(1f).height(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rot:", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.width(32.dp))
                            Slider(
                                value = rotationVal,
                                onValueChange = { r ->
                                    updateElementSpatial(activeCard, selectedElementKey, null, null, r, null, null, null, viewModel)
                                },
                                valueRange = 0f..360f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFD4AF37),
                                    activeTrackColor = Color(0xFFD4AF37)
                                ),
                                modifier = Modifier.weight(1f).height(24.dp)
                            )
                        }
                        
                        // Nudge arrow indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Micro-nudge:", color = Color.Gray, fontSize = 9.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            IconButton(onClick = { updateElementPosition(activeCard, selectedElementKey, -2f, 0f, viewModel) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Left", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                            IconButton(onClick = { updateElementPosition(activeCard, selectedElementKey, 0f, -2f, viewModel) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                            IconButton(onClick = { updateElementPosition(activeCard, selectedElementKey, 0f, 2f, viewModel) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                            IconButton(onClick = { updateElementPosition(activeCard, selectedElementKey, 2f, 0f, viewModel) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Right", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

        // 5. Sliding Canva Collapsible Bottom Dock (Tabs bar & expandable properties)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (activeToolCategory != null) {
                        Modifier.weight(0.3f)
                    } else {
                        Modifier.wrapContentHeight()
                    }
                )
                .background(Color(0xFF0A0C14))
        ) {
            // Expandable Property Sheet
            if (activeToolCategory != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF0F111B))
                        .border(BorderStroke(1.dp, Color(0xFF1E2436).copy(0.4f)), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Collapse Handle Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .background(Color(0xFF151926))
                                .clickable { activeToolCategory = null }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "EDITING: $activeToolCategory",
                                color = Color(0xFF00FFCC),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Collapse properties",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Detailed Property Panels
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            when (activeToolCategory) {
                                "TEXT" -> {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        // Font family picker first
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF111420))
                                                .padding(6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf("Space Grotesk", "Elegant Serif", "Modern Bold", "Tech Clean").forEach { font ->
                                                val active = activeCard.fontStyle == font
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .background(if (active) Color(0xFFD4AF37) else Color(0xFF1E2433), RoundedCornerShape(6.dp))
                                                        .clickable { viewModel.applyCardEdit(activeCard.copy(fontStyle = font)) }
                                                        .padding(vertical = 6.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(font, color = if (active) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            EditorFieldsPanel(activeCard, viewModel)
                                        }
                                    }
                                }
                                "LOGO" -> {
                                    // Custom graphics / Preset logos selector
                                    EditorCustomLayersPanel(activeCard, viewModel, onNavigate)
                                }
                                "IMAGE" -> {
                                    // Background patterns & user uploaded backdrops
                                    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("CUSTOM BRAND IMAGE FILL", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { viewModel.applyCardEdit(activeCard.copy(backgroundType = "PATTERN")) },
                                                colors = ButtonDefaults.buttonColors(containerColor = if (activeCard.backgroundType == "PATTERN") Color(0xFFD4AF37) else Color(0xFF1E2433)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Use Image Backdrop", fontSize = 11.sp)
                                            }
                                            Button(
                                                onClick = { viewModel.applyCardEdit(activeCard.copy(backgroundType = "GRADIENT")) },
                                                colors = ButtonDefaults.buttonColors(containerColor = if (activeCard.backgroundType != "PATTERN") Color(0xFFD4AF37) else Color(0xFF1E2433)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Use Color Fill", fontSize = 11.sp)
                                            }
                                        }
                                        
                                        // Upload controller
                                        val bgLauncher = rememberLauncherForActivityResult(
                                            contract = ActivityResultContracts.GetContent()
                                        ) { uri: Uri? ->
                                            if (uri != null) {
                                                viewModel.applyCardEdit(activeCard.copy(
                                                    backgroundType = "PATTERN",
                                                    backgroundImage = uri.toString()
                                                ))
                                            }
                                        }

                                        Button(
                                            onClick = { bgLauncher.launch("image/*") },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Upload Custom Background Image", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                "SHAPE" -> {
                                    // Add vector shape element
                                    EditorCustomLayersPanel(activeCard, viewModel, onNavigate)
                                }
                                "QR" -> {
                                    EditorQRPanel(activeCard, viewModel, onNavigate)
                                }
                                "COLOR" -> {
                                    EditorStylingPanel(activeCard, viewModel)
                                }
                                "BACKGROUND" -> {
                                    // Choose background frames or orientation layouts
                                    EditorStylingPanel(activeCard, viewModel)
                                }
                                "LAYERS" -> {
                                    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("LAYERS ORDER & ALIGNMENT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Snap to Grid", color = Color.Gray, fontSize = 10.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Switch(
                                                    checked = activeCard.snapToGrid,
                                                    onCheckedChange = { viewModel.applyCardEdit(activeCard.copy(snapToGrid = it)) },
                                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FFCC)),
                                                    modifier = Modifier.scale(0.8f)
                                                )
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { updateElementZIndex(activeCard, selectedElementKey, true, viewModel) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2433)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Bring Forward", fontSize = 10.sp)
                                            }
                                            Button(
                                                onClick = { updateElementZIndex(activeCard, selectedElementKey, false, viewModel) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2433)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Send Backward", fontSize = 10.sp)
                                            }
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            LayerShortcutsRow(
                                                nativeLayerTitles = nativeLayerTitles,
                                                visibleFields = visibleFields,
                                                customLayersList = customLayersList,
                                                selectedElementKey = selectedElementKey,
                                                onLayerSelect = { selectedElementKey = it }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Fixed Horizontally-scrollable Canva Category Tab bar at base of layout (including safe insets padding)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0E1119))
                    .padding(vertical = 10.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                
                CanvaTabIcon(
                    icon = Icons.Default.Create,
                    label = "Text",
                    active = activeToolCategory == "TEXT"
                ) {
                    activeToolCategory = if (activeToolCategory == "TEXT") null else "TEXT"
                }

                CanvaTabIcon(
                    icon = Icons.Default.Star,
                    label = "Logo",
                    active = activeToolCategory == "LOGO"
                ) {
                    activeToolCategory = if (activeToolCategory == "LOGO") null else "LOGO"
                }

                CanvaTabIcon(
                    icon = Icons.Default.Person,
                    label = "Image",
                    active = activeToolCategory == "IMAGE"
                ) {
                    activeToolCategory = if (activeToolCategory == "IMAGE") null else "IMAGE"
                }

                CanvaTabIcon(
                    icon = Icons.Default.Favorite,
                    label = "Shape",
                    active = activeToolCategory == "SHAPE"
                ) {
                    activeToolCategory = if (activeToolCategory == "SHAPE") null else "SHAPE"
                }

                CanvaTabIcon(
                    icon = Icons.Default.Send,
                    label = "QR",
                    active = activeToolCategory == "QR"
                ) {
                    activeToolCategory = if (activeToolCategory == "QR") null else "QR"
                }

                CanvaTabIcon(
                    icon = Icons.Default.Search,
                    label = "Color",
                    active = activeToolCategory == "COLOR"
                ) {
                    activeToolCategory = if (activeToolCategory == "COLOR") null else "COLOR"
                }

                CanvaTabIcon(
                    icon = Icons.Default.Home,
                    label = "Background",
                    active = activeToolCategory == "BACKGROUND"
                ) {
                    activeToolCategory = if (activeToolCategory == "BACKGROUND") null else "BACKGROUND"
                }

                CanvaTabIcon(
                    icon = Icons.Default.List,
                    label = "Layers",
                    active = activeToolCategory == "LAYERS"
                ) {
                    activeToolCategory = if (activeToolCategory == "LAYERS") null else "LAYERS"
                }

                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }

    // Rename Popup Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Visiting Card Title", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Template Title") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (renameInput.isNotEmpty()) {
                        viewModel.updateEditingCardName(renameInput)
                        showRenameDialog = false
                    }
                }) {
                    Text("Apply Name")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            },
            containerColor = Color(0xFF131722)
        )
    }

    // Export Dynamic Sheet Output
    if (showExportSheet) {
        ExportSettingsDialog(activeCard, isPremium) { showExportSheet = false }
    }
}

@Composable
fun CanvaTabIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = if (active) Color(0xFF00FFCC).copy(0.12f) else Color(0xFF1B2030),
                    shape = RoundedCornerShape(10.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (active) Color(0xFF00FFCC) else Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) Color(0xFF00FFCC) else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            color = if (active) Color(0xFF00FFCC) else Color.LightGray,
            fontSize = 9.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// TAB SELECT BUTTON
@Composable
fun RowScope.ToolboxTabButton(text: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                color = if (active) Color(0xFFD4AF37) else Color.Gray,
                fontSize = 12.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
            )
            if (active) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(16.dp, 2.dp)
                        .background(Color(0xFFD4AF37), CircleShape)
                )
            }
        }
    }
}

@Composable
fun CardTemplateDecorations(card: UserCard, accentColor: Color, isBackSide: Boolean = false) {
    val templateId = card.templateId.lowercase()
    val themeName = card.themeName
    val primaryHex = card.qrCodeColor
    val priColor = try { Color(android.graphics.Color.parseColor(primaryHex)) } catch (e: Exception) { accentColor }
    
    val isLiceriaGold = templateId.contains("liceria") || templateId.contains("black_gold_luxury")
    val isGoldOrLuxury = (templateId.contains("gold") || templateId.contains("luxury") || templateId.contains("luxe") || themeName.contains("Premium Black") || themeName.contains("Luxury") || themeName.contains("Lawyer")) && !isLiceriaGold
    val isBlueOrOcean = templateId.contains("blue") || templateId.contains("ocean") || templateId.contains("corp") || templateId.contains("metro") || themeName.contains("Corporate") || themeName.contains("Modern") || themeName.contains("Photography")
    val isTechOrCyber = templateId.contains("cyber") || templateId.contains("tech") || templateId.contains("ai") || templateId.contains("matrix") || themeName.contains("Technology") || themeName.contains("Startup") || themeName.contains("Finance") || themeName.contains("QR Business Cards")
    val isCreativeOrOrange = templateId.contains("creative") || templateId.contains("retro") || templateId.contains("orange") || templateId.contains("neon") || themeName.contains("Creative") || themeName.contains("Construction") || themeName.contains("Restaurant")
    val isMinimalOrGreen = templateId.contains("minimal") || templateId.contains("academic") || templateId.contains("science") || templateId.contains("clean") || templateId.contains("dentist") || themeName.contains("Medical") || themeName.contains("Education") || themeName.contains("Minimal")

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLiceriaGold) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                if (!isBackSide) {
                    // Draw the left diagonal dark background split
                    val leftPath = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(w * 0.55f, 0f)
                        lineTo(w * 0.42f, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(path = leftPath, color = Color(0xFF161616))
                    
                    // Draw the right light grey background split
                    val rightPath = Path().apply {
                        moveTo(w * 0.55f, 0f)
                        lineTo(w, 0f)
                        lineTo(w, h)
                        lineTo(w * 0.42f, h)
                        close()
                    }
                    drawPath(path = rightPath, color = Color(0xFFF2F4F7))
                    
                    // Beautiful metallic gold diagonal divider stroke line
                    val goldGradient = Brush.linearGradient(
                        colors = listOf(Color(0xFFC59B27), Color(0xFFFFF3B0), Color(0xFFD4AF37)),
                        start = androidx.compose.ui.geometry.Offset(w * 0.4f, h),
                        end = androidx.compose.ui.geometry.Offset(w * 0.6f, 0f)
                    )
                    
                    drawLine(
                        brush = goldGradient,
                        start = androidx.compose.ui.geometry.Offset(w * 0.55f, 0f),
                        end = androidx.compose.ui.geometry.Offset(w * 0.42f, h),
                        strokeWidth = 3.5f
                    )
                } else {
                    // Back side is full luxurious dark charcoal
                    drawRect(color = Color(0xFF161616), size = size)
                    
                    // Double gold borders for extreme luxury feel
                    val topGold = Color(0xFFD4AF37)
                    drawRect(
                        color = topGold.copy(alpha = 0.4f),
                        topLeft = androidx.compose.ui.geometry.Offset(10f, 10f),
                        size = androidx.compose.ui.geometry.Size(w - 20f, h - 20f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f)
                    )
                    drawRect(
                        color = topGold.copy(alpha = 0.15f),
                        topLeft = androidx.compose.ui.geometry.Offset(14f, 14f),
                        size = androidx.compose.ui.geometry.Size(w - 28f, h - 28f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.8f)
                    )
                }
            }
        } else if (isGoldOrLuxury) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                
                // Sweeping gold gradient waves
                val goldGradient = Brush.linearGradient(
                    colors = listOf(Color(0xFFD4AF37), Color(0xFFFFF3B0), Color(0xFFC59B27)),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.8f)
                )
                
                // Bottom-right sweeping wave metallic layer
                val bottomWave = Path().apply {
                    moveTo(w * 0.3f, h)
                    cubicTo(w * 0.5f, h * 0.8f, w * 0.7f, h * 0.4f, w, h * 0.5f)
                    lineTo(w, h)
                    close()
                }
                drawPath(path = bottomWave, brush = goldGradient, alpha = 0.85f)

                // Extra complementary wavy overlay
                val innerBottomWave = Path().apply {
                    moveTo(w * 0.42f, h)
                    cubicTo(w * 0.55f, h * 0.88f, w * 0.75f, h * 0.5f, w, h * 0.62f)
                    lineTo(w, h)
                    close()
                }
                drawPath(path = innerBottomWave, color = Color.Black.copy(alpha = 0.35f))

                // Left top organic flame banner
                val leftWave = Path().apply {
                    moveTo(0f, 0f)
                    cubicTo(w * 0.15f, h * 0.22f, w * 0.35f, h * 0.08f, w * 0.45f, 0f)
                    cubicTo(w * 0.32f, h * 0.32f, w * 0.12f, h * 0.48f, 0f, h * 0.62f)
                    close()
                }
                drawPath(path = leftWave, brush = goldGradient, alpha = 0.9f)
            }
        } else if (isBlueOrOcean) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val blueGrad = Brush.verticalGradient(
                    colors = listOf(Color(0xFF00F2FE), Color(0xFF4FACFE)),
                    startY = 0f,
                    endY = h
                )

                // Broad sweeping 3D fluid wave
                val rightCurve = Path().apply {
                    moveTo(w * 0.45f, 0f)
                    cubicTo(w * 0.6f, h * 0.2f, w * 0.5f, h * 0.6f, w * 0.85f, h)
                    lineTo(w, h)
                    lineTo(w, 0f)
                    close()
                }
                drawPath(path = rightCurve, brush = blueGrad, alpha = 0.85f)

                // Intersecting white high-contrast sweep wave (matches column 1 of reference image)
                val whiteCurve = Path().apply {
                    moveTo(w * 0.41f, 0f)
                    cubicTo(w * 0.58f, h * 0.22f, w * 0.47f, h * 0.58f, w * 0.82f, h)
                    lineTo(w * 0.85f, h)
                    cubicTo(w * 0.5f, h * 0.6f, w * 0.6f, h * 0.2f, w * 0.45f, 0f)
                    close()
                }
                drawPath(path = whiteCurve, color = Color.White, alpha = 0.95f)

                // Secondary overlapping midnight blue curve
                val innerBlueCurve = Path().apply {
                    moveTo(w * 0.65f, 0f)
                    cubicTo(w * 0.75f, h * 0.22f, w * 0.68f, h * 0.72f, w, h * 0.55f)
                    lineTo(w, 0f)
                    close()
                }
                drawPath(path = innerBlueCurve, color = Color(0xFF0A1530), alpha = 0.75f)
            }
        } else if (isTechOrCyber) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw hexagon repeating geometric mesh grid (matches column 3 reference navy card)
                val colorHex = priColor.copy(alpha = 0.08f)
                val hexRadius = 16f
                val hSpacing = hexRadius * 1.732f
                val vSpacing = hexRadius * 1.5f

                var row = 0
                while (row * vSpacing < h) {
                    val offsetY = row * vSpacing
                    val isOdd = row % 2 != 0
                    val startX = if (isOdd) hSpacing / 2f else 0f
                    var col = 0
                    while (col * hSpacing + startX < w) {
                        val centerX = col * hSpacing + startX
                        val hexPath = Path().apply {
                            for (i in 0..5) {
                                val angle = Math.toRadians((i * 60 - 30).toDouble())
                                val px = centerX + hexRadius * Math.cos(angle).toFloat()
                                val py = offsetY + hexRadius * Math.sin(angle).toFloat()
                                if (i == 0) moveTo(px, py) else lineTo(px, py)
                            }
                            close()
                        }
                        drawPath(path = hexPath, color = colorHex, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))
                        col++
                    }
                    row++
                }

                // Left/Right split panel diagonal line
                val wedgePath = Path().apply {
                    moveTo(w * 0.76f, 0f)
                    lineTo(w, 0f)
                    lineTo(w, h)
                    lineTo(w * 0.62f, h)
                    close()
                }
                drawPath(path = wedgePath, color = Color.White.copy(alpha = 0.05f))

                drawLine(
                    color = priColor.copy(alpha = 0.45f),
                    start = androidx.compose.ui.geometry.Offset(w * 0.76f, 0f),
                    end = androidx.compose.ui.geometry.Offset(w * 0.62f, h),
                    strokeWidth = 2.5f
                )
            }
        } else if (isCreativeOrOrange) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                val orangeGrad = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFA000), Color(0xFFFF5252)),
                    start = androidx.compose.ui.geometry.Offset(w * 0.5f, h),
                    end = androidx.compose.ui.geometry.Offset(w, h * 0.5f)
                )

                // Broad sweeping warm orange ribbon sweeps in the corner
                val bottomOrangeCorner = Path().apply {
                    moveTo(w * 0.44f, h)
                    cubicTo(w * 0.58f, h * 0.82f, w * 0.76f, h * 0.65f, w, h * 0.76f)
                    lineTo(w, h)
                    close()
                }
                drawPath(path = bottomOrangeCorner, brush = orangeGrad)

                // Left high-contrast modern chevron ribbon tab
                val leftChevronTab = Path().apply {
                    moveTo(0f, h * 0.28f)
                    lineTo(w * 0.12f, h * 0.5f)
                    lineTo(0f, h * 0.72f)
                    close()
                }
                
                val leftChevronShadow = Path().apply {
                    moveTo(0f, h * 0.22f)
                    lineTo(w * 0.14f, h * 0.5f)
                    lineTo(0f, h * 0.78f)
                    close()
                }
                drawPath(path = leftChevronShadow, color = Color.Black.copy(alpha = 0.22f))
                drawPath(path = leftChevronTab, brush = orangeGrad)
            }
        } else if (isMinimalOrGreen) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Elegant subtle parallel coordinate guidelines
                drawLine(
                    color = priColor.copy(alpha = 0.75f),
                    start = androidx.compose.ui.geometry.Offset(w * 0.12f, h * 0.88f),
                    end = androidx.compose.ui.geometry.Offset(w * 0.88f, h * 0.88f),
                    strokeWidth = 2.5f
                )

                drawLine(
                    color = priColor.copy(alpha = 0.25f),
                    start = androidx.compose.ui.geometry.Offset(w * 0.08f, h * 0.91f),
                    end = androidx.compose.ui.geometry.Offset(w * 0.92f, h * 0.91f),
                    strokeWidth = 1.5f
                )

                // Top right highlight segment accent corners
                val greenCorner = Path().apply {
                    moveTo(w * 0.86f, 0f)
                    lineTo(w, 0f)
                    lineTo(w, h * 0.24f)
                    close()
                }
                drawPath(path = greenCorner, color = priColor.copy(alpha = 0.12f))
            }
        }
    }
}

// DRAGGABLE, ROTATABLE WYSIWYG CANVAS PREVIEW ENGINE
@Composable
fun WYSIWYGCardCanvas(
    card: UserCard,
    selectedKey: String,
    onSelectKey: (String) -> Unit,
    viewModel: CardViewModel,
    isBackSide: Boolean = false
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

    val normalizedDensity = Density(
        density = LocalDensity.current.density,
        fontScale = 1.0f
    )

    CompositionLocalProvider(LocalDensity provides normalizedDensity) {
        val cardBgStart = try { Color(android.graphics.Color.parseColor(card.backgroundColor)) } catch (e: Exception) { Color(0xFF10121A) }
    val cardBgEnd = try { Color(android.graphics.Color.parseColor(card.gradientEndColor)) } catch (e: Exception) { Color(0xFF1E2130) }
    val accentColor = try { Color(android.graphics.Color.parseColor(card.qrCodeColor)) } catch (e: Exception) { Color(0xFFD4AF37) }

    val visibleFields = try {
        val list = mutableListOf<String>()
        val array = JSONArray(card.visibleFieldsJson)
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        list
    } catch (e: Exception) {
        listOf("fullName", "jobTitle", "companyName", "mobileNumber", "email", "website", "address")
    }

    // Determine Shape based on Card Shape parameter
    val cardShapeOutline = when (card.cardShape) {
        "ROUNDED_RECTANGLE" -> RoundedCornerShape(12.dp)
        "CIRCLE" -> CircleShape
        "LEAF_CUT" -> RoundedCornerShape(topStart = 32.dp, bottomEnd = 32.dp, topEnd = 0.dp, bottomStart = 0.dp)
        "HEXAGON" -> object : Shape {
            override fun createOutline(
                size: androidx.compose.ui.geometry.Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                val path = Path().apply {
                    moveTo(size.width * 0.5f, 0f)
                    lineTo(size.width, size.height * 0.22f)
                    lineTo(size.width, size.height * 0.78f)
                    lineTo(size.width * 0.5f, size.height)
                    lineTo(0f, size.height * 0.78f)
                    lineTo(0f, size.height * 0.22f)
                    close()
                }
                return Outline.Generic(path)
            }
        }
        else -> RoundedCornerShape(0.dp)
    }

    // Compose custom draggable container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(cardShapeOutline)
            .background(
                if (card.backgroundType == "GRADIENT") {
                    Brush.linearGradient(listOf(cardBgStart, cardBgEnd))
                } else if (card.backgroundType == "SOLID") {
                    Brush.linearGradient(listOf(cardBgStart, cardBgStart))
                } else {
                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                }
            )
    ) {
        CardTemplateDecorations(card = card, accentColor = accentColor, isBackSide = isBackSide)

        if (card.backgroundType == "PATTERN" && !card.backgroundImage.isNullOrEmpty()) {
            val bgPainter = coil.compose.rememberAsyncImagePainter(model = card.backgroundImage)
            Image(
                painter = bgPainter,
                contentDescription = "Custom Background Pattern",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (card.cardShape == "FOLDED") {
            // Draw a folding line indicator
            Canvas(modifier = Modifier.fillMaxSize()) {
                val midY = size.height / 2f
                val pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = androidx.compose.ui.geometry.Offset(0f, midY),
                    end = androidx.compose.ui.geometry.Offset(size.width, midY),
                    strokeWidth = 2f,
                    pathEffect = pathEffect
                )
            }
        }
        // Outer decorative frames and borders
        when (card.borderStyle) {
            "MINIMAL_GOLD" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .border(1.dp, accentColor.copy(0.6f), RoundedCornerShape(3.dp))
                )
            }
            "MODERN_DOUBLE" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .border(1.5.dp, accentColor, RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .border(0.5.dp, accentColor.copy(0.4f), RoundedCornerShape(1.dp))
                    )
                }
            }
            "CYBER_SLATE" -> {
                Box(modifier = Modifier.align(Alignment.TopStart).size(20.dp, 3.dp).background(Color(0xFF00FFCC)))
                Box(modifier = Modifier.align(Alignment.TopStart).size(3.dp, 20.dp).background(Color(0xFF00FFCC)))
                Box(modifier = Modifier.align(Alignment.BottomEnd).size(20.dp, 3.dp).background(Color(0xFF00FFCC)))
                Box(modifier = Modifier.align(Alignment.BottomEnd).size(3.dp, 20.dp).background(Color(0xFF00FFCC)))
            }
        }

        // Render layers in compiled ordered sorted list of ZIndex sequence
        data class ComponentLayer(
            val id: String,
            val zIndex: Int,
            val content: @Composable BoxScope.() -> Unit
        )

        val elementsList = mutableListOf<ComponentLayer>()

        // Name
        if (visibleFields.contains("fullName") && !isBackSide) {
            elementsList.add(ComponentLayer("fullName", card.fullNameZIndex) {
                WYSIWYGDraggableContainer(
                    id = "fullName",
                    initialX = card.fullNameX,
                    initialY = card.fullNameY,
                    rotation = card.fullNameRotation,
                    scale = card.fullNameScale,
                    selected = selectedKey == "fullName",
                    onSelect = { onSelectKey("fullName") },
                    card = card,
                    viewModel = viewModel,
                    density = density
                ) {
                    val fFamily = when (card.fontStyle) {
                        "Elegant Serif" -> FontFamily.Serif
                        "Tech Clean" -> FontFamily.Monospace
                        "Space Grotesk" -> FontFamily.SansSerif
                        else -> FontFamily.Default
                    }
                    Text(
                        text = card.fullName,
                        color = Color.White,
                        fontSize = card.fullNameSize.sp,
                        fontFamily = fFamily,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start,
                        maxLines = 1
                    )
                }
            })
        }

        // Title
        if (visibleFields.contains("jobTitle") && !isBackSide) {
            elementsList.add(ComponentLayer("jobTitle", card.jobTitleZIndex) {
                WYSIWYGDraggableContainer(
                    id = "jobTitle",
                    initialX = card.jobTitleX,
                    initialY = card.jobTitleY,
                    rotation = card.jobTitleRotation,
                    scale = card.jobTitleScale,
                    selected = selectedKey == "jobTitle",
                    onSelect = { onSelectKey("jobTitle") },
                    card = card,
                    viewModel = viewModel,
                    density = density
                ) {
                    val fFamily = when (card.fontStyle) {
                        "Elegant Serif" -> FontFamily.Serif
                        "Tech Clean" -> FontFamily.Monospace
                        "Space Grotesk" -> FontFamily.SansSerif
                        else -> FontFamily.Default
                    }
                    Text(
                        text = card.jobTitle.uppercase(),
                        color = accentColor,
                        fontSize = card.jobTitleSize.sp,
                        fontFamily = fFamily,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Start,
                        maxLines = 1
                    )
                }
            })
        }

        // Company Name
        if (visibleFields.contains("companyName") && (!isBackSide || card.templateId == "blank")) {
            elementsList.add(ComponentLayer("companyName", card.companyNameZIndex) {
                WYSIWYGDraggableContainer(
                    id = "companyName",
                    initialX = card.companyNameX,
                    initialY = card.companyNameY,
                    rotation = card.companyNameRotation,
                    scale = card.companyNameScale,
                    selected = selectedKey == "companyName",
                    onSelect = { onSelectKey("companyName") },
                    card = card,
                    viewModel = viewModel,
                    density = density
                ) {
                    val fFamily = when (card.fontStyle) {
                        "Elegant Serif" -> FontFamily.Serif
                        "Tech Clean" -> FontFamily.Monospace
                        "Space Grotesk" -> FontFamily.SansSerif
                        else -> FontFamily.Default
                    }
                    Text(
                        text = card.companyName,
                        color = Color.LightGray,
                        fontSize = card.companyNameSize.sp,
                        fontFamily = fFamily,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start,
                        maxLines = 1
                    )
                }
            })
        }

        // Mobile Field
        if (visibleFields.contains("mobileNumber") && !isBackSide) {
            elementsList.add(ComponentLayer("mobileNumber", card.mobileNumberZIndex) {
                WYSIWYGDraggableContainer(
                    id = "mobileNumber",
                    initialX = card.mobileNumberX,
                    initialY = card.mobileNumberY,
                    rotation = card.mobileNumberRotation,
                    scale = card.mobileNumberScale,
                    selected = selectedKey == "mobileNumber",
                    onSelect = { onSelectKey("mobileNumber") },
                    card = card,
                    viewModel = viewModel,
                    density = density
                ) {
                    Text(
                        text = "📞  " + card.mobileNumber,
                        color = Color.White,
                        fontSize = card.mobileNumberSize.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1
                    )
                }
            })
        }

        // Email Field
        if (visibleFields.contains("email") && !isBackSide) {
            elementsList.add(ComponentLayer("email", card.emailZIndex) {
                WYSIWYGDraggableContainer(
                    id = "email",
                    initialX = card.emailX,
                    initialY = card.emailY,
                    rotation = card.emailRotation,
                    scale = card.emailScale,
                    selected = selectedKey == "email",
                    onSelect = { onSelectKey("email") },
                    card = card,
                    viewModel = viewModel,
                    density = density
                ) {
                    Text(
                        text = "✉️  " + card.email,
                        color = Color.White,
                        fontSize = card.emailSize.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1
                    )
                }
            })
        }

        // Website Field
        if (visibleFields.contains("website")) {
            elementsList.add(ComponentLayer("website", card.websiteZIndex) {
                WYSIWYGDraggableContainer(
                    id = "website",
                    initialX = card.websiteX,
                    initialY = card.websiteY,
                    rotation = card.websiteRotation,
                    scale = card.websiteScale,
                    selected = selectedKey == "website",
                    onSelect = { onSelectKey("website") },
                    card = card,
                    viewModel = viewModel,
                    density = density
                ) {
                    Text(
                        text = "🌐  " + card.website,
                        color = Color.White,
                        fontSize = card.websiteSize.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1
                    )
                }
            })
        }

        // Address Field
        if (visibleFields.contains("address")) {
            elementsList.add(ComponentLayer("address", card.addressZIndex) {
                WYSIWYGDraggableContainer(
                    id = "address",
                    initialX = card.addressX,
                    initialY = card.addressY,
                    rotation = card.addressRotation,
                    scale = card.addressScale,
                    selected = selectedKey == "address",
                    onSelect = { onSelectKey("address") },
                    card = card,
                    viewModel = viewModel,
                    density = density
                ) {
                    Text(
                        text = "📍  " + card.address,
                        color = Color.White,
                        fontSize = card.addressSize.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1
                    )
                }
            })
        }

        // Native QR Code Layer representation
        if (card.qrCodeVisible && isBackSide) {
            elementsList.add(ComponentLayer("qrCode", card.qrCodeZIndex) {
                WYSIWYGDraggableContainer(
                    id = "qrCode",
                    initialX = card.qrCodeX,
                    initialY = card.qrCodeY,
                    rotation = card.qrCodeRotation,
                    scale = 1.0f,
                    selected = selectedKey == "qrCode",
                    onSelect = { onSelectKey("qrCode") },
                    card = card,
                    viewModel = viewModel,
                    density = density
                ) {
                    if (card.qrCodeType == "UPLOADED" && !card.qrCodeBase64Image.isNullOrEmpty()) {
                        val base64Bytes = try { android.util.Base64.decode(card.qrCodeBase64Image, android.util.Base64.DEFAULT) } catch (e: Exception) { null }
                        if (base64Bytes != null) {
                            val bmpVal = BitmapFactory.decodeByteArray(base64Bytes, 0, base64Bytes.size)
                            if (bmpVal != null) {
                                Image(
                                    bitmap = bmpVal.asImageBitmap(),
                                    contentDescription = "Custom QR",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(card.qrCodeSize.dp)
                                )
                            }
                        }
                    } else {
                        QRCodeComposable(
                            data = card.qrCodeData,
                            sizeDp = card.qrCodeSize.dp,
                            colorHex = card.qrCodeColor,
                            shape = card.qrCodeShape
                        )
                    }
                }
            })
        }

        // Parse custom design elements using remember blocks to comply with compose compiler try-catch constraints
        val parsedElements = remember(card.designElementsJson) {
            val list = mutableListOf<ParsedDesignElement>()
            try {
                val arr = JSONArray(card.designElementsJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        ParsedDesignElement(
                            id = obj.getString("id"),
                            type = obj.getString("type"),
                            content = obj.getString("content"),
                            x = obj.getDouble("x").toFloat(),
                            y = obj.getDouble("y").toFloat(),
                            colorHex = obj.optString("color", "#FFFFFF"),
                            fontSize = obj.optDouble("fontSize", 14.0).toFloat(),
                            isBold = obj.optBoolean("isBold", false),
                            rotation = obj.optDouble("rotation", 0.0).toFloat(),
                            scale = obj.optDouble("scale", 1.0).toFloat(),
                            zIndex = obj.optInt("zIndex", 0)
                        )
                    )
                }
            } catch (ignored: Exception) {}
            list
        }

        // Add custom parsed design elements layers safely
        parsedElements.forEach { element ->
            elementsList.add(ComponentLayer(element.id, element.zIndex) {
                WYSIWYGDraggableContainer(
                    id = element.id,
                    initialX = element.x,
                    initialY = element.y,
                    rotation = element.rotation,
                    scale = element.scale,
                    selected = selectedKey == element.id,
                    onSelect = { onSelectKey(element.id) },
                    card = card,
                    viewModel = viewModel,
                    density = density
                ) {
                    when (element.type) {
                        "TEXT" -> {
                            Text(
                                text = element.content,
                                color = Color(android.graphics.Color.parseColor(element.colorHex)),
                                fontSize = element.fontSize.sp,
                                fontWeight = if (element.isBold) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        "SHAPE" -> {
                            Box(
                                modifier = Modifier
                                    .size((40 * element.scale).dp)
                                    .background(Color(android.graphics.Color.parseColor(element.colorHex)))
                            )
                        }
                        "UPLOADED_IMAGE_QR" -> {
                            val base64Bytes = try { android.util.Base64.decode(element.content, android.util.Base64.DEFAULT) } catch (e: Exception) { null }
                            if (base64Bytes != null) {
                                val bmpVal = BitmapFactory.decodeByteArray(base64Bytes, 0, base64Bytes.size)
                                if (bmpVal != null) {
                                    Image(
                                        bitmap = bmpVal.asImageBitmap(),
                                        contentDescription = "Uploaded QR Layer",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(50.dp)
                                    )
                                }
                            }
                        }
                        "QR_CODE" -> {
                            QRCodeComposable(
                                data = element.content,
                                sizeDp = 50.dp,
                                colorHex = element.colorHex,
                                shape = "ROUNDED"
                            )
                        }
                    }
                }
            })
        }

        // Centered Backside Branding & Social Links helper layer (if Back Side is active and not blank)
        if (isBackSide && card.templateId != "blank") {
            elementsList.add(ComponentLayer("back_branding", 10) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Golden Flower/Badge Logo Icon representing modern premium studio branding
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(accentColor.copy(0.12f), CircleShape)
                            .border(1.5.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Logo Crown Icon",
                            tint = accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = card.companyName.uppercase(),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "PREMIUM VISITING COLLECTION",
                        color = accentColor,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Social Icons row at the bottom
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (card.facebook.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("f", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(card.facebook, color = Color.Gray, fontSize = 8.sp)
                            }
                        }
                        if (card.instagram.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("ig", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(card.instagram, color = Color.Gray, fontSize = 8.sp)
                            }
                        }
                        if (card.linkedIn.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("in", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(card.linkedIn, color = Color.Gray, fontSize = 8.sp)
                            }
                        }
                    }
                }
            })
        }

        // Sort by Z-Index priority and render!
        elementsList.sortBy { it.zIndex }
        elementsList.forEach { layer ->
            layer.content(this)
        }

        // Real-time Alignments & Snapping Guidelines (Canva style)
        if (selectedKey.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                val neonCyan = Color(0xFF00FFCC).copy(0.6f)
                val centerLineColor = Color.White.copy(0.2f)

                // 1. Center Horiz & Vert guidelines (Pulsing neutral grid lock indicators)
                drawLine(
                    color = centerLineColor,
                    start = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
                    strokeWidth = 1.5f,
                    pathEffect = pathEffect
                )
                drawLine(
                    color = centerLineColor,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                    strokeWidth = 1.5f,
                    pathEffect = pathEffect
                )

                // 2. Element tracking guidelines showing specific lock alignments
                val trackingCoords = when (selectedKey) {
                    "fullName" -> Pair(card.fullNameX, card.fullNameY)
                    "jobTitle" -> Pair(card.jobTitleX, card.jobTitleY)
                    "companyName" -> Pair(card.companyNameX, card.companyNameY)
                    "mobileNumber" -> Pair(card.mobileNumberX, card.mobileNumberY)
                    "email" -> Pair(card.emailX, card.emailY)
                    "website" -> Pair(card.websiteX, card.websiteY)
                    "address" -> Pair(card.addressX, card.addressY)
                    "qrCode" -> Pair(card.qrCodeX, card.qrCodeY)
                    else -> {
                        var found: Pair<Float, Float>? = null
                        try {
                            val array = JSONArray(card.designElementsJson)
                            for (i in 0 until array.length()) {
                                val obj = array.getJSONObject(i)
                                if (obj.getString("id") == selectedKey) {
                                    found = Pair(obj.getDouble("x").toFloat(), obj.getDouble("y").toFloat())
                                    break
                                }
                            }
                        } catch (e: Exception) {}
                        found
                    }
                }

                if (trackingCoords != null) {
                    val (tx, ty) = trackingCoords
                    // Scale values to px based on canvas density scale factor
                    // Let's draw horizontal tracker
                    drawLine(
                        color = neonCyan,
                        start = androidx.compose.ui.geometry.Offset(0f, ty * density),
                        end = androidx.compose.ui.geometry.Offset(size.width, ty * density),
                        strokeWidth = 1f,
                        pathEffect = pathEffect
                    )
                    // Let's draw vertical tracker
                    drawLine(
                        color = neonCyan,
                        start = androidx.compose.ui.geometry.Offset(tx * density, 0f),
                        end = androidx.compose.ui.geometry.Offset(tx * density, size.height),
                        strokeWidth = 1f,
                        pathEffect = pathEffect
                    )
                }
            }
        }

    } }
}

// GENERIC MOVABLE CARD DRAG DETECTOR CONTAINER
@Composable
fun BoxScope.WYSIWYGDraggableContainer(
    id: String,
    initialX: Float,
    initialY: Float,
    rotation: Float,
    scale: Float,
    selected: Boolean,
    onSelect: () -> Unit,
    card: UserCard,
    viewModel: CardViewModel,
    density: Float,
    content: @Composable () -> Unit
) {
    val isLocked = card.lockedElements.split(",").contains(id)

    Box(
        modifier = Modifier
            .offset(x = initialX.dp, y = initialY.dp)
            .graphicsLayer(
                rotationZ = rotation,
                scaleX = scale,
                scaleY = scale
            )
            .pointerInput(id, isLocked) {
                if (!isLocked) {
                    detectTransformGestures { centroid, pan, zoom, rotationChange ->
                        onSelect()
                        
                        val rawNewX = initialX + pan.x / density
                        val rawNewY = initialY + pan.y / density
                        
                        // Enforce 10px snapping intervals or free move
                        val newX = if (card.snapToGrid) {
                            Math.round(rawNewX / 10f) * 10f
                        } else rawNewX
                        val newY = if (card.snapToGrid) {
                            Math.round(rawNewY / 10f) * 10f
                        } else rawNewY

                        val newScale = (scale * zoom).coerceIn(0.40f, 2.80f)
                        val newRot = (rotation + rotationChange) % 360f

                        updateElementSpatial(card, id, newX, newY, newRot, newScale, null, null, viewModel)
                    }
                }
            }
            .border(
                1.dp,
                if (selected) {
                    if (isLocked) Color(0xFFFF5252) else Color(0xFFD4AF37)
                } else Color.Transparent,
                RoundedCornerShape(4.dp)
            )
            .clickable { onSelect() }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        content()

        if (isLocked && selected) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = Color(0xFFFF5252),
                modifier = Modifier
                    .size(12.dp)
                    .background(Color.Black.copy(0.7f), CircleShape)
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
            )
        }

        // Resizing corner handle at bottom right
        if (selected && !isLocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 6.dp, y = 6.dp)
                    .size(10.dp)
                    .background(Color(0xFF00FFCC), CircleShape)
                    .pointerInput(id + "_resize") {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val dragDelta = (dragAmount.x + dragAmount.y) / density
                            val newScale = (scale + dragDelta * 0.02f).coerceIn(0.40f, 2.80f)
                            updateElementSpatial(card, id, null, null, null, newScale, null, null, viewModel)
                        }
                    }
            )

            // Rotation handle at top center (Canva style)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-14).dp)
                    .size(14.dp)
                    .background(Color(0xFF1E2433), CircleShape)
                    .border(1.dp, Color(0xFF00FFCC), CircleShape)
                    .pointerInput(id + "_rotate") {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val dragDelta = dragAmount.x - dragAmount.y
                            val newRot = (rotation + dragDelta * 0.6f) % 360f
                            updateElementSpatial(card, id, null, null, newRot, null, null, null, viewModel)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Rotate",
                    tint = Color(0xFF00FFCC),
                    modifier = Modifier.size(8.dp)
                )
            }
        }
    }
}

// SPATIAL POSITION UPDATER ROUTINE FOR ALL LAYERS
fun updateElementPosition(card: UserCard, key: String, dx: Float, dy: Float, viewModel: CardViewModel) {
    val curX = when (key) {
        "fullName" -> card.fullNameX
        "jobTitle" -> card.jobTitleX
        "companyName" -> card.companyNameX
        "mobileNumber" -> card.mobileNumberX
        "email" -> card.emailX
        "website" -> card.websiteX
        "address" -> card.addressX
        "qrCode" -> card.qrCodeX
        else -> {
            try {
                val array = JSONArray(card.designElementsJson)
                var foundX = 0f
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    if (obj.getString("id") == key) { foundX = obj.getDouble("x").toFloat() }
                }
                foundX
            } catch (e: Exception) { 150f }
        }
    }

    val curY = when (key) {
        "fullName" -> card.fullNameY
        "jobTitle" -> card.jobTitleY
        "companyName" -> card.companyNameY
        "mobileNumber" -> card.mobileNumberY
        "email" -> card.emailY
        "website" -> card.websiteY
        "address" -> card.addressY
        "qrCode" -> card.qrCodeY
        else -> {
            try {
                val array = JSONArray(card.designElementsJson)
                var foundY = 0f
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    if (obj.getString("id") == key) { foundY = obj.getDouble("y").toFloat() }
                }
                foundY
            } catch (e: Exception) { 150f }
        }
    }

    var nextX = curX + dx
    var nextY = curY + dy

    if (card.snapToGrid) {
        nextX = Math.round(nextX / 10f) * 10f
        nextY = Math.round(nextY / 10f) * 10f
    }

    updateElementSpatial(card, key, nextX, nextY, null, null, null, null, viewModel)
}

// Z-INDEX BRING FORWARD OR SEND BACKWARD CONTROLLER
fun updateElementZIndex(card: UserCard, key: String, increment: Boolean, viewModel: CardViewModel) {
    val currentZ = when (key) {
        "fullName" -> card.fullNameZIndex
        "jobTitle" -> card.jobTitleZIndex
        "companyName" -> card.companyNameZIndex
        "mobileNumber" -> card.mobileNumberZIndex
        "email" -> card.emailZIndex
        "website" -> card.websiteZIndex
        "address" -> card.addressZIndex
        "qrCode" -> card.qrCodeZIndex
        else -> {
            try {
                val array = JSONArray(card.designElementsJson)
                var foundZ = 1
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    if (obj.getString("id") == key) { foundZ = obj.getInt("zIndex") }
                }
                foundZ
            } catch (e: Exception) { 1 }
        }
    }

    val finalZ = if (increment) currentZ + 1 else (currentZ - 1).coerceAtLeast(0)
    updateElementSpatial(card, key, null, null, null, null, null, finalZ, viewModel)
}

// SPACIAL FIELD EXTRACTORS
fun getElementRotation(card: UserCard, key: String): Float {
    return when (key) {
        "fullName" -> card.fullNameRotation
        "jobTitle" -> card.jobTitleRotation
        "companyName" -> card.companyNameRotation
        "mobileNumber" -> card.mobileNumberRotation
        "email" -> card.emailRotation
        "website" -> card.websiteRotation
        "address" -> card.addressRotation
        "qrCode" -> card.qrCodeRotation
        else -> {
            try {
                val array = JSONArray(card.designElementsJson)
                var foundRot = 0f
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    if (obj.getString("id") == key) { foundRot = obj.getDouble("rotation").toFloat() }
                }
                foundRot
            } catch (e: Exception) { 0f }
        }
    }
}

fun getElementScale(card: UserCard, key: String): Float {
    return when (key) {
        "fullName" -> card.fullNameScale
        "jobTitle" -> card.jobTitleScale
        "companyName" -> card.companyNameScale
        "mobileNumber" -> card.mobileNumberScale
        "email" -> card.emailScale
        "website" -> card.websiteScale
        "address" -> card.addressScale
        "qrCode" -> card.qrCodeSize / 80f
        else -> {
            try {
                val array = JSONArray(card.designElementsJson)
                var foundSc = 1.0f
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    if (obj.getString("id") == key) { foundSc = obj.getDouble("scale").toFloat() }
                }
                foundSc
            } catch (e: Exception) { 1.0f }
        }
    }
}

// CONSOLIDATED MASTER DATABASE ATOMIC MODIFIER FOR SPATIAL DATA
fun updateElementSpatial(
    card: UserCard,
    key: String,
    newX: Float?,
    newY: Float?,
    newRotation: Float?,
    newScale: Float?,
    newSize: Float?,
    newZIndex: Int?,
    viewModel: CardViewModel
) {
    var updated = card
    when (key) {
        "fullName" -> {
            updated = card.copy(
                fullNameX = newX ?: card.fullNameX,
                fullNameY = newY ?: card.fullNameY,
                fullNameRotation = newRotation ?: card.fullNameRotation,
                fullNameScale = newScale ?: card.fullNameScale,
                fullNameSize = newSize ?: card.fullNameSize,
                fullNameZIndex = newZIndex ?: card.fullNameZIndex
            )
        }
        "jobTitle" -> {
            updated = card.copy(
                jobTitleX = newX ?: card.jobTitleX,
                jobTitleY = newY ?: card.jobTitleY,
                jobTitleRotation = newRotation ?: card.jobTitleRotation,
                jobTitleScale = newScale ?: card.jobTitleScale,
                jobTitleSize = newSize ?: card.jobTitleSize,
                jobTitleZIndex = newZIndex ?: card.jobTitleZIndex
            )
        }
        "companyName" -> {
            updated = card.copy(
                companyNameX = newX ?: card.companyNameX,
                companyNameY = newY ?: card.companyNameY,
                companyNameRotation = newRotation ?: card.companyNameRotation,
                companyNameScale = newScale ?: card.companyNameScale,
                companyNameSize = newSize ?: card.companyNameSize,
                companyNameZIndex = newZIndex ?: card.companyNameZIndex
            )
        }
        "mobileNumber" -> {
            updated = card.copy(
                mobileNumberX = newX ?: card.mobileNumberX,
                mobileNumberY = newY ?: card.mobileNumberY,
                mobileNumberRotation = newRotation ?: card.mobileNumberRotation,
                mobileNumberScale = newScale ?: card.mobileNumberScale,
                mobileNumberSize = newSize ?: card.mobileNumberSize,
                mobileNumberZIndex = newZIndex ?: card.mobileNumberZIndex
            )
        }
        "email" -> {
            updated = card.copy(
                emailX = newX ?: card.emailX,
                emailY = newY ?: card.emailY,
                emailRotation = newRotation ?: card.emailRotation,
                emailScale = newScale ?: card.emailScale,
                emailSize = newSize ?: card.emailSize,
                emailZIndex = newZIndex ?: card.emailZIndex
            )
        }
        "website" -> {
            updated = card.copy(
                websiteX = newX ?: card.websiteX,
                websiteY = newY ?: card.websiteY,
                websiteRotation = newRotation ?: card.websiteRotation,
                websiteScale = newScale ?: card.websiteScale,
                websiteSize = newSize ?: card.websiteSize,
                websiteZIndex = newZIndex ?: card.websiteZIndex
            )
        }
        "address" -> {
            updated = card.copy(
                addressX = newX ?: card.addressX,
                addressY = newY ?: card.addressY,
                addressRotation = newRotation ?: card.addressRotation,
                addressScale = newScale ?: card.addressScale,
                addressSize = newSize ?: card.addressSize,
                addressZIndex = newZIndex ?: card.addressZIndex
            )
        }
        "qrCode" -> {
            updated = card.copy(
                qrCodeX = newX ?: card.qrCodeX,
                qrCodeY = newY ?: card.qrCodeY,
                qrCodeRotation = newRotation ?: card.qrCodeRotation,
                qrCodeSize = if (newScale != null) newScale * 80f else card.qrCodeSize,
                qrCodeZIndex = newZIndex ?: card.qrCodeZIndex
            )
        }
        else -> {
            try {
                val array = JSONArray(card.designElementsJson)
                val nextArray = JSONArray()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    if (obj.getString("id") == key) {
                        if (newX != null) obj.put("x", newX.toDouble())
                        if (newY != null) obj.put("y", newY.toDouble())
                        if (newRotation != null) obj.put("rotation", newRotation.toDouble())
                        if (newScale != null) obj.put("scale", newScale.toDouble())
                        if (newSize != null) obj.put("fontSize", newSize.toDouble())
                        if (newZIndex != null) obj.put("zIndex", newZIndex)
                    }
                    nextArray.put(obj)
                }
                updated = card.copy(designElementsJson = nextArray.toString())
            } catch (ignored: Exception) {}
        }
    }
    viewModel.applyCardEdit(updated)
}

// 1. TOOL PANEL - CARD FIELD EDITORS
@Composable
fun EditorFieldsPanel(card: UserCard, viewModel: CardViewModel) {
    val visibleFields = try {
        val list = mutableListOf<String>()
        val array = JSONArray(card.visibleFieldsJson)
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        list
    } catch (e: Exception) {
        mutableListOf()
    }

    fun toggleFieldVisibility(fieldKey: String, visible: Boolean) {
        val newList = visibleFields.toMutableList()
        if (visible) {
            if (!newList.contains(fieldKey)) newList.add(fieldKey)
        } else {
            newList.remove(fieldKey)
        }

        val arr = JSONArray()
        newList.forEach { arr.put(it) }
        viewModel.applyCardEdit(card.copy(visibleFieldsJson = arr.toString()))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("CARD VISIBILITY & PROFILE FIELDS", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        FieldEditRow("Full Name Logo", card.fullName, { viewModel.applyCardEdit(card.copy(fullName = it)) }, visibleFields.contains("fullName"), { toggleFieldVisibility("fullName", it) })
        FieldEditRow("Corporate Job Title", card.jobTitle, { viewModel.applyCardEdit(card.copy(jobTitle = it)) }, visibleFields.contains("jobTitle"), { toggleFieldVisibility("jobTitle", it) })
        FieldEditRow("Company Brand Name", card.companyName, { viewModel.applyCardEdit(card.copy(companyName = it)) }, visibleFields.contains("companyName"), { toggleFieldVisibility("companyName", it) })
        FieldEditRow("Direct Mobile", card.mobileNumber, { viewModel.applyCardEdit(card.copy(mobileNumber = it)) }, visibleFields.contains("mobileNumber"), { toggleFieldVisibility("mobileNumber", it) })
        FieldEditRow("WhatsApp Contact Link", card.whatsAppNumber, { viewModel.applyCardEdit(card.copy(whatsAppNumber = it)) }, visibleFields.contains("whatsAppNumber"), { toggleFieldVisibility("whatsAppNumber", it) })
        FieldEditRow("Digital Mailbox (Email)", card.email, { viewModel.applyCardEdit(card.copy(email = it)) }, visibleFields.contains("email"), { toggleFieldVisibility("email", it) })
        FieldEditRow("Business Website Link", card.website, { viewModel.applyCardEdit(card.copy(website = it)) }, visibleFields.contains("website"), { toggleFieldVisibility("website", it) })
        FieldEditRow("Physical Address Map", card.address, { viewModel.applyCardEdit(card.copy(address = it)) }, visibleFields.contains("address"), { toggleFieldVisibility("address", it) })
    }
}

@Composable
fun FieldEditRow(
    tagLabel: String,
    textVal: String,
    onTextChange: (String) -> Unit,
    visible: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        color = Color(0xFF131722),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFF222B3A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(tagLabel, color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (visible) "Visible" else "Hidden", color = if (visible) Color(0xFF00FFCC) else Color.Gray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = visible,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00FFCC),
                            checkedTrackColor = Color(0xFF00FFCC).copy(0.3f),
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.scale(0.85f)
                    )
                }
            }

            if (visible) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = textVal,
                    onValueChange = onTextChange,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFD4AF37)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// 2. TOOL PANEL - BACKGROUND & THEME COLOR STYLINGS
@Composable
fun EditorStylingPanel(card: UserCard, viewModel: CardViewModel) {
    val context = LocalContext.current
    val isPremium by viewModel.isUserPremium.collectAsState()
    val bgLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val count = viewModel.prefs.customBackgroundsUploadedCount
            viewModel.prefs.customBackgroundsUploadedCount = count + 1
            viewModel.applyCardEdit(card.copy(
                backgroundType = "PATTERN",
                backgroundImage = uri.toString()
            ))
            Toast.makeText(context, "Custom background applied successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    val presetsColors = listOf("#10121A", "#0B132B", "#0C2322", "#131722", "#28293F", "#1D1D2C", "#141518", "#121212")
    val accentsColors = listOf("#D4AF37", "#00FFCC", "#FF5E7E", "#5BC0BE", "#CBB26A", "#48CAE4", "#FF8C00", "#FFFFFF")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("PALETTE & DESIGN CUSTOMIZER", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.applyCardEdit(card.copy(backgroundType = "GRADIENT")) },
                colors = ButtonDefaults.buttonColors(containerColor = if (card.backgroundType == "GRADIENT") Color(0xFFD4AF37) else Color(0xFF1E2433)),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
            ) {
                Text("Gradient", color = if (card.backgroundType == "GRADIENT") Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { viewModel.applyCardEdit(card.copy(backgroundType = "SOLID")) },
                colors = ButtonDefaults.buttonColors(containerColor = if (card.backgroundType == "SOLID") Color(0xFFD4AF37) else Color(0xFF1E2433)),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
            ) {
                Text("Solid Base Color", color = if (card.backgroundType == "SOLID") Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Text("Solid/Gradient Start Hex", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            presetsColors.forEach { hex ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                        .border(1.5.dp, if (card.backgroundColor.lowercase() == hex.lowercase()) Color.White else Color.Transparent, CircleShape)
                        .clickable { viewModel.applyCardEdit(card.copy(backgroundColor = hex)) }
                )
            }
        }

        if (card.backgroundType == "GRADIENT") {
            Text("Gradient End Hex", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                presetsColors.forEach { hex ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                            .border(1.5.dp, if (card.gradientEndColor.lowercase() == hex.lowercase()) Color.White else Color.Transparent, CircleShape)
                        .clickable { viewModel.applyCardEdit(card.copy(gradientEndColor = hex)) }
                )
            }
        }
    }

        Text("Secondary Accent Branding", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            accentsColors.forEach { hex ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                        .border(1.5.dp, if (card.qrCodeColor.lowercase() == hex.lowercase()) Color.White else Color.Transparent, CircleShape)
                        .clickable { viewModel.applyCardEdit(card.copy(qrCodeColor = hex)) }
                )
            }
        }

        Text("Master Typography Family Style", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Space Grotesk", "Elegant Serif", "Modern Bold", "Tech Clean").forEach { font ->
                val active = card.fontStyle == font
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (active) Color(0xFFD4AF37) else Color(0xFF131722),
                    modifier = Modifier.clickable { viewModel.applyCardEdit(card.copy(fontStyle = font)) }
                ) {
                    Text(
                        text = font,
                        color = if (active) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Text("Silent Frame Silhouette Border", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("NONE", "MINIMAL_GOLD", "MODERN_DOUBLE", "CYBER_SLATE").forEach { style ->
                val active = card.borderStyle == style
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (active) Color(0xFFD4AF37) else Color(0xFF131722),
                    modifier = Modifier.clickable { viewModel.applyCardEdit(card.copy(borderStyle = style)) }
                ) {
                    Text(
                        text = style,
                        color = if (active) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Text("Premium Card Shape & Cutout", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "RECTANGLE" to "Standard Rect",
                "ROUNDED_RECTANGLE" to "Rounded Card",
                "SQUARE" to "Modern Square",
                "VERTICAL" to "Vertical Style",
                "FOLDED" to "Duo Folded",
                "CIRCLE" to "Circle Tag",
                "LEAF_CUT" to "Leaf Cutout",
                "HEXAGON" to "Geo Hexagon"
            ).forEach { (shapeValue, displayName) ->
                val active = card.cardShape == shapeValue
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (active) Color(0xFFD4AF37) else Color(0xFF131722),
                    modifier = Modifier.clickable { viewModel.applyCardEdit(card.copy(cardShape = shapeValue)) }
                ) {
                    Text(
                        text = displayName,
                        color = if (active) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Custom Background Image", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { bgLauncher.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Upload Photo", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            
            if (card.backgroundType == "PATTERN") {
                Button(
                    onClick = { viewModel.applyCardEdit(card.copy(backgroundType = "GRADIENT")) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear Image", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        val uploads = viewModel.prefs.customBackgroundsUploadedCount
        Text(
            text = "Custom Backgrounds Applied: $uploads (Unlimited)",
            color = Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// 3. TOOL PANEL - ADVANCED ACTIVE DECODING DECODER QR ACTIONS & HARDWARE CAMERA UPLOADS
@Composable
fun EditorQRPanel(
    card: UserCard, 
    viewModel: CardViewModel,
    onNavigate: (ActiveScreen) -> Unit
) {
    val context = LocalContext.current
    val isPremium by viewModel.isUserPremium.collectAsState()

    // Pick & Crop upload states
    var verificationResultText by remember { mutableStateOf<String?>(null) }
    var verificationWarningText by remember { mutableStateOf<String?>(null) }
    var showCropDialog by remember { mutableStateOf<Bitmap?>(null) }
    var cropCropBoundary by remember { mutableStateOf(0.15f) }

    // vCard dynamic include options
    var vcardIncludeName by remember { mutableStateOf(true) }
    var vcardIncludeTitle by remember { mutableStateOf(true) }
    var vcardIncludeCompany by remember { mutableStateOf(true) }
    var vcardIncludePhone by remember { mutableStateOf(true) }
    var vcardIncludeEmail by remember { mutableStateOf(true) }
    var vcardIncludeWebsite by remember { mutableStateOf(true) }
    var vcardIncludeAddress by remember { mutableStateOf(true) }

    // vCard custom attributes options
    var vcardUseCustomDetails by remember { mutableStateOf(false) }
    var vcardCustomName by remember { mutableStateOf(card.fullName) }
    var vcardCustomTitle by remember { mutableStateOf(card.jobTitle) }
    var vcardCustomCompany by remember { mutableStateOf(card.companyName) }
    var vcardCustomPhone by remember { mutableStateOf(card.mobileNumber) }
    var vcardCustomEmail by remember { mutableStateOf(card.email) }
    var vcardCustomWebsite by remember { mutableStateOf(card.website) }
    var vcardCustomAddress by remember { mutableStateOf(card.address) }

    // Reactively update vCard QR code data when details or options change
    LaunchedEffect(
        card.fullName,
        card.jobTitle,
        card.companyName,
        card.mobileNumber,
        card.email,
        card.website,
        card.address,
        card.qrCodeType,
        vcardIncludeName,
        vcardIncludeTitle,
        vcardIncludeCompany,
        vcardIncludePhone,
        vcardIncludeEmail,
        vcardIncludeWebsite,
        vcardIncludeAddress,
        vcardUseCustomDetails,
        vcardCustomName,
        vcardCustomTitle,
        vcardCustomCompany,
        vcardCustomPhone,
        vcardCustomEmail,
        vcardCustomWebsite,
        vcardCustomAddress
    ) {
        if (card.qrCodeType == "VCARD") {
            val generated = com.example.utils.QRGenerator.generateVCard(
                fullName = if (vcardUseCustomDetails) vcardCustomName else (if (vcardIncludeName) card.fullName else ""),
                jobTitle = if (vcardUseCustomDetails) vcardCustomTitle else (if (vcardIncludeTitle) card.jobTitle else ""),
                companyName = if (vcardUseCustomDetails) vcardCustomCompany else (if (vcardIncludeCompany) card.companyName else ""),
                phoneNumber = if (vcardUseCustomDetails) vcardCustomPhone else (if (vcardIncludePhone) card.mobileNumber else ""),
                email = if (vcardUseCustomDetails) vcardCustomEmail else (if (vcardIncludeEmail) card.email else ""),
                website = if (vcardUseCustomDetails) vcardCustomWebsite else (if (vcardIncludeWebsite) card.website else ""),
                address = if (vcardUseCustomDetails) vcardCustomAddress else (if (vcardIncludeAddress) card.address else "")
            )
            if (card.qrCodeData != generated) {
                viewModel.applyCardEdit(card.copy(qrCodeData = generated))
            }
        }
    }

    // Launcher for Gallery image upload
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStr: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmapVal = BitmapFactory.decodeStream(inputStr)
                if (bitmapVal != null) {
                    showCropDialog = bitmapVal
                } else {
                    Toast.makeText(context, "Failed to decode loaded image", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error fetching visual: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("AUTOMATED INTEGRATED QR-SYSTEM", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Show Link QR on Card", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Switch(
                checked = card.qrCodeVisible,
                onCheckedChange = { viewModel.applyCardEdit(card.copy(qrCodeVisible = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FFCC))
            )
        }

        if (card.qrCodeVisible) {
            // Options Switcher: Generate QR vs Upload QR Card
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        viewModel.applyCardEdit(card.copy(qrCodeType = "WEBSITE"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (card.qrCodeType != "UPLOADED") Color(0xFF00FFCC) else Color(0xFF1E2433)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                ) {
                    Text("Auto Matrix", color = if (card.qrCodeType != "UPLOADED") Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        viewModel.applyCardEdit(card.copy(qrCodeType = "UPLOADED"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (card.qrCodeType == "UPLOADED") Color(0xFF00FFCC) else Color(0xFF1E2433)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                ) {
                    Text("Upload Existing QR", color = if (card.qrCodeType == "UPLOADED") Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (card.qrCodeType != "UPLOADED") {
                // GENERATION CONFIGS
                Text("Automated Quick QR Actions", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("WEBSITE", "WHATSAPP", "EMAIL", "PHONE", "SOCIAL", "VCARD").forEach { type ->
                        val active = card.qrCodeType == type
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (active) Color(0xFFD4AF37) else Color(0xFF131722),
                            modifier = Modifier.clickable {
                                val data = when (type) {
                                    "WHATSAPP" -> "https://wa.me/${card.whatsAppNumber.replace(" ", "")}"
                                    "EMAIL" -> "mailto:${card.email}"
                                    "PHONE" -> "tel:${card.mobileNumber}"
                                    "SOCIAL" -> "https://instagram.com/${card.instagram}"
                                    "VCARD" -> com.example.utils.QRGenerator.generateVCard(
                                        fullName = if (vcardIncludeName) card.fullName else "",
                                        jobTitle = if (vcardIncludeTitle) card.jobTitle else "",
                                        companyName = if (vcardIncludeCompany) card.companyName else "",
                                        phoneNumber = if (vcardIncludePhone) card.mobileNumber else "",
                                        email = if (vcardIncludeEmail) card.email else "",
                                        website = if (vcardIncludeWebsite) card.website else "",
                                        address = if (vcardIncludeAddress) card.address else ""
                                    )
                                    else -> card.website
                                }
                                viewModel.applyCardEdit(card.copy(qrCodeType = type, qrCodeData = data))
                            }
                        ) {
                            Text(
                                text = if (type == "VCARD") "CONTACT CARD" else type,
                                color = if (active) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (card.qrCodeType == "VCARD") {
                    Surface(
                        color = Color(0xFF131722),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF222B3A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("QR Contact Details Configuration", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Choose between utilizing card details dynamically, or typing custom contact details to be encoded.", color = Color.Gray, fontSize = 11.sp)
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(0.3f), RoundedCornerShape(8.dp))
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (!vcardUseCustomDetails) Color(0xFF1E2433) else Color.Transparent, RoundedCornerShape(6.dp))
                                        .clickable { vcardUseCustomDetails = false }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Card Profile Details", color = if (!vcardUseCustomDetails) Color(0xFF00FFCC) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (vcardUseCustomDetails) Color(0xFF1E2433) else Color.Transparent, RoundedCornerShape(6.dp))
                                        .clickable { vcardUseCustomDetails = true }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Custom Details", color = if (vcardUseCustomDetails) Color(0xFF00FFCC) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (!vcardUseCustomDetails) {
                                Text("Active Front Card Fields Selection", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                VCardFieldToggleRow(label = "Full Name (${card.fullName})", checked = vcardIncludeName) { vcardIncludeName = it }
                                VCardFieldToggleRow(label = "Job Title (${card.jobTitle})", checked = vcardIncludeTitle) { vcardIncludeTitle = it }
                                VCardFieldToggleRow(label = "Company (${card.companyName})", checked = vcardIncludeCompany) { vcardIncludeCompany = it }
                                VCardFieldToggleRow(label = "Phone Number (${card.mobileNumber})", checked = vcardIncludePhone) { vcardIncludePhone = it }
                                VCardFieldToggleRow(label = "Email Address (${card.email})", checked = vcardIncludeEmail) { vcardIncludeEmail = it }
                                VCardFieldToggleRow(label = "Website (${card.website})", checked = vcardIncludeWebsite) { vcardIncludeWebsite = it }
                                VCardFieldToggleRow(label = "Address (${card.address})", checked = vcardIncludeAddress) { vcardIncludeAddress = it }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Custom QR Contact Fields", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "Reset to Card Details", 
                                        color = Color(0xFF00FFCC), 
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            vcardCustomName = card.fullName
                                            vcardCustomTitle = card.jobTitle
                                            vcardCustomCompany = card.companyName
                                            vcardCustomPhone = card.mobileNumber
                                            vcardCustomEmail = card.email
                                            vcardCustomWebsite = card.website
                                            vcardCustomAddress = card.address
                                        }
                                    )
                                }

                                OutlinedTextField(
                                    value = vcardCustomName,
                                    onValueChange = { vcardCustomName = it },
                                    label = { Text("Contact Full Name", fontSize = 11.sp) },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF00FFCC)),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = vcardCustomTitle,
                                    onValueChange = { vcardCustomTitle = it },
                                    label = { Text("Contact Job Title", fontSize = 11.sp) },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF00FFCC)),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = vcardCustomCompany,
                                    onValueChange = { vcardCustomCompany = it },
                                    label = { Text("Contact Company Name", fontSize = 11.sp) },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF00FFCC)),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = vcardCustomPhone,
                                    onValueChange = { vcardCustomPhone = it },
                                    label = { Text("Contact Phone Number", fontSize = 11.sp) },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF00FFCC)),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = vcardCustomEmail,
                                    onValueChange = { vcardCustomEmail = it },
                                    label = { Text("Contact Email Address", fontSize = 11.sp) },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF00FFCC)),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = vcardCustomWebsite,
                                    onValueChange = { vcardCustomWebsite = it },
                                    label = { Text("Contact Website", fontSize = 11.sp) },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF00FFCC)),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = vcardCustomAddress,
                                    onValueChange = { vcardCustomAddress = it },
                                    label = { Text("Contact Address", fontSize = 11.sp) },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF00FFCC)),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Encoded raw vCard preview:", color = Color.Gray, fontSize = 10.sp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(0.3f), RoundedCornerShape(4.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = card.qrCodeData,
                                    color = Color(0xFF00FFCC),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 10,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = card.qrCodeData,
                        onValueChange = { viewModel.applyCardEdit(card.copy(qrCodeData = it)) },
                        label = { Text("Encoded Destination URI Data") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // UPLOAD EXISTING BRIGHT QR BLOCK
                Surface(
                    color = Color(0xFF131722),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF222B3A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Upload camera/gallery physical codes:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { galleryPickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gallery", color = Color.Black, fontSize = 11.sp)
                            }

                            // Simulation Picker fallback
                            Button(
                                onClick = {
                                    val simulatedMatrix = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
                                    val canv = android.graphics.Canvas(simulatedMatrix)
                                    canv.drawColor(android.graphics.Color.WHITE)
                                    val p = android.graphics.Paint().apply { color = android.graphics.Color.BLACK }
                                    canv.drawRect(10f, 10f, 60f, 60f, p)
                                    canv.drawRect(20f, 20f, 50f, 50f, p)
                                    canv.drawRect(140f, 10f, 190f, 60f, p)
                                    canv.drawRect(10f, 140f, 60f, 190f, p)
                                    showCropDialog = simulatedMatrix
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2433)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Simulate Capture", fontSize = 11.sp)
                            }
                        }

                        if (card.qrCodeBase64Image != null) {
                            Text("Current Stored QR Upload Code:", color = Color.Gray, fontSize = 11.sp)
                            val base64Bytes = try { android.util.Base64.decode(card.qrCodeBase64Image, android.util.Base64.DEFAULT) } catch (e: Exception) { null }
                            if (base64Bytes != null) {
                                val bmp = BitmapFactory.decodeByteArray(base64Bytes, 0, base64Bytes.size)
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Stored custom QR",
                                        modifier = Modifier
                                            .size(100.dp)
                                            .align(Alignment.CenterHorizontally)
                                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { viewModel.applyCardEdit(card.copy(qrCodeBase64Image = null)) }) {
                                    Text("Remove Upload", color = Color.Red, fontSize = 11.sp)
                                }
                                Text("Decoded output: ${card.qrCodeData}", color = Color(0xFF00FFCC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Interactive Cropping limits popup dialog
            if (showCropDialog != null) {
                AlertDialog(
                    onDismissRequest = { showCropDialog = null },
                    title = { Text("Crop & Validate QR", color = Color.White) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("Adjust boundaries crop to isolate raw QR patterns cleanly:", color = Color.Gray, fontSize = 11.sp)
                            
                            Box(
                                modifier = Modifier
                                    .size(180.dp)
                                    .background(Color.White)
                                    .align(Alignment.CenterHorizontally)
                                    .border(2.dp, Color(0xFFD4AF37))
                            ) {
                                Image(
                                    bitmap = showCropDialog!!.asImageBitmap(),
                                    contentDescription = "To Crop",
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding((cropCropBoundary * 80).dp)
                                        .border(2.dp, Color.Red, RoundedCornerShape(2.dp))
                                )
                            }

                            Text("Boundary padding range: ${(cropCropBoundary * 100).toInt()}%", color = Color.LightGray, fontSize = 11.sp)
                            Slider(
                                value = cropCropBoundary,
                                onValueChange = { cropCropBoundary = it },
                                valueRange = 0.05f..0.45f,
                                colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val sourceBmp = showCropDialog!!
                                val margin = (sourceBmp.width * cropCropBoundary).toInt().coerceAtLeast(1)
                                val size = (sourceBmp.width - margin * 2).coerceAtLeast(10)
                                try {
                                    val croppedBmp = Bitmap.createBitmap(sourceBmp, margin, margin, size, size)
                                    
                                    var resultData = "https://pillaiplay.com/scanned"
                                    var hasError = false
                                    try {
                                        resultData = QRGenerator.decodeQRCodeFromBitmap(croppedBmp)
                                        verificationResultText = "Successfully Decoded QR: '$resultData'"
                                        verificationWarningText = null
                                    } catch (ex: Exception) {
                                        hasError = true
                                        verificationWarningText = "Warning: Unreadable QR Code pattern detected. Ensure the image is well-lit, non-blurry, and cropped closely. However, you can still place and use it as a custom barcode graphic layer."
                                        verificationResultText = null
                                    }

                                    val stream = java.io.ByteArrayOutputStream()
                                    croppedBmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                    val base64Str = android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.DEFAULT)

                                    viewModel.applyCardEdit(
                                        card.copy(
                                            qrCodeType = "UPLOADED",
                                            qrCodeBase64Image = base64Str,
                                            qrCodeData = resultData
                                        )
                                    )
                                    showCropDialog = null
                                    Toast.makeText(context, if (hasError) "Code uploaded with reading warnings" else "QR Validated & Decoded Successfully!", Toast.LENGTH_LONG).show()
                                } catch (ex: Exception) {
                                    Toast.makeText(context, "Crop bounds fault: ${ex.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Apply Crop")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCropDialog = null }) { Text("Cancel") }
                    },
                    containerColor = Color(0xFF131722)
                )
            }

            // Feedback Toast Messages
            if (verificationResultText != null) {
                Surface(color = Color(0xFF00FFCC).copy(0.15f), border = BorderStroke(1.dp, Color(0xFF00FFCC)), shape = RoundedCornerShape(8.dp)) {
                    Text(verificationResultText!!, color = Color(0xFF00FFCC), fontSize = 11.sp, modifier = Modifier.padding(10.dp))
                }
            }
            if (verificationWarningText != null) {
                Surface(color = Color(0xFFFF5E7E).copy(0.15f), border = BorderStroke(1.dp, Color(0xFFFF5E7E)), shape = RoundedCornerShape(8.dp)) {
                    Text(verificationWarningText!!, color = Color(0xFFFF5E7E), fontSize = 11.sp, lineHeight = 14.sp, modifier = Modifier.padding(10.dp))
                }
            }

            // Pixel Dots styling designs
            Text("QR Pixels Shape Design style", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("SQUARE", "ROUNDED", "CIRCLE").forEach { shape ->
                    val active = card.qrCodeShape == shape
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (active) Color(0xFFD4AF37) else Color(0xFF131722),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.applyCardEdit(card.copy(qrCodeShape = shape)) }
                    ) {
                        Text(
                            text = shape,
                            color = if (active) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            // Color design overlays (unlocked for all users)
            Text("Custom QR Outline Colors", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Box {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("#00FFCC", "#D4AF37", "#FF5E7E", "#5BC0BE", "#FFFFFF").forEach { cHex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(android.graphics.Color.parseColor(cHex)), CircleShape)
                                .border(1.5.dp, if (card.qrCodeColor == cHex) Color.White else Color.Transparent, CircleShape)
                                .clickable {
                                    viewModel.applyCardEdit(card.copy(qrCodeColor = cHex))
                                }
                        )
                    }
                }
            }
        }
    }
}

// 4. TOOL PANEL - INJECT STYLED LOGO, LABELS & EXTRA VIP QR LAYERS
@Composable
fun EditorCustomLayersPanel(
    card: UserCard, 
    viewModel: CardViewModel,
    onNavigate: (ActiveScreen) -> Unit
) {
    val context = LocalContext.current
    val isPremium by viewModel.isUserPremium.collectAsState()
    
    var customTextInput by remember { mutableStateOf("") }
    var stickerColorHex by remember { mutableStateOf("#FFFFFF") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("INJECT USER GRAPHICAL LAYERS", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        // Text blocks insertion
        Surface(
            color = Color(0xFF131722),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF222B3A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add Custom Text Block", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = customTextInput,
                    onValueChange = { customTextInput = it },
                    label = { Text("Display text (e.g. VAT, Slogan, Custom Info)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("#FFFFFF", "#D4AF37", "#00FFCC", "#FF5E7E", "#FF8C00").forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                                .border(1.5.dp, if (stickerColorHex == hex) Color.White else Color.Transparent, CircleShape)
                                .clickable { stickerColorHex = hex }
                        )
                    }
                }

                Button(
                    onClick = {
                        if (customTextInput.isNotEmpty()) {
                            try {
                                val currentArray = JSONArray(card.designElementsJson)
                                val newId = "custom_" + UUID.randomUUID().toString().substring(0, 6)

                                val newObj = org.json.JSONObject().apply {
                                    put("id", newId)
                                    put("type", "TEXT")
                                    put("name", "Custom Slogan text")
                                    put("content", customTextInput)
                                    put("x", 120.0)
                                    put("y", 195.0)
                                    put("color", stickerColorHex)
                                    put("fontSize", 11.0)
                                    put("isBold", false)
                                    put("isItalic", true)
                                    put("isUnderline", false)
                                    put("scale", 1.0)
                                    put("rotation", 0.0)
                                    put("zIndex", currentArray.length() + 10)
                                }
                                currentArray.put(newObj)
                                viewModel.applyCardEdit(card.copy(designElementsJson = currentArray.toString()))
                                customTextInput = ""
                                Toast.makeText(context, "Text injected successfully!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else {
                            Toast.makeText(context, "Input display text first!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Inject Custom Text Layer", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Custom shapes insertions
        Surface(
            color = Color(0xFF131722),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF222B3A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add Custom Decorative Shape", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                Button(
                    onClick = {
                        try {
                            val currentArray = JSONArray(card.designElementsJson)
                            val newId = "custom_shape_" + UUID.randomUUID().toString().substring(0, 6)

                            val newObj = org.json.JSONObject().apply {
                                put("id", newId)
                                put("type", "SHAPE")
                                put("name", "Custom Rect element")
                                put("content", "SQUARE")
                                put("x", 200.0)
                                put("y", 80.0)
                                put("color", card.qrCodeColor)
                                put("fontSize", 14.0)
                                put("isBold", false)
                                put("isItalic", false)
                                put("isUnderline", false)
                                put("scale", 1.0)
                                put("rotation", 0.0)
                                put("zIndex", currentArray.length() + 10)
                            }
                            currentArray.put(newObj)
                            viewModel.applyCardEdit(card.copy(designElementsJson = currentArray.toString()))
                            Toast.makeText(context, "Square design block injected!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Inject Square Accent Block", color = Color.Black)
                }
            }
        }

        // MULTIPLE / ADDITIONAL QR CODES (Fully unlocked)
        Surface(
            color = Color(0xFF131722),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF222B3A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box {
                Column(
                    modifier = Modifier
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Add Multiple QR Codes", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Creates secondary mini QR codes encoded with alternative destination links.", color = Color.Gray, fontSize = 11.sp, lineHeight = 15.sp)

                    Button(
                        onClick = {
                            try {
                                val currentArray = JSONArray(card.designElementsJson)
                                val newId = "custom_extra_qr_" + UUID.randomUUID().toString().substring(0, 6)

                                val newObj = org.json.JSONObject().apply {
                                    put("id", newId)
                                    put("type", "QR_CODE")
                                    put("name", "Extra QR Code")
                                    put("content", "https://pillaiplay.com/social")
                                    put("x", 180.0)
                                    put("y", 150.0)
                                    put("color", "#00FFCC")
                                    put("fontSize", 14.0)
                                    put("isBold", false)
                                    put("isItalic", false)
                                    put("isUnderline", false)
                                    put("scale", 1.0)
                                    put("rotation", 0.0)
                                    put("zIndex", currentArray.length() + 10)
                                }
                                currentArray.put(newObj)
                                viewModel.applyCardEdit(card.copy(designElementsJson = currentArray.toString()))
                                Toast.makeText(context, "Secondary QR code injected successfully!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = true
                    ) {
                        Text("Add Secondary Multi-QR", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Delete Selected Custom Layer component
        Button(
            onClick = {
                viewModel.applyCardEdit(card.copy(designElementsJson = "[]"))
                Toast.makeText(context, "All custom layers successfully wiped!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Red)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Clear All Custom Layers", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// 5. EXPORT CONFIGURABLE OUTPUT WORKSHOP DIALOGS
@Composable
fun ExportSettingsDialog(
    card: UserCard,
    isPremiumAccount: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var exportFormat by remember { mutableStateOf("PNG") }
    var exportQuality by remember { mutableStateOf("STANDARD") }
    var includeBleedAndCropMarks by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export & Share Business Card", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Select output formats compilation configuration:", color = Color.Gray, fontSize = 12.sp)

                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("PNG", "JPG", "PDF").forEach { format ->
                        val active = exportFormat == format
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (active) Color(0xFFD4AF37) else Color(0xFF222B3A),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { exportFormat = format }
                                .padding(2.dp)
                        ) {
                            Text(
                                text = format,
                                color = if (active) Color.Black else Color.White,
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                Text("Rendering Dimensions / Quality:", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QualityOptionRow("Standard resolution (800x480)", "STANDARD", exportQuality == "STANDARD") {
                        exportQuality = "STANDARD"
                    }
                    QualityOptionRow("HD Crisp (1600x960)", "HD", exportQuality == "HD") {
                        exportQuality = "HD"
                    }
                    QualityOptionRow("Ultra HD Pro Vector (3200x1920)", "ULTRA HD", exportQuality == "ULTRA HD") {
                        exportQuality = "ULTRA HD"
                    }
                }

                if (exportFormat == "PDF") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Professional Printing Setup", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Surface(
                        color = Color(0xFF1E2433),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF222B3A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable { includeBleedAndCropMarks = !includeBleedAndCropMarks }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = includeBleedAndCropMarks,
                                onCheckedChange = { includeBleedAndCropMarks = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF00FFCC),
                                    uncheckedColor = Color.Gray,
                                    checkmarkColor = Color.Black
                                ),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Bleed & Corner Crop Marks", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Generates a true-vector PDF with 8% outer border bleed margins and professional corner crop marks for alignment at physical print-shops.",
                                    color = Color.Gray,
                                    fontSize = 9.sp,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetFile = PDFExporter.exportCardToFile(
                        context = context,
                        card = card,
                        format = exportFormat,
                        quality = exportQuality,
                        includeBleedAndCropMarks = includeBleedAndCropMarks
                    )
                    if (targetFile != null && targetFile.exists()) {
                        triggerSystemShare(context, targetFile, exportFormat)
                        onDismiss()
                    } else {
                        Toast.makeText(context, "Export builder pipeline failed", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC))
            ) {
                Text("Render & Share", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = Color(0xFF131722),
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    )
}

@Composable
fun QualityOptionRow(label: String, valKey: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (active) Color(0xFF1E2433) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (active) Color(0xFFD4AF37) else Color(0xFF222B3A)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = active,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFD4AF37))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = Color.White, fontSize = 11.sp)
        }
    }
}

fun triggerSystemShare(context: Context, file: java.io.File, format: String) {
    try {
        val mime = when (format.uppercase()) {
            "PDF" -> "application/pdf"
            "JPG" -> "image/jpeg"
            else -> "image/png"
        }

        val authority = "${context.packageName}.fileprovider"
        val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "My Professional Business Card")
            putExtra(Intent.EXTRA_TEXT, "Created and composed with Pillai'Play Visiting Card Maker!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share business card via..."))
    } catch (e: Exception) {
        Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }
}

@Composable
fun FullscreenIcon(tint: Color = Color.White, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val width = size.width
        val height = size.height
        val strokeWidth = 2.dp.toPx()
        val cornerLen = 5.dp.toPx()
        
        // Top Left Corner
        drawRect(color = tint, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(cornerLen, strokeWidth))
        drawRect(color = tint, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(strokeWidth, cornerLen))
        
        // Top Right Corner
        drawRect(color = tint, topLeft = androidx.compose.ui.geometry.Offset(width - cornerLen, 0f), size = androidx.compose.ui.geometry.Size(cornerLen, strokeWidth))
        drawRect(color = tint, topLeft = androidx.compose.ui.geometry.Offset(width - strokeWidth, 0f), size = androidx.compose.ui.geometry.Size(strokeWidth, cornerLen))
        
        // Bottom Left Corner
        drawRect(color = tint, topLeft = androidx.compose.ui.geometry.Offset(0f, height - strokeWidth), size = androidx.compose.ui.geometry.Size(cornerLen, strokeWidth))
        drawRect(color = tint, topLeft = androidx.compose.ui.geometry.Offset(0f, height - cornerLen), size = androidx.compose.ui.geometry.Size(strokeWidth, cornerLen))
        
        // Bottom Right Corner
        drawRect(color = tint, topLeft = androidx.compose.ui.geometry.Offset(width - cornerLen, height - strokeWidth), size = androidx.compose.ui.geometry.Size(cornerLen, strokeWidth))
        drawRect(color = tint, topLeft = androidx.compose.ui.geometry.Offset(width - strokeWidth, height - cornerLen), size = androidx.compose.ui.geometry.Size(strokeWidth, cornerLen))
    }
}

@Composable
fun FullscreenExitIcon(tint: Color = Color.White, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(16.dp)) {
        val width = size.width
        val height = size.height
        val strokeWidth = 2.dp.toPx()
        val cornerLen = 5.dp.toPx()
        val offset = 2.dp.toPx()
        
        // Top Left pointing inward
        drawRect(color = tint, topLeft = androidx.compose.ui.geometry.Offset(offset, offset + cornerLen - strokeWidth), size = androidx.compose.ui.geometry.Size(cornerLen, strokeWidth))
        drawRect(color = tint, topLeft = androidx.compose.ui.geometry.Offset(offset + cornerLen - strokeWidth, offset), size = androidx.compose.ui.geometry.Size(strokeWidth, cornerLen))
        
        // Top Right pointing inward
        drawRect(color = tint, topLeft = androidx.compose.ui.geometry.Offset(width - offset - cornerLen, offset + cornerLen - strokeWidth), size = androidx.compose.ui.geometry.Size(cornerLen, strokeWidth))
        drawRect(color = tint, topLeft = androidx.compose.ui.geometry.Offset(width - offset - strokeWidth, offset), size = androidx.compose.ui.geometry.Size(strokeWidth, cornerLen))
        
        // Bottom Left pointing inward
        drawRect(color = tint, topLeft = androidx.compose.ui.geometry.Offset(offset, height - offset - cornerLen), size = androidx.compose.ui.geometry.Size(cornerLen, strokeWidth))
        drawRect(color = tint, topLeft = androidx.compose.ui.geometry.Offset(offset + cornerLen - strokeWidth, height - offset - cornerLen), size = androidx.compose.ui.geometry.Size(strokeWidth, cornerLen))
        
        // Bottom Right pointing inward
        drawRect(color = tint, topLeft = androidx.compose.ui.geometry.Offset(width - offset - cornerLen, height - offset - cornerLen), size = androidx.compose.ui.geometry.Size(cornerLen, strokeWidth))
        drawRect(color = tint, topLeft = androidx.compose.ui.geometry.Offset(width - offset - strokeWidth, height - offset - cornerLen), size = androidx.compose.ui.geometry.Size(strokeWidth, cornerLen))
    }
}

// ==========================================
// COMPANION ADAPTIVE SUB-COMPOSABLES
// ==========================================

@Composable
fun CompactTopBar(
    activeCard: UserCard,
    viewModel: CardViewModel,
    onNavigate: (ActiveScreen) -> Unit,
    onRenameClick: () -> Unit,
    onExportClick: () -> Unit,
    onFullscreenClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F111A))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconButton(
                onClick = { onNavigate(ActiveScreen.DASHBOARD) },
                modifier = Modifier.size(32.dp).background(Color(0xFF131722), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.clickable { onRenameClick() }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = activeCard.cardName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.widthIn(max = 120.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename",
                        modifier = Modifier.size(11.dp),
                        tint = Color(0xFFD4AF37)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            val autoSaveEnabled by viewModel.isAutoSaveEnabled.collectAsState()
            val autoSaveStatus by viewModel.autoSaveStatus.collectAsState()
            val autoSaveIntervalSeconds by viewModel.autoSaveIntervalSeconds.collectAsState()
            var showAutoSaveDialog by remember { mutableStateOf(false) }

            Surface(
                color = if (autoSaveEnabled) Color(0xFF1E2433) else Color(0x33FF0000),
                border = BorderStroke(0.5.dp, if (autoSaveEnabled) Color(0xFF00FFCC).copy(0.3f) else Color.Red.copy(0.3f)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .clickable { showAutoSaveDialog = true }
                    .padding(vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(
                                color = if (autoSaveStatus == "Saving...") Color(0xFFFFB300)
                                        else if (autoSaveEnabled) Color(0xFF00FFCC)
                                        else Color.Red,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (autoSaveStatus.startsWith("Saved at")) {
                            autoSaveStatus
                        } else if (autoSaveStatus == "Saving...") {
                            "Saving..."
                        } else if (autoSaveEnabled) {
                            "Auto-Save"
                        } else {
                            "Auto-Save Off"
                        },
                        color = Color.LightGray,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            var showSaveTemplateDialog by remember { mutableStateOf(false) }
            var templateNameInput by remember { mutableStateOf("${activeCard.cardName} Template") }

            Surface(
                color = Color(0xFF1B223C),
                border = BorderStroke(0.5.dp, Color(0xFFD4AF37).copy(0.4f)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .clickable { showSaveTemplateDialog = true }
                    .padding(vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Save Template",
                        tint = Color(0xFFD4AF37),
                        modifier = Modifier.size(9.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Save Template",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showSaveTemplateDialog) {
                AlertDialog(
                    onDismissRequest = { showSaveTemplateDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFD4AF37),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Save as My Template",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    containerColor = Color(0xFF0F111A),
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Store your exact current layout, colors, fonts, backgrounds, stamp sizes, QR/logo positions locally. You can use it as a base when creating new cards!",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                            OutlinedTextField(
                                value = templateNameInput,
                                onValueChange = { templateNameInput = it },
                                label = { Text("Template Name", color = Color.Gray, fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedLabelColor = Color(0xFFD4AF37)
                                )
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (templateNameInput.isNotEmpty()) {
                                    viewModel.saveAsMyTemplate(activeCard, templateNameInput)
                                    showSaveTemplateDialog = false
                                }
                            }
                        ) {
                            Text("Save Template", color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSaveTemplateDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }
                )
            }

            if (showAutoSaveDialog) {
                AlertDialog(
                    onDismissRequest = { showAutoSaveDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color(0xFF00FFCC),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Auto-Save Prefs",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    containerColor = Color(0xFF0F111A),
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Pillai'Play Visiting Card Maker stores modification milestones inside your device local database sandbox seamlessly.",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )

                            HorizontalDivider(color = Color(0xFF222B3A))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Periodic Backup Loop", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Backs up on modification", color = Color.Gray, fontSize = 10.sp)
                                }
                                Switch(
                                    checked = autoSaveEnabled,
                                    onCheckedChange = { viewModel.toggleAutoSave(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF00FFCC),
                                        checkedTrackColor = Color(0xFF00FFCC).copy(0.3f),
                                        uncheckedThumbColor = Color.LightGray,
                                        uncheckedTrackColor = Color.DarkGray
                                    )
                                )
                            }

                            if (autoSaveEnabled) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Save Interval Timer", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("${autoSaveIntervalSeconds}s", color = Color(0xFF00FFCC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Slider(
                                        value = autoSaveIntervalSeconds.toFloat(),
                                        onValueChange = { viewModel.updateAutoSaveInterval(it.toInt()) },
                                        valueRange = 5f..60f,
                                        steps = 10,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFF00FFCC),
                                            activeTrackColor = Color(0xFF00FFCC),
                                            inactiveTrackColor = Color.DarkGray
                                        )
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("5s (Rapid)", color = Color.Gray, fontSize = 9.sp)
                                        Text("60s (Slow)", color = Color.Gray, fontSize = 9.sp)
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFF222B3A))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (autoSaveEnabled) Color(0xFF00FFCC) else Color.Red, CircleShape)
                                )
                                Text(
                                    text = "Status: $autoSaveStatus",
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { showAutoSaveDialog = false }
                        ) {
                            Text("Done", color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = { viewModel.undo() },
                enabled = viewModel.hasUndo(),
                modifier = Modifier
                    .size(30.dp)
                    .background(if (viewModel.hasUndo()) Color(0xFF131722) else Color.Transparent, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Undo",
                    modifier = Modifier.size(15.dp),
                    tint = if (viewModel.hasUndo()) Color(0xFF00FFCC) else Color.DarkGray
                )
            }

            IconButton(
                onClick = { viewModel.redo() },
                enabled = viewModel.hasRedo(),
                modifier = Modifier
                    .size(30.dp)
                    .background(if (viewModel.hasRedo()) Color(0xFF131722) else Color.Transparent, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Redo",
                    modifier = Modifier.size(15.dp),
                    tint = if (viewModel.hasRedo()) Color(0xFF00FFCC) else Color.DarkGray
                )
            }

            IconButton(
                onClick = { onFullscreenClick() },
                modifier = Modifier
                    .size(30.dp)
                    .background(Color(0xFF131722), CircleShape)
            ) {
                FullscreenIcon(tint = Color(0xFF00FFCC))
            }

            Button(
                onClick = { onExportClick() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Export",
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CompactZoomController(
    currentZoom: Float,
    isFit: Boolean,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF10121A).copy(0.85f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF222B3A)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            IconButton(
                onClick = onZoomOut,
                modifier = Modifier.size(24.dp)
            ) {
                Text("-", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "${(currentZoom * 100).toInt()}%${if (isFit) " (Fit)" else ""}",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onZoomIn,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Zoom In",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(14.dp)
                    .background(Color.Gray)
            )
            TextButton(
                onClick = onReset,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Text("Fit", color = Color(0xFF00FFCC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LayerShortcutsRow(
    nativeLayerTitles: Map<String, String>,
    visibleFields: List<String>,
    customLayersList: List<Pair<String, String>>,
    selectedElementKey: String,
    onLayerSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F111A))
            .border(BorderStroke(1.dp, Color(0xFF131722)))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.List,
            contentDescription = "Layers List",
            tint = Color.Gray,
            modifier = Modifier.size(14.dp)
        )
        nativeLayerTitles.forEach { (key, title) ->
            val isVisible = key == "qrCode" || visibleFields.contains(key)
            if (isVisible) {
                val isSelected = selectedElementKey == key
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFFD4AF37) else Color(0xFF1E2433).copy(0.6f),
                    border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF222B3A)),
                    modifier = Modifier.clickable { onLayerSelect(key) }
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        customLayersList.forEach { (key, type) ->
            val isSelected = selectedElementKey == key
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Color(0xFF00FFCC) else Color(0xFF1E2433).copy(0.6f),
                border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF222B3A)),
                modifier = Modifier.clickable { onLayerSelect(key) }
            ) {
                Text(
                    text = "Layer: $type",
                    color = if (isSelected) Color.Black else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun DpadNudgeSlidersPanel(
    selectedElementKey: String,
    activeCard: UserCard,
    viewModel: CardViewModel,
    isDpadCollapsed: Boolean,
    onDpadCollapseToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF10121A))
            .border(BorderStroke(1.dp, Color(0xFF1E2433)))
            .padding(horizontal = 12.dp, vertical = if (isDpadCollapsed) 4.dp else 8.dp)
    ) {
        if (isDpadCollapsed) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NUDGE:",
                        color = Color(0xFFD4AF37),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "[${selectedElementKey.uppercase()}]",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.widthIn(max = 140.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { updateElementPosition(activeCard, selectedElementKey, -4f, 0f, viewModel) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Left", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                    }
                    IconButton(
                        onClick = { updateElementPosition(activeCard, selectedElementKey, 0f, -4f, viewModel) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                    }
                    IconButton(
                        onClick = { updateElementPosition(activeCard, selectedElementKey, 0f, 4f, viewModel) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                    }
                    IconButton(
                        onClick = { updateElementPosition(activeCard, selectedElementKey, 4f, 0f, viewModel) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Right", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                    }

                    Box(modifier = Modifier.width(1.dp).height(14.dp).background(Color.DarkGray))

                    val isLocked = activeCard.lockedElements.split(",").contains(selectedElementKey)
                    IconButton(
                        onClick = {
                            val newList = if (isLocked) {
                                activeCard.lockedElements.split(",").filter { it != selectedElementKey }.joinToString(",")
                            } else {
                                (activeCard.lockedElements.split(",") + selectedElementKey).filter { it.isNotEmpty() }.joinToString(",")
                            }
                            viewModel.applyCardEdit(activeCard.copy(lockedElements = newList))
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Position",
                            tint = if (isLocked) Color(0xFFFF5252) else Color.LightGray.copy(0.4f),
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(14.dp).background(Color.DarkGray))

                    TextButton(
                        onClick = { onDpadCollapseToggle(false) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("+ Sliders", color = Color(0xFF00FFCC), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LAYER: ${selectedElementKey.uppercase()}",
                        color = Color(0xFFD4AF37),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "Snap: " + (if (activeCard.snapToGrid) "Active (10px)" else "Inactive"),
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { updateElementPosition(activeCard, selectedElementKey, -4f, 0f, viewModel) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Left", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { updateElementPosition(activeCard, selectedElementKey, 0f, -4f, viewModel) },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                        IconButton(
                            onClick = { updateElementPosition(activeCard, selectedElementKey, 0f, 4f, viewModel) },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                    IconButton(
                        onClick = { updateElementPosition(activeCard, selectedElementKey, 4f, 0f, viewModel) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Right", tint = Color.White, modifier = Modifier.size(14.dp))
                    }

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.DarkGray))

                    IconButton(
                        onClick = { updateElementZIndex(activeCard, selectedElementKey, true, viewModel) },
                        modifier = Modifier.background(Color(0xFF1E2433), RoundedCornerShape(4.dp)).size(28.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Bring Forward", tint = Color(0xFF00FFCC), modifier = Modifier.size(14.dp))
                    }
                    IconButton(
                        onClick = { updateElementZIndex(activeCard, selectedElementKey, false, viewModel) },
                        modifier = Modifier.background(Color(0xFF1E2433), RoundedCornerShape(4.dp)).size(28.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Send Backward", tint = Color(0xFFFF5E7E), modifier = Modifier.size(14.dp))
                    }

                    Icon(
                        imageVector = if (activeCard.snapToGrid) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = "Snap toggle",
                        tint = if (activeCard.snapToGrid) Color(0xFF00FFCC) else Color.Gray,
                        modifier = Modifier.size(20.dp).clickable { viewModel.applyCardEdit(activeCard.copy(snapToGrid = !activeCard.snapToGrid)) }
                    )

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.DarkGray))

                    val isLocked = activeCard.lockedElements.split(",").contains(selectedElementKey)
                    IconButton(
                        onClick = {
                            val newList = if (isLocked) {
                                activeCard.lockedElements.split(",").filter { it != selectedElementKey }.joinToString(",")
                            } else {
                                (activeCard.lockedElements.split(",") + selectedElementKey).filter { it.isNotEmpty() }.joinToString(",")
                            }
                            viewModel.applyCardEdit(activeCard.copy(lockedElements = newList))
                        },
                        modifier = Modifier.background(if (isLocked) Color(0xFFFF5252).copy(0.15f) else Color(0xFF1E2433), RoundedCornerShape(4.dp)).size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Position",
                            tint = if (isLocked) Color(0xFFFF5252) else Color.LightGray.copy(0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.DarkGray))

                    IconButton(onClick = { onDpadCollapseToggle(true) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Collapse Nudge", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Rotate", tint = Color.Gray, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rotate", color = Color.Gray, fontSize = 9.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    var rotDegrees by remember(selectedElementKey) { mutableStateOf(getElementRotation(activeCard, selectedElementKey)) }
                    Slider(
                        value = rotDegrees,
                        onValueChange = {
                            rotDegrees = it
                            updateElementSpatial(activeCard, selectedElementKey, null, null, it, null, null, null, viewModel)
                        },
                        valueRange = -180f..180f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00FFCC), activeTrackColor = Color(0xFF00FFCC)),
                        modifier = Modifier.height(26.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Star, contentDescription = "Scale", tint = Color.Gray, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scale", color = Color.Gray, fontSize = 9.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    var elementScale by remember(selectedElementKey) { mutableStateOf(getElementScale(activeCard, selectedElementKey)) }
                    Slider(
                        value = elementScale,
                        onValueChange = {
                            elementScale = it
                            updateElementSpatial(activeCard, selectedElementKey, null, null, null, it, null, null, viewModel)
                        },
                        valueRange = 0.5f..2.5f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFD4AF37), activeTrackColor = Color(0xFFD4AF37)),
                        modifier = Modifier.height(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ToolboxHeightSelectorHeader(
    bottomPanelExpandedLevel: Int,
    onHeightChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1F2C))
            .border(BorderStroke(1.dp, Color(0xFF2E3547)))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Resize Panel",
                tint = Color(0xFFD4AF37),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "EDITOR TOOLBOX PANEL",
                color = Color.LightGray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Height:", color = Color.Gray, fontSize = 9.sp)
            Text(
                text = "HIDE",
                color = if (bottomPanelExpandedLevel == 0) Color(0xFF00FFCC) else Color.Gray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onHeightChange(0) }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
            Text(
                text = "COMPACT",
                color = if (bottomPanelExpandedLevel == 1) Color(0xFF00FFCC) else Color.Gray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onHeightChange(1) }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
            Text(
                text = "NORMAL",
                color = if (bottomPanelExpandedLevel == 2) Color(0xFF00FFCC) else Color.Gray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onHeightChange(2) }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
            Text(
                text = "TALL",
                color = if (bottomPanelExpandedLevel == 3) Color(0xFF00FFCC) else Color.Gray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onHeightChange(3) }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun VCardFieldToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = Color.LightGray,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF00FFCC),
                uncheckedColor = Color.Gray,
                checkmarkColor = Color.Black
            ),
            modifier = Modifier.size(24.dp)
        )
    }
}

