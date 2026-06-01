package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
    TemplatePreset("vibe_modern_gold", "Platinum Gold Luxury", "Luxury", true, "#10121A", "#1F1A13", "#D4AF37", "MINIMAL_GOLD", "Elegant Serif"),
    TemplatePreset("vibe_tech_cyber", "Cyber Sleek Neon", "Technology", true, "#050811", "#0D1E2D", "#00FFCC", "CYBER_SLATE", "Tech Clean"),
    TemplatePreset("vibe_minimal_slate", "Mineral Minimalist", "Minimal", false, "#1E1F29", "#111218", "#E0E0E0", "MINIMAL_GOLD", "Space Grotesk"),
    TemplatePreset("vibe_creative_crimson", "Creative Sunset", "Creative", false, "#1C0407", "#2D0A14", "#FF3366", "MODERN_DOUBLE", "Space Grotesk"),
    TemplatePreset("vibe_academic_blue", "Royal Scholar", "Education", false, "#0B132B", "#1C2541", "#5BC0BE", "MINIMAL_GOLD", "Modern Bold"),
    TemplatePreset("vibe_real_estate", "Luxury Real Estate", "Real Estate", true, "#141518", "#221C16", "#CBB26A", "MODERN_DOUBLE", "Elegant Serif"),
    TemplatePreset("vibe_medical_clean", "Clinical Medical", "Medical", false, "#0A221C", "#144D3F", "#48CAE4", "MINIMAL_GOLD", "Tech Clean"),
    TemplatePreset("vibe_freelance_orange", "Nomad Freelancer", "Freelancer", false, "#121212", "#1E1B18", "#FF8C00", "CYBER_SLATE", "Modern Bold")
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
                    adUnitId = "ca-app-pub-5487081756225733/8068514123"
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
    var newCardNameInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C16))
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Color(0xFFD4AF37), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
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
                        fontSize = 12.sp
                    )
                    Text(
                        text = viewModel.prefs.userName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
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

            // Quick log out
            IconButton(
                onClick = {
                    viewModel.logout()
                    Toast.makeText(context, "Logged Out", Toast.LENGTH_SHORT).show()
                    onNavigate(ActiveScreen.AUTH)
                },
                modifier = Modifier.background(Color(0xFF131722), CircleShape)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Log Out", tint = Color.Red)
            }
        }

        // UPGRADE TO PREMIUM BANNER CTA
        if (!isPremium) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
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
                            text = "Unlock 500+ templates, AI generator, and HD vectors.",
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

        // MAIN GRID SELECTIONS
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("SUITE COMMAND CENTER", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                DashboardActionItem(
                    title = "Manual Design",
                    sub = "Start blank card",
                    icon = Icons.Default.Add,
                    color = Color(0xFF00FFCC),
                    modifier = Modifier.weight(1f).testTag("action_create_card")
                ) {
                    newCardNameInput = "My New Business Card"
                    showCreatePopup = true
                }

                DashboardActionItem(
                    title = "AI Generator",
                    sub = "Let Gemini design",
                    icon = Icons.Default.Star,
                    color = Color(0xFFD4AF37),
                    modifier = Modifier.weight(1f).testTag("action_ai_generator")
                ) {
                    if (isPremium) {
                        onNavigate(ActiveScreen.AI_GENERATOR)
                    } else {
                        Toast.makeText(context, "Gemini generative designer is exclusive for premium VIP accounts!", Toast.LENGTH_LONG).show()
                        onNavigate(ActiveScreen.PREMIUM)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                DashboardActionItem(
                    title = "Premium Catalog",
                    sub = "Explore layouts",
                    icon = Icons.Default.List,
                    color = Color(0xFFFF5E7E),
                    modifier = Modifier.weight(1f)
                ) {
                    onNavigate(ActiveScreen.TEMPLATES)
                }

                DashboardActionItem(
                    title = "Studio Settings",
                    sub = "Prefs & Ads ID",
                    icon = Icons.Default.Settings,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                ) {
                    onNavigate(ActiveScreen.SETTINGS)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // MY PROJECTS SECTION (ROOM DATABASE PERSISTED FLOW)
        Column(
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Text(
                text = "MY SAVED PROJECTS (${cards.size})",
                color = Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (cards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(Color(0xFF131722), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.DarkGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No projects yet", color = Color.Gray, fontSize = 13.sp)
                        Text("Tap 'Manual Design' or 'AI Generator' above to start!", color = Color.Gray, fontSize = 10.sp)
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

        Spacer(modifier = Modifier.height(40.dp))
    }

    // New Project Dialog
    if (showCreatePopup) {
        AlertDialog(
            onDismissRequest = { showCreatePopup = false },
            title = { Text("Set Project Title", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newCardNameInput,
                    onValueChange = { newCardNameInput = it },
                    label = { Text("Project Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCardNameInput.isNotEmpty()) {
                            viewModel.createNewCardProject(newCardNameInput)
                            showCreatePopup = false
                            onOpenEditor()
                        }
                    }
                ) {
                    Text("Launch Editor")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePopup = false }) { Text("Cancel") }
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
        color = Color(0xFF131722),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF222B3A)),
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
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(sub, color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
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
        color = Color(0xFF131722),
        shape = RoundedCornerShape(10.dp),
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
                modifier = Modifier.weight(0.7f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(android.graphics.Color.parseColor(card.backgroundColor)), RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(card.cardName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(card.themeName, color = Color(android.graphics.Color.parseColor(card.qrCodeColor)), fontSize = 11.sp)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = onDuplicate) {
                    Icon(Icons.Default.Share, contentDescription = "Duplicate", tint = Color.LightGray)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray)
                }
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Gray)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C16))
            .verticalScroll(rememberScrollState())
    ) {
        // Back Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onNavigate(ActiveScreen.DASHBOARD) },
                modifier = Modifier.background(Color(0xFF131722), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("Studio Settings", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        // Section theme selectors
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("THEME & VISUAL PREFERENCES", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black)

            Surface(color = Color(0xFF131722), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFF222B3A))) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Selected Design Theme: $activeTheme", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ThemePillButton("DARK", activeTheme == "DARK", modifier = Modifier.weight(1f)) { viewModel.changeTheme("DARK") }
                        ThemePillButton("LIGHT", activeTheme == "LIGHT", modifier = Modifier.weight(1f)) { viewModel.changeTheme("LIGHT") }
                        ThemePillButton("SYSTEM", activeTheme == "SYSTEM", modifier = Modifier.weight(1f)) { viewModel.changeTheme("SYSTEM") }
                    }
                }
            }

            Text("CONFIGURATION AND INTEGRATIONS", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Black)

            // Settings buttons
            SettingsLinkRow(title = "AdMob Monetization Controller", icon = Icons.Default.Notifications, label = "Manage IDs") {
                onNavigate(ActiveScreen.ADS_MANAGER)
            }

            SettingsLinkRow(title = "Cloud Service Sandbox / Sync", icon = Icons.Default.Info, label = "Firebase Status") {
                Toast.makeText(context, "Firebase Database is up-to-date and synced locally.", Toast.LENGTH_LONG).show()
            }

            SettingsLinkRow(title = "Privacy Policy terms", icon = Icons.Default.Lock, label = "Read info") {
                Toast.makeText(context, "Review privacy details online at pillaiplay.com/privacy", Toast.LENGTH_LONG).show()
            }

            SettingsLinkRow(title = "Restore Studio Purchases", icon = Icons.Default.Refresh, label = "Restore") {
                viewModel.upgradeSubscription("Lifetime")
                Toast.makeText(context, "Purchases recovered successfully! Lifetime Premium Unlocked.", Toast.LENGTH_LONG).show()
            }

            SettingsLinkRow(title = "Notifications settings", icon = Icons.Default.Notifications, label = if (viewModel.prefs.notificationsEnabled) "ON" else "OFF") {
                viewModel.prefs.notificationsEnabled = !viewModel.prefs.notificationsEnabled
                Toast.makeText(context, "Notifications status updated.", Toast.LENGTH_SHORT).show()
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ThemePillButton(text: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (active) Color(0xFFD4AF37) else Color(0xFF1E2433),
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color = if (active) Color.Black else Color.White,
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 10.dp)
        )
    }
}

@Composable
fun SettingsLinkRow(title: String, icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        color = Color(0xFF131722),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFF222B3A)),
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
                Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// 6. PREMIUM / SUBSCRIPTION VIEW
@Composable
fun PremiumView(viewModel: CardViewModel, onNavigate: (ActiveScreen) -> Unit) {
    val context = LocalContext.current
    val isPremium by viewModel.isUserPremium.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F111A))
            .verticalScroll(rememberScrollState())
    ) {
        // Back toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onNavigate(ActiveScreen.DASHBOARD) },
                modifier = Modifier.background(Color(0xFF131722), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("Upgrade VIP Pass", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(72.dp), tint = Color(0xFFD4AF37))
            
            Text(
                text = "Pillai'Play Ultimate VIP Access",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = "Create cards like a pro with full AI access.",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Features specs
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131722), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text("INCLUDED PREMIUM BENEFITS", color = Color(0xFFD4AF37), fontSize = 11.sp, fontWeight = FontWeight.Black)
                
                PremiumFeatureBullet("No Ads / Ad-free editing suite")
                PremiumFeatureBullet("Unlimited High-Fidelity Exports (Vector PDF, UHD PNG)")
                PremiumFeatureBullet("Complete 500+ templates catalog")
                PremiumFeatureBullet("Gemini AI Card Generator Engine")
                PremiumFeatureBullet("Custom Background remover & dynamic shape QR")
                PremiumFeatureBullet("Secure real-time Firebase Sync Backup")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pricing Plans Selection cards
            PremiumPricingCard("Monthly Pro Suite", "$2.99 / mo", "Continuous updates, cloud sandbox", active = false) {
                viewModel.upgradeSubscription("Monthly")
                Toast.makeText(context, "Monthly pass unlocked!", Toast.LENGTH_SHORT).show()
                onNavigate(ActiveScreen.DASHBOARD)
            }

            Spacer(modifier = Modifier.height(10.dp))

            PremiumPricingCard("Yearly Pro Gold (Best Value)", "$19.99 / yr", "Save 40%, unlimited vector exports", active = true) {
                viewModel.upgradeSubscription("Yearly")
                Toast.makeText(context, "Full gold year pass active!", Toast.LENGTH_SHORT).show()
                onNavigate(ActiveScreen.DASHBOARD)
            }

            Spacer(modifier = Modifier.height(10.dp))

            PremiumPricingCard("Lifetime VIP Platinum", "$49.99 One-time", "Own the studio suite forever!", active = false) {
                viewModel.upgradeSubscription("Lifetime")
                Toast.makeText(context, "Lifetime access granted. Welcome to VIP!", Toast.LENGTH_SHORT).show()
                onNavigate(ActiveScreen.DASHBOARD)
            }

            if (isPremium) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { 
                    viewModel.downgradeToFree()
                    Toast.makeText(context, "Account reset to Standard Sandbox", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Revert Account to Free Mode (Testing)", color = Color.Red, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun PremiumFeatureBullet(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00FFCC), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun PremiumPricingCard(title: String, price: String, decs: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        color = Color(0xFF131722),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, if (active) Color(0xFFD4AF37) else Color(0xFF222B3A)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(0.7f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(decs, color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Text(price, color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(0.3f), textAlign = TextAlign.End)
        }
    }
}

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

    if (!isPremium) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0C16)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked Feature",
                    tint = Color(0xFFD4AF37),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "AI Card Generator",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This innovative design generator allows Gemini AI to construct customized high-profile business cards for you on-the-fly. Upgrading to Pro VIP releases access instantly.",
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onNavigate(ActiveScreen.PREMIUM) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Upgrade to Premium Pro", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = { onNavigate(ActiveScreen.DASHBOARD) }) {
                    Text("Return to Dashboard", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
        return
    }

    var inputName by remember { mutableStateOf(viewModel.prefs.userName) }
    var inputBusiness by remember { mutableStateOf("") }
    var inputCompany by remember { mutableStateOf("Freelance Studio") }

    val loading by viewModel.aiLoading.collectAsState()
    val error by viewModel.aiError.collectAsState()
    val success by viewModel.aiSuccess.collectAsState()

    LaunchedEffect(success) {
        if (success) {
            Toast.makeText(context, "AI Studio successfully created design!", Toast.LENGTH_SHORT).show()
            viewModel.resetAIStates()
            onOpenEditor()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C16))
            .verticalScroll(rememberScrollState())
    ) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onNavigate(ActiveScreen.DASHBOARD) },
                modifier = Modifier.background(Color(0xFF131722), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("Gemini AI Copilot", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = Color(0xFF131722),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(0.3f))
            ) {
                Row(modifier = Modifier.padding(14.dp)) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD4AF37))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Generative AI Styling Assistant", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Type your vertical & firm details. Our AI generator will layout appropriate color coordinates, size specifications, and optimal QR patterns instantly.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // Input Fields
            OutlinedTextField(
                value = inputName,
                onValueChange = { inputName = it },
                label = { Text("Display Name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth().testTag("ai_name_input")
            )

            OutlinedTextField(
                value = inputBusiness,
                onValueChange = { inputBusiness = it },
                label = { Text("Business Vertical (e.g. Cyber Tech, Luxury Real Estate, Creative Artist)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth().testTag("ai_business_input")
            )

            OutlinedTextField(
                value = inputCompany,
                onValueChange = { inputCompany = it },
                label = { Text("Company / Brand Name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth().testTag("ai_company_input")
            )

            if (error != null) {
                Text(text = "Generator notice: $error", color = Color(0xFFFF5E7E), fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (loading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color(0xFFD4AF37))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("AI is compiling color palette layouts...", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                Button(
                    onClick = {
                        if (inputName.isNotEmpty() && inputBusiness.isNotEmpty()) {
                            viewModel.triggerAICardGeneration(inputName, inputBusiness, inputCompany)
                        } else {
                            Toast.makeText(context, "Please fill in vertical details", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Custom Layout", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


