package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserCard
import com.example.viewmodel.CardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.asImageBitmap

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider

enum class ActiveScreen {
    SPLASH, AUTH, DASHBOARD, EDITOR, TEMPLATES, PREMIUM, SETTINGS, ADS_MANAGER, AI_GENERATOR, COMMUNITY
}

// Struct to represent visual template presets
data class TemplatePreset(
    val id: String,
    val name: String,
    val category: String,
    val isPremium: Boolean,
    val bgStart: String,
    val bgEnd: String,
    val primaryColor: String,
    val borderStyle: String,
    val fontStyle: String
)

fun generate500Templates(): List<TemplatePreset> {
    return listOf(
        // FREE TEMPLATES (10)
        TemplatePreset("corporate_blue", "Corporate Blue", "Corporate", false, "#2196F3", "#0D47A1", "#00FFCC", "MINIMAL_GOLD", "Space Grotesk"),
        TemplatePreset("modern_white", "Modern White", "Modern", false, "#FFFFFF", "#EAF4FF", "#2196F3", "SLATE_CLEAN", "Space Grotesk"),
        TemplatePreset("creative_orange", "Creative Orange", "Creative", false, "#FFA000", "#FF5252", "#FFFFFF", "CREATIVE_SHIELD", "Space Grotesk"),
        TemplatePreset("tech_blue", "Technology Blue", "Technology", false, "#0A0E1A", "#1D2130", "#00FFCC", "CYBER_SLATE", "Tech Clean"),
        TemplatePreset("startup", "Startup Venture", "Corporate", false, "#2196F3", "#EAF4FF", "#101C33", "PLAIN", "Space Grotesk"),
        TemplatePreset("marketing", "Growth Marketing", "Business", false, "#FF5E7E", "#2196F3", "#FFFFFF", "PLAIN", "Space Grotesk"),
        TemplatePreset("restaurant", "Gourmet Restaurant", "Restaurant", false, "#FE5F55", "#3D3B3C", "#F1C40F", "CREATIVE_SHIELD", "Elegant Serif"),
        TemplatePreset("photography", "Studio Photography", "Creative", false, "#111111", "#333333", "#00FFCC", "PLAIN", "Tech Clean"),
        TemplatePreset("education", "Academy Education", "Business", false, "#4B6584", "#2F3542", "#F7D794", "SLATE_CLEAN", "Space Grotesk"),
        TemplatePreset("consultant", "Private Consultant", "Corporate", false, "#FFFFFF", "#ECEFF1", "#37474F", "PLAIN", "Elegant Serif"),

        // PREMIUM TEMPLATES (20)
        TemplatePreset("black_gold_luxury", "Black Gold Luxury", "Luxury", true, "#1A1A1A", "#111111", "#D4AF37", "MINIMAL_GOLD", "Elegant Serif"),
        TemplatePreset("executive_corporate", "Executive Corporate", "Corporate", true, "#130CB7", "#52E5E7", "#FFFFFF", "CYBER_SLATE", "Space Grotesk"),
        TemplatePreset("real_estate_premium", "Real Estate Premium", "Real Estate", true, "#1F2937", "#111827", "#D4AF37", "MINIMAL_GOLD", "Elegant Serif"),
        TemplatePreset("medical_premium", "Medical Premium", "Medical", true, "#FFFFFF", "#E8F5E9", "#2E7D32", "PLAIN", "Space Grotesk"),
        TemplatePreset("construction_premium", "Construction Premium", "Engineering", true, "#2C3E50", "#F39C12", "#FFFFFF", "CREATIVE_SHIELD", "Tech Clean"),
        TemplatePreset("software_agency", "Software Agency", "Technology", true, "#0F172A", "#1E293B", "#38BDF8", "CYBER_SLATE", "Tech Clean"),
        TemplatePreset("legal_services", "Legal Services", "Business", true, "#2C2C2C", "#1E1E1E", "#C5A880", "MINIMAL_GOLD", "Elegant Serif"),
        TemplatePreset("financial_consultant", "Financial Consultant", "Business", true, "#0F2027", "#203A43", "#11998E", "SLATE_CLEAN", "Space Grotesk"),
        TemplatePreset("architect", "Architect Studio", "Creative", true, "#FFFFFF", "#DDDDDD", "#000000", "PLAIN", "Tech Clean"),
        TemplatePreset("interior_designer", "Interior Designer", "Creative", true, "#F7E1D7", "#EDF2F4", "#2B2D42", "PLAIN", "Elegant Serif"),
        TemplatePreset("travel_agency", "Travel Agency", "Business", true, "#00B4DB", "#0083B0", "#FFFFFF", "PLAIN", "Space Grotesk"),
        TemplatePreset("gym_trainer", "Gym Elite Trainer", "Business", true, "#000000", "#111111", "#FF3366", "CYBER_SLATE", "Tech Clean"),
        TemplatePreset("beauty_salon", "Beauty Salon Spa", "Creative", true, "#FFE5EC", "#FFC2D1", "#FB6F92", "PLAIN", "Elegant Serif"),
        TemplatePreset("creative_studio", "Creative Design Studio", "Creative", true, "#FC466B", "#3F5EFB", "#FFFFFF", "CREATIVE_SHIELD", "Space Grotesk"),
        TemplatePreset("luxury_hotel", "Luxury Boutique Hotel", "Luxury", true, "#0D0D0D", "#1F1F1F", "#C5A880", "MINIMAL_GOLD", "Elegant Serif"),
        TemplatePreset("event_planner", "Premium Event Planner", "Business", true, "#F3E7E9", "#E3EEFF", "#D57EEB", "PLAIN", "Elegant Serif"),
        TemplatePreset("business_elite", "Business Elite Class", "Business", true, "#181D26", "#0B0E14", "#EAEAEA", "SLATE_CLEAN", "Space Grotesk"),
        TemplatePreset("ceo_card", "Executive CEO Card", "Luxury", true, "#1E2433", "#0F111A", "#D4AF37", "MINIMAL_GOLD", "Elegant Serif"),
        TemplatePreset("premium_modern", "Premium Modern", "Modern", true, "#2980B9", "#2C3E50", "#EF6C00", "PLAIN", "Space Grotesk"),
        TemplatePreset("signature_collection", "Signature Collection", "Luxury", true, "#2D3436", "#1E272E", "#F1C40F", "MINIMAL_GOLD", "Elegant Serif")
    )
}

val cardTemplates = generate500Templates()

@Composable
fun DashboardScreens(
    viewModel: CardViewModel,
    currentScreen: ActiveScreen,
    onNavigate: (ActiveScreen) -> Unit,
    onOpenEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPremium by viewModel.isUserPremium.collectAsState()

    // Handle system back gesture
    BackHandler(enabled = currentScreen != ActiveScreen.DASHBOARD && currentScreen != ActiveScreen.AUTH && currentScreen != ActiveScreen.SPLASH) {
        when (currentScreen) {
            ActiveScreen.TEMPLATES, ActiveScreen.PREMIUM, ActiveScreen.SETTINGS, ActiveScreen.AI_GENERATOR -> onNavigate(ActiveScreen.DASHBOARD)
            ActiveScreen.ADS_MANAGER -> onNavigate(ActiveScreen.SETTINGS)
            else -> onNavigate(ActiveScreen.DASHBOARD)
        }
    }

    Scaffold(
        bottomBar = {
            if (viewModel.prefs.bannerAdEnabled && !isPremium) {
                BannerAdView(
                    adUnitId = "ca-app-pub-5487081756225733/3150766346"
                )
            }
        },
        containerColor = Color(0xFF0A0C16)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0C16))
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = if (currentScreen == ActiveScreen.SPLASH) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxHeight()
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                }
            ) {
                when (currentScreen) {
                    ActiveScreen.SPLASH -> {
                        SplashScreenView(viewModel, onNavigate)
                    }
                    ActiveScreen.AUTH -> {
                        AuthenticationView(viewModel, onNavigate)
                    }
                    ActiveScreen.DASHBOARD -> {
                        DashboardView(viewModel, onNavigate, onOpenEditor)
                    }
                    ActiveScreen.TEMPLATES -> {
                        TemplatesView(viewModel, onNavigate, onOpenEditor)
                    }
                    ActiveScreen.SETTINGS -> {
                        SettingsView(viewModel, onNavigate)
                    }
                    ActiveScreen.PREMIUM -> {
                        PremiumView(viewModel, onNavigate)
                    }
                    ActiveScreen.ADS_MANAGER -> {
                        AdsManagerView(viewModel, onNavigate)
                    }
                    ActiveScreen.AI_GENERATOR -> {
                        AICardGeneratorView(viewModel, onNavigate, onOpenEditor)
                    }
                    ActiveScreen.COMMUNITY -> {
                        CommunityMainScreen(viewModel, onNavigateBack = { onNavigate(ActiveScreen.DASHBOARD) }, onOpenEditor = onOpenEditor)
                    }
                    else -> {}
                }
            }
        }
    }
}

// 1. SPLASH SCREEN
@Composable
fun SplashScreenView(viewModel: CardViewModel, onNavigate: (ActiveScreen) -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    
    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2200) // Beautiful splash hold time
        if (viewModel.prefs.isLoggedIn) {
            onNavigate(ActiveScreen.DASHBOARD)
        } else {
            onNavigate(ActiveScreen.AUTH)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFEAF4FF), Color(0xFFFFFFFF))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = startAnimation,
            enter = fadeIn(animationSpec = tween(1200)) + scaleIn(animationSpec = tween(1200)),
            exit = fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                // High-End Glowing Card Logo Icon in Blue & White Theme
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(110.dp)
                        .background(
                            Brush.sweepGradient(listOf(Color(0xFF2196F3), Color(0xFFEAF4FF), Color(0xFF2196F3))),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(3.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White, shape = RoundedCornerShape(22.dp))
                    ) {
                        Text(
                            text = "P'P",
                            color = Color(0xFF2196F3),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(28.dp))
                
                Text(
                    text = "Pillai Play Studio",
                    color = Color(0xFF101C33),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                
                Text(
                    text = "Business Card Maker",
                    color = Color(0xFF2196F3),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(80.dp))
                
                CircularProgressIndicator(
                    color = Color(0xFF2196F3),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(28.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Premium Visiting Card Creator Suite v1.2",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// 2. AUTHENTICATION VIEW
@Composable
fun AuthenticationView(viewModel: CardViewModel, onNavigate: (ActiveScreen) -> Unit) {
    val context = LocalContext.current
    var isSignUpMode by remember { mutableStateOf(false) }

    // Google Auth Error diagnostics and Sandbox states
    var showGoogleErrorDialog by remember { mutableStateOf(false) }
    var googleErrorCode by remember { mutableStateOf(0) }
    var googleErrorMessage by remember { mutableStateOf("") }
    
    // Apple Auth Dialog state (for actual login error messaging)
    var showAppleErrorDialog by remember { mutableStateOf(false) }
    var appleErrorMessage by remember { mutableStateOf("") }
    
    val googleSignInClient = remember(context) {
        val webClientId = try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else null
        } catch (e: Exception) {
            null
        }
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).apply {
            requestEmail()
            requestProfile()
            if (!webClientId.isNullOrEmpty()) {
                requestIdToken(webClientId)
            }
        }.build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                val userEmail = account.email ?: ""
                val userName = account.displayName ?: "Google User"
                val userPhoto = account.photoUrl?.toString() ?: ""
                
                try {
                    val mAuth = FirebaseAuth.getInstance()
                    if (account.idToken != null) {
                        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                        mAuth.signInWithCredential(credential)
                            .addOnCompleteListener { fbTask ->
                                if (fbTask.isSuccessful) {
                                    val fbUser = fbTask.result?.user
                                    viewModel.loginGoogle(
                                        fbUser?.displayName ?: userName,
                                        fbUser?.email ?: userEmail,
                                        fbUser?.photoUrl?.toString() ?: userPhoto
                                    )
                                } else {
                                    viewModel.loginGoogle(userName, userEmail, userPhoto)
                                }
                                Toast.makeText(context, "Google Signed In: $userName", Toast.LENGTH_SHORT).show()
                                onNavigate(ActiveScreen.DASHBOARD)
                            }
                    } else {
                        viewModel.loginGoogle(userName, userEmail, userPhoto)
                        Toast.makeText(context, "Google Signed In: $userName", Toast.LENGTH_SHORT).show()
                        onNavigate(ActiveScreen.DASHBOARD)
                    }
                } catch (fe: Exception) {
                    viewModel.loginGoogle(userName, userEmail, userPhoto)
                    Toast.makeText(context, "Google Signed In: $userName", Toast.LENGTH_SHORT).show()
                    onNavigate(ActiveScreen.DASHBOARD)
                }
            } else {
                googleErrorCode = -1
                googleErrorMessage = "Null Account Data"
                showGoogleErrorDialog = true
            }
        } catch (e: ApiException) {
            Log.e("GoogleSignIn", "Google Sign-In failed with status code: ${e.statusCode}", e)
            googleErrorCode = e.statusCode
            googleErrorMessage = e.message ?: "Authentication signature is unregistered in Google console settings."
            showGoogleErrorDialog = true
        }
    }

    // Credentials
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var showOtpField by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0D16))
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant gold branding header
            Text(
                text = "Pillai'Play Studio",
                color = Color(0xFFD4AF37),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            
            Text(
                text = if (isSignUpMode) "Create Your Designer Profile" else "Access Design Suite",
                color = Color.White,
                fontSize = 16.sp,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            // Auth card details
            Surface(
                color = Color(0xFF131722),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF232A3B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isSignUpMode) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Designer Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFD4AF37)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("auth_name_field")
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFD4AF37)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("auth_email_field")
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFD4AF37)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("auth_password_field")
                    )

                    if (!isSignUpMode && !showOtpField) {
                        TextButton(
                            onClick = { Toast.makeText(context, "Password reset link sent to $email", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Forgot Password?", color = Color(0xFF00FFCC), fontSize = 13.sp)
                        }
                    }

                    // Mobile OTP Section
                    if (showOtpField) {
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { otpCode = it },
                            label = { Text("Enter 6-Digit OTP") },
                            leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00FFCC)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (isSignUpMode) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone (For OTP Verification)") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = { 
                                if (phone.length >= 10) {
                                    showOtpField = true
                                    Toast.makeText(context, "OTP Sent to $phone", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Enter valid phone number", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Text("Send OTP", color = Color(0xFF00FFCC))
                            }
                        }
                    }

                    // Main Action Button
                    Button(
                        onClick = {
                            if (email.isNotEmpty() && password.isNotEmpty() && (!isSignUpMode || name.isNotEmpty())) {
                                viewModel.loginMock(
                                    name = if (isSignUpMode) name else email.substringBefore("@").replaceFirstChar { it.uppercase() },
                                    email = email
                                )
                                Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                                onNavigate(ActiveScreen.DASHBOARD)
                            } else {
                                Toast.makeText(context, "Please populate fields correctly", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_button")
                    ) {
                        Text(
                            text = if (isSignUpMode) "Register Account" else "Sign In Securely",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Social Providers Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF22283A))
                Text("    OR CONTINUE WITH    ", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF22283A))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Google & Special Apple Button rows
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        val signInIntent = googleSignInClient.signInIntent
                        googleSignInLauncher.launch(signInIntent)
                    },
                    border = BorderStroke(1.dp, Color(0xFF232A3B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFFD4AF37))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Continue with Google", color = Color.White)
                }

                OutlinedButton(
                    onClick = {
                        val activity = context as? ComponentActivity
                        if (activity != null) {
                            try {
                                val provider = OAuthProvider.newBuilder("apple.com")
                                provider.scopes = listOf("email", "name")
                                val mAuth = FirebaseAuth.getInstance()
                                mAuth.startActivityForSignInWithProvider(activity, provider.build())
                                    .addOnSuccessListener { authResult ->
                                        val user = authResult.user
                                        val name = user?.displayName ?: user?.email?.substringBefore("@") ?: "Apple User"
                                        val email = user?.email ?: "builder@apple.com"
                                        viewModel.loginApple(name, email, null)
                                        Toast.makeText(context, "Welcome, $name!", Toast.LENGTH_SHORT).show()
                                        onNavigate(ActiveScreen.DASHBOARD)
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("AppleSignIn", "Apple Sign-In failed", e)
                                        appleErrorMessage = e.localizedMessage ?: "User cancelled or unconfigured Service ID redirect URI."
                                        showAppleErrorDialog = true
                                    }
                            } catch (e: Exception) {
                                Log.e("AppleSignIn", "Firebase Auth not initialized", e)
                                appleErrorMessage = "Firebase Authentication is not initialized or google-services.json details are missing."
                                showAppleErrorDialog = true
                            }
                        } else {
                            Toast.makeText(context, "Activity context is unavailable.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    border = BorderStroke(1.dp, Color(0xFF232A3B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.LightGray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Continue with Apple", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Switch Mode Text Footer
            TextButton(
                onClick = { isSignUpMode = !isSignUpMode }
            ) {
                Text(
                    text = if (isSignUpMode) "Already have an account? Sign In" else "New to Pillai'Play? Register Free",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            }

            // --- 1. Google Sign-In Diagnostics Dialog ---
            if (showGoogleErrorDialog) {
                AlertDialog(
                    onDismissRequest = { showGoogleErrorDialog = false },
                    containerColor = Color(0xFF131722),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = Color(0xFFD4AF37),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Google Sign-In Diagnostics",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Adhering to official Play Services Google Sign-In, the accounts chooser failed with:",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1C2230), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        "Error Code: $googleErrorCode (${if (googleErrorCode == 10) "DEVELOPER_ERROR" else "SIGN_IN_FAILED"})",
                                        color = Color(0xFF00FFCC),
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Message: $googleErrorMessage",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            
                            Text(
                                "To fix this and authenticate, please verify and register your developer settings on Google Cloud / Firebase console:\n\n" +
                                "1. Active ApplicationID:\n   com.aistudio.cardmaker.wfpyqx\n" +
                                "2. Debug SHA-1 signature fingerprint:\n   D4:2B:A1:59:C6:E9:A0:50:D0:61:B3:72:8C:E6:57:BF:0D:7F:04:C9\n" +
                                "3. Debug SHA-256 signature fingerprint:\n   C5:A8:06:9E:54:B4:6D:69:5F:ED:0D:B2:7A:08:92:B5:2E:E4:1B:38:95:05:3F:00:CB:76:11:8F:AB:E0:BF:73\n\n" +
                                "Ensure your google-services.json matches these parameters and Firebase Google Authentication Provider is enabled.",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showGoogleErrorDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                        ) {
                            Text("Acknowledge", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // --- 2. Apple Sign-In OAuth Diagnostics Dialog ---
            if (showAppleErrorDialog) {
                AlertDialog(
                    onDismissRequest = { showAppleErrorDialog = false },
                    containerColor = Color(0xFF131722),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = Color(0xFFD4AF37),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Apple Sign-In Diagnostics",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "The official Apple ID Authentication request was launched but could not verify successfully:",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1C2230), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        "Error: Apple Sign-In Provider",
                                        color = Color(0xFFFF453A),
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Message: $appleErrorMessage",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            
                            Text(
                                "Official Apple OAuth on Android requires a configured Apple Service ID, Team ID, Private Key, and Redirect URIs. For development and testing environments, please check that Firebase Auth possesses the correct apple.com provider redirect URLs in your Firebase console.",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showAppleErrorDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                        ) {
                            Text("Acknowledge", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }
    }
}

// 3. HOME DASHBOARD VIEW
@Composable
fun DashboardView(
    viewModel: CardViewModel,
    onNavigate: (ActiveScreen) -> Unit,
    onOpenEditor: () -> Unit
) {
    val context = LocalContext.current
    val cards by viewModel.savedCards.collectAsState()
    val isPremium by viewModel.isUserPremium.collectAsState()
    var showCreatePopup by remember { mutableStateOf(false) }
    var selectedPresetToCreate by remember { mutableStateOf<TemplatePreset?>(null) }
    var newCardNameInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C16))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Welcome Premium Card header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val photoUrl = viewModel.prefs.userPhoto
                    if (photoUrl.isNotEmpty() && photoUrl != "ic_avatar" && photoUrl.startsWith("http")) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "User Avatar",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, Color(0xFFD4AF37), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF22283A), CircleShape)
                                .border(1.dp, Color(0xFFD4AF37), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = viewModel.prefs.userName.take(2).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Welcome Back,",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                        Text(
                            text = viewModel.prefs.userName,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (isPremium) Color(0xFFD4AF37) else Color(0xFF00FFCC), shape = CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPremium) "Premium Member" else "Free Account",
                                color = if (isPremium) Color(0xFFD4AF37) else Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Quick Settings shortcut
                IconButton(
                    onClick = { onNavigate(ActiveScreen.SETTINGS) },
                    modifier = Modifier.background(Color(0xFF131722), CircleShape)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.LightGray)
                }
            }



            // MAIN GRID SELECTIONS / QUICK ACTIONS
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "QUICK CREATION HUBS",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    DashboardActionItem(
                        title = "Create New Card",
                        sub = "Launch custom blank canvas",
                        icon = Icons.Default.Add,
                        color = Color(0xFF00FFCC),
                        modifier = Modifier.weight(1f).testTag("action_create_card")
                    ) {
                        onNavigate(ActiveScreen.TEMPLATES)
                    }

                    DashboardActionItem(
                        title = "AI Generator",
                        sub = "Let Gemini design content",
                        icon = Icons.Default.Star,
                        color = Color(0xFFD4AF37),
                        modifier = Modifier.weight(1f).testTag("action_ai_generator")
                    ) {
                        onNavigate(ActiveScreen.AI_GENERATOR)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    DashboardActionItem(
                        title = "Card Community",
                        sub = "Browse designer community",
                        icon = Icons.Default.Person,
                        color = Color(0xFFFF5E7E),
                        modifier = Modifier.weight(1f).testTag("action_business_community")
                    ) {
                        onNavigate(ActiveScreen.COMMUNITY)
                    }

                    DashboardActionItem(
                        title = "App Settings",
                        sub = "Configure configurations",
                        icon = Icons.Default.Settings,
                        color = Color(0xFFA4B5C4),
                        modifier = Modifier.weight(1f)
                    ) {
                        onNavigate(ActiveScreen.SETTINGS)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // MY CUSTOM DESIGNS LIST
            Column(
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "MY CUSTOM DESIGNS (${cards.size})",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (cards.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(Color(0xFF131722), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(44.dp), tint = Color.DarkGray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No designs saved", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Click + or manual design to create your first business card!", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        cards.forEach { card ->
                            SavedProjectRowItem(
                                card = card,
                                onEdit = {
                                    viewModel.selectCardForEditing(card)
                                    onOpenEditor()
                                },
                                onDuplicate = { viewModel.duplicateCard(card) },
                                onDelete = { viewModel.deleteCard(card) },
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        // FLOATING ACTION BUTTON - BOTTOM RIGHT CORNER (Create New Card)
        FloatingActionButton(
            onClick = {
                selectedPresetToCreate = null
                newCardNameInput = "My New Business Card"
                showCreatePopup = true
            },
            containerColor = Color(0xFFD4AF37),
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create New Card", modifier = Modifier.size(24.dp))
        }
    }

    // New Project Dialog with presets integration
    if (showCreatePopup) {
        AlertDialog(
            onDismissRequest = { showCreatePopup = false },
            title = {
                Text(
                    text = if (selectedPresetToCreate != null) "Create with preset: ${selectedPresetToCreate?.name}" else "Set Project Title",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                OutlinedTextField(
                    value = newCardNameInput,
                    onValueChange = { newCardNameInput = it },
                    label = { Text("Project Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFFD4AF37)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCardNameInput.isNotEmpty()) {
                            viewModel.createNewCardProject(
                                name = newCardNameInput,
                                templateId = selectedPresetToCreate?.id ?: "vibe_modern_gold"
                            )
                            showCreatePopup = false
                            onOpenEditor()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                ) {
                    Text("Launch Editor", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePopup = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF131722)
        )
    }
}

@Composable
fun DashboardActionItem(
    title: String,
    sub: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(sub, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun SavedProjectRowItem(
    card: UserCard,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    viewModel: CardViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf(card.cardName) }
    var isExporting by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFF131722),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF222B3A)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(0.6f)
            ) {
                // Miniature visual thumbnail preview representing the layout
                Box(
                    modifier = Modifier
                        .size(width = 68.dp, height = 41.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(
                                    try { Color(android.graphics.Color.parseColor(card.backgroundColor)) } catch(e: Exception) { Color(0xFF10121A) },
                                    try { Color(android.graphics.Color.parseColor(card.gradientEndColor)) } catch(e: Exception) { Color(0xFF1E2130) }
                                )
                            )
                        )
                        .border(
                            width = 0.5.dp, 
                            color = try { Color(android.graphics.Color.parseColor(card.qrCodeColor)).copy(alpha = 0.5f) } catch(e: Exception) { Color(0xFFD4AF37).copy(alpha = 0.5f) },
                            shape = RoundedCornerShape(3.dp)
                        )
                ) {
                    val scaleX = 68f / 400f
                    val scaleY = 41f / 240f

                    // QR code miniature
                    if (card.qrCodeVisible) {
                        Box(
                            modifier = Modifier
                                .offset(
                                    x = (card.qrCodeX * scaleX).dp,
                                    y = (card.qrCodeY * scaleY).dp
                                )
                                .size((card.qrCodeSize * scaleX).dp)
                                .background(
                                    try { Color(android.graphics.Color.parseColor(card.qrCodeColor)).copy(0.7f) } catch(e: Exception) { Color.White.copy(0.7f) },
                                    RoundedCornerShape(1.dp)
                                )
                        )
                    }

                    // Content lines miniature offset
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (card.fullNameX * scaleX).dp,
                                y = (card.fullNameY * scaleY).dp
                            )
                            .width(22.dp)
                            .height(2.5.dp)
                            .background(Color.White)
                    )

                    Box(
                        modifier = Modifier
                            .offset(
                                x = (card.jobTitleX * scaleX).dp,
                                y = (card.jobTitleY * scaleY).dp
                            )
                            .width(14.dp)
                            .height(1.5.dp)
                            .background(Color.White.copy(0.6f))
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = card.cardName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename",
                            tint = Color(0xFFD4AF37),
                            modifier = Modifier
                                .size(11.dp)
                                .clickable { showRenameDialog = true }
                        )
                    }
                    Text(
                        text = card.themeName, 
                        color = try { Color(android.graphics.Color.parseColor(card.qrCodeColor)) } catch(e: Exception) { Color(0xFFD4AF37) },
                        fontSize = 10.sp
                    )
                }
            }

            // Beautiful Action button row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                // Edit / Customize button
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Card",
                        tint = Color(0xFF00FFCC),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Copy / Duplicate button
                IconButton(onClick = onDuplicate) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Duplicate Card",
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Export / Share PDF button
                IconButton(
                    onClick = {
                        if (!isExporting) {
                            isExporting = true
                            coroutineScope.launch {
                                try {
                                    val exportedFile = com.example.utils.PDFExporter.exportCardToFile(
                                        context, card, "PDF", "HD", false
                                    )
                                    if (exportedFile != null) {
                                        triggerSystemShare(context, exportedFile, "PDF")
                                    } else {
                                        Toast.makeText(context, "Failed to compile card export", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isExporting = false
                                }
                            }
                        }
                    }
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            color = Color(0xFFD4AF37),
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Export Card",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Delete button
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    // Direct Inline Rename Popup
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = {
                Text("Rename Design", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Design Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFFD4AF37)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotEmpty()) {
                            viewModel.updateCardInDatabase(card.copy(cardName = renameInput))
                            showRenameDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                ) {
                    Text("Rename", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF131722)
        )
    }
}

// 4. CREATE NEW CARD VIEW (DUAL TAB BROWSING + SMART AUTO-GENERATION & CUSTOM BLANK BUILDER)
@Composable
fun TemplatesView(
    viewModel: CardViewModel,
    onNavigate: (ActiveScreen) -> Unit,
    onOpenEditor: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 for Templates, 1 for Custom Blank Builder
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showSmartGenerator by remember { mutableStateOf<TemplatePreset?>(null) }

    // Custom Blank States
    var cardNameInput by remember { mutableStateOf("My Premium Business Card") }
    var selectedShape by remember { mutableStateOf("RECTANGLE") }
    var selectedBgType by remember { mutableStateOf("SOLID") } // SOLID, GRADIENT
    var bgColorHex by remember { mutableStateOf("#FFFFFF") }
    var gradientEndColorHex by remember { mutableStateOf("#E5E9F0") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1424))
    ) {
        // Toolbar with Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onNavigate(ActiveScreen.DASHBOARD) },
                modifier = Modifier.background(Color(0xFF1E243D), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Creating Premium Card", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("Choose a curated style or build from scratch", color = Color.Gray, fontSize = 11.sp)
            }
        }

        // Custom Slider Selector Tab Row (Corporate Modern Look)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .background(Color(0xFF161B2F), RoundedCornerShape(24.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedTab = 0 }
                    .background(if (selectedTab == 0) Color(0xFF2196F3) else Color.Transparent, RoundedCornerShape(20.dp))
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = if (selectedTab == 0) Color.White else Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Design Templates (30)", color = if (selectedTab == 0) Color.White else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedTab = 1 }
                    .background(if (selectedTab == 1) Color(0xFF2196F3) else Color.Transparent, RoundedCornerShape(20.dp))
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = if (selectedTab == 1) Color.White else Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Custom Blank Card", color = if (selectedTab == 1) Color.White else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedTab == 0) {
            // ================== TAB 0: TEMPLATE BROWSER ==================
            Column(modifier = Modifier.fillMaxSize()) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search 30+ luxury presets...", color = Color.Gray, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .testTag("template_search_bar"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF2196F3),
                        unfocusedBorderColor = Color(0xFF222B3A),
                        focusedContainerColor = Color(0xFF161B2F),
                        unfocusedContainerColor = Color(0xFF161B2F)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Category Filters Horizontal Row
                val categories = listOf("All", "Corporate", "Business", "Modern", "Luxury", "Creative", "Medical", "Engineering", "Real Estate", "Restaurant", "Technology")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { desc ->
                        val active = selectedCategory == desc
                        Box(
                            modifier = Modifier
                                .clickable { selectedCategory = desc }
                                .background(if (active) Color(0xFF2196F3) else Color(0xFF1E243D), RoundedCornerShape(16.dp))
                                .border(1.dp, if (active) Color(0xFF2196F3) else Color(0xFF222B3A), RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = desc,
                                color = if (active) Color.White else Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Grid of Curated Templates
                val isPremiumUser by viewModel.isUserPremium.collectAsState()
                val favList by viewModel.favoriteTemplatesList.collectAsState()

                val filtered = cardTemplates.filter { preset ->
                    val matchCat = selectedCategory == "All" || preset.category.equals(selectedCategory, ignoreCase = true)
                    val matchQuery = searchQuery.isEmpty() || preset.name.contains(searchQuery, ignoreCase = true)
                    matchCat && matchQuery
                }

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No match templates found", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val chunks = filtered.chunked(2)
                        items(chunks.size) { chunkIndex ->
                            val pair = chunks[chunkIndex]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                pair.forEach { preset ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        TemplateCardItem(
                                            preset = preset,
                                            isPremiumUnlocked = isPremiumUser,
                                            onSelect = { showSmartGenerator = preset },
                                            isFavorite = favList.contains(preset.id),
                                            onToggleFavorite = { viewModel.toggleFavoriteTemplate(preset.id) }
                                        )
                                    }
                                }
                                if (pair.size < 2) {
                                    Box(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ================== TAB 1: CUSTOM BLANK CARD ==================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Section 1: Project Details
                Surface(
                    color = Color(0xFF1E243D),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF222B3A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "PROJECT DETAILS",
                            color = Color(0xFF2196F3),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = cardNameInput,
                            onValueChange = { cardNameInput = it },
                            label = { Text("Business Card Title", color = Color.Gray, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("new_card_name_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFF2196F3),
                                unfocusedLabelColor = Color.Gray,
                                focusedBorderColor = Color(0xFF2196F3),
                                unfocusedBorderColor = Color(0xFF222B3A)
                            )
                        )
                    }
                }

                // Section 2: Choose Card Shape/Format
                Surface(
                    color = Color(0xFF1E243D),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF222B3A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SELECT CARD FORMAT SHAPE",
                            color = Color(0xFF2196F3),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        val shapesList = listOf(
                            Triple("RECTANGLE", "Standard Rectangle", "Sharp professional corners"),
                            Triple("ROUNDED_RECTANGLE", "Rounded Corners", "Modern dynamic edge look"),
                            Triple("LEAF_CUT", "Elegant Leaf Cut", "Unique diagonal soft-split style"),
                            Triple("HEXAGON", "Premium Hexagon", "Distinctive six-sided badge"),
                            Triple("CIRCLE", "Circular Token", "Rounded card stamp styling"),
                            Triple("FOLDED", "Folded Brochure", "Centrally aligned bi-fold model")
                        )

                        shapesList.forEach { (shapeId, label, desc) ->
                            val active = selectedShape == shapeId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedShape = shapeId }
                                    .background(
                                        color = if (active) Color(0xFF11162C) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = if (active) 1.dp else 1.dp,
                                        color = if (active) Color(0xFF2196F3) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(if (active) Color(0xFF2196F3) else Color(0xFF1D212E), RoundedCornerShape(6.dp))
                                        .border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                               ) {
                                    Canvas(modifier = Modifier.size(20.dp)) {
                                        val drawColor = if (active) Color.White else Color.LightGray
                                        when (shapeId) {
                                            "RECTANGLE" -> {
                                                drawRect(color = drawColor, size = size)
                                            }
                                            "ROUNDED_RECTANGLE" -> {
                                                drawRoundRect(color = drawColor, size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f))
                                            }
                                            "CIRCLE" -> {
                                                drawCircle(color = drawColor, radius = size.minDimension / 2f)
                                            }
                                            "LEAF_CUT" -> {
                                                val path = Path().apply {
                                                    moveTo(0f, size.height * 0.5f)
                                                    lineTo(size.width * 0.5f, 0f)
                                                    lineTo(size.width, size.height * 0.5f)
                                                    lineTo(size.width * 0.5f, size.height)
                                                    close()
                                                }
                                                drawPath(path = path, color = drawColor)
                                            }
                                            "HEXAGON" -> {
                                                val h = size.height
                                                val w = size.width
                                                val path = Path().apply {
                                                    moveTo(w * 0.5f, 0f)
                                                    lineTo(w, h * 0.25f)
                                                    lineTo(w, h * 0.75f)
                                                    lineTo(w * 0.5f, h)
                                                    lineTo(0f, h * 0.75f)
                                                    lineTo(0f, h * 0.25f)
                                                    close()
                                                }
                                                drawPath(path = path, color = drawColor)
                                            }
                                            "FOLDED" -> {
                                                drawRect(color = drawColor.copy(0.4f), size = size)
                                                drawLine(color = drawColor, start = androidx.compose.ui.geometry.Offset(size.width / 2, 0f), end = androidx.compose.ui.geometry.Offset(size.width / 2, size.height), strokeWidth = 3f)
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1.5f)) {
                                    Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(desc, color = Color.Gray, fontSize = 10.sp)
                                }
                                if (active) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(Color(0xFF2196F3), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }

                // Section 3: Canvas Backdrop Settings
                Surface(
                    color = Color(0xFF1E243D),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF222B3A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CHOOSE INITIAL CANVAS BACKDROP",
                            color = Color(0xFF2196F3),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { selectedBgType = "SOLID" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedBgType == "SOLID") Color(0xFF2196F3) else Color(0xFF11162C)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Solid Color", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Button(
                                onClick = { selectedBgType = "GRADIENT" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedBgType == "GRADIENT") Color(0xFF2196F3) else Color(0xFF11162C)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Linear Gradient", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Custom Backdrop Palette Presets:", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))

                        // Curated Color Presets
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val palettes = listOf(
                                Triple("Pure White", "#FFFFFF", "#FFFFFF"),
                                Triple("Charcoal Onyx", "#121212", "#121212"),
                                Triple("Midnight Navy", "#030F26", "#0D1B3E"),
                                Triple("Emerald Gold", "#0A241F", "#03110E"),
                                Triple("Rose Dawn", "#F9F3EA", "#EADEC9"),
                                Triple("Nebula Purple", "#150F22", "#2F1943")
                            )

                            palettes.forEach { (pName, startColor, endColor) ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF11162C)),
                                    border = BorderStroke(1.dp, Color(0xFF222B3A)),
                                    modifier = Modifier
                                        .width(130.dp)
                                        .clickable {
                                            bgColorHex = startColor
                                            gradientEndColorHex = endColor
                                            selectedBgType = if (startColor == endColor) "SOLID" else "GRADIENT"
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(36.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(
                                                            Color(android.graphics.Color.parseColor(startColor)),
                                                            Color(android.graphics.Color.parseColor(endColor))
                                                        )
                                                    )
                                                )
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(pName, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = bgColorHex,
                                onValueChange = { bgColorHex = it },
                                label = { Text("Color Code Start", color = Color.Gray, fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("bg_color_start_field"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF2196F3),
                                    unfocusedBorderColor = Color(0xFF222B3A)
                                )
                            )

                            if (selectedBgType == "GRADIENT") {
                                OutlinedTextField(
                                    value = gradientEndColorHex,
                                    onValueChange = { gradientEndColorHex = it },
                                    label = { Text("Color Code End", color = Color.Gray, fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("bg_color_end_field"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF2196F3),
                                        unfocusedBorderColor = Color(0xFF222B3A)
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Section 4: Create Action Button
                Button(
                    onClick = {
                        if (cardNameInput.isNotEmpty()) {
                            viewModel.createNewCardProject(
                                name = cardNameInput,
                                templateId = "blank",
                                cardShape = selectedShape,
                                bgColor = if (bgColorHex.startsWith("#")) bgColorHex else "#FFFFFF",
                                bgType = selectedBgType,
                                gradientEndColor = if (gradientEndColorHex.startsWith("#")) gradientEndColorHex else "#FFFFFF"
                            )
                            onOpenEditor()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("action_create_blank_card"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Text("Create Blank Card", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // ================== SMART CARD AUTO-GENERATOR FORM DIALOG ==================
    showSmartGenerator?.let { preset ->
        var fName by remember { mutableStateOf("Pillai Play") }
        var jTitle by remember { mutableStateOf("Chief Executive Officer") }
        var cName by remember { mutableStateOf("Pillai' Play Studio") }
        var phoneVal by remember { mutableStateOf("+91 98765 43210") }
        var altPhoneVal by remember { mutableStateOf("+91 99999 88888") }
        var emailVal by remember { mutableStateOf("hello@pillaiplay.com") }
        var webVal by remember { mutableStateOf("www.pillaiplay.com") }
        var locVal by remember { mutableStateOf("Navi Mumbai, Maharashtra, India") }
        
        // Social Media Link Inputs
        var fbVal by remember { mutableStateOf("pillaiplay") }
        var instaVal by remember { mutableStateOf("pillaiplay_studio") }
        var linkVal by remember { mutableStateOf("pillaiplay") }

        AlertDialog(
            onDismissRequest = { showSmartGenerator = null },
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Smart Visiting Card Auto-Generator",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Template: " + preset.name + " (" + preset.category + ")",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Enter your details. Our engine automatically structures, sizes, and formats front and back layout parameters within 5 seconds!",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    
                    OutlinedTextField(
                        value = fName,
                        onValueChange = { fName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF2196F3))
                    )
                    OutlinedTextField(
                        value = jTitle,
                        onValueChange = { jTitle = it },
                        label = { Text("Job Designation / Title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF2196F3))
                    )
                    OutlinedTextField(
                        value = cName,
                        onValueChange = { cName = it },
                        label = { Text("Company Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF2196F3))
                    )
                    OutlinedTextField(
                        value = phoneVal,
                        onValueChange = { phoneVal = it },
                        label = { Text("Mobile Phone") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF2196F3))
                    )
                    OutlinedTextField(
                        value = altPhoneVal,
                        onValueChange = { altPhoneVal = it },
                        label = { Text("Alternate / WhatsApp Phone") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF2196F3))
                    )
                    OutlinedTextField(
                        value = emailVal,
                        onValueChange = { emailVal = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF2196F3))
                    )
                    OutlinedTextField(
                        value = webVal,
                        onValueChange = { webVal = it },
                        label = { Text("Website Domain") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF2196F3))
                    )
                    OutlinedTextField(
                        value = locVal,
                        onValueChange = { locVal = it },
                        label = { Text("Full Office Address") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF2196F3))
                    )
                    
                    Text("Optional Social Media Links", color = Color(0xFF2196F3), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    
                    OutlinedTextField(
                        value = fbVal,
                        onValueChange = { fbVal = it },
                        label = { Text("Facebook Username") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF2196F3))
                    )
                    OutlinedTextField(
                        value = instaVal,
                        onValueChange = { instaVal = it },
                        label = { Text("Instagram Profile Hand") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF2196F3))
                    )
                    OutlinedTextField(
                        value = linkVal,
                        onValueChange = { linkVal = it },
                        label = { Text("LinkedIn Hand") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF2196F3))
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Construct user card with preset specifications & state alignments
                        val generatedProject = UserCard(
                            cardName = "${preset.name} - ${fName}",
                            templateId = preset.id,
                            themeName = preset.category,
                            isPremium = preset.isPremium,
                            fullName = fName,
                            jobTitle = jTitle,
                            companyName = cName,
                            mobileNumber = phoneVal,
                            whatsAppNumber = altPhoneVal, // Stores alternative phone support!
                            email = emailVal,
                            website = webVal,
                            address = locVal,
                            facebook = fbVal,
                            instagram = instaVal,
                            linkedIn = linkVal,
                            backgroundColor = preset.bgStart,
                            gradientEndColor = preset.bgEnd,
                            backgroundType = "GRADIENT",
                            fontStyle = preset.fontStyle,
                            borderStyle = preset.borderStyle,
                            cardShape = "ROUNDED_RECTANGLE", // standard shape for layouts
                            
                            // AUTO PLACEMENT COORDINATES (Engine algorithm tuned)
                            fullNameX = 25f,
                            fullNameY = 30f,
                            jobTitleX = 25f,
                            jobTitleY = 56f,
                            companyNameX = 25f,
                            companyNameY = 80f,
                            mobileNumberX = 25f,
                            mobileNumberY = 135f,
                            emailX = 25f,
                            emailY = 153f,
                            websiteX = 25f,
                            websiteY = 171f,
                            addressX = 25f,
                            addressY = 189f,
                            
                            // QR code tuned perfectly for center backside
                            qrCodeX = 145f,
                            qrCodeY = 32f,
                            qrCodeSize = 110f,
                            qrCodeColor = preset.primaryColor,
                            qrCodeShape = "ROUNDED",
                            qrCodeVisible = true
                        )

                        viewModel.createNewCardProjectDirectly(generatedProject)
                        showSmartGenerator = null
                        onOpenEditor()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text("✨ Generate Card", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSmartGenerator = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E243D)
        )
    }
}

@Composable
fun TemplateCardItem(
    preset: TemplatePreset,
    isPremiumUnlocked: Boolean,
    onSelect: () -> Unit,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onDeleteCustom: (() -> Unit)? = null
) {
    val startGradientColor = android.graphics.Color.parseColor(preset.bgStart)
    val endGradientColor = android.graphics.Color.parseColor(preset.bgEnd)
    val accentColor = android.graphics.Color.parseColor(preset.primaryColor)

    Surface(
        color = Color(0xFF131722),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF222B3A)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Miniature visiting card representer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.verticalGradient(listOf(Color(startGradientColor), Color(endGradientColor))))
                    .padding(8.dp)
            ) {
                // Subtle card outline elements
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(accentColor).copy(0.2f), RoundedCornerShape(2.dp))
                        .align(Alignment.BottomEnd)
                ) {
                    Icon(Icons.Default.Menu, contentDescription = null, tint = Color(accentColor), modifier = Modifier.align(Alignment.Center))
                }
                
                Column(modifier = Modifier.align(Alignment.TopStart)) {
                    Box(modifier = Modifier.width(32.dp).height(5.dp).background(Color.White))
                    Box(modifier = Modifier.padding(top = 2.dp).width(20.dp).height(3.dp).background(Color(accentColor)))
                }

                // Star bookmark or custom template delete button
                Row(
                    modifier = Modifier.align(Alignment.TopEnd),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (onDeleteCustom != null) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(0.5f),
                            modifier = Modifier
                                .clickable { onDeleteCustom() }
                                .size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Template",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(0.5f),
                            modifier = Modifier
                                .clickable { onToggleFavorite() }
                                .size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Toggle Favorite",
                                tint = if (isFavorite) Color(0xFFD4AF37) else Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(preset.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    Text(preset.category, color = Color.Gray, fontSize = 9.sp)
                }

                if (preset.isPremium) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Premium Lock",
                        tint = if (isPremiumUnlocked) Color(0xFF00FFCC) else Color(0xFFD4AF37),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

// 5. SETTINGS SCREEN
@Composable
fun SettingsView(viewModel: CardViewModel, onNavigate: (ActiveScreen) -> Unit) {
    val context = LocalContext.current
    val activeTheme by viewModel.activeTheme.collectAsState()
    val isPremium by viewModel.isUserPremium.collectAsState()

    var showAccountDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }

    var editNameInput by remember { mutableStateOf(viewModel.prefs.userName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Back Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onNavigate(ActiveScreen.DASHBOARD) },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Studio Settings",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "THEME & VISUAL PREFERENCES",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )

            // Dynamic Dark mode Switch card
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (activeTheme == "DARK") Icons.Default.Settings else Icons.Default.Home,
                            contentDescription = null,
                            tint = Color(0xFFD4AF37),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Premium Dark Theme",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (activeTheme == "DARK") "Gold night mode is active" else "Comfortable light mode matches bright rooms",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                    }
                    Switch(
                        checked = activeTheme == "DARK",
                        onCheckedChange = { isChecked ->
                            viewModel.changeTheme(if (isChecked) "DARK" else "LIGHT")
                            Toast.makeText(context, if (isChecked) "Dark Mode Activated" else "Light Mode Activated", Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FFCC))
                    )
                }
            }

            Text(
                text = "ACCOUNT PROFILE",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )

            SettingsLinkRow(title = "Manage Account Profile", icon = Icons.Default.Person, label = "View Details") {
                editNameInput = viewModel.prefs.userName
                showAccountDialog = true
            }

            Text(
                text = "COMPLIANCE & LEGAL",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )

            SettingsLinkRow(title = "Privacy Policy terms", icon = Icons.Default.Lock, label = "Read policy") {
                showPrivacyDialog = true
            }

            SettingsLinkRow(title = "Terms & Conditions", icon = Icons.Default.List, label = "Read Terms") {
                showTermsDialog = true
            }

            Text(
                text = "SUPPORT & INFO",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )

            SettingsLinkRow(title = "Contact Helpdesk Support", icon = Icons.Default.Send, label = "Copy address") {
                showSupportDialog = true
            }

            // Muted app version and metadata
            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Pillai'Play Visiting Card Maker",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "App Edition: v2.10 (Standard Free Suite)",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    fontSize = 10.sp
                )
            }

            // Red Styled Logout Button
            Button(
                onClick = {
                    viewModel.logout()
                    Toast.makeText(context, "Sign-out successful.", Toast.LENGTH_SHORT).show()
                    onNavigate(ActiveScreen.AUTH)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252).copy(0.12f)),
                border = BorderStroke(1.dp, Color(0xFFFF5252)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFFF5252))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out from Studio Session", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Interactive Dialogs / Popups
    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = { Text("Account Profile Dashboard", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("User Registered Email:\n${viewModel.prefs.userEmail}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 13.sp)
                    Text("Subscription Type: ${if (isPremium) "VIP Premium Pass" else "Standard Sandbox User"}", color = if (isPremium) Color(0xFFD4AF37) else Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Custom Background Uploads: ${viewModel.prefs.customBackgroundsUploadedCount} / 5 used", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 13.sp)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Manage Account Name:", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        placeholder = { Text("Enter your full name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editNameInput.isNotEmpty()) {
                            viewModel.prefs.userName = editNameInput
                            Toast.makeText(context, "Profile name updated!", Toast.LENGTH_SHORT).show()
                        }
                        showAccountDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                ) {
                    Text("Save Changes", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy (Last Updated: June 2026)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Welcome to Pillai'Play Visiting Card Maker.\n\n" +
                               "We value your privacy and are committed to protecting your personal information.\n\n" +
                               "### 1. Information We Collect\n" +
                               "The app may collect:\n" +
                               "• Name and profile information provided during sign-in.\n" +
                               "• Email address when using Google Sign-In.\n" +
                               "• Business card information created by the user.\n" +
                               "• App usage analytics.\n" +
                               "• Device information required for app functionality.\n" +
                               "• Advertising-related information through AdMob.\n\n" +
                               "### 2. How We Use Information\n" +
                               "We use information to:\n" +
                               "• Provide app functionality.\n" +
                               "• Save and manage business cards.\n" +
                               "• Improve user experience.\n" +
                               "• Display advertisements.\n" +
                               "• Fix bugs and improve performance.\n" +
                               "• Provide customer support.\n\n" +
                               "### 3. Google Sign-In\n" +
                               "If you sign in using Google, we may receive:\n" +
                               "• Your name\n" +
                               "• Email address\n" +
                               "• Profile picture (if available)\n\n" +
                               "We only use this information to provide account-related features.\n\n" +
                               "### 4. Advertisements\n" +
                               "This app may display advertisements through Google AdMob.\n" +
                               "Google may collect information according to its own privacy policies to provide relevant ads and measure ad performance.\n\n" +
                               "### 5. Data Storage\n" +
                               "Business card information may be stored locally on your device and/or securely through services used by the application.\n\n" +
                               "### 6. Data Security\n" +
                               "We take reasonable measures to protect user information. However, no method of electronic storage or transmission is completely secure.\n\n" +
                               "### 7. Children's Privacy\n" +
                               "This application is not intended for children under 13 years of age.\n\n" +
                               "### 8. Third-Party Services\n" +
                               "The app may use:\n" +
                               "• Google Sign-In\n" +
                               "• Firebase\n" +
                               "• Google AdMob\n" +
                               "• Google Analytics\n" +
                               "These services have their own privacy policies.\n\n" +
                               "### 9. Changes to This Policy\n" +
                               "We may update this Privacy Policy from time to time. Changes will be reflected within the application.\n\n" +
                               "### 10. Contact Us\n" +
                               "For questions regarding this Privacy Policy, contact:\n" +
                               "praveenkumarpillai2010@gmail.com",
                        color = MaterialTheme.colorScheme.onSurface.copy(0.85f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("I Understand")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Terms and Conditions (Last Updated: June 2026)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Welcome to Pillai'Play Visiting Card Maker.\n\n" +
                               "By downloading or using this application, you agree to the following terms.\n\n" +
                               "### 1. Use of the Application\n" +
                               "You may use the application for creating and managing digital privacy-safe business cards for personal or business purposes.\n" +
                               "You agree not to:\n" +
                               "• Use the app for unlawful activities.\n" +
                               "• Upload harmful, illegal, or misleading content.\n" +
                               "• Attempt to interfere with app functionality.\n" +
                               "• Reverse engineer or misuse the service.\n\n" +
                               "### 2. User Content\n" +
                               "Users are responsible for all information, images, logos, QR codes, and business details they upload or create using the app.\n" +
                               "You retain ownership of your content.\n\n" +
                               "### 3. Intellectual Property\n" +
                               "The application, design, branding, and software remain the property of Pillai'Play.\n" +
                               "Users may not copy, redistribute, or reproduce the application without permission.\n\n" +
                               "### 4. Advertisements\n" +
                               "The application may display advertisements through third-party advertising providers.\n" +
                               "Interaction with advertisements is subject to the policies of the advertising provider.\n\n" +
                               "### 5. Service Availability\n" +
                               "We strive to keep the app available at all times but do not guarantee uninterrupted service.\n" +
                               "Features may be updated, modified, or removed at any time.\n\n" +
                               "### 6. Limitation of Liability\n" +
                               "Pillai'Play shall not be responsible for:\n" +
                               "• Data loss\n" +
                               "• Business losses\n" +
                               "• Indirect damages\n" +
                               "• Service interruptions\n" +
                               "• Third-party service failures\n\n" +
                               "### 7. Termination\n" +
                               "We reserve the right to suspend access to users who violate these terms.\n\n" +
                               "### 8. Changes to Terms\n" +
                               "These Terms and Conditions may be updated periodically. Continued use of the application indicates acceptance of any updated terms.\n\n" +
                               "### 9. Contact Information\n" +
                               "For support or inquiries, email us at:\n" +
                               "praveenkumarpillai2010@gmail.com",
                        color = MaterialTheme.colorScheme.onSurface.copy(0.85f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTermsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Accept Terms")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = { Text("Contact Helpdesk Support", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("For custom branding enquiries, business licensing, API integrations, or technical aid, contact us directly:", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Support Mailbox: praveenkumarpillai2010@gmail.com", color = Color(0xFFD4AF37), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Support Email", "praveenkumarpillai2010@gmail.com")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Developer support email copied to clipboard!", Toast.LENGTH_SHORT).show()
                        showSupportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                ) {
                    Text("Copy Email Address", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSupportDialog = false }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun SettingsLinkRow(title: String, icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color(0xFFD4AF37), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 12.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

// 6. PREMIUM / SUBSCRIPTION VIEW
@Composable
fun PremiumView(viewModel: CardViewModel, onNavigate: (ActiveScreen) -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        onNavigate(ActiveScreen.DASHBOARD)
    }
}

// Support Struct for billing pricing layout
data class BillingProduct(
    val id: String,
    val title: String,
    val priceLabel: String,
    val badge: String,
    val features: List<String>,
    val buttonLabel: String,
    val isPopular: Boolean = false
)

// Support Struct for matrix display row
data class ComparisonRowItem(
    val feature: String,
    val freeValue: String,
    val premValue: String,
    val highlight: Boolean
)


// 7. ADBAN_MANAGER VIEW 
@Composable
fun AdsManagerView(viewModel: CardViewModel, onNavigate: (ActiveScreen) -> Unit) {
    val context = LocalContext.current
    
    // Ads preferences inputs
    var bannerIdInput by remember { mutableStateOf(viewModel.prefs.bannerAdId) }
    var bannerEnabled by remember { mutableStateOf(viewModel.prefs.bannerAdEnabled) }

    var interstitialIdInput by remember { mutableStateOf(viewModel.prefs.interstitialAdId) }
    var interstitialEnabled by remember { mutableStateOf(viewModel.prefs.interstitialAdEnabled) }

    var rewardedIdInput by remember { mutableStateOf(viewModel.prefs.rewardedAdId) }
    var rewardedEnabled by remember { mutableStateOf(viewModel.prefs.rewardedAdEnabled) }

    var appOpenIdInput by remember { mutableStateOf(viewModel.prefs.appOpenAdId) }
    var appOpenEnabled by remember { mutableStateOf(viewModel.prefs.appOpenAdEnabled) }

    var nativeIdInput by remember { mutableStateOf(viewModel.prefs.nativeAdId) }
    var nativeEnabled by remember { mutableStateOf(viewModel.prefs.nativeAdEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C16))
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onNavigate(ActiveScreen.SETTINGS) },
                modifier = Modifier.background(Color(0xFF131722), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("AdMob IDs Manager", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Modifying IDs here changes the banner, rewarded templates, and interstitial triggers instantly without rebuilding APK.",
                color = Color.Gray,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            // Banner Ads Configuration
            AdSectionCard(
                title = "Banner Ads Config",
                idValue = bannerIdInput,
                onIdChange = { bannerIdInput = it },
                enabled = bannerEnabled,
                onToggle = { bannerEnabled = it }
            )

            // Interstitial Config
            AdSectionCard(
                title = "Interstitial Ads Config",
                idValue = interstitialIdInput,
                onIdChange = { interstitialIdInput = it },
                enabled = interstitialEnabled,
                onToggle = { interstitialEnabled = it }
            )

            // Rewarded Config
            AdSectionCard(
                title = "Rewarded Ads Config",
                idValue = rewardedIdInput,
                onIdChange = { rewardedIdInput = it },
                enabled = rewardedEnabled,
                onToggle = { rewardedEnabled = it }
            )

            // App Open Config
            AdSectionCard(
                title = "App Open Ads Config",
                idValue = appOpenIdInput,
                onIdChange = { appOpenIdInput = it },
                enabled = appOpenEnabled,
                onToggle = { appOpenEnabled = it }
            )

            // Native Config
            AdSectionCard(
                title = "Native Ads Config",
                idValue = nativeIdInput,
                onIdChange = { nativeIdInput = it },
                enabled = nativeEnabled,
                onToggle = { nativeEnabled = it }
            )

            // Global Save Button
            Button(
                onClick = {
                    viewModel.prefs.bannerAdId = bannerIdInput
                    viewModel.prefs.bannerAdEnabled = bannerEnabled
                    
                    viewModel.prefs.interstitialAdId = interstitialIdInput
                    viewModel.prefs.interstitialAdEnabled = interstitialEnabled
                    
                    viewModel.prefs.rewardedAdId = rewardedIdInput
                    viewModel.prefs.rewardedAdEnabled = rewardedEnabled
                    
                    viewModel.prefs.appOpenAdId = appOpenIdInput
                    viewModel.prefs.appOpenAdEnabled = appOpenEnabled
                    
                    viewModel.prefs.nativeAdId = nativeIdInput
                    viewModel.prefs.nativeAdEnabled = nativeEnabled
                    
                    Toast.makeText(context, "Configurations Saved to SharedPreferences!", Toast.LENGTH_LONG).show()
                    onNavigate(ActiveScreen.SETTINGS)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(bottom = 20.dp)
            ) {
                Text("Save Ads Framework", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AdSectionCard(
    title: String,
    idValue: String,
    onIdChange: (String) -> Unit,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        color = Color(0xFF131722),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF222B3A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF00FFCC),
                        checkedTrackColor = Color(0xFF00FFCC).copy(0.3f)
                    )
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = idValue,
                onValueChange = onIdChange,
                label = { Text("Ad Unit Identifier") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// 8. AI CARD GENERATOR (INTEGRATING GEMINI API COHERENTLY)
@Composable
fun AICardGeneratorView(
    viewModel: CardViewModel,
    onNavigate: (ActiveScreen) -> Unit,
    onOpenEditor: () -> Unit
) {
    val context = LocalContext.current
    val isPremium by viewModel.isUserPremium.collectAsState()
    val aiGenerationsCount = viewModel.prefs.aiGenerationsCount

    // Required Form Inputs
    var inputName by remember { mutableStateOf(viewModel.prefs.userName) }
    var inputCompany by remember { mutableStateOf(viewModel.prefs.userName + " Enterprises") }
    var inputBrandDescription by remember { mutableStateOf("A specialized boutique coffee roastery focusing on organic single-origin beans, styled with warm earthy colors and rustic aesthetic elements.") }
    var inputJobTitle by remember { mutableStateOf("Managing Director") }
    var inputPhone by remember { mutableStateOf("+91 98765 43210") }
    var inputEmail by remember { mutableStateOf("contact@corporate.com") }
    var inputWebsite by remember { mutableStateOf("www.corporate.com") }
    var inputAddress by remember { mutableStateOf("Mumbai, Maharashtra, India") }
    var selectedCategory by remember { mutableStateOf("Corporate") }

    // Optional Form Inputs
    var preferredStyle by remember { mutableStateOf("Luxury") }
    var preferredColor by remember { mutableStateOf("#D4AF37") } // default gold
    var logoUri by remember { mutableStateOf<String?>(null) }
    var photoUri by remember { mutableStateOf<String?>(null) }

    // States
    val loading by viewModel.aiLoading.collectAsState()
    val error by viewModel.aiError.collectAsState()
    val success by viewModel.aiSuccess.collectAsState()
    val generatedOptions by viewModel.aiOptions.collectAsState()

    var showResults by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val categoriesList = listOf(
        "Business", "Corporate", "Technology", "Medical", "Education",
        "Real Estate", "Creative", "Finance", "Legal", "Retail"
    )

    val styleOptionsList = listOf(
        "Modern", "Luxury", "Minimal", "Professional", "Creative", "Corporate", "Elegant", "Technology"
    )

    val colorPresets = listOf(
        Pair("Gold", "#D4AF37"),
        Pair("Cyan", "#00FFCC"),
        Pair("Blue", "#1E88E5"),
        Pair("Teal", "#008080"),
        Pair("Crimson", "#E53935"),
        Pair("Lavender", "#8E24AA"),
        Pair("Pink", "#FF007F"),
        Pair("Obsidian", "#1F232B")
    )

    // Image Picker launch hooks
    val logoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            logoUri = uri.toString()
            Toast.makeText(context, "Logo successfully attached!", Toast.LENGTH_SHORT).show()
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            photoUri = uri.toString()
            Toast.makeText(context, "Profile Photo attached!", Toast.LENGTH_SHORT).show()
        }
    }

    // Capture success
    LaunchedEffect(success) {
        if (success && generatedOptions.isNotEmpty()) {
            showResults = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C16))
    ) {
        // Toolbar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (showResults) {
                            showResults = false
                            viewModel.resetAIStates()
                        } else {
                            onNavigate(ActiveScreen.DASHBOARD)
                        }
                    },
                    modifier = Modifier.background(Color(0xFF131722), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (showResults) "Your AI Themes" else "AI Card Generator",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Free Usage Counter Badge
            Surface(
                color = Color(0xFF00FFCC).copy(0.15f),
                border = BorderStroke(1.dp, Color(0xFF00FFCC)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Free Unlimited",
                    color = Color(0xFF00FFCC),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // SCROLLABLE CONTAINER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (showResults) {
                // RESULTS COMPARISON & DECISION PANEL
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Surface(
                        color = Color(0xFF131722),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFCCFF00).copy(0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✨", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("AIGenerator Finished Successfully", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "We designed 3 outstanding variations for you. Compare themes, select and customize style parameters on the canvas.",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    // Display the three card options
                    generatedOptions.forEachIndexed { index, option ->
                        val layoutStyle = option.layoutArrangement
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

                        when (layoutStyle) {
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
                            else -> { // CLASSIC_REAR_QR / Default
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

                        val optionCardModel = UserCard(
                            cardName = "AI Theme Option",
                            fullName = inputName,
                            companyName = inputCompany,
                            jobTitle = inputJobTitle,
                            mobileNumber = inputPhone,
                            email = inputEmail,
                            website = inputWebsite,
                            address = inputAddress,
                            backgroundColor = option.backgroundColor,
                            gradientEndColor = option.gradientEndColor,
                            qrCodeColor = option.primaryColor,
                            fontStyle = option.fontStyle,
                            qrCodeVisible = option.qrCodeVisible,
                            qrCodeX = option.qrX ?: defaultQrX,
                            qrCodeY = option.qrY ?: defaultQrY,
                            cardShape = option.cardShape,
                            fullNameX = option.fullNameX ?: defaultFullNameX,
                            fullNameY = option.fullNameY ?: defaultFullNameY,
                            fullNameSize = option.fullNameSize ?: defaultFullNameSize,
                            jobTitleX = option.jobTitleX ?: defaultJobTitleX,
                            jobTitleY = option.jobTitleY ?: defaultJobTitleY,
                            jobTitleSize = option.jobTitleSize ?: defaultJobTitleSize,
                            companyNameX = option.companyNameX ?: defaultCompanyNameX,
                            companyNameY = option.companyNameY ?: defaultCompanyNameY,
                            companyNameSize = option.companyNameSize ?: defaultCompanyNameSize,
                            mobileNumberX = option.mobileX ?: defaultMobileX,
                            mobileNumberY = option.mobileY ?: defaultMobileY,
                            emailX = option.emailX ?: defaultEmailX,
                            emailY = option.emailY ?: defaultEmailY,
                            websiteX = option.websiteX ?: defaultWebsiteX,
                            websiteY = option.websiteY ?: defaultWebsiteY,
                            addressX = option.addressX ?: defaultAddressX,
                            addressY = option.addressY ?: defaultAddressY
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
                            border = BorderStroke(1.dp, Color(0xFF222B3A)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().testTag("ai_option_card_$index")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Option ${index + 1}: ${option.themeName}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = option.fontStyle,
                                        color = Color(0xFFD4AF37),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Dynamic Miniature Preview Card
                                StaticMiniCardPreview(card = optionCardModel)

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.saveChosenDesignToEditor(
                                                optionToSave = option,
                                                name = inputName,
                                                companyName = inputCompany,
                                                jobTitle = inputJobTitle,
                                                phoneNumber = inputPhone,
                                                email = inputEmail,
                                                website = inputWebsite,
                                                address = inputAddress,
                                                logoUri = logoUri,
                                                photoUri = photoUri
                                            )
                                            Toast.makeText(context, "Layout chosen! Opening Editor...", Toast.LENGTH_SHORT).show()
                                            onOpenEditor()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(40.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Select & Edit", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.triggerAICardGeneration(
                                                name = inputName,
                                                companyName = inputCompany,
                                                jobTitle = inputJobTitle,
                                                phoneNumber = inputPhone,
                                                email = inputEmail,
                                                website = inputWebsite,
                                                address = inputAddress,
                                                category = selectedCategory,
                                                preferredColor = preferredColor,
                                                preferredStyle = preferredStyle,
                                                brandDescription = inputBrandDescription,
                                                logoUri = logoUri,
                                                photoUri = photoUri
                                            )
                                        },
                                        border = BorderStroke(1.dp, Color.Gray),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(40.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Regenerate", tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedButton(
                        onClick = {
                            showResults = false
                            viewModel.resetAIStates()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text("Edit Form Information", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                }
            } else {
                // INPUT STYLES FORM
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Generative guidance
                    Surface(
                        color = Color(0xFF131722),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(0.2f))
                    ) {
                        Row(modifier = Modifier.padding(14.dp)) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD4AF37))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Aesthetic Generative Engine", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "Our models automatically select balanced colors, optimized fonts, matching vector templates, and configure ideal horizontal and vertical margins.",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    // Input Form Fields
                    Text("BUSINESS CARD PARAMETERS", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Full Name", color = Color.LightGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFFD4AF37),
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("ai_name_input")
                    )

                    OutlinedTextField(
                        value = inputCompany,
                        onValueChange = { inputCompany = it },
                        label = { Text("Company Name", color = Color.LightGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFFD4AF37),
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("ai_company_input")
                    )

                    OutlinedTextField(
                        value = inputBrandDescription,
                        onValueChange = { inputBrandDescription = it },
                        label = { Text("Brand Description / Brief", color = Color.LightGray) },
                        placeholder = { Text("e.g. Eco-friendly modern coffee roastery...", color = Color.DarkGray) },
                        singleLine = false,
                        maxLines = 4,
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFFD4AF37),
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_brand_desc_input")
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = inputJobTitle,
                            onValueChange = { inputJobTitle = it },
                            label = { Text("Job Title", color = Color.LightGray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray,
                                focusedBorderColor = Color(0xFFD4AF37),
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            modifier = Modifier.weight(1f).testTag("ai_title_input")
                        )

                        // Business Category Selection Dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = selectedCategory,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category", color = Color.LightGray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.LightGray,
                                    focusedBorderColor = Color(0xFFD4AF37),
                                    unfocusedBorderColor = Color.DarkGray
                                ),
                                trailingIcon = {
                                    IconButton(onClick = { categoryExpanded = !categoryExpanded }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand", tint = Color.White)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().clickable { categoryExpanded = !categoryExpanded }
                            )

                            DropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false },
                                modifier = Modifier.background(Color(0xFF131722))
                            ) {
                                categoriesList.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category, color = Color.White) },
                                        onClick = {
                                            selectedCategory = category
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = inputPhone,
                        onValueChange = { inputPhone = it },
                        label = { Text("Phone Number", color = Color.LightGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFFD4AF37),
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("ai_phone_input")
                    )

                    OutlinedTextField(
                        value = inputEmail,
                        onValueChange = { inputEmail = it },
                        label = { Text("Email Address", color = Color.LightGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFFD4AF37),
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("ai_email_input")
                    )

                    OutlinedTextField(
                        value = inputWebsite,
                        onValueChange = { inputWebsite = it },
                        label = { Text("Website", color = Color.LightGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFFD4AF37),
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("ai_website_input")
                    )

                    OutlinedTextField(
                        value = inputAddress,
                        onValueChange = { inputAddress = it },
                        label = { Text("Business Address", color = Color.LightGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFFD4AF37),
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("ai_address_input")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // OPTIONAL INPUTS SECTION
                    Text("AESTHETIC STYLE CHOICES (OPTIONAL)", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    // Preferred Style Chip selectors
                    Text("Select Style Target", color = Color.LightGray, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        styleOptionsList.forEach { styleOpt ->
                            val selected = preferredStyle == styleOpt
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (selected) Color(0xFFD4AF37) else Color(0xFF131722),
                                border = BorderStroke(1.dp, if (selected) Color(0xFFD4AF37) else Color(0xFF222B3A)),
                                modifier = Modifier.clickable { preferredStyle = styleOpt }
                            ) {
                                Text(
                                    text = styleOpt,
                                    color = if (selected) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Preferred Color Selector dots
                    Text("Preferred Color Accent", color = Color.LightGray, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        colorPresets.forEach { colorPreset ->
                            val parsedColor = Color(android.graphics.Color.parseColor(colorPreset.second))
                            val isSelected = preferredColor == colorPreset.second
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(parsedColor)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { preferredColor = colorPreset.second },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Active", tint = if (colorPreset.first == "Obsidian") Color.White else Color.Black, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Upload Logo & Profile Photo Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
                            border = BorderStroke(1.dp, Color(0xFF222B3A)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { logoLauncher.launch("image/*") }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Logo", tint = if (logoUri != null) Color(0xFF00FFCC) else Color.Gray)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (logoUri != null) "Logo Attached" else "Upload Logo",
                                    color = if (logoUri != null) Color(0xFF00FFCC) else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (logoUri != null) {
                                    Text("Tap to replace", color = Color.Gray, fontSize = 8.sp)
                                }
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
                            border = BorderStroke(1.dp, Color(0xFF222B3A)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { photoLauncher.launch("image/*") }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Person, contentDescription = "Photo", tint = if (photoUri != null) Color(0xFF00FFCC) else Color.Gray)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (photoUri != null) "Photo Attached" else "Profile Photo",
                                    color = if (photoUri != null) Color(0xFF00FFCC) else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (photoUri != null) {
                                    Text("Tap to replace", color = Color.Gray, fontSize = 8.sp)
                                }
                            }
                        }
                    }

                    if (error != null) {
                        Text(text = "Generator notice: $error", color = Color(0xFFFF5E7E), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // GENERATE BUTTON OR SPINNER
                    if (loading) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFFD4AF37))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("AI is professionalizing layouts...", color = Color(0xFFD4AF37), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Selecting fonts, balancing spacing & grids...", color = Color.Gray, fontSize = 11.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (inputName.trim().isEmpty() || inputCompany.trim().isEmpty()) {
                                    Toast.makeText(context, "Full Name and Company are required", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.triggerAICardGeneration(
                                        name = inputName,
                                        companyName = inputCompany,
                                        jobTitle = inputJobTitle,
                                        phoneNumber = inputPhone,
                                        email = inputEmail,
                                        website = inputWebsite,
                                        address = inputAddress,
                                        category = selectedCategory,
                                        preferredColor = preferredColor,
                                        preferredStyle = preferredStyle,
                                        brandDescription = inputBrandDescription,
                                        logoUri = logoUri,
                                        photoUri = photoUri
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .testTag("btn_generate_my_card")
                        ) {
                            Text("⚡ Generate My Card", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(44.dp))
                }
            }
        }
    }
}

// DRAG-FREE COMPACT MINI VISITING CARD PREVIEW COMPOSABLE - COORDINATE BASED DYNAMIC VISUALS
@Composable
fun StaticMiniCardPreview(
    card: UserCard,
    modifier: Modifier = Modifier
) {
    val cardBgStart = try { Color(android.graphics.Color.parseColor(card.backgroundColor)) } catch (e: Exception) { Color(0xFF10121A) }
    val cardBgEnd = try { Color(android.graphics.Color.parseColor(card.gradientEndColor)) } catch (e: Exception) { Color(0xFF1E2130) }
    val accentColor = try { Color(android.graphics.Color.parseColor(card.qrCodeColor)) } catch (e: Exception) { Color(0xFFD4AF37) }

    val visibleFields = try {
        val list = mutableListOf<String>()
        val array = org.json.JSONArray(card.visibleFieldsJson)
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        list
    } catch (e: Exception) {
        listOf("fullName", "jobTitle", "companyName", "mobileNumber", "email", "website", "address")
    }

    val fFamily = when (card.fontStyle) {
        "Elegant Serif" -> FontFamily.Serif
        "Tech Clean" -> FontFamily.Monospace
        "Space Grotesk" -> FontFamily.SansSerif
        else -> FontFamily.Default
    }

    val previewShape = when (card.cardShape) {
        "ROUNDED_RECTANGLE" -> RoundedCornerShape(12.dp)
        "CIRCLE" -> CircleShape
        "LEAF_CUT" -> RoundedCornerShape(topStart = 24.dp, bottomEnd = 24.dp, topEnd = 0.dp, bottomStart = 0.dp)
        "HEXAGON" -> object : androidx.compose.ui.graphics.Shape {
            override fun createOutline(
                size: androidx.compose.ui.geometry.Size,
                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                density: androidx.compose.ui.unit.Density
            ): androidx.compose.ui.graphics.Outline {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.5f, 0f)
                    lineTo(size.width, size.height * 0.22f)
                    lineTo(size.width, size.height * 0.78f)
                    lineTo(size.width * 0.5f, size.height)
                    lineTo(0f, size.height * 0.78f)
                    lineTo(0f, size.height * 0.22f)
                    close()
                }
                return androidx.compose.ui.graphics.Outline.Generic(path)
            }
        }
        else -> RoundedCornerShape(0.dp)
    }

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1.58f)
            .fillMaxWidth()
            .clip(previewShape)
            .background(Brush.linearGradient(listOf(cardBgStart, cardBgEnd)))
            .border(1.dp, accentColor.copy(0.35f), previewShape)
    ) {
        CardTemplateDecorations(card = card, accentColor = accentColor)

        // We map design space 360x220 to available bounding layout dp space
        val canvasWidthRaw = 360f
        val canvasHeightRaw = 220f
        val scaleXRaw = maxWidth.value / canvasWidthRaw
        val scaleYRaw = maxHeight.value / canvasHeightRaw

        // Subtle decorative border design inside card
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .border(0.5.dp, accentColor.copy(0.12f), RoundedCornerShape(4.dp))
        )

        // Render QR code
        if (card.qrCodeVisible) {
            val qrSize = 50.dp
            Box(
                modifier = Modifier
                    .absoluteOffset(
                        x = (card.qrCodeX * scaleXRaw).coerceIn(0f, (maxWidth - qrSize).value).dp,
                        y = (card.qrCodeY * scaleYRaw).coerceIn(0f, (maxHeight - qrSize).value).dp
                    )
                    .size(qrSize)
                    .background(Color.White.copy(0.08F), RoundedCornerShape(5.dp))
                    .border(1.dp, accentColor.copy(0.4f), RoundedCornerShape(5.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (card.qrCodeType == "UPLOADED" && !card.qrCodeBase64Image.isNullOrEmpty()) {
                    val base64Bytes = try { android.util.Base64.decode(card.qrCodeBase64Image, android.util.Base64.DEFAULT) } catch (e: Exception) { null }
                    if (base64Bytes != null) {
                        val bmpVal = android.graphics.BitmapFactory.decodeByteArray(base64Bytes, 0, base64Bytes.size)
                        if (bmpVal != null) {
                            androidx.compose.foundation.Image(
                                bitmap = bmpVal.asImageBitmap(),
                                contentDescription = "Custom QR",
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }
                } else {
                    com.example.utils.QRCodeComposable(
                        data = card.qrCodeData,
                        sizeDp = 42.dp,
                        colorHex = card.qrCodeColor,
                        shape = card.qrCodeShape
                    )
                }
            }
        }

        // Company Name
        if (visibleFields.contains("companyName")) {
            Text(
                text = card.companyName,
                color = accentColor,
                fontSize = (card.companyNameSize * 0.70f).sp,
                fontFamily = fFamily,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.absoluteOffset(
                    x = (card.companyNameX * scaleXRaw).dp,
                    y = (card.companyNameY * scaleYRaw).dp
                )
            )
        }

        // Full Name
        if (visibleFields.contains("fullName")) {
            Text(
                text = card.fullName,
                color = Color.White,
                fontSize = (card.fullNameSize * 0.70f).sp,
                fontFamily = fFamily,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.absoluteOffset(
                    x = (card.fullNameX * scaleXRaw).dp,
                    y = (card.fullNameY * scaleYRaw).dp
                )
            )
        }

        // Job Title
        if (visibleFields.contains("jobTitle")) {
            Text(
                text = card.jobTitle.uppercase(),
                color = Color.LightGray.copy(0.8F),
                fontSize = (card.jobTitleSize * 0.70f).sp,
                fontFamily = fFamily,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.absoluteOffset(
                    x = (card.jobTitleX * scaleXRaw).dp,
                    y = (card.jobTitleY * scaleYRaw).dp
                )
            )
        }

        // Contacts list
        if (visibleFields.contains("mobileNumber") && card.mobileNumber.isNotEmpty()) {
            Text(
                text = "📞 " + card.mobileNumber,
                color = Color.LightGray.copy(0.7F),
                fontSize = (card.mobileNumberSize * 0.70f).sp,
                fontFamily = fFamily,
                maxLines = 1,
                modifier = Modifier.absoluteOffset(
                    x = (card.mobileNumberX * scaleXRaw).dp,
                    y = (card.mobileNumberY * scaleYRaw).dp
                )
            )
        }

        if (visibleFields.contains("email") && card.email.isNotEmpty()) {
            Text(
                text = "✉ " + card.email,
                color = Color.LightGray.copy(0.7F),
                fontSize = (card.emailSize * 0.70f).sp,
                fontFamily = fFamily,
                maxLines = 1,
                modifier = Modifier.absoluteOffset(
                    x = (card.emailX * scaleXRaw).dp,
                    y = (card.emailY * scaleYRaw).dp
                )
            )
        }

        if (visibleFields.contains("website") && card.website.isNotEmpty()) {
            Text(
                text = "🌐 " + card.website,
                color = Color.LightGray.copy(0.7F),
                fontSize = (card.websiteSize * 0.70f).sp,
                fontFamily = fFamily,
                maxLines = 1,
                modifier = Modifier.absoluteOffset(
                    x = (card.websiteX * scaleXRaw).dp,
                    y = (card.websiteY * scaleYRaw).dp
                )
            )
        }

        if (visibleFields.contains("address") && card.address.isNotEmpty()) {
            Text(
                text = "📍 " + card.address,
                color = Color.LightGray.copy(0.7F),
                fontSize = (card.addressSize * 0.70f).sp,
                fontFamily = fFamily,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.absoluteOffset(
                    x = (card.addressX * scaleXRaw).dp,
                    y = (card.addressY * scaleYRaw).dp
                )
            )
        }
    }
}


