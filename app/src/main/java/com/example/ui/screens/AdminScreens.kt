package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.*
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.i18n.*
import com.example.ui.theme.*

enum class AdminSection {
    HOME, USERS, APPLICATIONS, PARKINGS, SUBSCRIPTIONS, STATS_INCIDENTS, AUDIT
}

@Composable
fun AdminDashboardScreen(
    adminUser: UserEntity,
    users: List<UserEntity>,
    parkings: List<ParkingLocationEntity>,
    attendants: List<AttendantEntity>,
    sessions: List<ParkingSessionEntity>,
    payments: List<PaymentRecordEntity>,
    applications: List<ManagerApplicationEntity>,
    incidents: List<IncidentEntity>,
    auditLogs: List<AuditLogEntity>,
    onReviewApplication: (ManagerApplicationEntity, ApplicationStatus, String) -> Unit,
    onConfirmSubPayment: (PaymentRecordEntity) -> Unit,
    onToggleParkingSuspension: (ParkingLocationEntity) -> Unit,
    onToggleUserSuspension: (UserEntity) -> Unit,
    onSwitchToPersonal: () -> Unit,
    onLogout: () -> Unit
) {
    val en = LocalAppLanguage.current == AppLanguage.EN
    var section by remember { mutableStateOf(AdminSection.HOME) }
    BackHandler(enabled = section != AdminSection.HOME) {
        section = AdminSection.HOME
    }

    val pendingAppsCount = applications.count {
        it.status == ApplicationStatus.PENDING.name || it.status == ApplicationStatus.UNDER_REVIEW.name
    }
    val parkingRevenue = payments
        .filter { it.purpose == "STATIONNEMENT" && it.status == PaymentStatus.CONFIRMED.name }
        .sumOf { it.amountFcfa }
    val parkoSubRevenue = payments
        .filter { it.purpose == "ABONNEMENT" && it.status == PaymentStatus.CONFIRMED.name }
        .sumOf { it.amountFcfa }

    Column(modifier = Modifier.fillMaxSize().parkoAtmosphericBackground()) {
        if (section == AdminSection.HOME) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF062A7A), ParkoBlueDark, ParkoBlue)
                                )
                            )
                            .drawBehind {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.08f),
                                    radius = size.maxDimension * 0.40f,
                                    center = Offset(size.width * 0.90f, size.height * 0.20f)
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.12f),
                                    radius = size.maxDimension * 0.56f,
                                    center = Offset(size.width * 0.90f, size.height * 0.20f),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalButton(
                                    onClick = onSwitchToPersonal,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color.White.copy(alpha = 0.24f),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(tr("Espace Perso", "Personal Space"), fontWeight = FontWeight.Bold)
                                }
                                IconButton(onClick = onLogout) {
                                    Icon(Icons.Filled.Logout, contentDescription = tr("Déconnexion", "Sign out"), tint = Color.White, modifier = Modifier.size(26.dp))
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(66.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = 0.24f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = Color.White, modifier = Modifier.size(38.dp))
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(tr("Administration PARKO", "PARKO Administration"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(adminUser.fullName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.9f))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ParkoBlueLight,
                            contentColor = ParkoCharcoal,
                            modifier = Modifier.weight(1f).parkoCardOutline(18.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(tr("Abonnements", "Subscriptions"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ParkoBlueDark)
                                Spacer(Modifier.height(4.dp))
                                Text(formatFcfa(parkoSubRevenue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontFamily = JetBrainsMonoFamily)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = ParkoEmeraldLight,
                            contentColor = ParkoCharcoal,
                            modifier = Modifier.weight(1f).parkoCardOutline(18.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(tr("Recettes Parkings", "Parking Revenue"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ParkoEmeraldDark)
                                Spacer(Modifier.height(4.dp))
                                Text(formatFcfa(parkingRevenue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontFamily = JetBrainsMonoFamily)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).parkoCardOutline(22.dp)
                    ) {
                        Column {
                            ParkoMenuRow(
                                icon = Icons.Filled.People,
                                title = tr("Utilisateurs", "Users"),
                                trailingBadge = "${users.size}",
                                onClick = { section = AdminSection.USERS },
                                testTag = "admin_users_menu"
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            ParkoMenuRow(
                                icon = Icons.Filled.Verified,
                                title = tr("Gestionnaires", "Managers"),
                                trailingBadge = if (pendingAppsCount > 0) tr("$pendingAppsCount en attente", "$pendingAppsCount pending") else null,
                                iconTint = ParkoAmber,
                                onClick = { section = AdminSection.APPLICATIONS },
                                testTag = "admin_apps_menu"
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            ParkoMenuRow(
                                icon = Icons.Filled.LocalParking,
                                title = tr("Parkings", "Parkings"),
                                trailingBadge = "${parkings.count { !it.isSuspended }}",
                                onClick = { section = AdminSection.PARKINGS },
                                testTag = "admin_parkings_menu"
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            ParkoMenuRow(
                                icon = Icons.Filled.CardMembership,
                                title = tr("Abonnements & Paiements", "Subscriptions & Payments"),
                                onClick = { section = AdminSection.SUBSCRIPTIONS },
                                testTag = "admin_subs_menu"
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            ParkoMenuRow(
                                icon = Icons.Filled.BarChart,
                                title = tr("Statistiques & Incidents", "Statistics & Incidents"),
                                trailingBadge = "${incidents.size}",
                                iconTint = ParkoEmerald,
                                onClick = { section = AdminSection.STATS_INCIDENTS },
                                testTag = "admin_stats_menu"
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            ParkoMenuRow(
                                icon = Icons.Filled.Security,
                                title = tr("Audit & Sécurité", "Audit & Security"),
                                trailingBadge = "${auditLogs.size}",
                                iconTint = MaterialTheme.colorScheme.onSurface,
                                onClick = { section = AdminSection.AUDIT },
                                testTag = "admin_audit_menu"
                            )
                        }
                    }
                }
            }
        } else {
            val title = when (section) {
                AdminSection.USERS -> tr("Utilisateurs", "Users")
                AdminSection.APPLICATIONS -> tr("Gestionnaires", "Managers")
                AdminSection.PARKINGS -> tr("Parkings", "Parkings")
                AdminSection.SUBSCRIPTIONS -> tr("Abonnements", "Subscriptions")
                AdminSection.STATS_INCIDENTS -> tr("Statistiques & Incidents", "Stats & Incidents")
                AdminSection.AUDIT -> tr("Audit", "Audit")
                AdminSection.HOME -> ""
            }
            ParkoTopBar(title = title, onBack = { section = AdminSection.HOME })

            when (section) {
                AdminSection.USERS -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(users, key = { it.id }) { u ->
                            val role = UserRole.entries.firstOrNull { it.name == u.role } ?: UserRole.USER
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth().parkoCardOutline(16.dp)
                            ) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(u.fullName, fontWeight = FontWeight.Bold)
                                        Text("${u.phone} • ${role.localizedLabel(en)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (u.id != adminUser.id) {
                                        TextButton(onClick = { onToggleUserSuspension(u) }) {
                                            Text(
                                                if (u.isSuspended) tr("Réactiver", "Reactivate") else tr("Suspendre", "Suspend"),
                                                fontWeight = FontWeight.Bold,
                                                color = if (u.isSuspended) ParkoEmerald else ParkoRed
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                AdminSection.APPLICATIONS -> {
                    var selectedApp by remember { mutableStateOf<ManagerApplicationEntity?>(null) }
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(applications, key = { it.id }) { app ->
                            val st = ApplicationStatus.entries.firstOrNull { it.name == app.status } ?: ApplicationStatus.PENDING
                            Surface(
                                onClick = { selectedApp = app },
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth().parkoCardOutline(18.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(app.parkingName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        StatusPill(
                                            label = st.localizedLabel(en),
                                            containerColor = if (st == ApplicationStatus.APPROVED) ParkoEmeraldLight else ParkoAmberLight,
                                            contentColor = if (st == ApplicationStatus.APPROVED) ParkoEmeraldDark else ParkoAmberDark
                                        )
                                    }
                                    Text("${app.fullName} • ${app.phone}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text("${app.parkingNeighborhood}, ${app.parkingCity}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    selectedApp?.let { app ->
                        var note by remember { mutableStateOf(app.reviewNote ?: "") }
                        AlertDialog(
                            onDismissRequest = { selectedApp = null },
                            title = { Text(app.parkingName, fontWeight = FontWeight.Bold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("${app.fullName} • ${app.phone}", fontWeight = FontWeight.Bold)
                                    Text("${app.parkingAddress}, ${app.parkingCity}", style = MaterialTheme.typography.bodySmall)
                                    Text(tr("Pièces : Recto ✓ • Verso ✓ • Selfie ✓", "Docs: Front ✓ • Back ✓ • Selfie ✓"), color = ParkoEmeraldDark, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    OutlinedTextField(
                                        value = note,
                                        onValueChange = { note = it },
                                        label = { Text(tr("Remarque", "Note")) }
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        onReviewApplication(app, ApplicationStatus.APPROVED, note.ifBlank { if (en) "Compliant and approved" else "Dossier conforme et validé" })
                                        selectedApp = null
                                    }
                                ) { Text(tr("Approuver", "Approve"), fontWeight = FontWeight.Bold) }
                            },
                            dismissButton = {
                                Row {
                                    TextButton(
                                        onClick = {
                                            onReviewApplication(app, ApplicationStatus.REJECTED, note.ifBlank { if (en) "Insufficient documents" else "Justificatif insuffisant" })
                                            selectedApp = null
                                        }
                                    ) { Text(tr("Rejeter", "Reject"), fontWeight = FontWeight.Bold, color = ParkoRed) }
                                    TextButton(onClick = { selectedApp = null }) { Text(tr("Fermer", "Close"), fontWeight = FontWeight.Bold) }
                                }
                            }
                        )
                    }
                }

                AdminSection.PARKINGS -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(parkings, key = { it.id }) { p ->
                            val tier = SubscriptionTier.entries.firstOrNull { it.name == p.subscriptionTier } ?: SubscriptionTier.FREE_TRIAL
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth().parkoCardOutline(16.dp)
                            ) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(p.name, fontWeight = FontWeight.Bold)
                                        Text("${p.neighborhood}, ${p.city} • ${tier.localizedTitle(en)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Quota: ${p.quotaUsed}/${if (p.quotaLimit == Int.MAX_VALUE) "∞" else p.quotaLimit}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ParkoBlue)
                                    }
                                    TextButton(onClick = { onToggleParkingSuspension(p) }) {
                                        Text(
                                            if (p.isSuspended) tr("Réactiver", "Reactivate") else tr("Suspendre", "Suspend"),
                                            fontWeight = FontWeight.Bold,
                                            color = if (p.isSuspended) ParkoEmerald else ParkoRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                AdminSection.SUBSCRIPTIONS -> {
                    val subPayments = payments.filter { it.purpose == "ABONNEMENT" }
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(subPayments, key = { it.id }) { pay ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth().parkoCardOutline(16.dp)
                            ) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${pay.parkingName} • ${pay.referenceCode}", fontWeight = FontWeight.Bold)
                                        Text("${formatFcfa(pay.amountFcfa)} • ${PaymentMethodLabel(pay.method)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    }
                                    if (pay.status == PaymentStatus.PENDING.name) {
                                        Button(onClick = { onConfirmSubPayment(pay) }) { Text(tr("Valider", "Confirm"), fontWeight = FontWeight.Bold) }
                                    } else {
                                        PaymentStatusBadge(pay.status)
                                    }
                                }
                            }
                        }
                    }
                }

                AdminSection.STATS_INCIDENTS -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth().parkoCardOutline(18.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(tr("Indicateurs nationaux", "National metrics"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("• ${tr("Utilisateurs", "Users")} : ${users.size}", fontWeight = FontWeight.SemiBold)
                                    Text("• ${tr("Parkings actifs", "Active parkings")} : ${parkings.count { !it.isSuspended }}", fontWeight = FontWeight.SemiBold)
                                    Text("• ${tr("Gardiens actifs", "Active attendants")} : ${attendants.count { it.isActive }}", fontWeight = FontWeight.SemiBold)
                                    Text("• ${tr("Sessions totales", "Total sessions")} : ${sessions.size}", fontWeight = FontWeight.SemiBold)
                                    Text("• ${tr("Recettes parkings", "Parking revenue")} : ${formatFcfa(parkingRevenue)}", fontWeight = FontWeight.Bold)
                                    Text("• ${tr("Abonnements PARKO", "PARKO subscriptions")} : ${formatFcfa(parkoSubRevenue)}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        items(incidents, key = { it.id }) { inc ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth().parkoCardOutline(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("${inc.parkingName} • ${inc.type}", fontWeight = FontWeight.Bold)
                                    Text(inc.description, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                AdminSection.AUDIT -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(auditLogs, key = { it.id }) { log ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth().parkoCardOutline(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(log.action, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ParkoBlue)
                                        Text(formatDateTime(log.createdAt), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("${log.actorName} • ${log.targetType} (${log.targetId})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text(log.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                AdminSection.HOME -> Unit
            }
        }
    }
}
