package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.example.R
import com.example.data.VaultFile
import com.example.data.VaultFileRepository
import com.example.security.CryptographyManager
import com.example.security.SecurityManager
import com.example.utils.FileUtils
import com.example.utils.Localization
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// --- PRIMARY THEME COLORS ---
var isAppDarkTheme by mutableStateOf(false)

val MidnightNavy: Color @Composable get() = if (isAppDarkTheme) Color(0xFF000000) else Color(0xFFFFFFFF)
val CelestialDark: Color @Composable get() = if (isAppDarkTheme) Color(0xFF121212) else Color(0xFFF5F5F7)
val StarWhite: Color @Composable get() = if (isAppDarkTheme) Color(0xFFFFFFFF) else Color(0xFF111111)
val MoonGlow: Color @Composable get() = if (isAppDarkTheme) Color(0xFF999999) else Color(0xFF666666)
val ElectricBlue: Color @Composable get() = if (isAppDarkTheme) Color(0xFFD0BCFF) else Color(0xFF6750A4)
val DeepIndigo: Color @Composable get() = if (isAppDarkTheme) Color(0xFF2A2438) else Color(0xFFE8DEF8)
val CoralRed: Color @Composable get() = if (isAppDarkTheme) Color(0xFFCF6679) else Color(0xFFB3261E)

@Composable
fun CelestialGradientBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MidnightNavy, CelestialDark)
                )
            ),
        content = content
    )
}

// --- SETUP WIZARD ---
@Composable
fun SetupWizardScreen(viewModel: VaultViewModel, onFinished: () -> Unit) {
    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onKeyFileSelected(uri)
        }
    }

    var step by remember { mutableStateOf(1) }
    val lang = viewModel.appLanguageSetting

    CelestialGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Language Selection Row at Top
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { viewModel.updateLanguage(if (lang == "en") "fa" else "en") }
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language Selector",
                        tint = ElectricBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (lang == "en") "فارسی" else "English",
                        color = ElectricBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = Localization.getString(lang, "setup_wizard"),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = StarWhite,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = Localization.getString(lang, "app_info_desc"),
                fontSize = 13.sp,
                color = MoonGlow.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // STEP PROGRESS INDICATOR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(1, 2, 3).forEach { index ->
                    val isActive = step >= index
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isActive) ElectricBlue else CelestialDark)
                            .border(1.dp, if (isActive) ElectricBlue else MoonGlow.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = index.toString(),
                            color = if (isActive) Color.White else MoonGlow,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (index < 3) {
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(2.dp)
                                .background(if (step > index) ElectricBlue else CelestialDark)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // STEP CARDS
            Card(
                colors = CardDefaults.cardColors(containerColor = CelestialDark),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (step) {
                        1 -> {
                            Text(
                                text = Localization.getString(lang, "settings_change_password"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = StarWhite
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = viewModel.setupPassword,
                                onValueChange = { viewModel.setupPassword = it },
                                label = { Text(Localization.getString(lang, "setup_password_hint")) },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = StarWhite),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricBlue,
                                    unfocusedBorderColor = ElectricBlue.copy(alpha = 0.4f),
                                    focusedLabelColor = ElectricBlue,
                                    unfocusedLabelColor = MoonGlow
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("setup_password_input")
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = viewModel.setupConfirmPassword,
                                onValueChange = { viewModel.setupConfirmPassword = it },
                                label = { Text(Localization.getString(lang, "setup_confirm_password")) },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = StarWhite),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricBlue,
                                    unfocusedBorderColor = ElectricBlue.copy(alpha = 0.4f),
                                    focusedLabelColor = ElectricBlue,
                                    unfocusedLabelColor = MoonGlow
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("setup_confirm_password_input")
                            )
                        }
                        2 -> {
                            Text(
                                text = Localization.getString(lang, "settings_biometrics"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = StarWhite
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = Localization.getString(lang, "setup_biometric"),
                                fontSize = 14.sp,
                                color = MoonGlow.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = Localization.getString(lang, "settings_biometrics"),
                                    color = StarWhite,
                                    fontSize = 16.sp
                                )
                                Switch(
                                    checked = viewModel.setupBiometricEnabled,
                                    onCheckedChange = { viewModel.setupBiometricEnabled = it },
                                    modifier = Modifier.testTag("biometric_switch")
                                )
                            }
                        }
                        3 -> {
                            Text(
                                text = Localization.getString(lang, "setup_key_file_title"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = StarWhite
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = Localization.getString(lang, "setup_key_file_desc"),
                                fontSize = 13.sp,
                                color = MoonGlow.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { filePickerLauncher.launch("*/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("choose_key_file_button")
                            ) {
                                Icon(Icons.Default.FileOpen, contentDescription = "File")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(Localization.getString(lang, "setup_key_file_button"))
                            }
                            if (viewModel.setupKeyFileName.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = Localization.getString(lang, "setup_key_file_selected", viewModel.setupKeyFileName),
                                    fontSize = 13.sp,
                                    color = ElectricBlue,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Error Display
            if (viewModel.authErrorMsg != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = CoralRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CoralRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = viewModel.authErrorMsg ?: "",
                        color = CoralRed,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ACTIONS NAVIGATION
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (step > 1) {
                    OutlinedButton(
                        onClick = { step-- },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(Localization.getString(lang, "prev_btn"))
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                if (step < 3) {
                    Button(
                        onClick = {
                            if (step == 1) {
                                val isEn = viewModel.appLanguageSetting == "en"
                                if (viewModel.setupPassword.length < 6) {
                                    viewModel.authErrorMsg = if (isEn) "Password must be at least 6 characters" else "رمز عبور باید حداقل ۶ کاراکتر باشد"
                                } else if (viewModel.setupPassword != viewModel.setupConfirmPassword) {
                                    viewModel.authErrorMsg = if (isEn) "Passwords do not match" else "رمز عبور با تکرار آن مطابقت ندارد"
                                } else {
                                    viewModel.authErrorMsg = null
                                    step++
                                }
                            } else {
                                step++
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(Localization.getString(lang, "next_btn"))
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.completeSetup {
                                val successMsg = Localization.getString(lang, "setup_success_msg")
                                Toast.makeText(context, successMsg, Toast.LENGTH_LONG).show()
                                onFinished()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("complete_setup_button")
                    ) {
                        Text(Localization.getString(lang, "setup_complete_button"))
                    }
                }
            }
        }
    }
}

// --- LOGIN SCREEN ---
@Composable
fun LoginScreen(viewModel: VaultViewModel, onAuthenticated: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lang = viewModel.appLanguageSetting

    DisposableEffect(Unit) {
        viewModel.startLockoutTicker()
        onDispose {
            viewModel.stopLockoutTicker()
        }
    }

    CelestialGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo
            Image(
                painter = painterResource(id = R.drawable.img_app_icon),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(2.dp, ElectricBlue, CircleShape)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = viewModel.customLauncherName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = StarWhite
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (lang == "en") "Decrypted with key: ${viewModel.openedKeyFileName}" else "رمزگشایی شده با کلید: ${viewModel.openedKeyFileName}",
                fontSize = 13.sp,
                color = ElectricBlue,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = CelestialDark),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = Localization.getString(lang, "login_enter_password"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MoonGlow
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = viewModel.inputPassword,
                        onValueChange = { viewModel.inputPassword = it },
                        label = { Text(Localization.getString(lang, "login_password_placeholder")) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = StarWhite),
                        singleLine = true,
                        enabled = !viewModel.isLockedOut,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricBlue,
                            unfocusedBorderColor = ElectricBlue.copy(alpha = 0.4f),
                            focusedLabelColor = ElectricBlue,
                            unfocusedLabelColor = MoonGlow
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input")
                    )

                    if (viewModel.authErrorMsg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = viewModel.authErrorMsg ?: "",
                            color = CoralRed,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (viewModel.isLockedOut) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CoralRed.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, CoralRed)
                        ) {
                            Text(
                                text = Localization.getString(lang, "login_locked", viewModel.remainingLockTimeText),
                                color = CoralRed,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            viewModel.authenticateWithPassword {
                                onAuthenticated()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !viewModel.isLockedOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_submit_button")
                    ) {
                        Text(Localization.getString(lang, "login_button"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    if (viewModel.biometricSettingEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        IconButton(
                            onClick = {
                                val activityContext = context as? androidx.fragment.app.FragmentActivity
                                if (activityContext != null) {
                                    com.example.security.BiometricHelper.showBiometricPrompt(
                                        activity = activityContext,
                                        title = Localization.getString(lang, "login_biometric_prompt"),
                                        subtitle = Localization.getString(lang, "login_biometric_reason"),
                                        onError = {
                                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                        },
                                        onSuccess = {
                                            viewModel.authenticateBiometrically {
                                                onAuthenticated()
                                            }
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("biometric_login_button")
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = "Biometric", tint = ElectricBlue, modifier = Modifier.size(36.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(onClick = { activity?.finish() }) {
                Text("خروج از برنامه", color = CoralRed, fontSize = 15.sp)
            }
        }
    }
}

// --- DIRECT ATTEMPT SCREEN ---
@Composable
fun DirectAttemptScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val lang = SecurityManager.getLanguage(context)
    val appName = SecurityManager.getCustomAppName(context)
    
    CelestialGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Lock, contentDescription = "Secure", tint = ElectricBlue, modifier = Modifier.size(80.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = appName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = StarWhite
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = Localization.getString(lang, "login_direct_attempt_msg"),
                fontSize = 14.sp,
                color = MoonGlow.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { activity?.finish() },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(Localization.getString(lang, "close_app"))
            }
        }
    }
}

// --- INVALID KEY FILE SCREEN ---
@Composable
fun InvalidKeyFileScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val lang = SecurityManager.getLanguage(context)
    
    CelestialGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.GppBad, contentDescription = "Refused", tint = CoralRed, modifier = Modifier.size(80.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (lang == "en") "Unauthorized Access" else "دسترسی غیرمجاز",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = StarWhite
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = Localization.getString(lang, "login_invalid_key_msg"),
                fontSize = 14.sp,
                color = MoonGlow.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { activity?.finish() },
                colors = ButtonDefaults.buttonColors(containerColor = CoralRed, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(Localization.getString(lang, "close_app"))
            }
        }
    }
}

// --- SECURITY STATUS CARD (M3 Empty Space Visual Hint) ---
@Composable
fun SecurityStatusCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF6750A4), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Shield",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "امنیت فعال است",
                    color = Color(0xFF21005D),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "تمامی فایل‌ها با AES-256 رمزگذاری شده‌اند.",
                    color = Color(0xFF21005D).copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

// --- MAIN GALLERY DASHBOARD SCREEN ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(viewModel: VaultViewModel, onSettingsClicked: () -> Unit) {
    val context = LocalContext.current
    val filesState = viewModel.filteredFiles.collectAsState(initial = emptyList())
    val files = filesState.value
    var isImportSheetOpen by remember { mutableStateOf(false) }
    var fileToView by remember { mutableStateOf<VaultFile?>(null) }
    var isDeleteConfirmOpen by remember { mutableStateOf(false) }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.selectImportFiles(uris)
            isImportSheetOpen = true
        }
    }

    val lang = viewModel.appLanguageSetting

    Scaffold(
        containerColor = MidnightNavy,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CelestialDark),
                title = {
                    Text(viewModel.customLauncherName, color = StarWhite, fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = onSettingsClicked, modifier = Modifier.testTag("settings_button")) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = StarWhite)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { documentPickerLauncher.launch("*/*") },
                containerColor = Color(0xFFD0BCFF),
                contentColor = Color(0xFF21005D),
                modifier = Modifier
                    .padding(8.dp)
                    .testTag("add_file_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add File", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // REACTIVE STORAGE INDICATOR CARD
            val vaultBytes by viewModel.vaultStorageUsage.collectAsState()
            val usableBytes = viewModel.deviceUsableSpace
            val totalBytes = viewModel.deviceTotalSpace
            
            // Total used space on partition
            val partitionUsed = maxOf(0L, totalBytes - usableBytes)
            // The percentage of space used on partition
            val percentUsed = if (totalBytes > 0) ((partitionUsed.toFloat() / totalBytes) * 100).toInt() else 0
            
            val formattedPartitionUsed = FileUtils.formatFileSize(partitionUsed)
            val formattedTotal = FileUtils.formatFileSize(totalBytes)
            
            Card(
                colors = CardDefaults.cardColors(containerColor = CelestialDark),
                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = "Storage",
                                tint = ElectricBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (lang == "fa") "حافظه اشغال شده: $formattedPartitionUsed از $formattedTotal ($percentUsed%)" else "Device Storage Used: $formattedPartitionUsed of $formattedTotal ($percentUsed%)",
                                color = StarWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { if (totalBytes > 0) partitionUsed.toFloat() / totalBytes.toFloat() else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = ElectricBlue,
                        trackColor = ElectricBlue.copy(alpha = 0.2f)
                    )
                }
            }

            // SEARCH & SORT BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.searchQuery = it },
                    placeholder = { Text(Localization.getString(lang, "search_placeholder"), color = MoonGlow.copy(alpha = 0.5f)) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = StarWhite),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MoonGlow) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = ElectricBlue.copy(alpha = 0.4f),
                        unfocusedContainerColor = CelestialDark,
                        focusedContainerColor = CelestialDark
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_field")
                )

                Spacer(modifier = Modifier.width(12.dp))

                var sortMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier
                            .background(CelestialDark, RoundedCornerShape(12.dp))
                            .border(1.dp, ElectricBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .size(52.dp)
                            .testTag("sort_button")
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort", tint = StarWhite)
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        modifier = Modifier.background(CelestialDark)
                    ) {
                        DropdownMenuItem(
                            text = { Text(Localization.getString(lang, "sort_by_date"), color = StarWhite) },
                            onClick = {
                                viewModel.sortBy = "DATE"
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(Localization.getString(lang, "sort_by_name"), color = StarWhite) },
                            onClick = {
                                viewModel.sortBy = "NAME"
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(Localization.getString(lang, "sort_by_size"), color = StarWhite) },
                            onClick = {
                                viewModel.sortBy = "SIZE"
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Security Status banner below search bar
            SecurityStatusCard()

            // SELECTION ACTIONS ROW
            AnimatedVisibility(
                visible = viewModel.selectedFileIds.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepIndigo.copy(alpha = 0.2f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (lang == "en") "${viewModel.selectedFileIds.size} files selected" else "${viewModel.selectedFileIds.size} فایل انتخاب شده",
                        color = StarWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        IconButton(onClick = { viewModel.exportSelectedFiles {
                            Toast.makeText(context, Localization.getString(lang, "export_success"), Toast.LENGTH_SHORT).show()
                        } }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export", tint = StarWhite)
                        }
                        IconButton(onClick = { isDeleteConfirmOpen = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CoralRed)
                        }
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = StarWhite)
                        }
                    }
                }
            }

            // TABS
            val tabs = listOf(
                "Images" to Localization.getString(lang, "tab_images"),
                "Videos" to Localization.getString(lang, "tab_videos"),
                "Documents" to Localization.getString(lang, "tab_documents"),
                "APK" to Localization.getString(lang, "tab_apk"),
                "Archives" to Localization.getString(lang, "tab_archives"),
                "Audio" to Localization.getString(lang, "tab_audio"),
                "Others" to Localization.getString(lang, "tab_others")
            )

            ScrollableTabRow(
                selectedTabIndex = tabs.indexOfFirst { it.first == viewModel.activeTab },
                containerColor = CelestialDark,
                contentColor = ElectricBlue,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEach { (key, title) ->
                    Tab(
                        selected = viewModel.activeTab == key,
                        onClick = { viewModel.activeTab = key },
                        text = { Text(title, fontWeight = FontWeight.Bold) },
                        selectedContentColor = ElectricBlue,
                        unselectedContentColor = MoonGlow.copy(alpha = 0.7f)
                    )
                }
            }

            // GALLERY LIST / GRID
            val currentTabFiles = files.filter { it.category == viewModel.activeTab }

            if (currentTabFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_banner),
                            contentDescription = "Empty",
                            modifier = Modifier
                                .size(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, ElectricBlue.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = Localization.getString(lang, "empty_gallery_title"),
                            color = StarWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Localization.getString(lang, "empty_gallery_desc"),
                            color = MoonGlow.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                if (viewModel.activeTab == "Images") {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(110.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        items(currentTabFiles, key = { it.id }) { file ->
                            val isSelected = viewModel.selectedFileIds.contains(file.id)
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .combinedClickable(
                                        onClick = {
                                            if (viewModel.selectedFileIds.isNotEmpty()) {
                                                viewModel.toggleSelectFile(file.id)
                                            } else {
                                                fileToView = file
                                            }
                                        },
                                        onLongClick = {
                                            viewModel.toggleSelectFile(file.id)
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) ElectricBlue else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                // Decrypt image preview safely using Coil or cached bitmap
                                DecryptedImagePreview(file = file)

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.TopStart
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = ElectricBlue,
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        items(currentTabFiles, key = { it.id }) { file ->
                            val isSelected = viewModel.selectedFileIds.contains(file.id)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CelestialDark),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .combinedClickable(
                                        onClick = {
                                            if (viewModel.selectedFileIds.isNotEmpty()) {
                                                viewModel.toggleSelectFile(file.id)
                                            } else {
                                                fileToView = file
                                            }
                                        },
                                        onLongClick = {
                                            viewModel.toggleSelectFile(file.id)
                                        }
                                    ),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) ElectricBlue else MoonGlow.copy(alpha = 0.2f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (file.category) {
                                            "Videos" -> Icons.Default.PlayCircle
                                            "Audio" -> Icons.Default.AudioFile
                                            "Documents" -> Icons.Default.Description
                                            "APK" -> Icons.Default.Android
                                            "Archives" -> Icons.Default.FolderZip
                                            else -> Icons.Default.InsertDriveFile
                                        },
                                        contentDescription = "File Icon",
                                        tint = ElectricBlue,
                                        modifier = Modifier.size(36.dp)
                                    )

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.originalName,
                                            color = StarWhite,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row {
                                            Text(
                                                text = FileUtils.formatFileSize(file.fileSize),
                                                color = MoonGlow.copy(alpha = 0.6f),
                                                fontSize = 12.sp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(file.addedDate)),
                                                color = MoonGlow.copy(alpha = 0.6f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = ElectricBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // DECRYPT PROGRESS DIALOGS
        if (viewModel.isImporting) {
            Dialog(onDismissRequest = {}) {
                Card(colors = CardDefaults.cardColors(containerColor = CelestialDark)) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = ElectricBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(id = R.string.import_progress, viewModel.importProgress),
                            color = StarWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (viewModel.isExporting) {
            Dialog(onDismissRequest = {}) {
                Card(colors = CardDefaults.cardColors(containerColor = CelestialDark)) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = ElectricBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(id = R.string.export_progress, viewModel.exportProgress),
                            color = StarWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // IMPORT BOTTOM SHEET
        if (viewModel.importUris.isNotEmpty() && isImportSheetOpen) {
            AlertDialog(
                onDismissRequest = { isImportSheetOpen = false },
                containerColor = CelestialDark,
                title = { Text(stringResource(R.string.import_files_title), color = StarWhite) },
                text = {
                    Column {
                        Text(
                            text = "فایل‌های آماده مخفی‌سازی: ${viewModel.importUris.size} فایل",
                            color = MoonGlow
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                id = R.string.import_storage_info,
                                FileUtils.formatFileSize(viewModel.importAvailableStorage),
                                FileUtils.formatFileSize(viewModel.importRequiredStorage)
                            ),
                            color = if (viewModel.importRequiredStorage > viewModel.importAvailableStorage) CoralRed else ElectricBlue,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (viewModel.importRequiredStorage > viewModel.importAvailableStorage) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(id = R.string.import_insufficient_storage), color = CoralRed, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Transfer Options
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = viewModel.importOption == "COPY",
                                onClick = { viewModel.importOption = "COPY" }
                            )
                            Text(stringResource(id = R.string.import_option_copy), color = StarWhite, fontSize = 13.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = viewModel.importOption == "MOVE",
                                onClick = { viewModel.importOption = "MOVE" }
                            )
                            Text(stringResource(id = R.string.import_option_move), color = StarWhite, fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isImportSheetOpen = false
                            viewModel.startImport {
                                Toast.makeText(context, context.getString(R.string.import_success), Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        enabled = viewModel.importRequiredStorage <= viewModel.importAvailableStorage
                    ) {
                        Text("پنهان‌سازی")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isImportSheetOpen = false }) {
                        Text(stringResource(R.string.cancel), color = StarWhite)
                    }
                }
            )
        }

        // DELETE CONFIRMATION DIALOG
        if (isDeleteConfirmOpen) {
            AlertDialog(
                onDismissRequest = { isDeleteConfirmOpen = false },
                containerColor = CelestialDark,
                title = { Text(stringResource(R.string.delete_confirm_title), color = CoralRed) },
                text = { Text(stringResource(R.string.delete_confirm_desc), color = StarWhite) },
                confirmButton = {
                    Button(
                        onClick = {
                            isDeleteConfirmOpen = false
                            viewModel.deleteSelectedFiles()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                    ) {
                        Text(stringResource(R.string.delete_button))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isDeleteConfirmOpen = false }) {
                        Text(stringResource(R.string.cancel), color = StarWhite)
                    }
                }
            )
        }

        // FULL SCREEN MEDIA VIEWER DIALOG
        if (fileToView != null) {
            MediaViewerDialog(
                file = fileToView!!,
                onDismiss = { fileToView = null }
            )
        }
    }
}

// --- THUMBNAIL CACHE & DOWNSAMPLING HELPERS ---
object ThumbnailCache {
    val cache = android.util.LruCache<String, android.graphics.Bitmap>(120)
}

fun calculateInSampleSize(options: android.graphics.BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

// --- SAFE DECRYPTED IMAGE PREVIEW FOR GRID ITEMS ---
@Composable
fun DecryptedImagePreview(file: VaultFile) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(ThumbnailCache.cache.get(file.encryptedFileName)) }

    LaunchedEffect(file) {
        if (bitmap != null) return@LaunchedEffect // Skip decoding if already cached
        
        withContext(Dispatchers.IO) {
            try {
                val encFile = File(context.filesDir, file.encryptedFileName)
                if (encFile.exists()) {
                    val decryptedBytes = java.io.ByteArrayOutputStream()
                    encFile.inputStream().use { inputStream ->
                        CryptographyManager.decrypt(inputStream, decryptedBytes)
                    }
                    val byteArray = decryptedBytes.toByteArray()
                    
                    // Downsample options to avoid OOM and speed up rendering
                    val options = android.graphics.BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    android.graphics.BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
                    
                    options.inSampleSize = calculateInSampleSize(options, 250, 250)
                    options.inJustDecodeBounds = false
                    
                    val decoded = android.graphics.BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
                    if (decoded != null) {
                        ThumbnailCache.cache.put(file.encryptedFileName, decoded)
                        bitmap = decoded
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = file.originalName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CelestialDark),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = ElectricBlue, modifier = Modifier.size(24.dp))
        }
    }
}

// --- DETAILS / MEDIA PLAYERS FULLSCREEN VIEWER ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerDialog(
    file: VaultFile,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var decryptedFileState by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            try {
                // Decrypt to temp secure cache file for active playing/viewing
                val encFile = File(context.filesDir, file.encryptedFileName)
                if (encFile.exists()) {
                    val tempFile = File(context.cacheDir, "temp_view_${file.originalName}")
                    tempFile.outputStream().use { output ->
                        encFile.inputStream().use { input ->
                            CryptographyManager.decrypt(input, output)
                        }
                    }
                    decryptedFileState = tempFile
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    DisposableEffect(file) {
        onDispose {
            // Delete temp file for security when viewer closes
            try {
                val tempFile = File(context.cacheDir, "temp_view_${file.originalName}")
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CelestialGradientBackground {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                        title = {
                            Text(file.originalName, color = StarWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = StarWhite)
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    if (decryptedFileState == null) {
                        CircularProgressIndicator(color = ElectricBlue)
                    } else {
                        val tempFile = decryptedFileState!!
                        when (file.category) {
                            "Images" -> {
                                ZoomableImage(file = tempFile)
                            }
                            "Videos" -> {
                                VideoPlayerView(file = tempFile)
                            }
                            "Audio" -> {
                                AudioPlayerView(file = tempFile, name = file.originalName)
                            }
                            "APK" -> {
                                ApkInfoView(file = tempFile, context = context)
                            }
                            else -> {
                                GenericFileInfoView(file = file, tempFile = tempFile, context = context)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- ZOOMABLE IMAGE WITH GESTURES ---
@Composable
fun ZoomableImage(file: File) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset += offsetChange
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    if (scale > 1f) {
                        scale = 1f
                        offset = androidx.compose.ui.geometry.Offset.Zero
                    } else {
                        scale = 3f
                    }
                })
            }
            .transformable(state = transformableState)
    ) {
        AsyncImage(
            model = file,
            contentDescription = "Zoomed Image",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.Fit
        )
    }
}

// --- VIDEO PLAYER USING ANDROID VIEW ---
@Composable
fun VideoPlayerView(file: File) {
    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                val mediaController = MediaController(ctx)
                mediaController.setAnchorView(this)
                setMediaController(mediaController)
                setVideoURI(Uri.fromFile(file))
                setOnPreparedListener {
                    start()
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16 / 9f)
    )
}

// --- AUDIO PLAYER WITH BUILT-IN PLAYER ---
@Composable
fun AudioPlayerView(file: File, name: String) {
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var duration by remember { mutableStateOf(0) }
    var currentPosition by remember { mutableStateOf(0) }

    LaunchedEffect(file) {
        val player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            duration = this.duration
        }
        mediaPlayer = player

        // Poll position
        while (true) {
            currentPosition = player.currentPosition
            isPlaying = player.isPlaying
            kotlinx.coroutines.delay(500)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CelestialDark),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .border(1.dp, ElectricBlue.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = "Music", tint = ElectricBlue, modifier = Modifier.size(72.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(name, color = StarWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))

            Slider(
                value = currentPosition.toFloat(),
                onValueChange = {
                    mediaPlayer?.seekTo(it.toInt())
                },
                valueRange = 0f..duration.toFloat(),
                colors = SliderDefaults.colors(
                    activeTrackColor = ElectricBlue,
                    thumbColor = ElectricBlue
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(currentPosition), color = MoonGlow.copy(alpha = 0.6f), fontSize = 12.sp)
                Text(formatTime(duration), color = MoonGlow.copy(alpha = 0.6f), fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            IconButton(
                onClick = {
                    mediaPlayer?.let {
                        if (it.isPlaying) {
                            it.pause()
                            isPlaying = false
                        } else {
                            it.start()
                            isPlaying = true
                        }
                    }
                },
                modifier = Modifier
                    .background(ElectricBlue, CircleShape)
                    .size(64.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = StarWhite,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

private fun formatTime(millis: Int): String {
    val sec = (millis / 1000) % 60
    val min = (millis / 1000) / 60
    return String.format("%02d:%02d", min, sec)
}

// --- APK INFO AND SECURE INSTALL ---
@Composable
fun ApkInfoView(file: File, context: Context) {
    val pm = context.packageManager
    val info = pm.getPackageArchiveInfo(file.absolutePath, 0)
    var appLabel = ""
    var packageName = ""
    var version = ""
    var iconDrawable: Drawable? = null

    if (info != null) {
        info.applicationInfo?.sourceDir = file.absolutePath
        info.applicationInfo?.publicSourceDir = file.absolutePath
        appLabel = info.applicationInfo?.loadLabel(pm)?.toString() ?: ""
        packageName = info.packageName ?: ""
        version = info.versionName ?: ""
        try {
            iconDrawable = info.applicationInfo?.loadIcon(pm)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CelestialDark),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .border(1.dp, ElectricBlue.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (iconDrawable != null) {
                Image(
                    bitmap = iconDrawable.toBitmap().asImageBitmap(),
                    contentDescription = "Apk Icon",
                    modifier = Modifier.size(80.dp)
                )
            } else {
                Icon(Icons.Default.Android, contentDescription = "Apk", tint = ElectricBlue, modifier = Modifier.size(80.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(appLabel.ifEmpty { file.name }, color = StarWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("بسته: $packageName", color = MoonGlow.copy(alpha = 0.7f), fontSize = 13.sp)
            Text("نسخه: $version", color = MoonGlow.copy(alpha = 0.7f), fontSize = 13.sp)

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    try {
                        val apkUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(apkUri, "application/vnd.android.package-archive")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "امکان نصب وجود ندارد! مطمئن شوید که دسترسی به منابع ناشناخته را داده‌اید.", Toast.LENGTH_LONG).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.InstallMobile, contentDescription = "Install")
                Spacer(modifier = Modifier.width(8.dp))
                Text("نصب برنامه", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- GENERIC FILE INFO & EXPORT / SHARE VIEWER ---
@Composable
fun GenericFileInfoView(file: VaultFile, tempFile: File, context: Context) {
    var isTextFile by remember { mutableStateOf(false) }
    var textContent by remember { mutableStateOf("") }

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            val extension = FileUtils.getExtension(file.originalName)
            if (extension == "txt" && tempFile.length() < 100 * 1024) { // only load text file < 100KB
                try {
                    textContent = tempFile.readText()
                    isTextFile = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CelestialDark),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .border(1.dp, ElectricBlue.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Description, contentDescription = "File", tint = ElectricBlue, modifier = Modifier.size(72.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(file.originalName, color = StarWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(6.dp))
            Text("اندازه: ${FileUtils.formatFileSize(file.fileSize)}", color = MoonGlow.copy(alpha = 0.7f), fontSize = 13.sp)

            Spacer(modifier = Modifier.height(16.dp))

            if (isTextFile) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MidnightNavy),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        textContent,
                        color = MoonGlow,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Right
                    )
                }
            } else {
                Text(
                    text = "پیش‌نمایش برای این نوع فایل در دسترس نیست.",
                    color = MoonGlow.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(
                    onClick = {
                        try {
                            val fileUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                tempFile
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = file.mimeType
                                putExtra(Intent.EXTRA_STREAM, fileUri)
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری با"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "خطا در اشتراک‌گذاری فایل", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("اشتراک‌گذاری")
                }
            }
        }
    }
}

// --- SETTINGS VIEW ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: VaultViewModel, onBackClicked: () -> Unit) {
    val context = LocalContext.current
    var isChangePasswordOpen by remember { mutableStateOf(false) }
    var isAboutOpen by remember { mutableStateOf(false) }
    val lang = viewModel.appLanguageSetting

    val keyFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val name = FileUtils.getFileName(context, uri)
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val bytes = inputStream.readBytes()
                    val hash = SecurityManager.hashBytes(bytes)
                    viewModel.updateSecretKeyFile(hash, name)
                    val successMsg = Localization.getString(lang, "change_key_file_success")
                    Toast.makeText(context, successMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading key file", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        containerColor = MidnightNavy,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CelestialDark),
                title = {
                    Text(Localization.getString(lang, "settings_title"), color = StarWhite, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = StarWhite)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // General Settings Card
            Text(Localization.getString(lang, "settings_general"), color = ElectricBlue, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = CelestialDark),
                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Prevent Screenshots
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(Localization.getString(lang, "settings_prevent_screenshots"), color = StarWhite, fontWeight = FontWeight.Bold)
                            Text(Localization.getString(lang, "settings_prevent_screenshots_desc"), color = MoonGlow.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                        Switch(
                            checked = viewModel.preventScreenshots,
                            onCheckedChange = { viewModel.updatePreventScreenshots(it) },
                            modifier = Modifier.testTag("prevent_screenshots_switch")
                        )
                    }

                    Divider(color = ElectricBlue.copy(alpha = 0.2f))

                    // Prevent Recordings
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(Localization.getString(lang, "settings_prevent_recordings"), color = StarWhite, fontWeight = FontWeight.Bold)
                            Text(Localization.getString(lang, "settings_prevent_recordings_desc"), color = MoonGlow.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                        Switch(
                            checked = viewModel.preventRecordings,
                            onCheckedChange = { viewModel.updatePreventRecordings(it) },
                            modifier = Modifier.testTag("prevent_recordings_switch")
                        )
                    }

                    Divider(color = ElectricBlue.copy(alpha = 0.2f))

                    // Biometrics Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(Localization.getString(lang, "settings_biometrics"), color = StarWhite, fontWeight = FontWeight.Bold)
                            Text(Localization.getString(lang, "settings_biometrics_desc"), color = MoonGlow.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                        Switch(
                            checked = viewModel.biometricSettingEnabled,
                            onCheckedChange = { viewModel.updateBiometricSetting(it) },
                            modifier = Modifier.testTag("settings_biometric_switch")
                        )
                    }
                }
            }

            // Security Settings Card
            Text(Localization.getString(lang, "settings_security"), color = ElectricBlue, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = CelestialDark),
                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Change Password
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isChangePasswordOpen = true }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Pass", tint = ElectricBlue, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(Localization.getString(lang, "settings_change_password"), color = StarWhite, fontWeight = FontWeight.Bold)
                            Text(Localization.getString(lang, "settings_change_password_desc"), color = MoonGlow.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                    }

                    Divider(color = ElectricBlue.copy(alpha = 0.2f))

                    // Lock Timeout Minutes Selection
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(Localization.getString(lang, "settings_lock_timeout"), color = StarWhite, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(1, 5, 30).forEach { mins ->
                                val label = when (mins) {
                                    1 -> Localization.getString(lang, "settings_lock_timeout_1m")
                                    5 -> Localization.getString(lang, "settings_lock_timeout_5m")
                                    else -> Localization.getString(lang, "settings_lock_timeout_30m")
                                }
                                FilterChip(
                                    selected = viewModel.lockTimeoutMinutes == mins,
                                    onClick = { viewModel.updateLockTimeout(mins) },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricBlue,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Customize Theme & Icon Card
            Text(Localization.getString(lang, "settings_customization"), color = ElectricBlue, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = CelestialDark),
                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Theme Choice
                    Text(Localization.getString(lang, "settings_theme"), color = StarWhite, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("system", "light", "dark").forEach { theme ->
                            val label = when (theme) {
                                "light" -> Localization.getString(lang, "settings_theme_light")
                                "dark" -> Localization.getString(lang, "settings_theme_dark")
                                else -> Localization.getString(lang, "settings_theme_system")
                            }
                            FilterChip(
                                selected = viewModel.appThemeSetting == theme,
                                onClick = { viewModel.updateTheme(theme) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Divider(color = ElectricBlue.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

                    // Language Selection Block
                    Text(Localization.getString(lang, "lang_label"), color = StarWhite, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("fa" to Localization.getString(lang, "lang_fa"), "en" to Localization.getString(lang, "lang_en")).forEach { (code, label) ->
                            FilterChip(
                                selected = viewModel.appLanguageSetting == code,
                                onClick = { viewModel.updateLanguage(code) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Divider(color = ElectricBlue.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

                    // App Camouflage (Icon and Name Selector)
                    var tempAlias by remember { mutableStateOf(viewModel.selectedLauncherAlias) }
                    
                    Text(if (lang == "fa") "انتخاب ظاهر استتاری (آیکون و نام لانچر)" else "Choose Launcher Identity (Icon & Label)", color = StarWhite, fontWeight = FontWeight.Bold)
                    Text(if (lang == "fa") "یک ظاهر استتاری برای پنهان کردن برنامه از لانچر انتخاب کرده و گزینه ذخیره را بزنید." else "Select a camouflage appearance to disguise the app in your launcher, then click Save.", color = MoonGlow.copy(alpha = 0.6f), fontSize = 12.sp)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val aliasOptions = listOf(
                        Triple("com.example.MainActivityDefault", if (lang == "fa") "صندوقچه LUF (Vault)" else "LUF Vault (Default)", Icons.Default.Shield),
                        Triple("com.example.MainActivityCalculator", if (lang == "fa") "ماشین حساب (Calculator)" else "Calculator", Icons.Default.Calculate),
                        Triple("com.example.MainActivityWeather", if (lang == "fa") "هواشناسی (Weather)" else "Weather", Icons.Default.WbSunny),
                        Triple("com.example.MainActivityNotes", if (lang == "fa") "یادداشت‌ها (Notes)" else "Notes", Icons.Default.Description)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        aliasOptions.forEach { (alias, name, icon) ->
                            val isSelected = tempAlias == alias
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) ElectricBlue.copy(alpha = 0.15f) else Color.Transparent)
                                    .border(1.dp, if (isSelected) ElectricBlue else ElectricBlue.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .clickable { tempAlias = alias }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, contentDescription = name, tint = if (isSelected) ElectricBlue else MoonGlow, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(name, color = StarWhite, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.updateSelectedLauncherAlias(tempAlias)
                            val cleanName = when (tempAlias) {
                                "com.example.MainActivityDefault" -> "صندوقچه LUF"
                                "com.example.MainActivityCalculator" -> if (lang == "fa") "ماشین حساب" else "Calculator"
                                "com.example.MainActivityWeather" -> if (lang == "fa") "هواشناسی" else "Weather"
                                else -> if (lang == "fa") "یادداشت‌ها" else "Notes"
                            }
                            viewModel.updateCustomLauncherName(cleanName)
                            
                            val msg = if (lang == "fa") 
                                "تغییرات با موفقیت ذخیره شد! آیکون به عنوان $cleanName ذخیره گردید." 
                            else 
                                "Camouflage saved successfully! Disguised as $cleanName."
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(if (lang == "fa") "ذخیره تغییرات" else "Save Camouflage", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Divider(color = ElectricBlue.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

                    // Change Key File Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { keyFilePickerLauncher.launch("*/*") }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Key, contentDescription = "Key File", tint = ElectricBlue, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(Localization.getString(lang, "change_key_file"), color = StarWhite, fontWeight = FontWeight.Bold)
                            Text(Localization.getString(lang, "change_key_file_desc") + " (${viewModel.customLauncherName})", color = MoonGlow.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                    }
                }
            }

            // Developer Honors Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CelestialDark),
                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = Localization.getString(lang, "made_by"),
                        color = StarWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/thats-luffy"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "GitHub"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Localization.getString(lang, "visit_github"),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // About Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CelestialDark),
                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isAboutOpen = true }
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = "About", tint = ElectricBlue, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(Localization.getString(lang, "settings_about"), color = StarWhite, fontWeight = FontWeight.Bold)
                        Text(Localization.getString(lang, "settings_about_desc"), color = MoonGlow.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }
            }
        }

        // ABOUT DIALOG
        if (isAboutOpen) {
            AlertDialog(
                onDismissRequest = { isAboutOpen = false },
                containerColor = CelestialDark,
                title = { Text(Localization.getString(lang, "settings_about"), color = StarWhite) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.img_app_icon),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .border(1.dp, ElectricBlue, CircleShape)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(viewModel.customLauncherName + " v1.0", color = StarWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Localization.getString(lang, "app_info_desc"),
                            color = MoonGlow,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = Localization.getString(lang, "lang_label") + ": " + (if (lang == "fa") "فارسی 🇮🇷" else "English 🇺🇸"),
                            color = ElectricBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { isAboutOpen = false }, colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)) {
                        Text(if (lang == "en") "OK" else "تایید")
                    }
                }
            )
        }

        // CHANGE PASSWORD DIALOG
        if (isChangePasswordOpen) {
            var newPass by remember { mutableStateOf("") }
            var newPassConfirm by remember { mutableStateOf("") }
            var errorMsg by remember { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = { isChangePasswordOpen = false },
                containerColor = CelestialDark,
                title = { Text(Localization.getString(lang, "settings_change_password"), color = StarWhite) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newPass,
                            onValueChange = { newPass = it },
                            label = { Text(if (lang == "en") "New Password (min 6 characters)" else "رمز عبور جدید (حداقل ۶ کاراکتر)") },
                            visualTransformation = PasswordVisualTransformation(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = StarWhite),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = ElectricBlue.copy(alpha = 0.4f),
                                focusedLabelColor = ElectricBlue,
                                unfocusedLabelColor = MoonGlow
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newPassConfirm,
                            onValueChange = { newPassConfirm = it },
                            label = { Text(if (lang == "en") "Confirm New Password" else "تکرار رمز عبور جدید") },
                            visualTransformation = PasswordVisualTransformation(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = StarWhite),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = ElectricBlue.copy(alpha = 0.4f),
                                focusedLabelColor = ElectricBlue,
                                unfocusedLabelColor = MoonGlow
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (errorMsg != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(errorMsg!!, color = CoralRed, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPass.length < 6) {
                                errorMsg = if (lang == "en") "Password must be at least 6 characters" else "رمز عبور باید حداقل ۶ کاراکتر باشد"
                            } else if (newPass != newPassConfirm) {
                                errorMsg = if (lang == "en") "Passwords do not match" else "رمز عبور با تکرار آن مطابقت ندارد"
                            } else {
                                viewModel.changePassword(newPass)
                                isChangePasswordOpen = false
                                Toast.makeText(context, if (lang == "en") "Password changed successfully" else "رمز عبور با موفقیت تغییر یافت", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text(if (lang == "en") "Save" else "ذخیره")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isChangePasswordOpen = false }) {
                        Text(Localization.getString(lang, "cancel"), color = StarWhite)
                    }
                }
            )
        }
    }
}
