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

    // Active state tracker for designer toolbox selection
    var activeToolTab by remember { mutableStateOf("FIELDS") }
    var selectedElementKey by remember { mutableStateOf("fullName") }
    // Initialize zoom factor to -1f representing automatic "Fit to Screen" mode
    var editorZoomFactor by remember { mutableStateOf(-1.0f) }

    // Dialog trigger states
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf(activeCard.cardName) }
    var showExportSheet by remember { mutableStateOf(false) }

    // Redesign & space-optimization states
    var isFullscreenMode by remember { mutableStateOf(false) }
    var isHeaderCollapsed by remember { mutableStateOf(false) }
    var isDpadCollapsed by remember { mutableStateOf(true) } // Collapsed by default to maximize vertical canvas space
    var bottomPanelExpandedLevel by remember { mutableStateOf(2) } // 0 = Hidden, 1 = Compact, 2 = Half/Standard, 3 = Tall

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
        if (isFullscreenMode) {
            // --- FULLSCREEN EDITING MODE ---
            // Maximize screen space entirely for the card preview canvas, hiding all distracting elements.
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF0F111A)),
                contentAlignment = Alignment.Center
            ) {
                // Measure fit zoom factor in fullscreen
                val fitZoomFactor = remember(maxWidth, maxHeight) {
                    val padWidth = (maxWidth - 48.dp).coerceAtLeast(10.dp)
                    val padHeight = (maxHeight - 48.dp).coerceAtLeast(10.dp)
                    val horizontalZoom = padWidth.value / 400f
                    val verticalZoom = padHeight.value / 240f
                    minOf(horizontalZoom, verticalZoom).coerceIn(0.2f, 2.5f)
                }

                val currentZoom = if (editorZoomFactor == -1.0f) fitZoomFactor else editorZoomFactor

                // Interactive Scrollable Canvas with Smooth Zooms and Drag-Drops
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                if (zoom != 1.0f) {
                                    val baseZoom = if (editorZoomFactor == -1.0f) fitZoomFactor else editorZoomFactor
                                    editorZoomFactor = (baseZoom * zoom).coerceIn(0.2f, 2.5f)
                                }
                            }
                        }
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .padding(24.dp)
                            .width((400 * currentZoom).dp)
                            .height((240 * currentZoom).dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(400.dp)
                                .height(240.dp)
                                .scale(currentZoom)
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

                // FLOATING FULLSCREEN OVERLAYS (TRANSLUCENT & NON-BLOCKING)
                
                // 1. Top floating actions (Exit Fullscreen & Undo/Redo)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Transparent Exit Button
                    Button(
                        onClick = { isFullscreenMode = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC131722)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF222B3A)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        FullscreenExitIcon(tint = Color(0xFF00FFCC))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exit Fullscreen", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Floating Compact History Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.undo() },
                            enabled = viewModel.hasUndo(),
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (viewModel.hasUndo()) Color(0xCC131722) else Color.Transparent, CircleShape)
                                .border(BorderStroke(1.dp, if (viewModel.hasUndo()) Color(0xFF222B3A) else Color.Transparent), CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Undo", modifier = Modifier.size(16.dp), tint = if (viewModel.hasUndo()) Color(0xFF00FFCC) else Color.DarkGray)
                        }
                        IconButton(
                            onClick = { viewModel.redo() },
                            enabled = viewModel.hasRedo(),
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (viewModel.hasRedo()) Color(0xCC131722) else Color.Transparent, CircleShape)
                                .border(BorderStroke(1.dp, if (viewModel.hasRedo()) Color(0xFF222B3A) else Color.Transparent), CircleShape)
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Redo", modifier = Modifier.size(16.dp), tint = if (viewModel.hasRedo()) Color(0xFF00FFCC) else Color.DarkGray)
                        }
                    }
                }

                // 2. Bottom floating quick manipulators (Layer shortcuts & Zoom/Nudge HUD)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Translucent floating horizontal layer selector
                    Box(
                        modifier = Modifier
                            .background(Color(0xDD10121A), RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Color(0xFF1E2433)), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            Icon(Icons.Default.List, contentDescription = "Layers", tint = Color.Gray, modifier = Modifier.size(14.dp))
                            nativeLayerTitles.forEach { (key, title) ->
                                val isVisible = key == "qrCode" || visibleFields.contains(key)
                                if (isVisible) {
                                    val isSelected = selectedElementKey == key
                                    Text(
                                        text = title,
                                        color = if (isSelected) Color(0xFFD4AF37) else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier
                                            .clickable { selectedElementKey = key }
                                            .background(if (isSelected) Color(0xFF1E2433) else Color.Transparent, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            customLayersList.forEach { (key, type) ->
                                val isSelected = selectedElementKey == key
                                Text(
                                    text = type,
                                    color = if (isSelected) Color(0xFF00FFCC) else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier
                                        .clickable { selectedElementKey = key }
                                        .background(if (isSelected) Color(0xFF1E2433) else Color.Transparent, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Floating unified HUD with Zoom controls & 4-directional micro D-Pad
                    Surface(
                        color = Color(0xDD10121A),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFF222B3A))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            // Zoom buttons
                            IconButton(onClick = { editorZoomFactor = (currentZoom - 0.15f).coerceIn(0.2f, 2.5f) }, modifier = Modifier.size(24.dp)) {
                                Text("-", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("${(currentZoom * 100).toInt()}%${if (editorZoomFactor == -1.0f) " (Fit)" else ""}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { editorZoomFactor = (currentZoom + 0.15f).coerceIn(0.2f, 2.5f) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                            
                            Box(modifier = Modifier.width(1.dp).height(12.dp).background(Color.Gray))

                            // Compact Nudge Controls (Left, Up, Down, Right)
                            Text("Move:", color = Color.Gray, fontSize = 9.sp)
                            IconButton(onClick = { updateElementPosition(activeCard, selectedElementKey, -4f, 0f, viewModel) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Left", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                            IconButton(onClick = { updateElementPosition(activeCard, selectedElementKey, 0f, -4f, viewModel) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                            IconButton(onClick = { updateElementPosition(activeCard, selectedElementKey, 0f, 4f, viewModel) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                            IconButton(onClick = { updateElementPosition(activeCard, selectedElementKey, 4f, 0f, viewModel) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Right", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        } else {
            // --- STANDARD SCREEN (COMPACT & FULLY RESPONSIVE ADAPTIVE LAYOUT) ---
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF07080C))
            ) {
                // Determine layout direction based on available width/height (landscape/wide vs portrait/tall)
                val isLandscape = maxWidth > maxHeight || maxWidth >= 600.dp

                if (isLandscape) {
                    // --- LANDSCAPE / TABLET LAYOUT ---
                    Row(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Left Pane: Toolbar and Card Preview
                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight()
                                .drawBehind {
                                    val strokeWidth = 1.dp.toPx()
                                    val x = size.width - strokeWidth / 2
                                    drawLine(
                                        color = Color(0xFF1E2433),
                                        start = androidx.compose.ui.geometry.Offset(x, 0f),
                                        end = androidx.compose.ui.geometry.Offset(x, size.height),
                                        strokeWidth = strokeWidth
                                    )
                                }
                        ) {
                            // Uniform compact TopBar
                            CompactTopBar(
                                activeCard = activeCard,
                                viewModel = viewModel,
                                onNavigate = onNavigate,
                                onRenameClick = {
                                    renameInput = activeCard.cardName
                                    showRenameDialog = true
                                },
                                onExportClick = { showExportSheet = true },
                                onFullscreenClick = { isFullscreenMode = true }
                            )

                            // Card Preview Box
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(Color(0xFF0F111A)),
                                contentAlignment = Alignment.Center
                            ) {
                                val fitZoomFactor = remember(maxWidth, maxHeight) {
                                    val padWidth = (maxWidth - 32.dp).coerceAtLeast(10.dp)
                                    val padHeight = (maxHeight - 32.dp).coerceAtLeast(10.dp)
                                    val horizontalZoom = padWidth.value / 400f
                                    val verticalZoom = padHeight.value / 240f
                                    minOf(horizontalZoom, verticalZoom).coerceIn(0.2f, 2.5f)
                                }

                                val currentZoom = if (editorZoomFactor == -1.0f) fitZoomFactor else editorZoomFactor

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(Unit) {
                                            detectTransformGestures { _, _, zoom, _ ->
                                                if (zoom != 1.0f) {
                                                    val baseZoom = if (editorZoomFactor == -1.0f) fitZoomFactor else editorZoomFactor
                                                    editorZoomFactor = (baseZoom * zoom).coerceIn(0.2f, 2.5f)
                                                }
                                            }
                                        }
                                        .verticalScroll(rememberScrollState())
                                        .horizontalScroll(rememberScrollState()),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .width((400 * currentZoom).dp)
                                            .height((240 * currentZoom).dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(400.dp)
                                                .height(240.dp)
                                                .scale(currentZoom)
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

                                // Float Zoom Overlay
                                CompactZoomController(
                                    currentZoom = currentZoom,
                                    isFit = editorZoomFactor == -1.0f,
                                    onZoomOut = { editorZoomFactor = (currentZoom - 0.15f).coerceIn(0.2f, 2.5f) },
                                    onZoomIn = { editorZoomFactor = (currentZoom + 0.15f).coerceIn(0.2f, 2.5f) },
                                    onReset = { editorZoomFactor = -1.0f },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 8.dp)
                                )
                            }
                        }

                        // Right Pane: Active Layer shortcuts, Toolbox tabs & scrollable toolbox panels
                        Column(
                            modifier = Modifier
                                .weight(1.0f)
                                .fillMaxHeight()
                                .background(Color(0xFF131722))
                        ) {
                            // Layer selection shortcuts row at top of panel
                            LayerShortcutsRow(
                                nativeLayerTitles = nativeLayerTitles,
                                visibleFields = visibleFields,
                                customLayersList = customLayersList,
                                selectedElementKey = selectedElementKey,
                                onLayerSelect = { selectedElementKey = it }
                            )

                            // Unified Collapsible D-pad / Spatial Sliders
                            DpadNudgeSlidersPanel(
                                selectedElementKey = selectedElementKey,
                                activeCard = activeCard,
                                viewModel = viewModel,
                                isDpadCollapsed = isDpadCollapsed,
                                onDpadCollapseToggle = { isDpadCollapsed = it }
                            )

                            // Toolbox tabs select row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1A1F2C))
                                    .border(BorderStroke(1.dp, Color(0xFF2E3547))),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                ToolboxTabButton("Profile Fields", activeToolTab == "FIELDS") { activeToolTab = "FIELDS" }
                                ToolboxTabButton("Styling & Font", activeToolTab == "STYLING") { activeToolTab = "STYLING" }
                                ToolboxTabButton("QR Manager", activeToolTab == "QR_CODE") { activeToolTab = "QR_CODE" }
                                ToolboxTabButton("Add Custom", activeToolTab == "CUSTOM_LAYERS") { activeToolTab = "CUSTOM_LAYERS" }
                            }

                            // Active panel content region filling the remaining right-pane height
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
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
                    }
                } else {
                    // --- PORTRAIT LAYOUT (PHONES) ---
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Compact TopBar
                        CompactTopBar(
                            activeCard = activeCard,
                            viewModel = viewModel,
                            onNavigate = onNavigate,
                            onRenameClick = {
                                renameInput = activeCard.cardName
                                showRenameDialog = true
                            },
                            onExportClick = { showExportSheet = true },
                            onFullscreenClick = { isFullscreenMode = true }
                        )

                        // Card Preview Viewport
                        val previewWeight = when (bottomPanelExpandedLevel) {
                            0 -> 1.0f  // HUD/Collapsed Tool panels -> Card Canvas gets ALL screen space
                            1 -> 0.62f // Compact panel -> Card Canvas gets 62% height
                            2 -> 0.46f // Half/Standard panel -> Card Canvas gets 46% height
                            3 -> 0.30f // Tall panel -> Card Canvas gets 30% height
                            else -> 0.46f
                        }

                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(previewWeight)
                                .background(Color(0xFF0F111A))
                                .border(BorderStroke(1.dp, Color(0xFF1E2433))),
                            contentAlignment = Alignment.Center
                        ) {
                            val fitZoomFactor = remember(maxWidth, maxHeight) {
                                val padWidth = (maxWidth - 32.dp).coerceAtLeast(10.dp)
                                val padHeight = (maxHeight - 32.dp).coerceAtLeast(10.dp)
                                val horizontalZoom = padWidth.value / 400f
                                val verticalZoom = padHeight.value / 240f
                                minOf(horizontalZoom, verticalZoom).coerceIn(0.2f, 2.5f)
                            }

                            val currentZoom = if (editorZoomFactor == -1.0f) fitZoomFactor else editorZoomFactor

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, _, zoom, _ ->
                                            if (zoom != 1.0f) {
                                                val baseZoom = if (editorZoomFactor == -1.0f) fitZoomFactor else editorZoomFactor
                                                editorZoomFactor = (baseZoom * zoom).coerceIn(0.2f, 2.5f)
                                            }
                                        }
                                    }
                                    .verticalScroll(rememberScrollState())
                                    .horizontalScroll(rememberScrollState()),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .width((400 * currentZoom).dp)
                                        .height((240 * currentZoom).dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(400.dp)
                                            .height(240.dp)
                                            .scale(currentZoom)
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

                            // Float Zoom Overlay
                            CompactZoomController(
                                currentZoom = currentZoom,
                                isFit = editorZoomFactor == -1.0f,
                                onZoomOut = { editorZoomFactor = (currentZoom - 0.15f).coerceIn(0.2f, 2.5f) },
                                onZoomIn = { editorZoomFactor = (currentZoom + 0.15f).coerceIn(0.2f, 2.5f) },
                                onReset = { editorZoomFactor = -1.0f },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp)
                            )
                        }

                        // Layer select shortcuts bar
                        LayerShortcutsRow(
                            nativeLayerTitles = nativeLayerTitles,
                            visibleFields = visibleFields,
                            customLayersList = customLayersList,
                            selectedElementKey = selectedElementKey,
                            onLayerSelect = { selectedElementKey = it }
                        )

                        // D-pad nudge sliders
                        DpadNudgeSlidersPanel(
                            selectedElementKey = selectedElementKey,
                            activeCard = activeCard,
                            viewModel = viewModel,
                            isDpadCollapsed = isDpadCollapsed,
                            onDpadCollapseToggle = { isDpadCollapsed = it }
                        )

                        // Height Resize control Header
                        ToolboxHeightSelectorHeader(
                            bottomPanelExpandedLevel = bottomPanelExpandedLevel,
                            onHeightChange = { bottomPanelExpandedLevel = it }
                        )

                        // Tab headers and scrollable content panel
                        if (bottomPanelExpandedLevel > 0) {
                            val panelWeight = when (bottomPanelExpandedLevel) {
                                1 -> 0.38f // COMPACT
                                2 -> 0.54f // NORMAL
                                3 -> 0.70f // TALL
                                else -> 0.54f
                            }

                            // Toolbox tabs select row
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

                            // Active panel content region
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(panelWeight)
                                    .background(Color(0xFF0A0C16))
                            ) {
                                when (activeToolTab) {
                                    "FIELDS" -> EditorFieldsPanel(activeCard, viewModel)
                                    "STYLING" -> EditorStylingPanel(activeCard, viewModel)
                                    "QR_CODE" -> EditorQRPanel(activeCard, viewModel, onNavigate)
                                    "CUSTOM_LAYERS" -> EditorCustomLayersPanel(activeCard, viewModel, onNavigate)
                                }
                            }
                        } else {
                            // Toolbox fully collapsed restore indicator
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF131722))
                                    .clickable { bottomPanelExpandedLevel = 2 } // Restore to standard size
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Restore Panel",
                                    tint = Color(0xFF00FFCC),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Editor Toolbox is Collapsed — Click to Restore Controls",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
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

    // Compose custom draggable container
    Box(
        modifier = Modifier
            .fillMaxSize()
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
        if (card.backgroundType == "PATTERN" && !card.backgroundImage.isNullOrEmpty()) {
            val bgPainter = coil.compose.rememberAsyncImagePainter(model = card.backgroundImage)
            Image(
                painter = bgPainter,
                contentDescription = "Custom Background Pattern",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
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
        vcardIncludeAddress
    ) {
        if (card.qrCodeType == "VCARD") {
            val generated = com.example.utils.QRGenerator.generateVCard(
                fullName = if (vcardIncludeName) card.fullName else "",
                jobTitle = if (vcardIncludeTitle) card.jobTitle else "",
                companyName = if (vcardIncludeCompany) card.companyName else "",
                phoneNumber = if (vcardIncludePhone) card.mobileNumber else "",
                email = if (vcardIncludeEmail) card.email else "",
                website = if (vcardIncludeWebsite) card.website else "",
                address = if (vcardIncludeAddress) card.address else ""
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
                            Text("Choose which details from this card form are embedded inside the scannable QR coordinate. Modern iOS/Android lens apps automatically prompt 'Add to contacts' upon sensing vCard format.", color = Color.Gray, fontSize = 10.sp)
                            
                            VCardFieldToggleRow(label = "Full Name (${card.fullName})", checked = vcardIncludeName) { vcardIncludeName = it }
                            VCardFieldToggleRow(label = "Job Title (${card.jobTitle})", checked = vcardIncludeTitle) { vcardIncludeTitle = it }
                            VCardFieldToggleRow(label = "Company (${card.companyName})", checked = vcardIncludeCompany) { vcardIncludeCompany = it }
                            VCardFieldToggleRow(label = "Phone Number (${card.mobileNumber})", checked = vcardIncludePhone) { vcardIncludePhone = it }
                            VCardFieldToggleRow(label = "Email Address (${card.email})", checked = vcardIncludeEmail) { vcardIncludeEmail = it }
                            VCardFieldToggleRow(label = "Website (${card.website})", checked = vcardIncludeWebsite) { vcardIncludeWebsite = it }
                            VCardFieldToggleRow(label = "Address (${card.address})", checked = vcardIncludeAddress) { vcardIncludeAddress = it }

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

