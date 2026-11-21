package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ParkingSessionEntity
import com.example.data.model.AppSpace
import com.example.data.model.PaymentMethod
import com.example.data.model.SessionStatus
import com.example.data.model.SubscriptionTier
import com.example.data.model.ThemeMode
import com.example.data.model.UserRole
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.tr
import com.example.ui.screens.*
import com.example.ui.theme.ParkoBlue
import com.example.ui.theme.ParkoBlueDark
import com.example.ui.theme.ParkoBlueLight
import com.example.ui.theme.ParkoSlate
import com.example.ui.viewmodel.ParkoViewModel

private enum class PersonalTab { HOME, TICKETS, HISTORY, PROFILE }
private enum class PersonalOverlay {
    NONE, VEHICLES, NOTIFICATIONS, TICKET_DETAIL,
    BECOME_MANAGER, SUBSCRIPTIONS, SUB_PAYMENT
}

private enum class ProTab { DASHBOARD, REGISTER, SCANNER, HISTORY }
private enum class ProOverlay {
    NONE, VERIFY_SESSION, TICKET_GENERATED, REPORTS, ATTENDANTS, INCIDENTS, SUBSCRIPTIONS, SUB_PAYMENT
}

@Composable
fun ParkoApp(viewModel: ParkoViewModel) {
    val isReady by viewModel.isReady.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val activeSpace by viewModel.activeSpace.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val appLanguage = remember(language) { AppLanguage.fromStorage(language) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    CompositionLocalProvider(LocalAppLanguage provides appLanguage) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (!isReady) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (currentUser == null) {
                    AuthFlow(viewModel = viewModel)
                } else {
                val user = currentUser!!
                key(user.id) {
                    when (activeSpace) {
                        AppSpace.PERSONAL -> PersonalSpaceShell(viewModel = viewModel, user = user)
                        AppSpace.PRO -> ProSpaceShell(viewModel = viewModel, user = user)
                        AppSpace.ADMIN -> {
                            LaunchedEffect(Unit) {
                                viewModel.touchProSession()
                            }
                            val users by viewModel.allUsers.collectAsStateWithLifecycle()
                            val parkings by viewModel.allParkings.collectAsStateWithLifecycle()
                            val attendants by viewModel.allAttendants.collectAsStateWithLifecycle()
                            val sessions by viewModel.allSessions.collectAsStateWithLifecycle()
                            val payments by viewModel.allPayments.collectAsStateWithLifecycle()
                            val applications by viewModel.allApplications.collectAsStateWithLifecycle()
                            val incidents by viewModel.allIncidents.collectAsStateWithLifecycle()
                            val auditLogs by viewModel.allAuditLogs.collectAsStateWithLifecycle()

                            AdminDashboardScreen(
                                adminUser = user,
                                users = users,
                                parkings = parkings,
                                attendants = attendants,
                                sessions = sessions,
                                payments = payments,
                                applications = applications,
                                incidents = incidents,
                                auditLogs = auditLogs,
                                onReviewApplication = viewModel::reviewApplication,
                                onConfirmSubPayment = viewModel::confirmSubscriptionPayment,
                                onToggleParkingSuspension = viewModel::toggleParkingSuspension,
                                onToggleUserSuspension = viewModel::toggleUserSuspension,
                                onSwitchToPersonal = { viewModel.switchSpace(AppSpace.PERSONAL) },
                                onLogout = viewModel::logout
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun PersonalSpaceShell(
    viewModel: ParkoViewModel,
    user: com.example.data.local.UserEntity
) {
    val vehicles by viewModel.userVehicles.collectAsStateWithLifecycle()
    val sessions by viewModel.userSessions.collectAsStateWithLifecycle()
    val notifications by viewModel.userNotifications.collectAsStateWithLifecycle()
    val application by viewModel.userApplication.collectAsStateWithLifecycle()
    val activeParking by viewModel.activeParking.collectAsStateWithLifecycle()
    val canAccessPro by viewModel.canAccessProSpace.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(PersonalTab.HOME) }
    var overlay by remember { mutableStateOf(PersonalOverlay.NONE) }
    var selectedTicket by remember { mutableStateOf<ParkingSessionEntity?>(null) }
    var selectedTier by remember { mutableStateOf(SubscriptionTier.STANDARD) }

    BackHandler(enabled = overlay != PersonalOverlay.NONE || currentTab != PersonalTab.HOME) {
        if (overlay != PersonalOverlay.NONE) {
            overlay = when (overlay) {
                PersonalOverlay.SUB_PAYMENT -> PersonalOverlay.SUBSCRIPTIONS
                else -> PersonalOverlay.NONE
            }
        } else {
            currentTab = PersonalTab.HOME
        }
    }

    val unreadCount = notifications.count { !it.isRead }
    val activeTicketsCount = sessions.count {
        it.status == SessionStatus.ACTIVE.name ||
            it.status == SessionStatus.EXIT_PENDING.name ||
            it.status == SessionStatus.NEEDS_VERIFICATION.name
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (overlay == PersonalOverlay.NONE) {
                val isDark = MaterialTheme.colorScheme.background.luminance() < 0.3f
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = if (isDark) Color.White else ParkoBlueDark,
                    selectedTextColor = if (isDark) Color.White else ParkoBlueDark,
                    indicatorColor = if (isDark) ParkoBlue.copy(alpha = 0.34f) else ParkoBlueLight,
                    unselectedIconColor = if (isDark) Color(0xFF94A3B8) else ParkoSlate,
                    unselectedTextColor = if (isDark) Color(0xFF94A3B8) else ParkoSlate
                )
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        NavigationBarItem(
                            selected = currentTab == PersonalTab.HOME,
                            onClick = { currentTab = PersonalTab.HOME },
                            icon = { Icon(Icons.Filled.Home, contentDescription = tr("Accueil", "Home"), modifier = Modifier.size(26.dp)) },
                            label = { Text(tr("Accueil", "Home"), fontWeight = FontWeight.Bold) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("nav_personal_home")
                        )
                        NavigationBarItem(
                            selected = currentTab == PersonalTab.TICKETS,
                            onClick = { currentTab = PersonalTab.TICKETS },
                            icon = { Icon(Icons.Filled.QrCode2, contentDescription = tr("Tickets", "Tickets"), modifier = Modifier.size(26.dp)) },
                            label = { Text(tr("Tickets", "Tickets"), fontWeight = FontWeight.Bold) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("nav_personal_tickets")
                        )
                        NavigationBarItem(
                            selected = currentTab == PersonalTab.HISTORY,
                            onClick = { currentTab = PersonalTab.HISTORY },
                            icon = { Icon(Icons.Filled.History, contentDescription = tr("Historique", "History"), modifier = Modifier.size(26.dp)) },
                            label = { Text(tr("Historique", "History"), fontWeight = FontWeight.Bold) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("nav_personal_history")
                        )
                        NavigationBarItem(
                            selected = currentTab == PersonalTab.PROFILE,
                            onClick = { currentTab = PersonalTab.PROFILE },
                            icon = { Icon(Icons.Filled.Person, contentDescription = tr("Profil", "Profile"), modifier = Modifier.size(26.dp)) },
                            label = { Text(tr("Profil", "Profile"), fontWeight = FontWeight.Bold) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("nav_personal_profile")
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (overlay) {
                PersonalOverlay.VEHICLES -> VehiclesScreen(
                    vehicles = vehicles,
                    onBack = { overlay = PersonalOverlay.NONE },
                    onSaveVehicle = viewModel::saveVehicle,
                    onDeleteVehicle = viewModel::deleteVehicle
                )
                PersonalOverlay.NOTIFICATIONS -> NotificationsScreen(
                    notifications = notifications,
                    onBack = { overlay = PersonalOverlay.NONE },
                    onMarkAllRead = viewModel::markNotificationsRead
                )
                PersonalOverlay.TICKET_DETAIL -> {
                    val ticket = selectedTicket
                    if (ticket != null) {
                        TicketDetailScreen(
                            session = ticket,
                            isProSender = false,
                            onBack = { overlay = PersonalOverlay.NONE },
                            onSavedNotice = viewModel::showMessage
                        )
                    } else {
                        overlay = PersonalOverlay.NONE
                    }
                }
                PersonalOverlay.BECOME_MANAGER -> ManagerApplicationScreen(
                    user = user,
                    existingApplication = application,
                    onBack = { overlay = PersonalOverlay.NONE },
                    onSubmit = { fullName, birthDate, phone, email, city, neighborhood,
                                 parkingName, parkingAddress, parkingCity, parkingNeighborhood,
                                 parkingType, acceptedVehicleTypes, dailyVolume, attendantsCount,
                                 applicantStatus, idFrontUri, idBackUri, selfieUri, consentAccepted ->
                        viewModel.submitManagerApplication(
                            fullName, birthDate, phone, email, city, neighborhood,
                            parkingName, parkingAddress, parkingCity, parkingNeighborhood,
                            parkingType, acceptedVehicleTypes, dailyVolume, attendantsCount,
                            applicantStatus, idFrontUri, idBackUri, selfieUri, consentAccepted
                        ) {}
                    },
                    onGoToSubscriptions = { overlay = PersonalOverlay.SUBSCRIPTIONS }
                )
                PersonalOverlay.SUBSCRIPTIONS -> SubscriptionsScreen(
                    currentParking = activeParking,
                    application = application,
                    isManagerEligible = (user.role == UserRole.MANAGER.name || application?.status == com.example.data.model.ApplicationStatus.APPROVED.name) && activeParking != null,
                    onBack = { overlay = PersonalOverlay.NONE },
                    onSelectTier = { tier ->
                        if (tier == SubscriptionTier.FREE_TRIAL) {
                            viewModel.activateSubscription(tier, PaymentMethod.CASH, true, switchToProOnActivate = true) {
                                overlay = PersonalOverlay.NONE
                            }
                        } else {
                            selectedTier = tier
                            overlay = PersonalOverlay.SUB_PAYMENT
                        }
                    }
                )
                PersonalOverlay.SUB_PAYMENT -> SubscriptionPaymentScreen(
                    tier = selectedTier,
                    onBack = { overlay = PersonalOverlay.SUBSCRIPTIONS },
                    onPay = { method, confirmed ->
                        viewModel.activateSubscription(selectedTier, method, confirmed, switchToProOnActivate = true) {
                            overlay = PersonalOverlay.NONE
                        }
                    }
                )
                PersonalOverlay.NONE -> when (currentTab) {
                    PersonalTab.HOME -> PersonalHomeScreen(
                        user = user,
                        themeMode = themeMode,
                        activeTicketsCount = activeTicketsCount,
                        recentSessions = sessions,
                        unreadNotifications = unreadCount,
                        canAccessPro = canAccessPro,
                        application = application,
                        isAdmin = user.role == UserRole.SUPER_ADMIN.name,
                        onToggleTheme = {
                            val next = if (themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
                            viewModel.setThemeMode(next)
                        },
                        onOpenVehicles = { overlay = PersonalOverlay.VEHICLES },
                        onOpenTickets = { currentTab = PersonalTab.TICKETS },
                        onOpenHistory = { currentTab = PersonalTab.HISTORY },
                        onOpenNotifications = { overlay = PersonalOverlay.NOTIFICATIONS },
                        onOpenProfile = { currentTab = PersonalTab.PROFILE },
                        onOpenTicketDetail = { s ->
                            selectedTicket = s
                            overlay = PersonalOverlay.TICKET_DETAIL
                        },
                        onOpenSubscriptions = { overlay = PersonalOverlay.SUBSCRIPTIONS },
                        onSwitchToPro = { viewModel.switchSpace(AppSpace.PRO) },
                        onSwitchToAdmin = { viewModel.switchSpace(AppSpace.ADMIN) }
                    )
                    PersonalTab.TICKETS -> TicketsScreen(
                        sessions = sessions,
                        onOpenTicketDetail = { s ->
                            selectedTicket = s
                            overlay = PersonalOverlay.TICKET_DETAIL
                        },
                        onClaimTicket = { code, plate, onDone ->
                            viewModel.claimTicketByCode(code, plate) { claimed ->
                                onDone()
                                selectedTicket = claimed
                                overlay = PersonalOverlay.TICKET_DETAIL
                            }
                        }
                    )
                    PersonalTab.HISTORY -> PersonalHistoryScreen(
                        sessions = sessions,
                        onOpenTicket = { s ->
                            selectedTicket = s
                            overlay = PersonalOverlay.TICKET_DETAIL
                        }
                    )
                    PersonalTab.PROFILE -> SettingsProfileScreen(
                        user = user,
                        language = language,
                        themeMode = themeMode,
                        canAccessPro = canAccessPro,
                        application = application,
                        onOpenVehicles = { overlay = PersonalOverlay.VEHICLES },
                        onOpenNotifications = { overlay = PersonalOverlay.NOTIFICATIONS },
                        onOpenBecomeManager = { overlay = PersonalOverlay.BECOME_MANAGER },
                        onOpenSubscriptions = { overlay = PersonalOverlay.SUBSCRIPTIONS },
                        onSwitchToPro = { viewModel.switchSpace(AppSpace.PRO) },
                        onSwitchToAdmin = { viewModel.switchSpace(AppSpace.ADMIN) },
                        onUpdateProfile = viewModel::updateProfile,
                        onSetLanguage = viewModel::setLanguage,
                        onSetThemeMode = viewModel::setThemeMode,
                        onLogout = viewModel::logout
                    )
                }
            }
        }
    }
}

@Composable
private fun ProSpaceShell(
    viewModel: ParkoViewModel,
    user: com.example.data.local.UserEntity
) {
    val en = LocalAppLanguage.current == AppLanguage.EN
    val activeParking by viewModel.activeParking.collectAsStateWithLifecycle()
    val canAccessPro by viewModel.canAccessProSpace.collectAsStateWithLifecycle()
    val sessions by viewModel.proSessions.collectAsStateWithLifecycle()
    val payments by viewModel.proPayments.collectAsStateWithLifecycle()
    val attendants by viewModel.proAttendants.collectAsStateWithLifecycle()
    val incidents by viewModel.proIncidents.collectAsStateWithLifecycle()
    val recoveries by viewModel.proRecoveries.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val lastCreatedPlainCode by viewModel.lastCreatedPlainCode.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(ProTab.DASHBOARD) }
    var overlay by remember { mutableStateOf(ProOverlay.NONE) }
    var focusedSession by remember { mutableStateOf<ParkingSessionEntity?>(null) }
    var openedWithoutQr by remember { mutableStateOf(false) }
    var ticketFromRegister by remember { mutableStateOf(false) }
    var selectedTier by remember { mutableStateOf(SubscriptionTier.STANDARD) }

    LaunchedEffect(Unit) {
        viewModel.touchProSession()
        viewModel.refreshConnectivity()
    }

    LaunchedEffect(user.isSuspended, activeParking, canAccessPro) {
        if (user.isSuspended || (activeParking != null && !canAccessPro)) {
            viewModel.switchSpace(AppSpace.PERSONAL)
        }
    }

    BackHandler(enabled = overlay != ProOverlay.NONE || currentTab != ProTab.DASHBOARD) {
        if (overlay != ProOverlay.NONE) {
            if (overlay == ProOverlay.VERIFY_SESSION) {
                focusedSession?.let { viewModel.cancelExitVerification(it) }
            }
            if (overlay == ProOverlay.TICKET_GENERATED) {
                ticketFromRegister = false
                viewModel.clearCreatedSecretCode()
            }
            overlay = when (overlay) {
                ProOverlay.SUB_PAYMENT -> ProOverlay.SUBSCRIPTIONS
                else -> ProOverlay.NONE
            }
        } else {
            currentTab = ProTab.DASHBOARD
        }
    }

    val application by viewModel.userApplication.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (overlay == ProOverlay.NONE) {
                val isDark = MaterialTheme.colorScheme.background.luminance() < 0.3f
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = if (isDark) Color.White else ParkoBlueDark,
                    selectedTextColor = if (isDark) Color.White else ParkoBlueDark,
                    indicatorColor = if (isDark) ParkoBlue.copy(alpha = 0.34f) else ParkoBlueLight,
                    unselectedIconColor = if (isDark) Color(0xFF94A3B8) else ParkoSlate,
                    unselectedTextColor = if (isDark) Color(0xFF94A3B8) else ParkoSlate
                )
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        NavigationBarItem(
                            selected = currentTab == ProTab.DASHBOARD,
                            onClick = { currentTab = ProTab.DASHBOARD },
                            icon = { Icon(Icons.Filled.Home, contentDescription = tr("Accueil Pro", "Pro Home"), modifier = Modifier.size(26.dp)) },
                            label = { Text(tr("Accueil", "Home"), fontWeight = FontWeight.Bold) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("nav_pro_home")
                        )
                        NavigationBarItem(
                            selected = currentTab == ProTab.REGISTER,
                            onClick = { currentTab = ProTab.REGISTER },
                            icon = { Icon(Icons.Filled.AddCircle, contentDescription = tr("Enregistrer", "Register"), modifier = Modifier.size(26.dp)) },
                            label = { Text(tr("Enregistrer", "Register"), fontWeight = FontWeight.Bold) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("nav_pro_register")
                        )
                        NavigationBarItem(
                            selected = currentTab == ProTab.SCANNER,
                            onClick = { currentTab = ProTab.SCANNER },
                            icon = { Icon(Icons.Filled.QrCodeScanner, contentDescription = tr("Scanner", "Scan"), modifier = Modifier.size(26.dp)) },
                            label = { Text(tr("Scanner", "Scan"), fontWeight = FontWeight.Bold) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("nav_pro_scanner")
                        )
                        NavigationBarItem(
                            selected = currentTab == ProTab.HISTORY,
                            onClick = { currentTab = ProTab.HISTORY },
                            icon = { Icon(Icons.Filled.History, contentDescription = tr("Historique", "History"), modifier = Modifier.size(26.dp)) },
                            label = { Text(tr("Historique", "History"), fontWeight = FontWeight.Bold) },
                            colors = navItemColors,
                            modifier = Modifier.testTag("nav_pro_history")
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (overlay) {
                ProOverlay.TICKET_GENERATED -> {
                    val s = focusedSession?.let { fs -> sessions.firstOrNull { it.id == fs.id } ?: fs }
                    if (s != null) {
                        TicketDetailScreen(
                            session = s,
                            isProSender = true,
                            generatedSecretCode = if (ticketFromRegister) lastCreatedPlainCode else null,
                            onBack = {
                                ticketFromRegister = false
                                viewModel.clearCreatedSecretCode()
                                overlay = ProOverlay.NONE
                                currentTab = ProTab.DASHBOARD
                            },
                            onSendToClient = { phone ->
                                viewModel.sendTicketToClient(s, phone) { updated ->
                                    focusedSession = updated
                                }
                            },
                            onSavedNotice = viewModel::showMessage
                        )
                    } else overlay = ProOverlay.NONE
                }
                ProOverlay.VERIFY_SESSION -> {
                    val s = focusedSession?.let { fs -> sessions.firstOrNull { it.id == fs.id } ?: fs }
                    if (s != null) {
                        val latestRecovery = recoveries.filter { it.sessionId == s.id }.maxByOrNull { it.createdAt }
                        VehicleVerificationScreen(
                            session = s,
                            currentUser = user,
                            isOnline = isOnline,
                            latestRecovery = latestRecovery,
                            openedWithoutQr = openedWithoutQr,
                            onBack = {
                                viewModel.cancelExitVerification(s)
                                overlay = ProOverlay.NONE
                            },
                            onOpenQr = {
                                ticketFromRegister = false
                                viewModel.clearCreatedSecretCode()
                                overlay = ProOverlay.TICKET_GENERATED
                            },
                            onVerifyMobileMoney = { method ->
                                viewModel.verifySessionMobileMoney(s, method) { updated ->
                                    focusedSession = updated
                                }
                            },
                            onVerifyWithdrawalCode = { code ->
                                viewModel.verifyWithdrawalCode(s, code) { updated ->
                                    focusedSession = updated
                                }
                            },
                            onUnlockBlockedWithdrawal = { reason ->
                                viewModel.unlockBlockedWithdrawal(s, reason) { updated ->
                                    focusedSession = updated
                                }
                            },
                            onReportPhysicalMismatch = { desc ->
                                viewModel.reportPhysicalMismatch(s, desc) { updated ->
                                    focusedSession = updated
                                }
                            },
                            onSubmitExceptionalRecovery = { rType, cName, plate, desc, phys ->
                                viewModel.submitExceptionalRecovery(s, rType, cName, plate, desc, phys) {}
                            },
                            onReviewRecovery = { rec, approve, reason ->
                                viewModel.reviewExceptionalRecovery(rec, approve, reason)
                            },
                            onAuthorizeExit = { physical, reason ->
                                viewModel.authorizeExit(s, physical, reason) {
                                    viewModel.clearCreatedSecretCode()
                                    overlay = ProOverlay.NONE
                                    currentTab = ProTab.DASHBOARD
                                }
                            },
                            onReportIncident = { type, desc ->
                                viewModel.reportIncident(s, type, desc) {
                                    overlay = ProOverlay.NONE
                                }
                            }
                        )
                    } else overlay = ProOverlay.NONE
                }
                ProOverlay.REPORTS -> FinancialReportsScreen(
                    parking = activeParking,
                    payments = payments,
                    onBack = { overlay = ProOverlay.NONE }
                )
                ProOverlay.ATTENDANTS -> AttendantsScreen(
                    parking = activeParking,
                    attendants = attendants,
                    onBack = { overlay = ProOverlay.NONE },
                    onInvite = viewModel::inviteAttendant,
                    onToggleStatus = viewModel::toggleAttendantStatus
                )
                ProOverlay.INCIDENTS -> IncidentsScreen(
                    incidents = incidents,
                    onBack = { overlay = ProOverlay.NONE },
                    onReport = { type, desc, done -> viewModel.reportIncident(null, type, desc, done) },
                    onResolve = viewModel::resolveIncident
                )
                ProOverlay.SUBSCRIPTIONS -> SubscriptionsScreen(
                    currentParking = activeParking,
                    application = application,
                    isManagerEligible = user.role == UserRole.MANAGER.name && activeParking != null,
                    onBack = { overlay = ProOverlay.NONE },
                    onSelectTier = { tier ->
                        if (tier == SubscriptionTier.FREE_TRIAL) {
                            viewModel.activateSubscription(tier, PaymentMethod.CASH, true, switchToProOnActivate = false) {
                                overlay = ProOverlay.NONE
                            }
                        } else {
                            selectedTier = tier
                            overlay = ProOverlay.SUB_PAYMENT
                        }
                    }
                )
                ProOverlay.SUB_PAYMENT -> SubscriptionPaymentScreen(
                    tier = selectedTier,
                    onBack = { overlay = ProOverlay.SUBSCRIPTIONS },
                    onPay = { method, confirmed ->
                        viewModel.activateSubscription(selectedTier, method, confirmed, switchToProOnActivate = false) {
                            overlay = ProOverlay.NONE
                        }
                    }
                )
                ProOverlay.NONE -> when (currentTab) {
                    ProTab.DASHBOARD -> ProDashboardScreen(
                        user = user,
                        parking = activeParking,
                        sessions = sessions,
                        payments = payments,
                        attendants = attendants,
                        incidents = incidents,
                        recoveries = recoveries,
                        isOnline = isOnline,
                        onRegisterVehicle = { currentTab = ProTab.REGISTER },
                        onOpenScanner = { currentTab = ProTab.SCANNER },
                        onOpenReports = { overlay = ProOverlay.REPORTS },
                        onOpenAttendants = { overlay = ProOverlay.ATTENDANTS },
                        onOpenIncidents = { overlay = ProOverlay.INCIDENTS },
                        onOpenSubscriptions = { overlay = ProOverlay.SUBSCRIPTIONS },
                        onReviewRecovery = { rec, approve, reason ->
                            viewModel.reviewExceptionalRecovery(rec, approve, reason)
                        },
                        onSelectSession = { s ->
                            viewModel.beginExitVerification(
                                session = s,
                                onReady = { locked ->
                                    focusedSession = locked
                                    openedWithoutQr = false
                                    overlay = ProOverlay.VERIFY_SESSION
                                },
                                onBlocked = {}
                            )
                        },
                        onSwitchToPersonal = { viewModel.switchSpace(AppSpace.PERSONAL) }
                    )
                    ProTab.REGISTER -> RegisterVehicleEntryScreen(
                        parking = activeParking,
                        onBack = { currentTab = ProTab.DASHBOARD },
                        onSubmit = { type, plate, brandModel, color, ownerName, ownerPhone, fee, method, photoUri ->
                            viewModel.registerVehicleEntry(
                                type, plate, brandModel, color, ownerName, ownerPhone, fee, method, photoUri
                            ) { createdSession ->
                                ticketFromRegister = true
                                focusedSession = createdSession
                                overlay = ProOverlay.TICKET_GENERATED
                            }
                        }
                    )
                    ProTab.SCANNER -> QrScannerScreen(
                        activeSessions = sessions.filter { it.status != SessionStatus.COMPLETED.name },
                        onBack = { currentTab = ProTab.DASHBOARD },
                        onLookupQuery = { query, isWithoutQr ->
                            viewModel.scanSessionByQuery(
                                query = query,
                                onFound = { found ->
                                    viewModel.beginExitVerification(
                                        session = found,
                                        onReady = { locked ->
                                            focusedSession = locked
                                            openedWithoutQr = isWithoutQr
                                            overlay = ProOverlay.VERIFY_SESSION
                                        },
                                        onBlocked = {}
                                    )
                                },
                                onNotFound = {
                                    viewModel.showMessage(
                                        if (en) "Ticket not found: no matching session."
                                        else "Ticket introuvable : aucun stationnement correspondant."
                                    )
                                }
                            )
                        }
                    )
                    ProTab.HISTORY -> ProHistoryScreen(
                        sessions = sessions,
                        onSelectSession = { s ->
                            viewModel.beginExitVerification(
                                session = s,
                                onReady = { locked ->
                                    focusedSession = locked
                                    openedWithoutQr = false
                                    overlay = ProOverlay.VERIFY_SESSION
                                },
                                onBlocked = {}
                            )
                        }
                    )
                }
            }
        }
    }
}
