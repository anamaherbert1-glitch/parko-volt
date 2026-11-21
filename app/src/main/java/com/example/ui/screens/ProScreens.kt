package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.*
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.i18n.*
import com.example.ui.theme.*
import com.example.util.ParkoSecurityConfig
import java.util.Calendar

@Composable
fun ProDashboardScreen(
    user: UserEntity,
    parking: ParkingLocationEntity?,
    sessions: List<ParkingSessionEntity>,
    payments: List<PaymentRecordEntity>,
    attendants: List<AttendantEntity>,
    incidents: List<IncidentEntity>,
    recoveries: List<ExceptionalRecoveryEntity> = emptyList(),
    isOnline: Boolean = true,
    onRegisterVehicle: () -> Unit,
    onOpenScanner: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenAttendants: () -> Unit,
    onOpenIncidents: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onReviewRecovery: (ExceptionalRecoveryEntity, Boolean, String) -> Unit = { _, _, _ -> },
    onSelectSession: (ParkingSessionEntity) -> Unit,
    onSwitchToPersonal: () -> Unit
) {
    val en = LocalAppLanguage.current == AppLanguage.EN
    val activeCount = sessions.count { it.status != SessionStatus.COMPLETED.name && it.status != SessionStatus.EXPIRED.name }
    val completedCount = sessions.count { it.status == SessionStatus.COMPLETED.name }
    val startOfDay = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }.timeInMillis
    }
    val todayEntries = sessions.count { it.entryTime >= startOfDay }
    val confirmedParkingPayments = payments.filter { it.purpose == "STATIONNEMENT" && it.status == PaymentStatus.CONFIRMED.name }
    val todayRevenue = confirmedParkingPayments.filter { it.createdAt >= startOfDay }.sumOf { it.amountFcfa }
    val openIncidentsCount = incidents.count { it.status != IncidentStatus.RESOLVED.name }

    val tier = SubscriptionTier.entries.firstOrNull { it.name == parking?.subscriptionTier } ?: SubscriptionTier.STANDARD
    val quotaUsed = parking?.quotaUsed ?: 0
    val quotaLimit = parking?.quotaLimit ?: 50
    val quotaNearLimit = quotaLimit != Int.MAX_VALUE && quotaUsed >= (quotaLimit * 0.8).toInt()
    val quotaExhausted = quotaLimit != Int.MAX_VALUE && quotaUsed >= quotaLimit
    val userRole = UserRole.entries.firstOrNull { it.name == user.role }

    LazyColumn(
        modifier = Modifier.fillMaxSize().parkoAtmosphericBackground(),
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
                            center = Offset(size.width * 0.92f, size.height * 0.20f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.14f),
                            radius = size.maxDimension * 0.54f,
                            center = Offset(size.width * 0.92f, size.height * 0.20f),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = parking?.name ?: tr("Espace Pro PARKO", "PARKO Pro Space"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${user.fullName} • ${userRole?.localizedLabel(en) ?: "Pro"}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    FilledTonalButton(
                        onClick = onSwitchToPersonal,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.24f),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("switch_personal_button")
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Perso", "Personal"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (!isOnline) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ParkoRedLight,
                    contentColor = ParkoRedDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 12.dp)
                        .parkoCardOutline(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.WifiOff, contentDescription = null, tint = ParkoRedDark)
                        Text(
                            tr(
                                "Mode hors connexion : la validation définitive de sortie est bloquée.",
                                "Offline mode: final vehicle exit validation is blocked."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Surface(
                onClick = { if (userRole == UserRole.MANAGER) onOpenSubscriptions() },
                enabled = userRole == UserRole.MANAGER,
                shape = RoundedCornerShape(20.dp),
                color = if (quotaExhausted) ParkoRedLight else if (quotaNearLimit) ParkoAmberLight else ParkoBlueLight,
                contentColor = ParkoCharcoal,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .parkoCardOutline(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${tr("Offre", "Plan")} ${tier.localizedTitle(en)} • ${if (parking?.subscriptionActive == true) tr("Actif", "Active") else tr("Expiré", "Expired")}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ParkoBlueDark
                        )
                        Text(
                            text = if (quotaLimit == Int.MAX_VALUE) "$quotaUsed / ${tr("Illimité", "Unlimited")}" else "$quotaUsed / $quotaLimit",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = ParkoMonoFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (quotaLimit != Int.MAX_VALUE) {
                        LinearProgressIndicator(
                            progress = { (quotaUsed.toFloat() / quotaLimit.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                            color = if (quotaExhausted) ParkoRed else if (quotaNearLimit) ParkoAmber else ParkoBlue,
                            trackColor = Color.White
                        )
                    }
                    if (quotaExhausted || parking?.subscriptionActive == false) {
                        Text(
                            text = tr(
                                "Quota atteint — sorties toujours autorisées.",
                                "Quota reached — exits remain allowed."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = ParkoRedDark
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.weight(1f).parkoCardOutline(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ParkoBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = ParkoBlue, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(tr("Aujourd'hui", "Today"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(tr("$todayEntries entrées", "$todayEntries entries"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(tr("$completedCount sorties", "$completedCount exits"), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.weight(1f).parkoCardOutline(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ParkoEmeraldLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.LocalParking, contentDescription = null, tint = ParkoEmeraldDark, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(tr("En parking", "Parked now"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(tr("$activeCount véhicules", "$activeCount vehicles"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("${tr("Recette", "Revenue")}: ${formatFcfa(todayRevenue)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ParkoEmerald)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onRegisterVehicle,
                        modifier = Modifier.weight(1f).height(54.dp).testTag("pro_register_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Enregistrer", "Register"), fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onOpenScanner,
                        colors = ButtonDefaults.buttonColors(containerColor = ParkoCharcoal, contentColor = Color.White),
                        modifier = Modifier.weight(1f).height(54.dp).testTag("pro_scanner_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Scanner un QR", "Scan QR"), fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            val isManagerOrSup = userRole == UserRole.MANAGER || userRole == UserRole.SUPERVISOR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isManagerOrSup) {
                    AssistChip(
                        onClick = onOpenReports,
                        label = { Text(tr("Rapports", "Reports"), fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Filled.BarChart, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        colors = parkoAssistChipColors(),
                        border = parkoAssistChipBorder(),
                        modifier = Modifier.testTag("pro_reports_chip")
                    )
                }
                if (userRole == UserRole.MANAGER) {
                    AssistChip(
                        onClick = onOpenAttendants,
                        label = { Text("${tr("Gardiens", "Attendants")} (${attendants.count { it.isActive }})", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        colors = parkoAssistChipColors(),
                        border = parkoAssistChipBorder(),
                        modifier = Modifier.testTag("pro_attendants_chip")
                    )
                }
                AssistChip(
                    onClick = onOpenIncidents,
                    label = { Text("Incidents ($openIncidentsCount)", fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Filled.Report, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    colors = parkoAssistChipColors(),
                    border = parkoAssistChipBorder(),
                    modifier = Modifier.testTag("pro_incidents_chip")
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        val pendingRecoveries = recoveries.filter { it.status == RecoveryStatus.PENDING.name }
        if ((userRole == UserRole.MANAGER || userRole == UserRole.SUPERVISOR) && pendingRecoveries.isNotEmpty()) {
            item {
                var reviewTarget by remember { mutableStateOf<Pair<ExceptionalRecoveryEntity, Boolean>?>(null) }
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        tr("Récupérations exceptionnelles en attente", "Pending exceptional recoveries"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ParkoAmberDark
                    )
                    pendingRecoveries.forEach { rec ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ParkoAmberLight,
                            contentColor = ParkoCharcoal,
                            modifier = Modifier.fillMaxWidth().parkoCardOutline(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${rec.sessionRef} • ${rec.declaredPlateNumber}", fontWeight = FontWeight.Bold)
                                Text("${rec.reasonType} — ${rec.claimantName}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text(rec.description, style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { reviewTarget = rec to true },
                                        modifier = Modifier.weight(1f)
                                    ) { Text(tr("Approuver", "Approve"), fontWeight = FontWeight.Bold) }
                                    OutlinedButton(
                                        onClick = { reviewTarget = rec to false },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ParkoRedDark),
                                        modifier = Modifier.weight(1f)
                                    ) { Text(tr("Refuser", "Reject"), fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                }
                reviewTarget?.let { (rec, approve) ->
                    var reason by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { reviewTarget = null },
                        title = {
                            Text(
                                if (approve) tr("Approuver la récupération", "Approve recovery")
                                else tr("Refuser la récupération", "Reject recovery"),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${rec.sessionRef} (${rec.declaredPlateNumber}) • ${rec.claimantName}", fontWeight = FontWeight.SemiBold)
                                OutlinedTextField(
                                    value = reason,
                                    onValueChange = { reason = it },
                                    label = { Text(tr("Motif obligatoire *", "Mandatory reason *")) },
                                    minLines = 2,
                                    colors = parkoTextFieldColors(),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                onReviewRecovery(rec, approve, reason)
                                reviewTarget = null
                            }) { Text(tr("Confirmer", "Confirm"), fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = {
                            TextButton(onClick = { reviewTarget = null }) { Text(tr("Annuler", "Cancel")) }
                        }
                    )
                }
            }
        }

        item {
            Text(
                text = tr("Véhicules en parking", "Vehicles parked"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        val activeSessions = sessions.filter { it.status != SessionStatus.COMPLETED.name }
        if (activeSessions.isEmpty()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    EmptyStateCard(
                        icon = Icons.Filled.LocalParking,
                        title = tr("Parking vide", "Parking empty")
                    )
                }
            }
        } else {
            items(activeSessions, key = { it.id }) { s ->
                Surface(
                    onClick = { onSelectSession(s) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 5.dp)
                        .parkoCardOutline(16.dp)
                        .testTag("pro_active_session_${s.plateNumber}")
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        VehicleThumbnail(
                            photoUri = s.vehiclePhotoUri,
                            drawableKey = s.vehicleDrawableKey,
                            vehicleType = s.vehicleType,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${s.vehicleLabel} • ${s.plateNumber}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${s.ownerName} • ${s.referenceCode} • ${formatTimeShort(s.entryTime)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            PaymentStatusBadge(s.paymentStatus)
                            Spacer(Modifier.height(4.dp))
                            Text(formatFcfa(s.feeFcfa), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, fontFamily = ParkoMonoFamily)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RegisterVehicleEntryScreen(
    parking: ParkingLocationEntity?,
    onBack: () -> Unit,
    onSubmit: (VehicleType, String, String, String, String, String, Int, PaymentMethod, String?) -> Unit
) {
    val en = LocalAppLanguage.current == AppLanguage.EN
    var selectedType by remember { mutableStateOf(VehicleType.MOTO) }
    var plate by remember { mutableStateOf("") }
    var brandModel by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var ownerPhone by remember { mutableStateOf("+228 ") }
    var feeText by remember { mutableStateOf(VehicleType.MOTO.defaultFeeFcfa.toString()) }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var photoUri by remember { mutableStateOf<String?>(null) }

    val tier = SubscriptionTier.entries.firstOrNull { it.name == parking?.subscriptionTier } ?: SubscriptionTier.STANDARD
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photoUri = uri.toString()
    }

    Column(modifier = Modifier.fillMaxSize().parkoAtmosphericBackground()) {
        ParkoTopBar(title = tr("Enregistrer un véhicule", "Register a vehicle"), onBack = onBack)

        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(tr("Type de véhicule", "Vehicle type"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VehicleType.entries.take(3).forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = {
                            selectedType = type
                            feeText = type.defaultFeeFcfa.toString()
                        },
                        label = { Text(type.localizedLabel(en), fontWeight = FontWeight.Bold) },
                        colors = parkoFilterChipColors(),
                        border = parkoFilterChipBorder(selectedType == type),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VehicleType.entries.drop(3).forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = {
                            selectedType = type
                            feeText = type.defaultFeeFcfa.toString()
                        },
                        label = { Text(type.localizedLabel(en), fontWeight = FontWeight.Bold) },
                        colors = parkoFilterChipColors(),
                        border = parkoFilterChipBorder(selectedType == type),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value = plate,
                onValueChange = { plate = it.uppercase() },
                label = { Text(tr("Plaque * (ex: TG-1234-AA)", "Plate * (e.g. TG-1234-AA)")) },
                singleLine = true,
                colors = parkoTextFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("entry_plate_input")
            )
            OutlinedTextField(
                value = brandModel,
                onValueChange = { brandModel = it },
                label = { Text(tr("Marque / Modèle", "Brand / Model")) },
                singleLine = true,
                colors = parkoTextFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = color,
                onValueChange = { color = it },
                label = { Text(tr("Couleur", "Color")) },
                singleLine = true,
                colors = parkoTextFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = ownerName,
                onValueChange = { ownerName = it },
                label = { Text(tr("Propriétaire *", "Owner *")) },
                singleLine = true,
                colors = parkoTextFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("entry_owner_input")
            )
            OutlinedTextField(
                value = ownerPhone,
                onValueChange = { ownerPhone = it },
                label = { Text(tr("Téléphone (+228...)", "Phone (+228...)")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                colors = parkoTextFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            if (ParkoSecurityConfig.isDemoModeEnabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {
                            ownerName = "Amadou Diallo"
                            ownerPhone = "+228 90 12 34 56"
                        },
                        label = { Text("Amadou (+228 90...)", fontWeight = FontWeight.SemiBold) },
                        colors = parkoAssistChipColors(),
                        border = parkoAssistChipBorder()
                    )
                    AssistChip(
                        onClick = {
                            ownerName = "Codjo Agbeko"
                            ownerPhone = "+228 98 11 22 33"
                        },
                        label = { Text("Codjo (+228 98...)", fontWeight = FontWeight.SemiBold) },
                        colors = parkoAssistChipColors(),
                        border = parkoAssistChipBorder()
                    )
                }
            }
            OutlinedTextField(
                value = feeText,
                onValueChange = { feeText = it.filter { c -> c.isDigit() } },
                label = { Text(tr("Tarif (FCFA)", "Fee (FCFA)")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = parkoTextFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text(tr("Paiement", "Payment"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaymentMethod.entries.forEach { method ->
                    FilterChip(
                        selected = paymentMethod == method,
                        onClick = { paymentMethod = method },
                        label = { Text(method.localizedLabel(en), fontWeight = FontWeight.Bold) },
                        colors = parkoFilterChipColors(),
                        border = parkoFilterChipBorder(paymentMethod == method)
                    )
                }
            }

            if (tier.allowsPhotos) {
                OutlinedButton(
                    onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (photoUri == null) tr("Photo du véhicule", "Vehicle photo")
                        else tr("Photo jointe ✓", "Photo attached ✓"),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Button(
                onClick = {
                    onSubmit(
                        selectedType,
                        plate,
                        brandModel,
                        color,
                        ownerName,
                        ownerPhone,
                        feeText.toIntOrNull() ?: selectedType.defaultFeeFcfa,
                        paymentMethod,
                        photoUri
                    )
                },
                modifier = Modifier.fillMaxWidth().height(54.dp).testTag("entry_submit_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.QrCode2, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(tr("Générer ticket, code & QR", "Generate ticket, code & QR"), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun QrScannerScreen(
    activeSessions: List<ParkingSessionEntity>,
    onBack: () -> Unit,
    onLookupQuery: (String, Boolean) -> Unit
) {
    var manualQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .parkoScannerAtmosphericBackground()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.Filled.Close, contentDescription = tr("Fermer scanner", "Close scanner"), tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Text(
                tr("Scanner un QR", "Scan a QR"),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QrScannerCameraBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        tr("Recherche manuelle", "Manual lookup"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    OutlinedTextField(
                        value = manualQuery,
                        onValueChange = { manualQuery = it },
                        label = { Text(tr("Plaque, téléphone ou code PK-...", "Plate, phone or PK-... code")) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ParkoBlueMid,
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedLabelColor = Color.White.copy(alpha = 0.85f),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.65f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("scanner_manual_input")
                    )
                    Button(
                        onClick = { onLookupQuery(manualQuery, true) },
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("scanner_search_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Rechercher", "Search"), fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (activeSessions.isNotEmpty()) {
                Text(
                    tr("Tickets actifs", "Active tickets"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                activeSessions.take(4).forEach { s ->
                    Surface(
                        onClick = { onLookupQuery(s.qrToken, false) },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("${s.vehicleLabel} (${s.plateNumber})", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("${s.referenceCode} • ${s.ownerName}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                            }
                            Icon(Icons.Filled.QrCode2, contentDescription = tr("Scanner", "Scan"), tint = ParkoEmerald, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun VehicleVerificationScreen(
    session: ParkingSessionEntity,
    currentUser: UserEntity,
    isOnline: Boolean = true,
    latestRecovery: ExceptionalRecoveryEntity? = null,
    openedWithoutQr: Boolean,
    onBack: () -> Unit,
    onOpenQr: () -> Unit,
    onVerifyMobileMoney: (PaymentMethod) -> Unit,
    onVerifyWithdrawalCode: (String) -> Unit,
    onUnlockBlockedWithdrawal: (String) -> Unit,
    onReportPhysicalMismatch: (String) -> Unit,
    onSubmitExceptionalRecovery: (reasonType: String, claimantName: String, declaredPlate: String, description: String, physicalChecked: Boolean) -> Unit,
    onReviewRecovery: (ExceptionalRecoveryEntity, Boolean, String) -> Unit,
    onAuthorizeExit: (physicalChecked: Boolean, auditReason: String) -> Unit,
    onReportIncident: (IncidentType, String) -> Unit
) {
    val en = LocalAppLanguage.current == AppLanguage.EN
    var physicalChecked by remember { mutableStateOf(false) }
    var mismatchSelected by remember(session.physicalMismatchReported) { mutableStateOf(session.physicalMismatchReported) }
    var secretCodeInput by remember { mutableStateOf("") }
    var auditReason by remember {
        mutableStateOf(
            if (openedWithoutQr) {
                if (en) "Client without QR ticket — ID & recovery form checked" else "Client sans ticket QR — identité et formulaire de récupération contrôlés"
            } else ""
        )
    }
    var showIncidentDialog by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var showUnlockDialog by remember { mutableStateOf(false) }
    var recoveryDecisionApprove by remember { mutableStateOf<Boolean?>(null) }

    val alreadyCompleted = session.status == SessionStatus.COMPLETED.name
    val isExpired = session.status == SessionStatus.EXPIRED.name
    val needsVerification = session.status == SessionStatus.NEEDS_VERIFICATION.name
    val lockedByAnother = session.status == SessionStatus.EXIT_PENDING.name &&
        session.exitPendingByUserId != null &&
        session.exitPendingByUserId != currentUser.id &&
        (session.exitPendingExpiresAt ?: 0L) > System.currentTimeMillis()
    val paymentConfirmed = session.paymentStatus == PaymentStatus.CONFIRMED.name
    val secretCodeVerified = session.withdrawalVerifiedAt != null
    val recoveryApproved = latestRecovery?.status == RecoveryStatus.APPROVED.name
    val isManagerOrSup = currentUser.role == UserRole.MANAGER.name || currentUser.role == UserRole.SUPERVISOR.name
    val vTypeLabel = (VehicleType.entries.firstOrNull { it.name == session.vehicleType } ?: VehicleType.AUTRE).localizedLabel(en)

    val canAuthorizeExit = isOnline &&
        !alreadyCompleted &&
        !isExpired &&
        !needsVerification &&
        !lockedByAnother &&
        !mismatchSelected &&
        !session.physicalMismatchReported &&
        physicalChecked &&
        paymentConfirmed &&
        (secretCodeVerified || recoveryApproved)

    Column(modifier = Modifier.fillMaxSize().parkoAtmosphericBackground()) {
        ParkoTopBar(title = tr("Vérification véhicule", "Vehicle verification"), onBack = onBack)

        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            VehicleThumbnail(
                photoUri = session.vehiclePhotoUri,
                drawableKey = session.vehicleDrawableKey,
                vehicleType = session.vehicleType,
                modifier = Modifier.fillMaxWidth().height(160.dp).parkoCardOutline(14.dp)
            )

            if (!isOnline && !alreadyCompleted) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ParkoRedLight,
                    contentColor = ParkoRedDark,
                    modifier = Modifier.fillMaxWidth().parkoCardOutline(16.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.WifiOff, contentDescription = null, tint = ParkoRedDark)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            tr("Mode hors connexion : validation définitive de sortie bloquée.", "Offline mode: final exit validation blocked."),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (lockedByAnother) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ParkoRedLight,
                    contentColor = ParkoRedDark,
                    modifier = Modifier.fillMaxWidth().parkoCardOutline(16.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = ParkoRedDark)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            tr(
                                "Sortie déjà en cours par ${session.exitPendingByName ?: "un autre gardien"}. Validation bloquée.",
                                "Exit already in progress by ${session.exitPendingByName ?: "another attendant"}. Blocked."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Status Banner (Section 1.3 & Test 2)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = when {
                    alreadyCompleted || needsVerification || isExpired -> ParkoRedLight
                    session.status == SessionStatus.EXIT_PENDING.name -> ParkoAmberLight
                    else -> ParkoEmeraldLight
                },
                contentColor = ParkoCharcoal,
                modifier = Modifier.fillMaxWidth().parkoCardOutline(16.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when {
                            alreadyCompleted || needsVerification || isExpired -> Icons.Filled.GppBad
                            session.status == SessionStatus.EXIT_PENDING.name -> Icons.Filled.HourglassTop
                            else -> Icons.Filled.VerifiedUser
                        },
                        contentDescription = null,
                        tint = when {
                            alreadyCompleted || needsVerification || isExpired -> ParkoRedDark
                            session.status == SessionStatus.EXIT_PENDING.name -> ParkoAmberDark
                            else -> ParkoEmeraldDark
                        },
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (alreadyCompleted) {
                                tr(
                                    "Ce ticket a déjà été utilisé pour une sortie. Toute nouvelle validation est interdite.",
                                    "This ticket has already been used for an exit. Any further validation is forbidden."
                                )
                            } else {
                                (SessionStatus.entries.firstOrNull { it.name == session.status } ?: SessionStatus.ACTIVE).localizedLabel(en)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (alreadyCompleted || needsVerification) ParkoRedDark else ParkoCharcoal
                        )
                        Text(
                            text = "${session.referenceCode} • ${formatDateTime(session.entryTime)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Full 9 Ticket Information Fields (Section 1.3)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth().parkoCardOutline(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    VerificationInfoRow(tr("Référence ticket", "Ticket ref"), session.referenceCode)
                    VerificationInfoRow(tr("Type de véhicule", "Vehicle type"), vTypeLabel)
                    VerificationInfoRow(tr("Plaque", "License plate"), session.plateNumber)
                    VerificationInfoRow(tr("Marque & modèle", "Brand & model"), session.vehicleLabel)
                    VerificationInfoRow(tr("Couleur", "Color"), session.vehicleColor)
                    VerificationInfoRow(tr("Propriétaire", "Owner"), "${session.ownerName} (${session.ownerPhone.ifBlank { tr("Non renseigné", "N/A") }})")
                    VerificationInfoRow(tr("Date & heure d'entrée", "Entry date/time"), formatDateTime(session.entryTime))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tr("Statut du ticket", "Ticket status"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        SessionStatusBadge(session.status)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(PaymentMethodLabel(session.paymentMethod), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(formatFcfa(session.feeFcfa), fontFamily = ParkoMonoFamily, fontWeight = FontWeight.Bold)
                        }
                        PaymentStatusBadge(session.paymentStatus)
                    }
                }
            }

            if (!paymentConfirmed && !alreadyCompleted) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ParkoAmberLight,
                    contentColor = ParkoCharcoal,
                    modifier = Modifier.fillMaxWidth().parkoCardOutline(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(tr("Paiement en attente", "Payment pending"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ParkoAmberDark)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onVerifyMobileMoney(PaymentMethod.CASH) },
                                modifier = Modifier.weight(1f)
                            ) { Text(tr("Reçu Espèces", "Cash Received"), fontWeight = FontWeight.Bold) }
                            OutlinedButton(
                                onClick = { onVerifyMobileMoney(PaymentMethod.MIXX_BY_YAS) },
                                modifier = Modifier.weight(1f)
                            ) { Text(tr("Vérifier MoMo", "Verify MoMo"), fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }

            if (!alreadyCompleted) {
                // Section 1.4: Reinforced Physical Check & Non-conformity Alert
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().parkoCardOutline(18.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            tr("Contrôle physique comparatif", "Physical comparison check"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            tr(
                                "Comparez physiquement : Type ($vTypeLabel), Plaque (${session.plateNumber}), Modèle (${session.vehicleLabel}), Couleur (${session.vehicleColor}).",
                                "Compare physically: Type ($vTypeLabel), Plate (${session.plateNumber}), Model (${session.vehicleLabel}), Color (${session.vehicleColor})."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            onClick = { if (!mismatchSelected) physicalChecked = !physicalChecked },
                            shape = RoundedCornerShape(14.dp),
                            color = if (physicalChecked && !mismatchSelected) ParkoEmeraldLight else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, if (physicalChecked && !mismatchSelected) ParkoEmerald else MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = physicalChecked && !mismatchSelected,
                                    onCheckedChange = { if (!mismatchSelected) physicalChecked = it },
                                    modifier = Modifier.testTag("physical_check_checkbox")
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    tr("Contrôle physique du véhicule effectué", "Physical vehicle check completed"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                mismatchSelected = true
                                physicalChecked = false
                                onReportPhysicalMismatch(
                                    if (en) "Vehicle does not match ticket details (${session.plateNumber})"
                                    else "Les informations du véhicule présenté ne correspondent pas au ticket (${session.plateNumber})."
                                )
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ParkoRedDark),
                            modifier = Modifier.fillMaxWidth().testTag("report_physical_mismatch_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(tr("Signaler une différence physique", "Report physical mismatch"), fontWeight = FontWeight.Bold)
                        }

                        if (mismatchSelected || session.physicalMismatchReported) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ParkoRedLight,
                                contentColor = ParkoRedDark,
                                modifier = Modifier.fillMaxWidth().parkoCardOutline(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        tr(
                                            "Attention : les informations du véhicule ne correspondent pas au ticket. Sortie refusée.",
                                            "Warning: vehicle information does not match the ticket. Exit refused."
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ParkoRedDark
                                    )
                                    Button(
                                        onClick = { showIncidentDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = ParkoRed),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(tr("Signaler un incident", "Report an incident"), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 1.3: Secret Withdrawal Code Verification
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().parkoCardOutline(18.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            tr("Vérification de l'autorisation de retrait", "Withdrawal authorization check"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (secretCodeVerified) {
                            StatusPill(
                                label = tr("Code secret vérifié ✓", "Secret code verified ✓"),
                                containerColor = ParkoEmeraldLight,
                                contentColor = ParkoEmeraldDark,
                                icon = Icons.Filled.Verified
                            )
                        } else if (session.withdrawalBlockedForManager) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ParkoRedLight,
                                contentColor = ParkoRedDark,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        tr(
                                            "Vérification bloquée après ${session.withdrawalAttempts} tentatives erronées. Intervention du gestionnaire ou superviseur requise.",
                                            "Blocked after ${session.withdrawalAttempts} failed attempts. Manager or supervisor intervention required."
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isManagerOrSup) {
                                        Button(
                                            onClick = { showUnlockDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = ParkoAmberDark),
                                            modifier = Modifier.fillMaxWidth().testTag("manager_unlock_withdrawal_button")
                                        ) {
                                            Text(tr("Débloquer la vérification", "Unlock verification"), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = secretCodeInput,
                                onValueChange = { secretCodeInput = it.filter { ch -> ch.isDigit() }.take(6) },
                                label = { Text(tr("Saisir le code secret de retrait", "Enter secret withdrawal code")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                colors = parkoTextFieldColors(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().testTag("withdrawal_code_input")
                            )
                            if (session.withdrawalAttempts > 0) {
                                Text(
                                    tr(
                                        "Tentatives échouées : ${session.withdrawalAttempts} / ${ParkoSecurityConfig.withdrawalManagerBlockAttempts}",
                                        "Failed attempts: ${session.withdrawalAttempts} / ${ParkoSecurityConfig.withdrawalManagerBlockAttempts}"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ParkoRedDark
                                )
                            }
                            Button(
                                onClick = { onVerifyWithdrawalCode(secretCodeInput) },
                                enabled = secretCodeInput.length == 6 && !lockedByAnother,
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("verify_withdrawal_code_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(tr("Vérifier le code", "Verify code"), fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Exceptional recovery without secret code (Section 3)
                        if (latestRecovery != null) {
                            val rStatus = RecoveryStatus.entries.firstOrNull { it.name == latestRecovery.status } ?: RecoveryStatus.PENDING
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when (rStatus) {
                                    RecoveryStatus.APPROVED -> ParkoEmeraldLight
                                    RecoveryStatus.REJECTED -> ParkoRedLight
                                    RecoveryStatus.PENDING -> ParkoAmberLight
                                },
                                contentColor = ParkoCharcoal,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        "${tr("Récupération exceptionnelle", "Exceptional recovery")} : ${rStatus.localizedLabel(en)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("${latestRecovery.reasonType} • ${latestRecovery.claimantName}", style = MaterialTheme.typography.bodySmall)
                                    if (!latestRecovery.decisionReason.isNullOrBlank()) {
                                        Text("${tr("Décision", "Decision")}: ${latestRecovery.decisionReason}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                    if (rStatus == RecoveryStatus.PENDING && isManagerOrSup) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { recoveryDecisionApprove = true },
                                                modifier = Modifier.weight(1f).testTag("approve_recovery_button")
                                            ) { Text(tr("Approuver la récupération", "Approve"), fontWeight = FontWeight.Bold) }
                                            OutlinedButton(
                                                onClick = { recoveryDecisionApprove = false },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ParkoRedDark),
                                                modifier = Modifier.weight(1f).testTag("reject_recovery_button")
                                            ) { Text(tr("Refuser", "Reject"), fontWeight = FontWeight.Bold) }
                                        }
                                    }
                                }
                            }
                        }

                        if (!secretCodeVerified && !recoveryApproved) {
                            OutlinedButton(
                                onClick = { showRecoveryDialog = true },
                                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("exceptional_recovery_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(tr("Client sans code de retrait", "Client without withdrawal code"), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Section 1.5: Strict Authorize Exit Button
                Button(
                    onClick = { showExitConfirmDialog = true },
                    enabled = canAuthorizeExit,
                    modifier = Modifier.fillMaxWidth().height(54.dp).testTag("authorize_exit_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Autoriser la sortie", "Authorize exit"), fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onOpenQr,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("open_send_qr_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Imprimer / Envoyer le ticket QR", "Print / Send QR ticket"), fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = { showIncidentDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ParkoRed),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("report_incident_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Report, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(tr("Signaler un incident", "Report incident"), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = { Text(tr("Validation définitive", "Final confirmation"), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    tr(
                        "Confirmer définitivement la sortie de ce véhicule ?",
                        "Confirm final exit of this vehicle?"
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmDialog = false
                        onAuthorizeExit(physicalChecked, auditReason)
                    },
                    modifier = Modifier.testTag("confirm_final_exit_button")
                ) {
                    Text(tr("Confirmer la sortie", "Confirm exit"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text(tr("Annuler", "Cancel"), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showUnlockDialog) {
        var unlockReason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showUnlockDialog = false },
            title = { Text(tr("Débloquer la vérification", "Unlock verification"), fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = unlockReason,
                    onValueChange = { unlockReason = it },
                    label = { Text(tr("Motif de l'intervention *", "Intervention reason *")) },
                    minLines = 2,
                    colors = parkoTextFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    showUnlockDialog = false
                    onUnlockBlockedWithdrawal(unlockReason)
                }) { Text(tr("Débloquer", "Unlock"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showUnlockDialog = false }) { Text(tr("Annuler", "Cancel")) }
            }
        )
    }

    if (showRecoveryDialog) {
        val reasons = listOf("Ticket perdu", "Téléphone éteint", "Code secret oublié", "Problème d'affichage")
        var selectedReason by remember { mutableStateOf(reasons.first()) }
        var claimantName by remember { mutableStateOf(session.ownerName) }
        var declaredPlate by remember { mutableStateOf(session.plateNumber) }
        var desc by remember { mutableStateOf("") }
        var confirmPhysical by remember { mutableStateOf(physicalChecked) }

        AlertDialog(
            onDismissRequest = { showRecoveryDialog = false },
            title = { Text(tr("Récupération exceptionnelle", "Exceptional recovery"), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reasons.forEach { r ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedReason == r, onClick = { selectedReason = r })
                            Text(r, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    OutlinedTextField(
                        value = claimantName,
                        onValueChange = { claimantName = it },
                        label = { Text(tr("Nom du client demandeur *", "Claimant name *")) },
                        singleLine = true,
                        colors = parkoTextFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = declaredPlate,
                        onValueChange = { declaredPlate = it.uppercase() },
                        label = { Text(tr("Plaque déclarée *", "Declared plate *")) },
                        singleLine = true,
                        colors = parkoTextFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text(tr("Description et justificatif *", "Description & proof *")) },
                        minLines = 2,
                        colors = parkoTextFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = confirmPhysical, onCheckedChange = { confirmPhysical = it })
                        Text(tr("Contrôle physique confirmé", "Physical check confirmed"), style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showRecoveryDialog = false
                    onSubmitExceptionalRecovery(selectedReason, claimantName, declaredPlate, desc, confirmPhysical)
                }) { Text(tr("Soumettre au responsable", "Submit for approval"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showRecoveryDialog = false }) { Text(tr("Annuler", "Cancel")) }
            }
        )
    }

    recoveryDecisionApprove?.let { approve ->
        val rec = latestRecovery
        if (rec != null) {
            var reason by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { recoveryDecisionApprove = null },
                title = {
                    Text(
                        if (approve) tr("Approuver la récupération", "Approve recovery")
                        else tr("Refuser la récupération", "Reject recovery"),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text(tr("Motif obligatoire *", "Mandatory reason *")) },
                        minLines = 2,
                        colors = parkoTextFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        onReviewRecovery(rec, approve, reason)
                        recoveryDecisionApprove = null
                    }) { Text(tr("Confirmer", "Confirm"), fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { recoveryDecisionApprove = null }) { Text(tr("Annuler", "Cancel")) }
                }
            )
        }
    }

    if (showIncidentDialog) {
        var selectedType by remember { mutableStateOf(IncidentType.VEHICLE_MISMATCH) }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showIncidentDialog = false },
            title = { Text(tr("Signaler un incident", "Report an incident")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("Type d'incident", "Incident type"), style = MaterialTheme.typography.labelMedium)
                    IncidentType.entries.take(4).forEach { t ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedType == t, onClick = { selectedType = t })
                            Text(t.localizedLabel(en), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text(tr("Description détaillée *", "Detailed description *")) },
                        minLines = 2,
                        colors = parkoTextFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onReportIncident(selectedType, desc)
                    showIncidentDialog = false
                }) { Text(tr("Envoyer au gestionnaire", "Send to manager")) }
            },
            dismissButton = {
                TextButton(onClick = { showIncidentDialog = false }) { Text(tr("Annuler", "Cancel")) }
            }
        )
    }
}

@Composable
private fun VerificationInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProHistoryScreen(
    sessions: List<ParkingSessionEntity>,
    onSelectSession: (ParkingSessionEntity) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf("TOUS") }

    val filtered = remember(sessions, search, tab) {
        sessions.filter { s ->
            val matchesTab = when (tab) {
                "PRESENTS" -> s.status != SessionStatus.COMPLETED.name
                "SORTIS" -> s.status == SessionStatus.COMPLETED.name
                else -> true
            }
            val matchesSearch = search.isBlank() ||
                s.plateNumber.contains(search, ignoreCase = true) ||
                s.ownerName.contains(search, ignoreCase = true) ||
                s.referenceCode.contains(search, ignoreCase = true)
            matchesTab && matchesSearch
        }
    }

    Column(modifier = Modifier.fillMaxSize().parkoAtmosphericBackground()) {
        ParkoTopBar(title = tr("Historique du parking", "Parking history"))

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text(tr("Rechercher plaque, propriétaire, code...", "Search plate, owner, code...")) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            colors = parkoTextFieldColors(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = tab == "TOUS",
                onClick = { tab = "TOUS" },
                label = { Text("${tr("Tous", "All")} (${sessions.size})", fontWeight = FontWeight.Bold) },
                colors = parkoFilterChipColors(),
                border = parkoFilterChipBorder(tab == "TOUS")
            )
            FilterChip(
                selected = tab == "PRESENTS",
                onClick = { tab = "PRESENTS" },
                label = { Text(tr("Présents", "Parked"), fontWeight = FontWeight.Bold) },
                colors = parkoFilterChipColors(),
                border = parkoFilterChipBorder(tab == "PRESENTS")
            )
            FilterChip(
                selected = tab == "SORTIS",
                onClick = { tab = "SORTIS" },
                label = { Text(tr("Sortis", "Exited"), fontWeight = FontWeight.Bold) },
                colors = parkoFilterChipColors(),
                border = parkoFilterChipBorder(tab == "SORTIS")
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered, key = { it.id }) { s ->
                Surface(
                    onClick = { onSelectSession(s) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth().parkoCardOutline(16.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${s.vehicleLabel} • ${s.plateNumber}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${s.ownerName} • ${s.attendantName}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${s.referenceCode} • ${formatDateTime(s.entryTime)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = ParkoBlue)
                            if (s.exitWithoutQr) {
                                Text(tr("Sortie sans QR", "Exit without QR"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ParkoAmberDark)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            SessionStatusBadge(s.status)
                            Spacer(Modifier.height(4.dp))
                            Text(formatFcfa(s.feeFcfa), fontFamily = ParkoMonoFamily, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FinancialReportsScreen(
    parking: ParkingLocationEntity?,
    payments: List<PaymentRecordEntity>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val en = LocalAppLanguage.current == AppLanguage.EN
    var period by remember { mutableStateOf("30j") }
    val parkingPayments = payments.filter { it.purpose == "STATIONNEMENT" }
    val confirmedPayments = parkingPayments.filter { it.status == PaymentStatus.CONFIRMED.name }
    val pendingTotal = parkingPayments.filter { it.status == PaymentStatus.PENDING.name }.sumOf { it.amountFcfa }
    val totalConfirmed = confirmedPayments.sumOf { it.amountFcfa }
    val cashTotal = confirmedPayments.filter { it.method == PaymentMethod.CASH.name }.sumOf { it.amountFcfa }
    val momoTotal = confirmedPayments.filter { it.method != PaymentMethod.CASH.name }.sumOf { it.amountFcfa }

    val tier = SubscriptionTier.entries.firstOrNull { it.name == parking?.subscriptionTier } ?: SubscriptionTier.STANDARD

    val chartData = remember(period, totalConfirmed, en) {
        when (period) {
            "7j" -> if (en) listOf("Mon" to 12500, "Tue" to 15000, "Wed" to 14200, "Thu" to 18400, "Fri" to 21000, "Sat" to 24500, "Sun" to 18900)
            else listOf("Lun" to 12500, "Mar" to 15000, "Mer" to 14200, "Jeu" to 18400, "Ven" to 21000, "Sam" to 24500, "Dim" to 18900)
            "90j" -> if (en) listOf("Mo 1" to 98000, "Mo 2" to 112000, "Mo 3" to totalConfirmed)
            else listOf("Mois 1" to 98000, "Mois 2" to 112000, "Mois 3" to totalConfirmed)
            else -> listOf("W1" to 24000, "W2" to 29500, "W3" to 33000, "W4" to (totalConfirmed - 86500).coerceAtLeast(18000))
        }
    }

    Column(modifier = Modifier.fillMaxSize().parkoAtmosphericBackground()) {
        ParkoTopBar(title = tr("Rapports financiers", "Financial reports"), onBack = onBack)

        Column(
            modifier = Modifier
                .widthIn(max = 580.dp)
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("7j" to tr("7j", "7d"), "30j" to tr("30j", "30d"), "90j" to tr("90j", "90d")).forEach { (key, label) ->
                    FilterChip(
                        selected = period == key,
                        onClick = { period = key },
                        label = { Text(label, fontWeight = FontWeight.Bold) },
                        colors = parkoFilterChipColors(),
                        border = parkoFilterChipBorder(period == key)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth().parkoCardOutline(22.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = formatFcfa(totalConfirmed),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ParkoMonoFamily
                            )
                            Text(tr("Revenus ($period)", "Revenue ($period)"), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusPill("+12%", ParkoEmeraldLight, ParkoEmeraldDark, Icons.Filled.TrendingUp)
                    }

                    RevenueBarChart(values = chartData, modifier = Modifier.fillMaxWidth())

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Payments, contentDescription = null, tint = ParkoAmber, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(tr("Espèces", "Cash"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(formatFcfa(cashTotal), fontWeight = FontWeight.Bold, fontFamily = ParkoMonoFamily)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = ParkoBlue, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Mobile Money", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(formatFcfa(momoTotal), fontWeight = FontWeight.Bold, fontFamily = ParkoMonoFamily)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("En attente", "Pending"), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ParkoAmberDark)
                        Text(formatFcfa(pendingTotal), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, fontFamily = ParkoMonoFamily, color = ParkoAmberDark)
                    }
                }
            }

            Button(
                onClick = {
                    val csvHeader = "Reference,Moyen,MontantFCFA,Statut,Gardien\n"
                    val csvRows = parkingPayments.joinToString("\n") {
                        "${it.referenceCode},${it.method},${it.amountFcfa},${it.status},${it.attendantName}"
                    }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_TEXT, csvHeader + csvRows)
                    }
                    context.startActivity(
                        Intent.createChooser(
                            intent,
                            if (en) "Export PARKO CSV report" else "Exporter rapport CSV PARKO"
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.FileDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (tier.allowsExports) tr("Exporter CSV", "Export CSV")
                    else tr("Partager CSV", "Share CSV"),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun AttendantsScreen(
    parking: ParkingLocationEntity?,
    attendants: List<AttendantEntity>,
    onBack: () -> Unit,
    onInvite: (String, String, UserRole, () -> Unit) -> Unit,
    onToggleStatus: (AttendantEntity) -> Unit
) {
    val en = LocalAppLanguage.current == AppLanguage.EN
    var showInviteDialog by remember { mutableStateOf(false) }
    val tier = SubscriptionTier.entries.firstOrNull { it.name == parking?.subscriptionTier } ?: SubscriptionTier.STANDARD

    Column(modifier = Modifier.fillMaxSize().parkoAtmosphericBackground()) {
        ParkoTopBar(
            title = tr("Gestion des gardiens", "Attendants"),
            onBack = onBack,
            actions = {
                FilledTonalButton(
                    onClick = { showInviteDialog = true },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = ParkoBlueLight,
                        contentColor = ParkoBlueDark
                    ),
                    border = BorderStroke(1.2.dp, ParkoBlue.copy(alpha = 0.45f)),
                    modifier = Modifier.testTag("invite_attendant_button")
                ) {
                    Text(tr("Inviter +", "Invite +"), fontWeight = FontWeight.Bold)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "${attendants.count { it.isActive }} / ${tier.maxAttendants} max • ${tier.localizedTitle(en)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ParkoBlue
                )
            }

            items(attendants, key = { it.id }) { att ->
                val badge = BadgeTier.entries.firstOrNull { it.name == att.badgeTier } ?: BadgeTier.BRONZE
                val role = UserRole.entries.firstOrNull { it.name == att.role } ?: UserRole.ATTENDANT
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth().parkoCardOutline(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(ParkoBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(att.fullName.take(1), fontWeight = FontWeight.Bold, color = ParkoBlue)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(att.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${role.localizedLabel(en)} • ${att.phone}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusPill(
                                label = if (att.isActive) tr("Actif", "Active") else tr("Suspendu", "Suspended"),
                                containerColor = if (att.isActive) ParkoEmeraldLight else ParkoRedLight,
                                contentColor = if (att.isActive) ParkoEmeraldDark else ParkoRedDark
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${att.registrationsCount} ${tr("entrées", "in")} • ${att.exitsCount} ${tr("sorties", "out")} • ${att.procedureScore}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            StatusPill("Badge ${badge.label}", ParkoAmberLight, ParkoAmberDark, Icons.Filled.EmojiEvents)
                        }
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { onToggleStatus(att) }) {
                                Text(if (att.isActive) tr("Suspendre l'accès", "Suspend access") else tr("Réactiver", "Reactivate"), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInviteDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("+228 ") }
        var role by remember { mutableStateOf(UserRole.ATTENDANT) }

        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text(tr("Inviter un gardien", "Invite an attendant"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(tr("Nom complet *", "Full name *")) }, colors = parkoTextFieldColors(), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(tr("Téléphone (+228...) *", "Phone (+228...) *")) }, colors = parkoTextFieldColors(), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = role == UserRole.ATTENDANT,
                            onClick = { role = UserRole.ATTENDANT },
                            label = { Text(UserRole.ATTENDANT.localizedLabel(en), fontWeight = FontWeight.Bold) },
                            colors = parkoFilterChipColors(),
                            border = parkoFilterChipBorder(role == UserRole.ATTENDANT)
                        )
                        FilterChip(
                            selected = role == UserRole.SUPERVISOR,
                            onClick = { role = UserRole.SUPERVISOR },
                            label = { Text(UserRole.SUPERVISOR.localizedLabel(en), fontWeight = FontWeight.Bold) },
                            colors = parkoFilterChipColors(),
                            border = parkoFilterChipBorder(role == UserRole.SUPERVISOR)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { onInvite(name, phone, role) { showInviteDialog = false } }) {
                    Text(tr("Inviter", "Invite"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) { Text(tr("Annuler", "Cancel"), fontWeight = FontWeight.Bold) }
            }
        )
    }
}

@Composable
fun IncidentsScreen(
    incidents: List<IncidentEntity>,
    onBack: () -> Unit,
    onReport: (IncidentType, String, () -> Unit) -> Unit,
    onResolve: (IncidentEntity, String) -> Unit
) {
    val en = LocalAppLanguage.current == AppLanguage.EN
    var showNewDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().parkoAtmosphericBackground()) {
        ParkoTopBar(
            title = tr("Incidents", "Incidents"),
            onBack = onBack,
            actions = {
                IconButton(onClick = { showNewDialog = true }) {
                    Icon(Icons.Filled.AddAlert, contentDescription = tr("Nouvel incident", "New incident"), tint = ParkoRed)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (incidents.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.Verified,
                        title = tr("Aucun incident signalé", "No incidents reported")
                    )
                }
            } else {
                items(incidents, key = { it.id }) { inc ->
                    val type = IncidentType.entries.firstOrNull { it.name == inc.type } ?: IncidentType.REGISTRATION_ERROR
                    val isResolved = inc.status == IncidentStatus.RESOLVED.name
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth().parkoCardOutline(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(type.localizedLabel(en), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                StatusPill(
                                    label = if (isResolved) tr("Résolu", "Resolved") else tr("Ouvert", "Open"),
                                    containerColor = if (isResolved) ParkoEmeraldLight else ParkoRedLight,
                                    contentColor = if (isResolved) ParkoEmeraldDark else ParkoRedDark
                                )
                            }
                            Text(inc.description, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${inc.attendantName} • ${inc.sessionRef ?: inc.parkingName} • ${formatDateTime(inc.createdAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            inc.resolutionNote?.let {
                                Text("${tr("Décision", "Decision")} : $it", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ParkoEmerald)
                            }
                            if (!isResolved) {
                                TextButton(onClick = { onResolve(inc, if (en) "Verified and closed by manager" else "Vérifié et clôturé par le gestionnaire") }) {
                                    Text(tr("Marquer comme résolu", "Mark as resolved"), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewDialog) {
        var type by remember { mutableStateOf(IncidentType.PAYMENT_DISPUTE) }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewDialog = false },
            title = { Text(tr("Déclarer un incident", "Report an incident"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    IncidentType.entries.take(5).forEach { t ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = type == t, onClick = { type = t })
                            Text(t.localizedLabel(en), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description *") }, colors = parkoTextFieldColors(), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = { onReport(type, desc) { showNewDialog = false } }) { Text(tr("Enregistrer", "Save"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showNewDialog = false }) { Text(tr("Annuler", "Cancel"), fontWeight = FontWeight.Bold) }
            }
        )
    }
}
