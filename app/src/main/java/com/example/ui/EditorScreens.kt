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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    // Active state tracker for designer toolbox selection
    var activeToolTab by remember { mutableStateOf("FIELDS") }
    var selectedElementKey by remember { mutableStateOf("fullName") }
    var editorZoomFactor by remember { mutableStateOf(1.0f) }

    // Dialog trigger states
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf(activeCard.cardName) }
    var showExportSheet by remember { mutableStateOf(false) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07080C))
    ) {
        // EDITOR TOPBAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onNavigate(ActiveScreen.DASHBOARD) },
                    modifier = Modifier.background(Color(0xFF131722), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(24.dp), tint = Color.White)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.clickable {
                    renameInput = activeCard.cardName
                    showRenameDialog = true
                }) {
                    Text(
                        activeCard.cardName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.widthIn(max = 120.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Rename template", color = Color(0xFFD4AF37), fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color(0xFFD4AF37))
                    }
                }
            }

            // Undo, Redo, Export
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { viewModel.undo() },
                    enabled = viewModel.hasUndo(),
                    modifier = Modifier.background(if (viewModel.hasUndo()) Color(0xFF131722) else Color.Transparent, CircleShape)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Undo Action",
                        modifier = Modifier.size(20.dp),
                        tint = if (viewModel.hasUndo()) Color(0xFF00FFCC) else Color.DarkGray
                    )
                }

                IconButton(
                    onClick = { viewModel.redo() },
                    enabled = viewModel.hasRedo(),
                    modifier = Modifier.background(if (viewModel.hasRedo()) Color(0xFF131722) else Color.Transparent, CircleShape)
                ) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "Redo Action",
                        modifier = Modifier.size(20.dp),
                        tint = if (viewModel.hasRedo()) Color(0xFF00FFCC) else Color.DarkGray
                    )
                }

                Button(
                    onClick = { showExportSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // SCROLLABLE FULL PREVIEW WITH FIT-TO-SCREEN ZOOM CAPABILITIES
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.40f)
                .background(Color(0xFF0F111A))
                .border(BorderStroke(1.dp, Color(0xFF1E2433))),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(24.dp)
                        .width((400 * editorZoomFactor).dp)
                        .height((240 * editorZoomFactor).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(400.dp)
                            .height(240.dp)
                            .scale(editorZoomFactor)
                    ) {
                        WYSIWYGCardCanvas(
                            card = activeCard,
                            selectedKey = selectedElementKey,
                            onSelectKey = { selectedElementKey = it },
                            viewModel = viewModel
                        )
                    }
                }
            }

            // Floating Zoom Controller Panel overlay
            Surface(
                color = Color(0xFF10121A).copy(0.85f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF222B3A)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    IconButton(
                        onClick = { editorZoomFactor = (editorZoomFactor - 0.15f).coerceIn(0.5f, 2.5f) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("-", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text(
                        "${(editorZoomFactor * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { editorZoomFactor = (editorZoomFactor + 0.15f).coerceIn(0.5f, 2.5f) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(16.dp)
                            .background(Color.Gray)
                    )
                    TextButton(
                        onClick = { editorZoomFactor = 1.0f },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Reset", color = Color(0xFF00FFCC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // HORIZONTAL LAYER SELECTOR SHORTCUTS GRID
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F111A))
                .border(BorderStroke(1.dp, Color(0xFF131722)))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.List, contentDescription = "Layers List", tint = Color.Gray, modifier = Modifier.size(16.dp))
            nativeLayerTitles.forEach { (key, title) ->
                val isVisible = key == "qrCode" || visibleFields.contains(key)
                if (isVisible) {
                    val isSelected = selectedElementKey == key
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFFD4AF37) else Color(0xFF1E2433).copy(0.6f),
                        border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF222B3A)),
                        modifier = Modifier.clickable { selectedElementKey = key }
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Custom design element layers rendered safely without exceptions
            customLayersList.forEach { (key, type) ->
                val isSelected = selectedElementKey == key
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) Color(0xFF00FFCC) else Color(0xFF1E2433).copy(0.6f),
                    border = BorderStroke(1.dp, if (isSelected) Color.White else Color(0xFF222B3A)),
                    modifier = Modifier.clickable { selectedElementKey = key }
                ) {
                    Text(
                        text = "Layer: $type",
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // LAYER MANIPULATORS D-PAD CONTROLLER & COMMAND CONSOLE
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF10121A))
                .border(BorderStroke(1.dp, Color(0xFF1E2433)))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ACTIVE LAYER: ${selectedElementKey.uppercase()}",
                        color = Color(0xFFD4AF37),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Snap-to-Grid: " + (if (activeCard.snapToGrid) "Active (10px)" else "Inactive (Free format)"),
                        color = Color.Gray,
                        fontSize = 9.sp
                    )
                }

                // D-Pad and Spatial Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Position Increments/Decrements Nudge Controls
                    IconButton(onClick = { updateElementPosition(activeCard, selectedElementKey, -4f, 0f, viewModel) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Left", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { updateElementPosition(activeCard, selectedElementKey, 0f, -4f, viewModel) }) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { updateElementPosition(activeCard, selectedElementKey, 0f, 4f, viewModel) }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(onClick = { updateElementPosition(activeCard, selectedElementKey, 4f, 0f, viewModel) }) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Right", tint = Color.White, modifier = Modifier.size(16.dp))
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(Color.DarkGray)
                    )

                    // LAYER REORDERING controls (Bring Forward / Send Backward)
                    IconButton(
                        onClick = { updateElementZIndex(activeCard, selectedElementKey, true, viewModel) },
                        modifier = Modifier
                            .background(Color(0xFF1E2433), RoundedCornerShape(4.dp))
                            .size(32.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Bring Forward", tint = Color(0xFF00FFCC), modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { updateElementZIndex(activeCard, selectedElementKey, false, viewModel) },
                        modifier = Modifier
                            .background(Color(0xFF1E2433), RoundedCornerShape(4.dp))
                            .size(32.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Send Backward", tint = Color(0xFFFF5E7E), modifier = Modifier.size(18.dp))
                    }

                    // Snap to Grid Check action Box
                    Icon(
                        imageVector = if (activeCard.snapToGrid) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = "Snap toggle",
                        tint = if (activeCard.snapToGrid) Color(0xFF00FFCC) else Color.Gray,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { viewModel.applyCardEdit(activeCard.copy(snapToGrid = !activeCard.snapToGrid)) }
                    )
                }
            }

            // Scale and Rotation slider control row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Rotation Control
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
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF00FFCC), activeTrackColor = Color(0xFF00FFCC))
                    )
                }

                // Scale / Size Font Control
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
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFD4AF37), activeTrackColor = Color(0xFFD4AF37))
                    )
                }
            }
        }

        // TOOLBOX NAVIGATION TABS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131722))
                .border(BorderStroke(1.dp, Color(0xFF1E2433))),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            ToolboxTabButton("Profile Fields", activeToolTab == "FIELDS") { activeToolTab = "FIELDS" }
            ToolboxTabButton("Styling & Font", activeToolTab == "STYLING") { activeToolTab = "STYLING" }
            ToolboxTabButton("QR Manager", activeToolTab == "QR_CODE") { activeToolTab = "QR_CODE" }
            ToolboxTabButton("Add Custom", activeToolTab == "CUSTOM_LAYERS") { activeToolTab = "CUSTOM_LAYERS" }
        }

        // ACTIVE TOOL PANEL CONTENT
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.60f)
                .background(Color(0xFF0A0C16))
        ) {
            when (activeToolTab) {
                "FIELDS" -> EditorFieldsPanel(activeCard, viewModel)
                "STYLING" -> EditorStylingPanel(activeCard, viewModel)
                "QR_CODE" -> EditorQRPanel(activeCard, viewModel, onNavigate)
                "CUSTOM_LAYERS" -> EditorCustomLayersPanel(activeCard, viewModel, onNavigate)
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

// DRAGGABLE, ROTATABLE WYSIWYG CANVAS PREVIEW ENGINE
@Composable
fun WYSIWYGCardCanvas(
    card: UserCard,
    selectedKey: String,
    onSelectKey: (String) -> Unit,
    viewModel: CardViewModel
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

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

    // Compose custom draggable container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (card.backgroundType == "GRADIENT") {
                    Brush.linearGradient(listOf(cardBgStart, cardBgEnd))
                } else {
                    Brush.linearGradient(listOf(cardBgStart, cardBgStart))
                }
            )
    ) {
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
        if (visibleFields.contains("fullName")) {
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
        if (visibleFields.contains("jobTitle")) {
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
        if (visibleFields.contains("companyName")) {
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
        if (visibleFields.contains("mobileNumber")) {
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
        if (visibleFields.contains("email")) {
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
        if (card.qrCodeVisible) {
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

        // Sort by Z-Index priority and render!
        elementsList.sortBy { it.zIndex }
        elementsList.forEach { layer ->
            layer.content(this)
        }

        // WATERMARK IF STANDALONE SANDBOX FREE MODE
        if (!card.isPremium) {
            Text(
                text = "Made by Pillai\'Play Maker",
                color = Color.LightGray.copy(0.40f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 12.dp)
            )
        }
    }
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
    Box(
        modifier = Modifier
            .offset(x = initialX.dp, y = initialY.dp)
            .graphicsLayer(
                rotationZ = rotation,
                scaleX = scale,
                scaleY = scale
            )
            .pointerInput(id) {
                detectDragGestures(
                    onDragStart = { onSelect() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val rawNewX = initialX + dragAmount.x / density
                        val rawNewY = initialY + dragAmount.y / density
                        
                        // Enforce 10px snapping intervals or free move
                        val newX = if (card.snapToGrid) {
                            Math.round(rawNewX / 10f) * 10f
                        } else rawNewX
                        val newY = if (card.snapToGrid) {
                            Math.round(rawNewY / 10f) * 10f
                        } else rawNewY

                        updateElementSpatial(card, id, newX, newY, null, null, null, null, viewModel)
                    }
                )
            }
            .border(
                1.dp,
                if (selected) Color(0xFFD4AF37) else Color.Transparent,
                RoundedCornerShape(4.dp)
            )
            .clickable { onSelect() }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        content()

        // Double Click/Tap resizing corner handle at bottom right
        if (selected) {
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
                    listOf("WEBSITE", "WHATSAPP", "EMAIL", "PHONE", "SOCIAL").forEach { type ->
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
                                    else -> card.website
                                }
                                viewModel.applyCardEdit(card.copy(qrCodeType = type, qrCodeData = data))
                            }
                        ) {
                            Text(
                                text = type,
                                color = if (active) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = card.qrCodeData,
                    onValueChange = { viewModel.applyCardEdit(card.copy(qrCodeData = it)) },
                    label = { Text("Encoded Destination URI Data") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
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

            // Color design overlays (Only for VIP subscribers!)
            Text("Premium Custom QR Outline Colors", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Box {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .alpha(if (isPremium) 1f else 0.45f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("#00FFCC", "#D4AF37", "#FF5E7E", "#5BC0BE", "#FFFFFF").forEach { cHex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(android.graphics.Color.parseColor(cHex)), CircleShape)
                                .border(1.5.dp, if (card.qrCodeColor == cHex) Color.White else Color.Transparent, CircleShape)
                                .clickable(enabled = isPremium) {
                                    viewModel.applyCardEdit(card.copy(qrCodeColor = cHex))
                                }
                        )
                    }
                }

                if (!isPremium) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(0.35f))
                            .clickable { onNavigate(ActiveScreen.PREMIUM) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock", tint = Color(0xFFD4AF37), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("VIP custom styling colors", color = Color(0xFFD4AF37), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
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

        // MULTIPLE / ADDITIONAL QR CODES (VIP Feature gate limits check!)
        Surface(
            color = Color(0xFF131722),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF222B3A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .alpha(if (isPremium) 1f else 0.45f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Add Multiple QR Codes (VIP Premium)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Creates secondary mini QR codes encoded with alternative destination links.", color = Color.Gray, fontSize = 11.sp, lineHeight = 15.sp)

                    Button(
                        onClick = {
                            if (isPremium) {
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
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isPremium
                    ) {
                        Text("Add Secondary Multi-QR", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (!isPremium) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(0.35f))
                            .clickable { onNavigate(ActiveScreen.PREMIUM) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock", tint = Color(0xFFD4AF37), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Upgrade Premium for Multiple QR Codes", color = Color(0xFFD4AF37), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
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

                Text("Rendering Dimensions:", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

                if (!isPremiumAccount && exportQuality != "STANDARD") {
                    Text(
                        text = "⚠️ Free account limits: HD/UHD is simulating watermark elements. Go Premium for pure vector elements.",
                        color = Color(0xFFFF5E7E),
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetFile = PDFExporter.exportCardToFile(context, card, exportFormat, exportQuality)
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
