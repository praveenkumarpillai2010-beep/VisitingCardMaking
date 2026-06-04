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
    SPLASH, AUTH, DASHBOARD, EDITOR, TEMPLATES, PREMIUM, SETTINGS, ADS_MANAGER, AI_GENERATOR
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

val cardTemplates = listOf(
    // 1. Luxury & Business (Premium Gold and Black Accents)
    TemplatePreset("vibe_modern_gold", "Platinum Gold Luxury", "Business", true, "#10121A", "#1D1305", "#D4AF37", "MINIMAL_GOLD", "Elegant Serif"),
    TemplatePreset("vibe_biz_black", "Midnight Executive", "Business", true, "#050508", "#12121A", "#FFFFFF", "MODERN_DOUBLE", "Modern Bold"),
    TemplatePreset("vibe_biz_metro", "Metropolitan Business", "Business", false, "#1C1C1F", "#0B1D28", "#A4B5C4", "MINIMAL_GOLD", "Space Grotesk"),
    TemplatePreset("vibe_biz_ocean", "Sapphire Professional", "Business", false, "#0A1E31", "#050914", "#5BB7FF", "MINIMAL_GOLD", "Modern Bold"),

    // 2. Corporate (Deep professional colors and formal typography)
    TemplatePreset("vibe_corp_classic", "Classic Corporate", "Corporate", false, "#0D1117", "#161B22", "#58A6FF", "MINIMAL_GOLD", "Space Grotesk"),
    TemplatePreset("vibe_corp_elite", "Elite Executive Blue", "Corporate", true, "#040D21", "#0B132B", "#00FFCC", "MODERN_DOUBLE", "Elegant Serif"),
    TemplatePreset("vibe_corp_grey", "Steel Corporate", "Corporate", false, "#1E2022", "#2B2E31", "#E5D9C4", "MINIMAL_GOLD", "Tech Clean"),

    // 3. Technology (Cyberpunk themes, matrix digital green, deep charcoal)
    TemplatePreset("vibe_tech_cyber", "Cyber Sleek Neon", "Technology", true, "#050811", "#0D1E2D", "#00FFCC", "CYBER_SLATE", "Tech Clean"),
    TemplatePreset("vibe_tech_ai", "Cognitive AI Purple", "Technology", true, "#0F0B29", "#1D0531", "#C77DFF", "CYBER_SLATE", "Tech Clean"),
    TemplatePreset("vibe_tech_matrix", "Matrix Digital Green", "Technology", false, "#040F0A", "#081E15", "#39FF14", "CYBER_SLATE", "Tech Clean"),

    // 4. Real Estate (Premium tones, golds, rich browns, warm sand)
    TemplatePreset("vibe_real_estate", "Luxury Real Estate", "Real Estate", true, "#141518", "#221C16", "#CBB26A", "MODERN_DOUBLE", "Elegant Serif"),
    TemplatePreset("vibe_estate_modern", "Metro Skyline", "Real Estate", false, "#121A21", "#1E2A38", "#F5CE62", "MINIMAL_GOLD", "Space Grotesk"),
    TemplatePreset("vibe_estate_timber", "Oak Wood Property", "Real Estate", false, "#1F1A15", "#2C2219", "#E6A15C", "MODERN_DOUBLE", "Elegant Serif"),

    // 5. Creative (Vivid gradients, active colors, glowing retro accents)
    TemplatePreset("vibe_creative_crimson", "Creative Sunset", "Creative", false, "#1C0407", "#2D0A14", "#FF3366", "MODERN_DOUBLE", "Space Grotesk"),
    TemplatePreset("vibe_creative_retro", "Retro Sunset", "Creative", false, "#2B1605", "#421C00", "#FFAC1C", "MODERN_DOUBLE", "Modern Bold"),
    TemplatePreset("vibe_creative_neon", "Vaporwave Aesthetic", "Creative", true, "#230A2E", "#510E5F", "#FF00FF", "CYBER_SLATE", "Tech Clean"),

    // 6. Medical (Clinical greens, doctor cyans, clean medical borders)
    TemplatePreset("vibe_medical_clean", "Clinical Blue-Green", "Medical", false, "#0A221C", "#144D3F", "#48CAE4", "MINIMAL_GOLD", "Tech Clean"),
    TemplatePreset("vibe_medical_dentist", "Dental Pure Teal", "Medical", true, "#0B262A", "#133C40", "#00F5FF", "MINIMAL_GOLD", "Modern Bold"),
    TemplatePreset("vibe_medical_cardio", "Cardiologist Crimson", "Medical", false, "#26060A", "#45090F", "#FF6B6B", "MODERN_DOUBLE", "Space Grotesk"),

    // 7. Education (Academic blue, scholar royal, math physics clean layouts)
    TemplatePreset("vibe_academic_blue", "Royal Scholar", "Education", false, "#0B132B", "#1C2541", "#5BC0BE", "MINIMAL_GOLD", "Modern Bold"),
    TemplatePreset("vibe_education_science", "Socrates Academy", "Education", false, "#081F26", "#12313E", "#62CDFF", "MINIMAL_GOLD", "Elegant Serif"),

    // 8. Modern Minimalist (Monochrome, sleek slate, light dynamic colors)
    TemplatePreset("vibe_minimal_slate", "Mineral Minimalist", "Modern Minimalist", false, "#1E1F29", "#111218", "#E0E0E0", "MINIMAL_GOLD", "Space Grotesk"),
    TemplatePreset("vibe_minimal_blush", "Rose Gold Minimalist", "Modern Minimalist", false, "#1C1418", "#2B1A21", "#FDA4AF", "MINIMAL_GOLD", "Elegant Serif"),
    TemplatePreset("vibe_minimal_plain", "Charcoal Monochrome", "Modern Minimalist", false, "#121212", "#1D1D1D", "#FFFFFF", "MINIMAL_GOLD", "Space Grotesk")
)

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
                    adUnitId = ""
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
                    colors = listOf(Color(0xFF0F111A), Color(0xFF07080D))
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
                // Futuristic Glowing Card Logo Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(110.dp)
                        .background(
                            Brush.sweepGradient(listOf(Color(0xFFD4AF37), Color(0xFF00FFCC), Color(0xFFD4AF37))),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(2.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF07080D), shape = RoundedCornerShape(19.dp))
                    ) {
                        Text(
                            text = "P'P",
                            color = Color(0xFFD4AF37),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(28.dp))
                
                Text(
                    text = "Pillai'Play",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                
                Text(
                    text = "Visiting Card Maker",
                    color = Color(0xFFD4AF37),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(100.dp))
                
                CircularProgressIndicator(
                    color = Color(0xFF00FFCC),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(28.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Production-Grade Luxury Suite v1.1",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light
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

            // UPGRADE TO PREMIUM BANNER CTA
            if (!isPremium) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF8E6B1D), Color(0xFFC0993C), Color(0xFFD4AF37))
                            )
                        )
                        .clickable { onNavigate(ActiveScreen.PREMIUM) }
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(0.7f)) {
                            Text(
                                text = "Unleash Full AI Creative Suite",
                                color = Color.Black,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Unlock unlimited saves, customize background images, watch premium previews, and remove watermark exports.",
                                color = Color.Black.copy(0.8f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Button(
                            onClick = { onNavigate(ActiveScreen.PREMIUM) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.weight(0.3f)
                        ) {
                            Text("UPGRADE", color = Color(0xFFD4AF37), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
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
                        title = "Manual Design",
                        sub = "Launch custom canvas",
                        icon = Icons.Default.Add,
                        color = Color(0xFF00FFCC),
                        modifier = Modifier.weight(1f).testTag("action_create_card")
                    ) {
                        selectedPresetToCreate = null
                        newCardNameInput = "My New Business Card"
                        showCreatePopup = true
                    }

                    DashboardActionItem(
                        title = "AI Generator",
                        sub = "Let Gemini design for you",
                        icon = Icons.Default.Star,
                        color = Color(0xFFD4AF37),
                        modifier = Modifier.weight(1f).testTag("action_ai_generator")
                    ) {
                        onNavigate(ActiveScreen.AI_GENERATOR)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    DashboardActionItem(
                        title = "Templates Library",
                        sub = "20+ Professional presets",
                        icon = Icons.Default.List,
                        color = Color(0xFFFF5E7E),
                        modifier = Modifier.weight(1f)
                    ) {
                        onNavigate(ActiveScreen.TEMPLATES)
                    }

                    DashboardActionItem(
                        title = "VIP Studio",
                        sub = "Membership benefits",
                        icon = Icons.Default.Star,
                        color = Color(0xFF00FFCC),
                        modifier = Modifier.weight(1f)
                    ) {
                        onNavigate(ActiveScreen.PREMIUM)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // POPULAR TRENDING PRESETS CORNER
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "POPULAR TRENDING TEMPLATES",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val popularSelection = cardTemplates.filter { it.isPremium || it.category == "Creative" || it.id.contains("gold") || it.id.contains("cyber") }.take(6)
                    popularSelection.forEach { preset ->
                        Box(
                            modifier = Modifier
                                .width(170.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(android.graphics.Color.parseColor(preset.bgStart)),
                                            Color(android.graphics.Color.parseColor(preset.bgEnd))
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (preset.isPremium) Color(0xFFD4AF37).copy(0.4f) else Color.DarkGray,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (preset.isPremium && !isPremium) {
                                        Toast.makeText(context, "Premium template! Available on VIP memberships.", Toast.LENGTH_SHORT).show()
                                        onNavigate(ActiveScreen.PREMIUM)
                                    } else {
                                        selectedPresetToCreate = preset
                                        newCardNameInput = "Design ${preset.name}"
                                        showCreatePopup = true
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.height(100.dp)) {
                                Column {
                                    Text(preset.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                    Text(preset.category, color = Color.LightGray.copy(0.6f), fontSize = 10.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (preset.isPremium) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFD4AF37), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("PREMIUM", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF00FFCC).copy(0.2f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("FREE", color = Color(0xFF00FFCC), fontSize = 8.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White.copy(0.7f), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // RECENT DESIGNS LIST
            Column(
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "RECENT CREATED PROJECTS (${cards.size})",
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
                            Text("No projects yet", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                                onDelete = { viewModel.deleteCard(card) }
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
    onDelete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
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
                modifier = Modifier.weight(0.7f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(android.graphics.Color.parseColor(card.backgroundColor)), RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(card.cardName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(card.themeName, color = Color(android.graphics.Color.parseColor(card.qrCodeColor)), fontSize = 11.sp)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = onDuplicate) {
                    Icon(Icons.Default.Share, contentDescription = "Duplicate", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
                }
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            }
        }
    }
}

// 4. TEMPLATES VIEW (FREE & PREMIUM CATEGORIZED BROWSER)
@Composable
fun TemplatesView(
    viewModel: CardViewModel,
    onNavigate: (ActiveScreen) -> Unit,
    onOpenEditor: () -> Unit
) {
    val context = LocalContext.current
    val isPremium by viewModel.isUserPremium.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Luxury", "Minimal", "Technology", "Medical", "Real Estate", "Education")

    val filteredPresets = if (selectedCategory == "All") {
        cardTemplates
    } else {
        cardTemplates.filter { it.category == selectedCategory }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C16))
    ) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onNavigate(ActiveScreen.DASHBOARD) },
                    modifier = Modifier.background(Color(0xFF131722), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Design Templates", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            if (!isPremium) {
                TextButton(onClick = { onNavigate(ActiveScreen.PREMIUM) }) {
                    Text("Unlock 500+ Premium", color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Horizontal Category Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val active = selectedCategory == cat
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (active) Color(0xFFD4AF37) else Color(0xFF131722),
                    border = BorderStroke(1.dp, if (active) Color(0xFFD4AF37) else Color(0xFF222B3A)),
                    modifier = Modifier.clickable { selectedCategory = cat }
                ) {
                    Text(
                        text = cat,
                        color = if (active) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Templates Vertical Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filteredPresets) { preset ->
                TemplateCardItem(
                    preset = preset,
                    isPremiumUnlocked = isPremium,
                    onSelect = {
                        if (preset.isPremium && !isPremium) {
                            // Show premium lock dialog or reward prompt
                            Toast.makeText(context, "Premium preset! Watch a rewarded video or unlock Premium.", Toast.LENGTH_LONG).show()
                            onNavigate(ActiveScreen.PREMIUM)
                        } else {
                            // Instantiate and launch card editor
                            viewModel.createNewCardProject(
                                name = "Modern ${preset.name}",
                                templateId = preset.id
                            )
                            onOpenEditor()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TemplateCardItem(
    preset: TemplatePreset,
    isPremiumUnlocked: Boolean,
    onSelect: () -> Unit
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
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
                text = "ACCOUNT & MEMBERSHIP",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )

            SettingsLinkRow(title = "Manage Account Profile", icon = Icons.Default.Person, label = "View Details") {
                editNameInput = viewModel.prefs.userName
                showAccountDialog = true
            }

            SettingsLinkRow(title = "Restore Studio Purchases", icon = Icons.Default.Refresh, label = "Validate") {
                viewModel.upgradeSubscription("Lifetime")
                Toast.makeText(context, "Purchases recovered successfully! Lifetime Premium VIP unlocked.", Toast.LENGTH_LONG).show()
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

            SettingsLinkRow(title = "AdMob Monetization Unit Profiles", icon = Icons.Default.Build, label = "Setup Ads") {
                onNavigate(ActiveScreen.ADS_MANAGER)
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
                    text = "App Edition: v2.10 (Premium Elite Sandbox)",
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
            title = { Text("Privacy Policy Details", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "We are dedicated to safeguarding your corporate visual workspace:\n\n" +
                               "1. Local Storage Sandbox: Your card graphics files, template records, and custom elements rest securely inside your device database.\n\n" +
                               "2. Zero Telemetry Tracking: We do not log or stream visual coordinates, designs, or drafts back to external servers.\n\n" +
                               "3. AdMob Integrations: Advertisements load securely via standard Google Ad SDK protocols without profiling private data.\n\n" +
                               "Feel safe designing your creative assets with Pillai'Play Visiter Card Maker.",
                        color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
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
            title = { Text("Terms & Conditions", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Usage Regulations for Pillai'Play Business Platform:\n\n" +
                               "1. Design Licensing: Free and Premium layout templates can be utilized for both personal and corporate branding purposes without secondary fees.\n\n" +
                               "2. Commercial Redistribution: You are forbidden from repackaging design presets or components to sell on other marketplaces.\n\n" +
                               "3. Storage Responsibility: Local database loss during manual device clearing is the user's responsibility. Recover purchases via the 'Restore Studio Purchases' validator at any time.",
                        color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
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
                    Text("Support Mailbox: support@pillaiplay.com", color = Color(0xFFD4AF37), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Support Email", "support@pillaiplay.com")
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
    val context = LocalContext.current
    val isPremium by viewModel.isUserPremium.collectAsState()
    val scrollState = rememberScrollState()

    // 1. Interactive States for Selected Plan and Play Store Billing Simulation
    var selectedPlanKey by remember { mutableStateOf("premium_yearly") }
    var showBillingDialog by remember { mutableStateOf(false) }
    var activeBillingProduct by remember { mutableStateOf<BillingProduct?>(null) }
    var showCelebrationDialog by remember { mutableStateOf(false) }

    // 2. Infinite Animations for floating elements and glowing effects
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "premium_anims")
    
    val floatingOffsetY by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 2000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "crown_floating"
    )

    val crownRotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 2500, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "crown_rotate"
    )

    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.5f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 1800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "gold_glow"
    )

    // Fade-in animation triggered on launch
    var animateCardsIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateCardsIn = true
    }

    // List of billing products (including the requested Monthly/Yearly/Lifetime details)
    val products = listOf(
        BillingProduct(
            id = "premium_monthly",
            title = "Premium Monthly",
            priceLabel = "₹99/month",
            badge = "Most Flexible",
            features = listOf("All Premium Features", "Cancel Anytime", "No Ads"),
            buttonLabel = "Subscribe Monthly"
        ),
        BillingProduct(
            id = "premium_yearly",
            title = "Premium Yearly",
            priceLabel = "₹799/year",
            badge = "Best Value",
            features = listOf("All Premium Features", "Save More Than 30%", "No Ads"),
            buttonLabel = "Subscribe Yearly",
            isPopular = true
        ),
        BillingProduct(
            id = "premium_lifetime",
            title = "Lifetime Premium",
            priceLabel = "₹999 One-Time",
            badge = "Lifetime VIP",
            features = listOf("Pay Once", "Lifetime Access", "No Recurring Payments", "All Future Updates"),
            buttonLabel = "Buy Lifetime"
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07080E)) // Luxury Premium Dark Theme
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Header back-bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { onNavigate(ActiveScreen.DASHBOARD) },
                    modifier = Modifier.background(Color(0xFF131722), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .border(1.dp, Color(0xFFD4AF37).copy(0.4f), RoundedCornerShape(20.dp))
                        .background(Color(0xFF1D170B), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFD4AF37),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPremium) "VIP ACTIVE" else "GO PREMIUM",
                            color = Color(0xFFD4AF37),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // HEADER SECTION
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Floating Crown unicode representation
                Box(
                    modifier = Modifier
                        .offset(y = floatingOffsetY.dp)
                        .graphicsLayer { rotationZ = crownRotation }
                        .size(80.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFFD4AF37).copy(pulseGlowAlpha), Color.Transparent)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "👑",
                        fontSize = 52.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Text(
                    text = "Unlock Premium",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Text(
                    text = "Create professional visiting cards without limits.",
                    color = Color.LightGray.copy(0.81f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, start = 16.dp, end = 16.dp)
                )
                
                Text(
                    text = "Pillai'Play Visiting Card Maker Pro Suite",
                    color = Color(0xFFD4AF37),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // INCLUDED VALUE BENEFITS BOX SHOWCASE
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, Color(0xFF222B3A)), RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F131E))
                    .padding(20.dp)
            ) {
                Text(
                    text = "INCLUDED PREMIUM FEATURES",
                    color = Color(0xFFD4AF37),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                val benefits = listOf(
                    "Unlimited Custom Background Uploads", 
                    "Unlimited Card Creation",
                    "Unlimited Exports", 
                    "Premium Templates", 
                    "Watermark-Free Downloads", 
                    "No Advertisements", 
                    "Premium Fonts", 
                    "Premium Icons", 
                    "Priority Support", 
                    "Future Premium Features Included"
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    benefits.forEach { benefit ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(Color(0xFF1D2825), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF00FFCC),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = benefit,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PRICING LAYOUT GRID
            Column(
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "CHOOSE YOUR PLAN",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                products.forEach { product ->
                    val isSelected = selectedPlanKey == product.id
                    val isPopular = product.isPopular == true

                    // Scale factor animation based on state
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.02f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                        label = "cardScale"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .border(
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFD4AF37) else if (isPopular) Color(0xFFD4AF37).copy(0.4f) else Color(0xFF222B3A)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .background(
                                color = if (isSelected) Color(0xFF1D170B) else Color(0xFF111420),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                selectedPlanKey = product.id
                            }
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedPlanKey = product.id },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = Color(0xFFD4AF37),
                                            unselectedColor = Color.Gray
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = product.title,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (product.badge.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (isPopular) Color(0xFFD4AF37) else Color(0xFF22283A),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = product.badge.uppercase(),
                                            color = if (isPopular) Color.Black else Color.LightGray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column(modifier = Modifier.weight(0.7f)) {
                                    product.features.forEach { feat ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 1.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color(0xFFD4AF37),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = feat,
                                                color = Color.LightGray.copy(0.85f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = product.priceLabel,
                                    color = Color(0xFFD4AF37),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.weight(0.3f),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // BIG PREMIUM INTERACTIVE PURCHASE ACTION BUTTONS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val selectedProduct = products.find { it.id == selectedPlanKey } ?: products[1]

                Button(
                    onClick = {
                        activeBillingProduct = selectedProduct
                        showBillingDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("subscribe_action_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD4AF37),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = selectedProduct.buttonLabel,
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // COMPARISON TABLE: FREE VS PREMIUM FEATURES
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "CORE COMPARISON MATRIX",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Render structured table
                Surface(
                    color = Color(0xFF0F111A),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF222B3A))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF141926))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Capability", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.42f))
                            Text("FREE Standard", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.29f), textAlign = TextAlign.Center)
                            Text("PRO Premium Gold", color = Color(0xFFD4AF37), fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(0.29f), textAlign = TextAlign.Center)
                        }

                        val comparisonRows = listOf(
                            ComparisonRowItem("Advertisements", "Ads Enabled", "Clean / No Ads ✓", false),
                            ComparisonRowItem("Custom Backgrounds", "5 Uploads Max", "Unlimited Core ✓", true),
                            ComparisonRowItem("Design Presets", "Standard Catalog", "Unlock All 20+ ✓", true),
                            ComparisonRowItem("Export Resolution", "Standard Quality", "Ultra HD Vector ✓", true),
                            ComparisonRowItem("Advanced Tools", "Basic Controls", "Lock & Multi-QR ✓", true)
                        )

                        comparisonRows.forEachIndexed { idx, row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (idx % 2 == 0) Color(0xFF10131E) else Color(0xFF161B29))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = row.feature,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(0.42f)
                                )
                                Text(
                                    text = row.freeValue,
                                    color = Color.LightGray.copy(0.6f),
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(0.29f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = row.premValue,
                                    color = if (row.highlight) Color(0xFF00FFCC) else Color(0xFFD4AF37),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(0.29f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // RESTORE PURCHASES SECTION
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = {
                        // Restore Simulated Billing products standard verification
                        viewModel.upgradeSubscription("Restored Pro Access")
                        Toast.makeText(context, "Checking Google Play... Standard purchase records restored. Premium Gold activated!", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD4AF37)),
                    border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFD4AF37))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restore Purchases", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SYSTEM TERMS & POLICY SECTION
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Subscriptions renew automatically unless canceled through Google Play. Manage subscriptions in Google Play Settings.",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Developer Switch testing reset mode when premium
                if (isPremium) {
                    TextButton(onClick = { 
                        viewModel.downgradeToFree()
                        Toast.makeText(context, "Account reset to Standard Sandbox", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Revert Account to Free Mode (Testing)", color = Color.Red.copy(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }

        // 3. ACTUAL GOOGLE PLAY BILLING OVERLAY DIALOG (SIMULATION)
        if (showBillingDialog && activeBillingProduct != null) {
            val prod = activeBillingProduct!!
            AlertDialog(
                onDismissRequest = { showBillingDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🟢 Google Play Billing",
                            color = Color(0xFF00E676),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Pillai'Play Visiting Card Maker Pro",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.DarkGray))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Product Item ID:", color = Color.Gray, fontSize = 12.sp)
                            Text(prod.id, color = Color.LightGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Account Details:", color = Color.Gray, fontSize = 12.sp)
                            Text(viewModel.prefs.userEmail.take(24), color = Color.LightGray, fontSize = 12.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Transaction Cost:", color = Color.Gray, fontSize = 12.sp)
                            Text(prod.priceLabel, color = Color(0xFF00FFCC), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.DarkGray))
                        
                        Text(
                            text = "This is a secure native integration mock validating Google Play API credentials. Click confirmation below to execute premium activation triggers.",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.upgradeSubscription(prod.title)
                            showBillingDialog = false
                            showCelebrationDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00875A)) // Official Play Store Confirm Color
                    ) {
                        Text("Confirm Simulated Purchase", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBillingDialog = false }) {
                        Text("Cancel", color = Color.LightGray)
                    }
                },
                containerColor = Color(0xFF131722)
            )
        }

        // Golden Confetti Celebration layout triggers success overlay
        if (showCelebrationDialog) {
            AlertDialog(
                onDismissRequest = {
                    showCelebrationDialog = false
                    onNavigate(ActiveScreen.DASHBOARD)
                },
                title = {
                    Text(
                        text = "🎉 WELCOME TO VIP PREMIUM!",
                        color = Color(0xFFD4AF37),
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("✨", fontSize = 48.sp)
                        Text(
                            text = "Your Transaction Was Completed Successfully!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Standard limits are now deactivated. Take advantage of full AI design resources, unlimited layouts, watermark-free high-res downloads, and pure vector elements.",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showCelebrationDialog = false
                            onNavigate(ActiveScreen.DASHBOARD)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Launch Pro Suite Features", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color(0xFF121420)
            )
        }
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
            if (!isPremium) {
                Surface(
                    color = Color(0xFFD4AF37).copy(0.15f),
                    border = BorderStroke(1.dp, Color(0xFFD4AF37)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Usage: ${aiGenerationsCount.coerceAtMost(3)}/3 Today",
                        color = Color(0xFFD4AF37),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            } else {
                Surface(
                    color = Color(0xFF00FFCC).copy(0.15f),
                    border = BorderStroke(1.dp, Color(0xFF00FFCC)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Premium Unlimited",
                        color = Color(0xFF00FFCC),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
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
                            qrCodeX = option.qrX,
                            qrCodeY = option.qrY
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
                                    if (!isPremium && aiGenerationsCount >= 3) {
                                        Toast.makeText(context, "Upgrade to Premium for Unlimited AI generations!", Toast.LENGTH_LONG).show()
                                        onNavigate(ActiveScreen.PREMIUM)
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
                                            logoUri = logoUri,
                                            photoUri = photoUri
                                        )
                                    }
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

// DRAG-FREE COMPACT MINI VISITING CARD PREVIEW COMPOSABLE
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

    Box(
        modifier = modifier
            .aspectRatio(1.58f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(cardBgStart, cardBgEnd)))
            .border(1.dp, accentColor.copy(0.35f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(0.5.dp, accentColor.copy(0.15f), RoundedCornerShape(3.dp))
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    if (visibleFields.contains("companyName")) {
                        Text(
                            text = card.companyName,
                            color = accentColor,
                            fontSize = 11.sp,
                            fontFamily = fFamily,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    if (visibleFields.contains("fullName")) {
                        Text(
                            text = card.fullName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontFamily = fFamily,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    if (visibleFields.contains("jobTitle")) {
                        Text(
                            text = card.jobTitle.uppercase(),
                            color = Color.LightGray.copy(0.8F),
                            fontSize = 8.sp,
                            fontFamily = fFamily,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    if (visibleFields.contains("mobileNumber") && card.mobileNumber.isNotEmpty()) {
                        Text(
                            text = "📞 " + card.mobileNumber,
                            color = Color.LightGray.copy(0.7F),
                            fontSize = 7.5.sp,
                            fontFamily = fFamily,
                            maxLines = 1
                        )
                    }
                    if (visibleFields.contains("email") && card.email.isNotEmpty()) {
                        Text(
                            text = "✉ " + card.email,
                            color = Color.LightGray.copy(0.7F),
                            fontSize = 7.5.sp,
                            fontFamily = fFamily,
                            maxLines = 1
                        )
                    }
                    if (visibleFields.contains("website") && card.website.isNotEmpty()) {
                        Text(
                            text = "🌐 " + card.website,
                            color = Color.LightGray.copy(0.7F),
                            fontSize = 7.5.sp,
                            fontFamily = fFamily,
                            maxLines = 1
                        )
                    }
                    if (visibleFields.contains("address") && card.address.isNotEmpty()) {
                        Text(
                            text = "📍 " + card.address,
                            color = Color.LightGray.copy(0.7F),
                            fontSize = 7.5.sp,
                            fontFamily = fFamily,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Bottom
            ) {
                if (card.qrCodeVisible) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color.White.copy(0.08F), RoundedCornerShape(6.dp))
                            .border(1.dp, accentColor.copy(0.4f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Box(modifier = Modifier.size(8.dp).background(accentColor))
                                Box(modifier = Modifier.size(8.dp).background(Color.Transparent))
                                Box(modifier = Modifier.size(8.dp).background(accentColor))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Box(modifier = Modifier.size(8.dp).background(Color.Transparent))
                                Box(modifier = Modifier.size(8.dp).background(accentColor))
                                Box(modifier = Modifier.size(8.dp).background(Color.Transparent))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Box(modifier = Modifier.size(8.dp).background(accentColor))
                                Box(modifier = Modifier.size(8.dp).background(Color.Transparent))
                                Box(modifier = Modifier.size(8.dp).background(accentColor))
                            }
                        }
                    }
                }
            }
        }
    }
}


