package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.viewmodel.CardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Helper for generating local mock card models
fun createMockCardForProfessional(
    name: String, title: String, comp: String, mail: String, phone: String, web: String,
    bgStart: String = "#1A103C", bgEnd: String = "#0A0520", primaryColorHex: String = "#00FFCC",
    font: String = "Space Grotesk", border: String = "CYBER_SLATE"
): UserCard {
    return UserCard(
        id = (10000..99999).random(),
        cardName = "$name Code",
        templateId = "vibe_custom",
        themeName = "Community Custom",
        fullName = name,
        jobTitle = title,
        companyName = comp,
        email = mail,
        mobileNumber = phone,
        website = web,
        address = "Bandra, Mumbai, Maharastra, India",
        backgroundColor = bgStart,
        gradientEndColor = bgEnd,
        qrCodeColor = primaryColorHex,
        fontStyle = font,
        borderStyle = border
    )
}

// Generate fallback professional directory members
fun getMockProfessionals(viewModel: CardViewModel): List<Professional> {
    return listOf(
        Professional(
            id = "alex_rivera",
            name = "Alex Rivera",
            profession = "Developer Evangelist",
            company = "Cyberdyne Systems",
            location = "Bandra West, Mumbai",
            email = "alex@cyberdyne.io",
            mobile = "+1 (555) 019-2831",
            website = "cyberdyne.io/alex",
            bio = "Spreading cloud knowledge. Hit me up if you want cybernetic visiting templates!",
            category = "Technology",
            directoryRole = "Developers",
            avatarColorHex = "#0288D1",
            isFeatured = true,
            isPopular = true,
            matchingCard = createMockCardForProfessional("Alex Rivera", "Developer Evangelist", "Cyberdyne Systems", "alex@cyberdyne.io", "+1 (555) 019-2831", "cyberdyne.io/alex", "#0A0F1D", "#0E1C38", "#00FFCC", "Tech Clean", "CYBER_SLATE")
        ),
        Professional(
            id = "elena_rostova",
            name = "Elena Rostova",
            profession = "Managing Director",
            company = "North Star Capital",
            location = "Colaba, Mumbai",
            email = "e.rostova@northstar.com",
            mobile = "+44 20 7946 0192",
            website = "northstar.com",
            bio = "Venture investments & executive consulting. Seeking modern premium business cards.",
            category = "Premium",
            directoryRole = "Business Owners",
            avatarColorHex = "#C2185B",
            isFeatured = true,
            matchingCard = createMockCardForProfessional("Elena Rostova", "Managing Director", "North Star Capital", "e.rostova@northstar.com", "+44 20 7946 0192", "northstar.com", "#111111", "#1C1C1C", "#D4AF37", "Elegant Serif", "MINIMAL_GOLD")
        ),
        Professional(
            id = "sarah_jenkins",
            name = "Sarah Jenkins",
            profession = "Creative Director",
            company = "PixelCraft Studio",
            location = "Indiranagar, Bengaluru",
            email = "hello@sarahj.design",
            mobile = "+91 98980 12345",
            website = "sarahj.design",
            bio = "Crafting luxury high-density mobile UIs. Looking for creative templates with front & back elements.",
            category = "Creative",
            directoryRole = "Designers",
            avatarColorHex = "#E040FB",
            isFeatured = false,
            isPopular = true,
            matchingCard = createMockCardForProfessional("Sarah Jenkins", "Creative Director", "PixelCraft Studio", "hello@sarahj.design", "+91 98980 12345", "sarahj.design", "#1C0407", "#2D0A14", "#FF3366", "Space Grotesk", "MODERN_DOUBLE")
        ),
        Professional(
            id = "dr_aditi_sharma",
            name = "Dr. Aditi Sharma",
            profession = "Cardiologist",
            company = "City Heart Clinic",
            location = "Andheri, Mumbai",
            email = "aditi@heartclinic.in",
            mobile = "+91 91234 56789",
            website = "aditisharma.in",
            bio = "Dedicated cardiologist with 12+ years experience. Modern, tidy design card values is what she is seeking.",
            category = "Medical",
            directoryRole = "Doctors",
            avatarColorHex = "#00897B",
            isFeatured = false,
            matchingCard = createMockCardForProfessional("Dr. Aditi Sharma", "Cardiologist", "City Heart Clinic", "aditi@heartclinic.in", "+91 91234 56789", "aditisharma.in", "#0A221C", "#144D3F", "#48CAE4", "Tech Clean", "MINIMAL_GOLD")
        ),
        Professional(
            id = "rajesh_patel",
            name = "Rajesh Patel",
            profession = "Chief Broker",
            company = "Patel & Sons Real Estate",
            location = "Andheri, Mumbai",
            email = "rajesh@patelrealty.com",
            mobile = "+91 88877 66554",
            website = "patelrealty.com",
            bio = "Luxury properties and residential assets in Mumbai. Reach out for design collaborations.",
            category = "Real Estate",
            directoryRole = "Real Estate Agents",
            avatarColorHex = "#F57C00",
            isFeatured = true,
            matchingCard = createMockCardForProfessional("Rajesh Patel", "Chief Broker", "Patel & Sons Real Estate", "rajesh@patelrealty.com", "+91 88877 66554", "patelrealty.com", "#141518", "#221C16", "#CBB26A", "Elegant Serif", "MODERN_DOUBLE")
        )
    )
}

enum class CommunityTab {
    DESIGNS, // Shared visual card gallery feed
    NETWORKING, // Directory finding & QR Connect
    CHATS, // Creator direct messaging
    ALERTS, // Alerts & system updates
    PROFILE // My editable creator profile dashboard
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityMainScreen(
    viewModel: CardViewModel,
    onNavigateBack: () -> Unit,
    onOpenEditor: () -> Unit
) {
    var activeTab by remember { mutableStateOf(CommunityTab.DESIGNS) }
    var selectedProfessionalForChat by remember { mutableStateOf<Professional?>(null) }
    var viewingProfessionalProfile by remember { mutableStateOf<Professional?>(null) }
    var viewingCreatorProfile by remember { mutableStateOf<CreatorProfile?>(null) }
    var showingPremiumPaywallDialog by remember { mutableStateOf(false) }

    // Safety Intercept with Jetpack Back Handler
    BackHandler {
        if (viewingCreatorProfile != null) {
            viewingCreatorProfile = null
        } else if (viewingProfessionalProfile != null) {
            viewingProfessionalProfile = null
        } else if (selectedProfessionalForChat != null) {
            selectedProfessionalForChat = null
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            if (selectedProfessionalForChat == null && viewingProfessionalProfile == null && viewingCreatorProfile == null) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF00FFCC), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Design Community",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("community_back_btn")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.LightGray)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showingPremiumPaywallDialog = true }) {
                            Icon(Icons.Default.Star, contentDescription = "Premium Paywall", tint = Color(0xFFD4AF37))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1022))
                )
            }
        },
        bottomBar = {
            if (selectedProfessionalForChat == null && viewingProfessionalProfile == null && viewingCreatorProfile == null) {
                NavigationBar(
                    containerColor = Color(0xFF0D1022)
                ) {
                    val tabs = listOf(
                        CommunityTab.DESIGNS to "Designs" to Icons.Default.Home,
                        CommunityTab.NETWORKING to "Connect" to Icons.Default.Person,
                        CommunityTab.CHATS to "Chats" to Icons.Default.Email,
                        CommunityTab.ALERTS to "Alerts" to Icons.Default.Notifications,
                        CommunityTab.PROFILE to "Me" to Icons.Default.AccountCircle
                    )
                    tabs.forEach { item ->
                        val tab = item.first.first
                        val title = item.first.second
                        val icon = item.second
                        NavigationBarItem(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            icon = { Icon(icon, contentDescription = title, modifier = Modifier.size(22.dp)) },
                            label = { Text(title, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF00FFCC),
                                selectedTextColor = Color(0xFF00FFCC),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color(0xFF1E293B)
                            )
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFF0A0C16)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF0A0C16))
        ) {
            when {
                viewingCreatorProfile != null -> {
                    CreatorProfileDetailView(
                        creator = viewingCreatorProfile!!,
                        viewModel = viewModel,
                        onBack = { viewingCreatorProfile = null },
                        onOpenEditor = onOpenEditor
                    )
                }
                viewingProfessionalProfile != null -> {
                    ProfessionalProfileView(
                        professional = viewingProfessionalProfile!!,
                        viewModel = viewModel,
                        onBack = { viewingProfessionalProfile = null },
                        onChat = {
                            selectedProfessionalForChat = viewingProfessionalProfile
                            viewingProfessionalProfile = null
                        }
                    )
                }
                selectedProfessionalForChat != null -> {
                    CommunityChatDetailScreen(
                        professional = selectedProfessionalForChat!!,
                        viewModel = viewModel,
                        onBack = { selectedProfessionalForChat = null }
                    )
                }
                else -> {
                    when (activeTab) {
                        CommunityTab.DESIGNS -> DesignsFeedTabContent(
                            viewModel = viewModel,
                            onOpenEditor = onOpenEditor,
                            onViewCreator = { viewingCreatorProfile = it }
                        )
                        CommunityTab.NETWORKING -> NetworkingTabContent(
                            viewModel = viewModel,
                            onViewProfile = { viewingProfessionalProfile = it },
                            onChat = { selectedProfessionalForChat = it }
                        )
                        CommunityTab.CHATS -> ChatsTabContent(
                            viewModel = viewModel,
                            onChatSelected = { selectedProfessionalForChat = it }
                        )
                        CommunityTab.ALERTS -> AlertsTabContent(viewModel = viewModel)
                        CommunityTab.PROFILE -> SettingsAndProfileTabContent(
                            viewModel = viewModel,
                            onOpenEditor = onOpenEditor
                        )
                    }
                }
            }

            if (showingPremiumPaywallDialog) {
                PremiumUpgradeDialog(viewModel = viewModel) {
                    showingPremiumPaywallDialog = false
                }
            }
        }
    }
}

// ---------------- DESIGNS FEED TAB (CARD COMMUNITY SECTION) ----------------
@Composable
fun DesignsFeedTabContent(
    viewModel: CardViewModel,
    onOpenEditor: () -> Unit,
    onViewCreator: (CreatorProfile) -> Unit
) {
    val context = LocalContext.current
    val searchQuery by viewModel.communitySearchQuery.collectAsState()
    val sharedCards by viewModel.communitySharedCards.collectAsState()
    val likedCardIds by viewModel.likedSharedCardIds.collectAsState()
    val favoriteCardIds by viewModel.favoriteSharedCardIds.collectAsState()
    val blockedUsers by viewModel.communityBlocked.collectAsState()
    val reportedCardIds by viewModel.reportedCardIds.collectAsState()

    var selectedCategory by remember { mutableStateOf("All") }
    // SORT STATES: TRENDING, LATEST, MOST_DOWNLOADED
    var sortMethod by remember { mutableStateOf("Trending") }
    var showUploadDialog by remember { mutableStateOf(false) }

    // Filter shared card lists based on category, search string, reported rules, and blocked creators
    val processedCards = remember(sharedCards, searchQuery, selectedCategory, sortMethod, blockedUsers, reportedCardIds) {
        var list = sharedCards.filter {
            !blockedUsers.contains(it.creatorId) && !reportedCardIds.contains(it.id)
        }

        if (selectedCategory != "All") {
            list = list.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }

        if (searchQuery.isNotEmpty()) {
            list = list.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true) ||
                        it.creatorName.contains(searchQuery, ignoreCase = true) ||
                        it.creatorUsername.contains(searchQuery, ignoreCase = true)
            }
        }

        // Sort algorithm
        when (sortMethod) {
            "Trending" -> list.sortedByDescending { it.viewsCount + it.likesCount * 3 }
            "Latest" -> list.sortedByDescending { it.createdTime }
            "Most Downloaded".uppercase(), "Most Downloaded" -> list.sortedByDescending { it.downloadsCount }
            else -> list
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Upper search visual wrapper
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D1022))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setCommunitySearch(it) },
                    placeholder = { Text("Search title, description or username...", color = Color.Gray, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.LightGray) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setCommunitySearch("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.LightGray)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("community_search_designs"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF161B33),
                        unfocusedContainerColor = Color(0xFF161B33),
                        focusedBorderColor = Color(0xFF00FFCC),
                        unfocusedBorderColor = Color(0xFF222B45)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable category horizontal row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val categories = listOf("All", "Business", "Corporate", "Creative", "Medical", "Technology", "Real Estate", "Restaurant", "Premium")
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) Color(0xFF00FFCC) else Color(0xFF1E293B),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) Color.Black else Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Toggle Sort row: Trending, Latest, Most Downloaded
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Trending", "Latest", "Most Downloaded").forEach { sort ->
                        val isSelected = sortMethod == sort
                        TextButton(
                            onClick = { sortMethod = sort },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (isSelected) Color(0xFF00FFCC) else Color.Gray
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when(sort) {
                                        "Latest" -> Icons.Default.Refresh
                                        "Most Downloaded" -> Icons.Default.Check
                                        else -> Icons.Default.Star
                                    },
                                    contentDescription = sort,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(sort, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }

            // Results vertical list scroll view
            if (processedCards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No Shared Designs Found", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Be the first to upload a spectacular layout design!", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(processedCards) { sharedCard ->
                        SharedCommunityCardItem(
                            card = sharedCard,
                            viewModel = viewModel,
                            liked = likedCardIds.contains(sharedCard.id),
                            favorited = favoriteCardIds.contains(sharedCard.id),
                            onViewCreator = {
                                // build Creator detail object
                                val creatorProfile = CreatorProfile(
                                    id = sharedCard.creatorId,
                                    username = sharedCard.creatorUsername,
                                    name = sharedCard.creatorName,
                                    bio = "Designer in the ${sharedCard.category} community. Connect of collaboration models.",
                                    companyName = "Design Independent Studio",
                                    phone = "+1 (555) 012-3456",
                                    email = "${sharedCard.creatorUsername}@community.net",
                                    website = "www.${sharedCard.creatorUsername}.com",
                                    isVerified = sharedCard.isVerifiedCreator,
                                    isPremium = sharedCard.isPremiumCreator,
                                    followersCount = 420,
                                    followingCount = 180,
                                    likesReceivedCount = sharedCard.likesCount + 100,
                                    totalDesignsCount = 3
                                )
                                onViewCreator(creatorProfile)
                            },
                            onOpenEditor = onOpenEditor
                        )
                    }
                }
            }
        }

        // Floating Action Button to share card
        FloatingActionButton(
            onClick = { showUploadDialog = true },
            containerColor = Color(0xFF00FFCC),
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("upload_community_card_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Card")
        }
    }

    if (showUploadDialog) {
        UploadDesignDialog(
            viewModel = viewModel,
            onDismiss = { showUploadDialog = false }
        )
    }
}

@Composable
fun SharedCommunityCardItem(
    card: CommunitySharedCard,
    viewModel: CardViewModel,
    liked: Boolean,
    favorited: Boolean,
    onViewCreator: () -> Unit,
    onOpenEditor: () -> Unit
) {
    val context = LocalContext.current
    var isFrontSelected by remember { mutableStateOf(true) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    var showActionMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13172F)),
        border = BorderStroke(1.dp, Color(0xFF222B45))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Creator details & Action menu dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onViewCreator() }
                ) {
                    // Profile Circle fallback
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(android.graphics.Color.parseColor(card.creatorAvatarColor)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = card.creatorName.take(2).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = card.creatorName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (card.isVerifiedCreator) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = Color(0xFF2196F3), modifier = Modifier.size(15.dp))
                            }
                            if (card.isPremiumCreator) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Star, contentDescription = "Premium Status", tint = Color(0xFFD4AF37), modifier = Modifier.size(15.dp))
                            }
                        }
                        Text(
                            text = "@${card.creatorUsername}",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }

                Box {
                    IconButton(onClick = { showActionMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Actions", tint = Color.LightGray)
                    }
                    DropdownMenu(
                        expanded = showActionMenu,
                        onDismissRequest = { showActionMenu = false },
                        modifier = Modifier.background(Color(0xFF1A1F3B))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Report Design / Inappropriate", color = Color.Red) },
                            onClick = {
                                showActionMenu = false
                                showReportDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red) }
                        )
                        DropdownMenuItem(
                            text = { Text("Block User", color = Color.Red) },
                            onClick = {
                                showActionMenu = false
                                viewModel.toggleBlockProfessional(card.creatorId)
                                Toast.makeText(context, "Creator blocked securely.", Toast.LENGTH_SHORT).show()
                            },
                            leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red) }
                        )
                        DropdownMenuItem(
                            text = { Text("Admin Remove Design", color = Color.Yellow) },
                            onClick = {
                                showActionMenu = false
                                viewModel.adminDeleteSharedCard(card.id)
                                Toast.makeText(context, "Design removed (Admin rule applied).", Toast.LENGTH_SHORT).show()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Yellow) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body: Title & Category Label
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = card.title,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFF5E7E).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = card.category.uppercase(),
                        color = Color(0xFFFF5E7E),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = card.description,
                color = Color.LightGray,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Render Business card preview card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFF090B15))
                    .clip(RoundedCornerShape(12.dp))
                    .border(0.5.dp, Color(0xFF222B45), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Interactive front or back card layout preview representation
                if (isFrontSelected) {
                    StaticMiniCardPreview(card = card.frontCard)
                } else {
                    val back = card.backCard ?: card.frontCard.copy(fullName = "REVERSE SIDE - DESIGN ONLY", jobTitle = "")
                    StaticMiniCardPreview(card = back)
                }

                // Front/Back Selector badge overlay if backCard exists
                if (card.backCard != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        listOf(true to "Front", false to "Back").forEach { mode ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isFrontSelected == mode.first) Color(0xFF00FFCC) else Color.Transparent)
                                    .clickable { isFrontSelected = mode.first }
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = mode.second,
                                    fontSize = 10.sp,
                                    color = if (isFrontSelected == mode.first) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Actions panel: Likes, Downloads, Save Favorite, and Use Design
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Like item
                    IconButtonRow(
                        imageVector = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        count = card.likesCount.toString(),
                        tint = if (liked) Color.Red else Color.LightGray,
                        tag = "like_btn_${card.id}"
                    ) {
                        viewModel.likeSharedCard(card.id)
                    }

                    // Favorite item (Star icon)
                    IconButtonRow(
                        imageVector = Icons.Default.Star,
                        count = "Fav",
                        tint = if (favorited) Color(0xFFD4AF37) else Color.LightGray,
                        tag = "fav_btn_${card.id}"
                    ) {
                        viewModel.favoriteSharedCard(card.id)
                        Toast.makeText(context, if (favorited) "Removed from favorites." else "Linked to favorite layouts list!", Toast.LENGTH_SHORT).show()
                    }

                    // Download item
                    IconButtonRow(
                        imageVector = Icons.Default.Check,
                        count = card.downloadsCount.toString(),
                        tint = Color.LightGray,
                        tag = "download_btn_${card.id}"
                    ) {
                        viewModel.downloadSharedCard(card.id)
                        Toast.makeText(context, "Design package downloaded successfully!", Toast.LENGTH_SHORT).show()
                    }
                }

                Button(
                    onClick = {
                        viewModel.createNewCardProjectDirectly(card.frontCard)
                        onOpenEditor()
                        Toast.makeText(context, "Layout template loaded successfully inside your Editor!", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("use_design_btn_${card.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Use This Design", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }

    if (showReportDialog) {
        Dialog(onDismissRequest = { showReportDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13172F)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Report Design Card", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        placeholder = { Text("Briefly describe why this asset violates policies...", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00FFCC),
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showReportDialog = false }) {
                            Text("Cancel", color = Color.LightGray)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (reportReason.isNotEmpty()) {
                                    viewModel.reportSharedCard(card.id, reportReason)
                                    showReportDialog = false
                                    Toast.makeText(context, "Safety report filed, content hidden from feed.", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("File Report", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IconButtonRow(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    count: String,
    tint: Color,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Icon(imageVector, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(count, color = Color.LightGray, fontSize = 11.sp)
    }
}

// ---------------- UPLOAD CARD TO COMMUNITY DIALOG ----------------
@Composable
fun UploadDesignDialog(
    viewModel: CardViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val savedCards by viewModel.savedCards.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Business") }

    var selectedFrontIndex by remember { mutableStateOf(-1) }
    var selectedBackIndex by remember { mutableStateOf(-1) }

    var expandedCategory by remember { mutableStateOf(false) }
    var expandedFront by remember { mutableStateOf(false) }
    var expandedBack by remember { mutableStateOf(false) }

    val categories = listOf("Business", "Corporate", "Creative", "Medical", "Technology", "Real Estate", "Restaurant", "Premium")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF13172F)),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Upload Design to Community",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
                Text(
                    text = "Publish your business card works so others can view or use them as a design template.",
                    color = Color.Gray,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title Input
                Text("Design Title", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("E.g. Creative Luxury Black Gold Theme", color = Color.Gray, fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_title_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00FFCC),
                        unfocusedBorderColor = Color(0xFF222B45)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description Input
                Text("Theme Description", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Briefly describe fonts, styling or appropriate company sectors...", color = Color.Gray, fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .testTag("upload_desc_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00FFCC),
                        unfocusedBorderColor = Color(0xFF222B45)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Dropdown Selection
                Text("Select Category", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedCategory = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_cat_dropdown"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF222B45))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(category, fontSize = 13.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF13172F))
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = Color.White) },
                                onClick = {
                                    category = cat
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Front Card Selector
                Text("Select Front Layout", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedFront = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_front_dropdown"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF222B45))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val displayText = if (selectedFrontIndex in savedCards.indices) savedCards[selectedFrontIndex].cardName else "Choose front canvas..."
                            Text(displayText, fontSize = 13.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = expandedFront,
                        onDismissRequest = { expandedFront = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF13172F))
                    ) {
                        if (savedCards.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No custom cards found inside Drafts", color = Color.Gray) },
                                onClick = { expandedFront = false }
                            )
                        } else {
                            savedCards.forEachIndexed { idx, draft ->
                                DropdownMenuItem(
                                    text = { Text(draft.cardName, color = Color.White) },
                                    onClick = {
                                        selectedFrontIndex = idx
                                        expandedFront = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Back Card Selector (Optional)
                Text("Select Back Layout (Optional)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedBack = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_back_dropdown"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF222B45))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val displayText = if (selectedBackIndex in savedCards.indices) savedCards[selectedBackIndex].cardName else "No reverse layout (Single side template)"
                            Text(displayText, fontSize = 13.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = expandedBack,
                        onDismissRequest = { expandedBack = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF13172F))
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Single sided design)", color = Color.LightGray) },
                            onClick = {
                                selectedBackIndex = -1
                                expandedBack = false
                            }
                        )
                        savedCards.forEachIndexed { idx, draft ->
                            DropdownMenuItem(
                                text = { Text(draft.cardName, color = Color.White) },
                                onClick = {
                                    selectedBackIndex = idx
                                    expandedBack = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons: Cancel & Submit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (title.isEmpty() || description.isEmpty() || selectedFrontIndex == -1) {
                                Toast.makeText(context, "Please write a title, description, and match a front layout card.", Toast.LENGTH_SHORT).show()
                            } else {
                                val frontCard = savedCards[selectedFrontIndex]
                                val backCard = if (selectedBackIndex in savedCards.indices) savedCards[selectedBackIndex] else null
                                viewModel.uploadCustomCardToCommunity(title, description, category, frontCard, backCard)
                                Toast.makeText(context, "Layout uploaded live into community feed!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("upload_submit_btn")
                    ) {
                        Text("Publish Design", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------- NETWORKING TAB CONTENT ----------------
// Fuses previous feed tab content and discover system cleanly inside are bottom connection navigation.
@Composable
fun NetworkingTabContent(
    viewModel: CardViewModel,
    onViewProfile: (Professional) -> Unit,
    onChat: (Professional) -> Unit
) {
    var directoryMode by remember { mutableStateOf("Professionals") } // Professionals vs Connection Requests

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = if (directoryMode == "Professionals") 0 else 1,
            containerColor = Color(0xFF0D1022),
            contentColor = Color(0xFF00FFCC)
        ) {
            Tab(
                selected = directoryMode == "Professionals",
                onClick = { directoryMode = "Professionals" },
                text = { Text("Professional Finder") }
            )
            Tab(
                selected = directoryMode == "QR Connect",
                onClick = { directoryMode = "QR Connect" },
                text = { Text("Connections Center") }
            )
        }

        if (directoryMode == "Professionals") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Local Directory", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Tap to connect with founders", color = Color.Gray, fontSize = 11.sp)
            }

            val professionals = getMockProfessionals(viewModel)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(professionals) { prof ->
                    FeedProfessionalCardItem(prof, viewModel, onViewProfile, onChat)
                }
            }
        } else {
            // Connections Hub pane (Connections requests, active connections, pending connection)
            ConnectionsTabContent(viewModel = viewModel, onViewProfile = onViewProfile)
        }
    }
}

@Composable
fun FeedProfessionalCardItem(
    prof: Professional,
    viewModel: CardViewModel,
    onViewProfile: (Professional) -> Unit,
    onChat: (Professional) -> Unit
) {
    val connections by viewModel.communityConnections.collectAsState()
    val isConnected = connections.contains(prof.id)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewProfile(prof) },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF13172F))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(android.graphics.Color.parseColor(prof.avatarColorHex)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = prof.name.take(2).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(prof.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(prof.profession, color = Color(0xFF00FFCC), fontSize = 12.sp)
                Text(prof.company, color = Color.Gray, fontSize = 11.sp)
            }

            IconButton(onClick = { onChat(prof) }) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Message",
                    tint = if (isConnected) Color(0xFF00FFCC) else Color.Gray
                )
            }
        }
    }
}

// ------------------- OTHER COMMUNITY MODULES (INTEGRITY HOOKS) -------------------
// These keep the structures compatible with the rest of the application
@Composable
fun ChatsTabContent(
    viewModel: CardViewModel,
    onChatSelected: (Professional) -> Unit
) {
    val professionals = getMockProfessionals(viewModel)
    val blocked = viewModel.communityBlocked.collectAsState().value

    val chatList = remember(professionals, blocked) {
        professionals.filter { !blocked.contains(it.id) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Active Chats", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(12.dp))

        if (chatList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No conversations active. Connect with members!", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(chatList) { prof ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChatSelected(prof) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF13172F))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(android.graphics.Color.parseColor(prof.avatarColorHex)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(prof.name.take(2).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prof.name, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Tap to view direct chat logs...", color = Color.Gray, fontSize = 11.sp)
                            }
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionsTabContent(
    viewModel: CardViewModel,
    onViewProfile: (Professional) -> Unit
) {
    val connections by viewModel.communityConnections.collectAsState()
    val incoming by viewModel.communityIncomingRequests.collectAsState()
    val professionals = getMockProfessionals(viewModel)

    val activeConnectionsList = remember(professionals, connections) {
        professionals.filter { connections.contains(it.id) }
    }

    val pendingRequestsList = remember(professionals, incoming) {
        professionals.filter { incoming.contains(it.id) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Pending incoming requests section
        if (pendingRequestsList.isNotEmpty()) {
            Text("Pending Connections Requests", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            pendingRequestsList.forEach { p ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF181D3D))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(p.name, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(p.profession, color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.acceptIncomingRequest(p.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC))
                            ) {
                                Text("Accept", color = Color.Black)
                            }
                            OutlinedButton(onClick = { viewModel.declineIncomingRequest(p.id) }) {
                                Text("Decline", color = Color.LightGray)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // QR Connect pane trigger
        Text("QR Fast Exchange", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))
        QRConnectPane(viewModel)

        Spacer(modifier = Modifier.height(24.dp))

        // Active Connection List
        Text("My Network Connections (${activeConnectionsList.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))

        if (activeConnectionsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No active connections inside network.", color = Color.Gray)
            }
        } else {
            activeConnectionsList.forEach { connection ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { onViewProfile(connection) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF13172F))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(android.graphics.Color.parseColor(connection.avatarColorHex)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(connection.name.take(2).uppercase(), color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(connection.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(connection.profession, color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QRConnectPane(viewModel: CardViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161A36)),
        border = BorderStroke(1.dp, Color(0xFF2E3A5E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF00FFCC), modifier = Modifier.size(60.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text("Exchange Badge instantly via QR", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Let colleagues scan your QR code inside the editor to directly import your visiting card.", color = Color.LightGray, textAlign = TextAlign.Center, fontSize = 11.sp)
        }
    }
}

@Composable
fun AlertsTabContent(viewModel: CardViewModel) {
    val alerts by viewModel.communityNotifications.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Notifications Centre", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            if (alerts.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearAllNotifications() }) {
                    Text("Clear All", color = Color(0xFF00FFCC))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (alerts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(52.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("All quiet here!", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(alerts) { alert ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF13172F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        when (alert.eventType) {
                                            "LIKE" -> Color.Red.copy(alpha = 0.2f)
                                            "SAVE" -> Color(0xFFD4AF37).copy(alpha = 0.2f)
                                            "MESSAGE" -> Color(0xFF00FFCC).copy(alpha = 0.2f)
                                            else -> Color.Blue.copy(alpha = 0.2f)
                                        }, CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (alert.eventType) {
                                        "LIKE" -> Icons.Default.Favorite
                                        "SAVE" -> Icons.Default.Star
                                        "MESSAGE" -> Icons.Default.Email
                                        else -> Icons.Default.Info
                                    },
                                    contentDescription = null,
                                    tint = when (alert.eventType) {
                                        "LIKE" -> Color.Red
                                        "SAVE" -> Color(0xFFD4AF37)
                                        "MESSAGE" -> Color(0xFF00FFCC)
                                        else -> Color.White
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(alert.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(alert.content, color = Color.LightGray, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- Creator Profile Detail screen popup ----------------
@Composable
fun CreatorProfileDetailView(
    creator: CreatorProfile,
    viewModel: CardViewModel,
    onBack: () -> Unit,
    onOpenEditor: () -> Unit
) {
    val context = LocalContext.current
    val followedCreators by viewModel.followedCreatorIds.collectAsState()
    val isFollowing = followedCreators.contains(creator.id)

    // Filter cards designed by this user
    val sharedCards by viewModel.communitySharedCards.collectAsState()
    val creatorDesigns = remember(sharedCards, creator.id) {
        sharedCards.filter { it.creatorId == creator.id }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFF0A0C16))
    ) {
        // BLUE AND WHITE PROFESSIONAL BACKGROUND COVER HEADER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFFFFFFFF))
                    )
                )
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            // Creator Badge / Follow button overlay
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.followCreator(creator.id)
                        Toast.makeText(context, if (isFollowing) "Unfollowed creator." else "Following creator successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) Color.Gray else Color(0xFF00FFCC)
                    )
                ) {
                    Icon(if (isFollowing) Icons.Default.Check else Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isFollowing) "Following" else "Follow", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Main info overlapping card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = creator.name,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
                if (creator.isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = Color(0xFF2196F3), modifier = Modifier.size(18.dp))
                }
                if (creator.isPremium) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Star, contentDescription = "Premium Badge", tint = Color(0xFFD4AF37), modifier = Modifier.size(18.dp))
                }
            }

            Text("@${creator.username}", color = Color.Gray, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(10.dp))

            // Stats row grid
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13172F)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf(
                        "Designs" to creatorDesigns.size.toString(),
                        "Followers" to (creator.followersCount + (if (isFollowing) 1 else 0)).toString(),
                        "Following" to creator.followingCount.toString(),
                        "Likes" to creator.likesReceivedCount.toString()
                    ).forEach { stat ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stat.second, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text(stat.first, color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(creator.bio, color = Color.LightGray, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(12.dp))

            // Contact Handles Block
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ContactRowItem(Icons.Default.Home, creator.companyName)
                ContactRowItem(Icons.Default.Home, creator.location)
                ContactRowItem(Icons.Default.Info, creator.website)
                ContactRowItem(Icons.Default.Email, creator.email)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Creator Designs section header
            Text("Shared Portfolio Designs (${creatorDesigns.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))

            if (creatorDesigns.isEmpty()) {
                Text("No published cards inside creator's showcase.", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
            } else {
                creatorDesigns.forEach { sc ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF13172F)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(sc.title, color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(modifier = Modifier.height(140.dp).fillMaxWidth().background(Color.Black)) {
                                StaticMiniCardPreview(card = sc.frontCard)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    viewModel.createNewCardProjectDirectly(sc.frontCard)
                                    onOpenEditor()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Use This Design", color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactRowItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, contentDescription = null, tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = Color.LightGray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ------------------- MAIN SETTINGS & PROFILE TAB ("ME" TAB CONTENT) -------------------
@Composable
fun SettingsAndProfileTabContent(
    viewModel: CardViewModel,
    onOpenEditor: () -> Unit
) {
    val context = LocalContext.current
    val savedCards by viewModel.savedCards.collectAsState()
    val myProfile by viewModel.myProfile.collectAsState()
    val sharedCards by viewModel.communitySharedCards.collectAsState()
    val likedCardIds by viewModel.likedSharedCardIds.collectAsState()
    val favoriteCardIds by viewModel.favoriteSharedCardIds.collectAsState()
    val downloadedCardIds by viewModel.downloadedSharedCardIds.collectAsState()

    var isEditMode by remember { mutableStateOf(false) }

    // Editable form field states
    var editUsername by remember { mutableStateOf(myProfile.username) }
    var editName by remember { mutableStateOf(myProfile.name) }
    var editBio by remember { mutableStateOf(myProfile.bio) }
    var editCompany by remember { mutableStateOf(myProfile.companyName) }
    var editPhone by remember { mutableStateOf(myProfile.phone) }
    var editEmail by remember { mutableStateOf(myProfile.email) }
    var editWebsite by remember { mutableStateOf(myProfile.website) }
    var editLocation by remember { mutableStateOf(myProfile.location) }
    var editInstagram by remember { mutableStateOf(myProfile.instagram) }
    var editFacebook by remember { mutableStateOf(myProfile.facebook) }
    var editLinkedin by remember { mutableStateOf(myProfile.linkedin) }
    var editIsPublic by remember { mutableStateOf(myProfile.isPublic) }

    var selectedPortfolioTab by remember { mutableStateOf("Drafts") } // Drafts, Published, Saved, Downloaded, Favorites

    // Filter portfolios appropriately
    val filteredPortfolioCards = remember(selectedPortfolioTab, savedCards, sharedCards, likedCardIds, favoriteCardIds, downloadedCardIds, myProfile.id) {
        when (selectedPortfolioTab) {
            "Drafts" -> savedCards
            "Published" -> sharedCards.filter { it.creatorId == myProfile.id }.map { it.frontCard }
            "Saved" -> sharedCards.filter { favoriteCardIds.contains(it.id) }.map { it.frontCard }
            "Downloaded" -> sharedCards.filter { downloadedCardIds.contains(it.id) }.map { it.frontCard }
            "Favorites" -> sharedCards.filter { likedCardIds.contains(it.id) }.map { it.frontCard }
            else -> savedCards
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFF0A0C16))
    ) {
        if (!isEditMode) {
            // VIEW PROFILE PANEL - BLUE AND WHITE PROFESSIONAL THEME SPEC
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0D47A1), Color(0xFF1E88E5), Color(0xFFFFFFFF))
                        )
                    )
            ) {
                // Settings action trigger
                IconButton(
                    onClick = { isEditMode = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .testTag("edit_profile_btn")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = Color.White)
                }

                // Profile card representation
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color(0xFF13172F), CircleShape)
                            .border(2.dp, Color(0xFF00FFCC), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = myProfile.name.take(2).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(myProfile.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            if (myProfile.isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.CheckCircle, contentDescription = "Verified Creator", tint = Color(0xFF2196F3), modifier = Modifier.size(16.dp))
                            }
                            if (myProfile.isPremium) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Star, contentDescription = "Premium Creator", tint = Color(0xFFD4AF37), modifier = Modifier.size(16.dp))
                            }
                        }
                        Text("@${myProfile.username}", color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            }

            // Stats indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val stats = listOf(
                    "Designs" to myProfile.totalDesignsCount.toString(),
                    "Followers" to myProfile.followersCount.toString(),
                    "Following" to myProfile.followingCount.toString(),
                    "Likes" to myProfile.likesReceivedCount.toString()
                )
                stats.forEach { s ->
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF13172F))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(s.second, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text(s.first, color = Color.Gray, fontSize = 9.sp)
                        }
                    }
                }
            }

            // Contact & Details card info
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13172F)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Creator Bio", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(myProfile.bio, color = Color.LightGray, fontSize = 12.sp)

                    Divider(color = Color(0xFF1E293B))

                    ContactRowItem(Icons.Default.Home, myProfile.companyName)
                    ContactRowItem(Icons.Default.Phone, myProfile.phone)
                    ContactRowItem(Icons.Default.Email, myProfile.email)
                    ContactRowItem(Icons.Default.Home, myProfile.location)
                    ContactRowItem(Icons.Default.Info, myProfile.website)

                    Divider(color = Color(0xFF1E293B))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Profile Visibility", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .background(
                                    if (myProfile.isPublic) Color(0xFF00FFCC).copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (myProfile.isPublic) "PUBLIC SHOWCASE" else "PRIVATE ENCLAVE",
                                color = if (myProfile.isPublic) Color(0xFF00FFCC) else Color.LightGray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // MY CARDS PORTFOLIO SECTION
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "My Cards Workspace",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable category row filters: Drafts, Published, Saved, Downloaded, Favorites
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val states = listOf("Drafts", "Published", "Saved", "Downloaded", "Favorites")
                    items(states) { tab ->
                        val isSelected = selectedPortfolioTab == tab
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) Color(0xFF00FFCC) else Color(0xFF1E293B),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedPortfolioTab = tab }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tab,
                                color = if (isSelected) Color.Black else Color.LightGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (filteredPortfolioCards.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Home, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No cards inside this workspace", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredPortfolioCards) { cardItem ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clickable {
                                        viewModel.createNewCardProjectDirectly(cardItem)
                                        onOpenEditor()
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF13172F))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = cardItem.cardName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .background(Color.Black.copy(alpha = 0.3f))
                                    ) {
                                        StaticMiniCardPreview(card = cardItem)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(50.dp))

        } else {
            // EDIT PROFILE MODE FORM
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Edit Creator Profile",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
                Text(text = "Customize your social cards hub parameters.", color = Color.Gray, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(16.dp))

                EditProfileField("Username", editUsername, "test_username") { editUsername = it }
                EditProfileField("Display Name", editName, "Your name") { editName = it }
                EditProfileField("Bio Description", editBio, "I am a visual graphic designer...") { editBio = it }
                EditProfileField("Company Name", editCompany, "LLC Enterprises") { editCompany = it }
                EditProfileField("Contact Number", editPhone, "+1 000 000 0000") { editPhone = it }
                EditProfileField("Business Email", editEmail, "contact@mail.com") { editEmail = it }
                EditProfileField("Location Coordinates", editLocation, "Mumbai, India") { editLocation = it }
                EditProfileField("Website URL", editWebsite, "www.mywebsite.link") { editWebsite = it }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Social Links Handle Names", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                EditProfileField("Instagram (username)", editInstagram, "insta_id") { editInstagram = it }
                EditProfileField("Facebook (slug)", editFacebook, "fb_id") { editFacebook = it }
                EditProfileField("LinkedIn (slug)", editLinkedin, "linkedin_id") { editLinkedin = it }

                Spacer(modifier = Modifier.height(12.dp))

                // Privacy Switch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Make Profile Public", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Ensure community users can search and use your templates.", color = Color.Gray, fontSize = 11.sp)
                    }
                    Switch(
                        checked = editIsPublic,
                        onCheckedChange = { editIsPublic = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00FFCC),
                            checkedTrackColor = Color(0xFF13172F)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons: Cancel & Submit Updates
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { isEditMode = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Discard", color = Color.LightGray)
                    }

                    Button(
                        onClick = {
                            if (editUsername.isEmpty() || editName.isEmpty()) {
                                Toast.makeText(context, "Username and Display name cannot be blank.", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.saveFullCreatorProfile(
                                    username = editUsername,
                                    name = editName,
                                    bio = editBio,
                                    companyName = editCompany,
                                    phone = editPhone,
                                    email = editEmail,
                                    location = editLocation,
                                    website = editWebsite,
                                    instagram = editInstagram,
                                    facebook = editFacebook,
                                    linkedin = editLinkedin,
                                    coverBanner = "#004B49",
                                    profilePhoto = "",
                                    isPublic = editIsPublic
                                )
                                isEditMode = false
                                Toast.makeText(context, "Creator Profile successfully live synced!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("save_profile_trigger")
                    ) {
                        Text("Save & Publish Updates", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}

@Composable
fun EditProfileField(
    title: String,
    value: String,
    hint: String,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(hint, color = Color.Gray, fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF00FFCC),
                unfocusedBorderColor = Color(0xFF222B45)
            )
        )
    }
}

// ---------------- PREMIUM UPGRADE PAYWALL DIALOG ----------------
@Composable
fun PremiumUpgradeDialog(
    viewModel: CardViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF13172F)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(54.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text("Developer Creator Pro Upgrade", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp, textAlign = TextAlign.Center)
                Text("Join 12,000+ creators sharing high fidelity visiting layouts.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(16.dp))

                PremiumPromoFeatureItem("Unlimited Templates Publication Feed")
                PremiumPromoFeatureItem("Verified Gold Creator Badge")
                PremiumPromoFeatureItem("Export Premium high resolution PDF formats")
                PremiumPromoFeatureItem("Disable all Google banner visual ads")

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        viewModel.forceTogglePremiumStatus()
                        Toast.makeText(context, "Pro Subscription activated successfully!", Toast.LENGTH_LONG).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Unlock Creator Pro - $1.99/mo", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss) {
                    Text("Maybe Later", color = Color.LightGray)
                }
            }
        }
    }
}

@Composable
fun PremiumPromoFeatureItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = Color.LightGray, fontSize = 12.sp)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalProfileView(
    professional: Professional,
    viewModel: CardViewModel,
    onBack: () -> Unit,
    onChat: () -> Unit
) {
    val context = LocalContext.current
    val connections by viewModel.communityConnections.collectAsState()
    val isConnected = connections.contains(professional.id)
    val sentRequests by viewModel.communitySentRequests.collectAsState()
    val isRequestSent = sentRequests.contains(professional.id)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFF0A0C16))
    ) {
        // Upper Blue/White professional theme gradient header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFFFFFFFF))
                    )
                )
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = professional.name,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.CheckCircle, contentDescription = "Verified Profile", tint = Color(0xFF2196F3), modifier = Modifier.size(18.dp))
            }

            Text(professional.profession, color = Color(0xFF00FFCC), fontSize = 14.sp)
            Text(professional.company, color = Color.Gray, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(14.dp))

            // Action connect bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (!isConnected && !isRequestSent) {
                            viewModel.sendConnectionRequest(professional.id)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) Color.Gray else Color(0xFF00FFCC)
                    ),
                    modifier = Modifier.weight(1.5f)
                ) {
                    val label = when {
                        isConnected -> "Connected"
                        isRequestSent -> "Pending acceptance..."
                        else -> "Request connection"
                    }
                    Text(label, color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onChat,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Chat", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("About Me", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(professional.bio, color = Color.LightGray, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(16.dp))

            Text("Contact Handles", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ContactRowItem(Icons.Default.Home, professional.location)
                ContactRowItem(Icons.Default.Info, professional.website)
                ContactRowItem(Icons.Default.Email, professional.email)
                ContactRowItem(Icons.Default.Phone, professional.mobile)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Professional matching card layout representation
            Text("Professional Card Design", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF222B45), RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                StaticMiniCardPreview(card = professional.matchingCard)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.createNewCardProjectDirectly(professional.matchingCard)
                    Toast.makeText(context, "Card loaded inside template editor!", Toast.LENGTH_LONG).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Use Card as Template", color = Color.Black)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityChatDetailScreen(
    professional: Professional,
    viewModel: CardViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var textMessageInput by remember { mutableStateOf("") }
    val chatsMap by viewModel.communityChats.collectAsState()
    val chatMessages = chatsMap[professional.id] ?: emptyList()
    val isMuted = viewModel.communityMuted.collectAsState().value.contains(professional.id)
    val isBlocked = viewModel.communityBlocked.collectAsState().value.contains(professional.id)
    var showChatActionsMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C16))
    ) {
        // Chat Header TopAppBar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(android.graphics.Color.parseColor(professional.avatarColorHex)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(professional.name.take(2).uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(professional.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(if (isMuted) "Muted" else "Online • Active Now", color = if (isMuted) Color.Gray else Color(0xFF00FFCC), fontSize = 10.sp)
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.LightGray)
                }
            },
            actions = {
                Box {
                    IconButton(onClick = { showChatActionsMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Chat Actions", tint = Color.LightGray)
                    }
                    DropdownMenu(
                        expanded = showChatActionsMenu,
                        onDismissRequest = { showChatActionsMenu = false },
                        modifier = Modifier.background(Color(0xFF1A1F3B))
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isMuted) "Unmute Updates" else "Mute Chat", color = Color.White) },
                            onClick = {
                                viewModel.toggleMuteProfessional(professional.id)
                                showChatActionsMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isBlocked) "Unblock Chat" else "Block Conversation", color = Color.Red) },
                            onClick = {
                                viewModel.toggleBlockProfessional(professional.id)
                                showChatActionsMenu = false
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1022))
        )

        // Conversation list container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (chatMessages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No messages yet", color = Color.Gray, fontSize = 13.sp)
                        Text("Type below to introduce your visual card details!", color = Color.DarkGray, fontSize = 11.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    reverseLayout = false
                ) {
                    items(chatMessages) { msg ->
                        val isSelf = msg.senderId == "self"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelf) Color(0xFF0D47A1) else Color(0xFF1E293B)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(msg.text, color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom text message input field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D1022))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    // Prepopulate chat with card invitation details directly!
                    textMessageInput = "Hello! Check out my newly published visiting card showcase design. I'd love your comments."
                },
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = "Attach Card", tint = Color(0xFF00FFCC))
            }

            OutlinedTextField(
                value = textMessageInput,
                onValueChange = { textMessageInput = it },
                placeholder = { Text("Write your message here...", color = Color.Gray, fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00FFCC),
                    unfocusedBorderColor = Color(0xFF222B45)
                ),
                shape = RoundedCornerShape(20.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = {
                    if (textMessageInput.isNotEmpty()) {
                        viewModel.sendCommunityMessage(professional.id, textMessageInput)
                        textMessageInput = ""
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF00FFCC)),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.Black, modifier = Modifier.size(16.dp))
            }
        }
    }
}
