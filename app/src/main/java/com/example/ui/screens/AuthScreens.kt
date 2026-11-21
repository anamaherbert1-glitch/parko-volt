package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.SeedData
import com.example.ui.components.ParkoLogoBadge
import com.example.ui.components.parkoAtmosphericBackground
import com.example.ui.components.parkoCardOutline
import com.example.ui.components.parkoTextFieldColors
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.tr
import com.example.ui.theme.*
import com.example.ui.viewmodel.ParkoViewModel
import com.example.util.ParkoSecurityConfig

enum class AuthStep { WELCOME, LOGIN, REGISTER }

@Composable
fun AuthFlow(
    viewModel: ParkoViewModel
) {
    var step by remember { mutableStateOf(AuthStep.WELCOME) }
    var showForgotDialog by remember { mutableStateOf(false) }
    val currentLang = LocalAppLanguage.current

    BackHandler(enabled = step != AuthStep.WELCOME) {
        step = AuthStep.WELCOME
    }

    val toggleLanguage = {
        val next = if (currentLang == AppLanguage.FR) AppLanguage.EN.storageValue else AppLanguage.FR.storageValue
        viewModel.setLanguage(next)
    }

    when (step) {
        AuthStep.WELCOME -> WelcomeScreen(
            onLoginClick = { step = AuthStep.LOGIN },
            onRegisterClick = { step = AuthStep.REGISTER },
            onDemoUserClick = { viewModel.loginDemo(SeedData.DEMO_USER_ID) },
            onDemoClientClick = { viewModel.loginDemo(SeedData.CLIENT_USER_ID) },
            onDemoAdminClick = { viewModel.loginDemo(SeedData.ADMIN_USER_ID) },
            onToggleLanguage = toggleLanguage
        )
        AuthStep.LOGIN -> LoginScreen(
            onBack = { step = AuthStep.WELCOME },
            onSubmit = { id, pass, remember ->
                viewModel.login(id, pass, remember) {}
            },
            onForgotClick = { showForgotDialog = true },
            onGoRegister = { step = AuthStep.REGISTER },
            onDemoClick = { viewModel.loginDemo(SeedData.DEMO_USER_ID) },
            onToggleLanguage = toggleLanguage
        )
        AuthStep.REGISTER -> RegisterScreen(
            onBack = { step = AuthStep.WELCOME },
            onSubmit = { first, last, phone, email, pass, confirm ->
                viewModel.register(first, last, phone, email, pass, confirm) {}
            },
            onGoLogin = { step = AuthStep.LOGIN }
        )
    }

    if (showForgotDialog) {
        ForgotPasswordDialog(
            onDismiss = { showForgotDialog = false },
            onReset = { phone, otpCode, newPass ->
                viewModel.resetPassword(phone, newPass, otpCode) {
                    showForgotDialog = false
                }
            }
        )
    }
}

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onDemoUserClick: () -> Unit,
    onDemoClientClick: () -> Unit,
    onDemoAdminClick: () -> Unit,
    onToggleLanguage: () -> Unit
) {
    val lang = LocalAppLanguage.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .parkoAtmosphericBackground()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_welcome_hero),
                contentDescription = tr("PARKO mobilité urbaine", "PARKO urban mobility"),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                ParkoBlueDark.copy(alpha = 0.88f),
                                ParkoBlue.copy(alpha = 0.50f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    onClick = onToggleLanguage,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.24f),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("welcome_lang_toggle")
                ) {
                    Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (lang == AppLanguage.FR) "FR / EN" else "EN / FR",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ParkoLogoBadge(size = 66.dp, inverted = true)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "PARKO",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = tr("Le parking en toute confiance", "Parking with total confidence"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = tr("Bienvenue sur PARKO", "Welcome to PARKO"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = painterResource(id = R.drawable.img_onboarding_moto),
                contentDescription = tr("Stationnement sécurisé PARKO", "PARKO secure parking"),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .parkoCardOutline(22.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("welcome_login_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(tr("Se connecter", "Sign in"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onRegisterClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("welcome_register_button"),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(tr("Créer un compte", "Create an account"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }

            if (ParkoSecurityConfig.isDemoModeEnabled) {
                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = tr("Mode Test / Démo (désactivé en production)", "Test / Demo Mode (disabled in production)"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onDemoClientClick,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("demo_codjo_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(tr("Client", "Client"), maxLines = 1, fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(
                        onClick = onDemoUserClick,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("demo_amadou_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(tr("Gestionnaire", "Manager"), maxLines = 1, fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(
                        onClick = onDemoAdminClick,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("demo_admin_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Admin", maxLines = 1, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onSubmit: (String, String, Boolean) -> Unit,
    onForgotClick: () -> Unit,
    onGoRegister: () -> Unit,
    onDemoClick: () -> Unit,
    onToggleLanguage: () -> Unit
) {
    val lang = LocalAppLanguage.current
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .parkoAtmosphericBackground()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Retour", "Back"), modifier = Modifier.size(26.dp))
            }
            TextButton(onClick = onToggleLanguage) {
                Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (lang == AppLanguage.FR) "English" else "Français", fontWeight = FontWeight.Bold)
            }
        }
        Column(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ParkoLogoBadge(size = 68.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(tr("Connexion", "Sign in"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(22.dp))

            OutlinedTextField(
                value = identifier,
                onValueChange = { identifier = it },
                label = { Text(tr("Téléphone (+228...) ou e-mail", "Phone (+228...) or email")) },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(24.dp)) },
                singleLine = true,
                colors = parkoTextFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag("login_identifier_input"),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(tr("Mot de passe", "Password")) },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(24.dp)) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = tr("Afficher ou masquer le mot de passe", "Show or hide password"),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                colors = parkoTextFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag("login_password_input"),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                    Text(tr("Se souvenir de moi", "Remember me"), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onForgotClick) {
                    Text(tr("Mot de passe oublié ?", "Forgot password?"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onSubmit(identifier, password, rememberMe) },
                modifier = Modifier.fillMaxWidth().height(54.dp).testTag("login_submit_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(tr("Se connecter", "Sign in"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }

            if (ParkoSecurityConfig.isDemoModeEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onDemoClick,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("demo_instant_login_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Bolt, contentDescription = null, tint = ParkoBlue, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(tr("Mode Test : Amadou Diallo", "Test Mode: Amadou Diallo"), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onGoRegister) {
                Text(tr("Pas encore de compte ? Créer un compte", "No account yet? Create one"), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onSubmit: (String, String, String, String, String, String) -> Unit,
    onGoLogin: () -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+228 ") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .parkoAtmosphericBackground()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Retour", "Back"), modifier = Modifier.size(26.dp))
            }
            Text(tr("Créer un compte", "Create an account"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text(tr("Prénom *", "First name *")) },
                singleLine = true,
                colors = parkoTextFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag("register_firstname_input"),
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text(tr("Nom *", "Last name *")) },
                singleLine = true,
                colors = parkoTextFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag("register_lastname_input"),
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(tr("Téléphone (Togo) *", "Phone (Togo) *")) },
                placeholder = { Text("+228 90 12 34 56") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                colors = parkoTextFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag("register_phone_input"),
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(tr("Adresse e-mail (optionnel)", "Email address (optional)")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                colors = parkoTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(tr("Mot de passe (min. 6 caractères) *", "Password (min. 6 chars) *")) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                colors = parkoTextFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag("register_password_input"),
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text(tr("Confirmer le mot de passe *", "Confirm password *")) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                colors = parkoTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = { onSubmit(firstName, lastName, phone, email, password, confirmPassword) },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("register_submit_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(tr("Valider l'inscription", "Complete sign up"))
            }
            TextButton(onClick = onGoLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(tr("Déjà inscrit ? Se connecter", "Already registered? Sign in"))
            }
        }
    }
}

@Composable
fun ForgotPasswordDialog(
    onDismiss: () -> Unit,
    onReset: (String, String, String) -> Unit
) {
    var phone by remember { mutableStateOf("+228 ") }
    var otpCode by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Récupération du compte", "Account recovery")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (ParkoSecurityConfig.isDemoModeEnabled) {
                        tr(
                            "Mode Test : saisissez votre numéro togolais et le code de vérification SMS (1234 en mode test local).",
                            "Test Mode: enter your Togo phone number and SMS verification code (1234 in local test mode)."
                        )
                    } else {
                        tr(
                            "La réinitialisation du mot de passe nécessite un service SMS vérifié. Contactez le support PARKO en mode hors-ligne.",
                            "Password reset requires a verified SMS service. Contact PARKO support in offline mode."
                        )
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(tr("Numéro de téléphone", "Phone number")) },
                    singleLine = true,
                    colors = parkoTextFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { otpCode = it },
                    label = { Text(tr("Code de vérification SMS", "SMS verification code")) },
                    singleLine = true,
                    colors = parkoTextFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it },
                    label = { Text(tr("Nouveau mot de passe", "New password")) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = parkoTextFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onReset(phone, otpCode, newPass) }
            ) {
                Text(tr("Réinitialiser", "Reset"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("Annuler", "Cancel")) }
        }
    )
}
