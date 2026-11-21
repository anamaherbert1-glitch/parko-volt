package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import com.example.data.local.*
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.i18n.*
import com.example.ui.theme.*
import java.util.Calendar

@Composable
fun ProDashboardScreen(
    user: UserEntity,
    parking: ParkingLocationEntity?,
    sessions: List<ParkingSessionEntity>,
    payments: List<PaymentRecordEntity>,
    attendants: List<AttendantEntity>,
    incidents: List<IncidentEntity>,
    onRegisterVehicle: () -> Unit,
    onOpenScanner: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenAttendants: () -> Unit,
    onOpenIncidents: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onSelectSession: (ParkingSessionEntity) -> Unit,
    onSwitchToPersonal: () -> Unit
) {
    val en = LocalAppLanguage.current == AppLanguage.EN
    val activeCount = sessions.count { it.status == SessionStatus.ACTIVE.name || it.status == SessionStatus.NEEDS_VERIFICATION.name }
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
                            fontFamily = JetBrainsMonoFamily,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = onOpenReports,
                    label = { Text(tr("Rapports", "Reports"), fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Filled.BarChart, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.testTag("pro_reports_chip")
                )
                AssistChip(
                    onClick = onOpenAttendants,
                    label = { Text("${tr("Gardiens", "Attendants")} (${attendants.count { it.isActive }})", fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.testTag("pro_attendants_chip")
                )
                AssistChip(
                    onClick = onOpenIncidents,
                    label = { Text("Incidents ($openIncidentsCount)", fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Filled.Report, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    modifier = Modifier.testTag("pro_incidents_chip")
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
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
                            Text(formatFcfa(s.feeFcfa), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, fontFamily = JetBrainsMonoFamily)
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
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            OutlinedTextField(
                value = plate,
                onValueChange = { plate = it.uppercase() },
                label = { Text(tr("Plaque * (ex: TG-1234-AA)", "Plate * (e.g. TG-1234-AA)")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("entry_plate_input")
            )
            OutlinedTextField(
                value = brandModel,
                onValueChange = { brandModel = it },
                label = { Text(tr("Marque / Modèle", "Brand / Model")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = color,
                onValueChange = { color = it },
                label = { Text(tr("Couleur", "Color")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = ownerName,
                onValueChange = { ownerName = it },
                label = { Text(tr("Propriétaire *", "Owner *")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("entry_owner_input")
            )
            OutlinedTextField(
                value = ownerPhone,
                onValueChange = { ownerPhone = it },
                label = { Text(tr("Téléphone (+228...)", "Phone (+228...)")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {
                        ownerName = "Amadou Diallo"
                        ownerPhone = "+228 90 12 34 56"
                    },
                    label = { Text("Amadou (+228 90...)", fontWeight = FontWeight.SemiBold) }
                )
                AssistChip(
                    onClick = {
                        ownerName = "Codjo Agbeko"
                        ownerPhone = "+228 98 11 22 33"
                    },
                    label = { Text("Codjo (+228 98...)", fontWeight = FontWeight.SemiBold) }
                )
            }
            OutlinedTextField(
                value = feeText,
                onValueChange = { feeText = it.filter { c -> c.isDigit() } },
                label = { Text(tr("Tarif (FCFA)", "Fee (FCFA)")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(tr("Paiement", "Payment"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaymentMethod.entries.forEach { method ->
                    FilterChip(
                        selected = paymentMethod == method,
                        onClick = { paymentMethod = method },
                        label = { Text(method.localizedLabel(en), fontWeight = FontWeight.Bold) }
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
    openedWithoutQr: Boolean,
    onBack: () -> Unit,
    onOpenQr: () -> Unit,
    onVerifyMobileMoney: (PaymentMethod) -> Unit,
    onAuthorizeExit: (physicalChecked: Boolean, withoutQr: Boolean, auditReason: String) -> Unit,
    onReportIncident: (IncidentType, String) -> Unit
) {
    val en = LocalAppLanguage.current == AppLanguage.EN
    var physicalChecked by remember { mutableStateOf(false) }
    var withoutQr by remember { mutableStateOf(openedWithoutQr) }
    var auditReason by remember {
        mutableStateOf(
            if (openedWithoutQr) {
                if (en) "Client lost QR ticket — ID checked" else "Client ayant égaré son ticket QR — pièce d'identité vérifiée"
            } else ""
        )
    }
    var showIncidentDialog by remember { mutableStateOf(false) }

    val alreadyCompleted = session.status == SessionStatus.COMPLETED.name
    val paymentConfirmed = session.paymentStatus == PaymentStatus.CONFIRMED.name

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
                modifier = Modifier.fillMaxWidth().height(170.dp).parkoCardOutline(14.dp)
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = when {
                    alreadyCompleted -> ParkoAmberLight
                    session.status == SessionStatus.NEEDS_VERIFICATION.name -> ParkoRedLight
                    else -> ParkoEmeraldLight
                },
                contentColor = ParkoCharcoal,
                modifier = Modifier.fillMaxWidth().parkoCardOutline(16.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when {
                            alreadyCompleted -> Icons.Filled.Warning
                            session.status == SessionStatus.NEEDS_VERIFICATION.name -> Icons.Filled.GppMaybe
                            else -> Icons.Filled.CheckCircle
                        },
                        contentDescription = null,
                        tint = when {
                            alreadyCompleted -> ParkoAmberDark
                            session.status == SessionStatus.NEEDS_VERIFICATION.name -> ParkoRedDark
                            else -> ParkoEmeraldDark
                        },
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = when {
                                alreadyCompleted -> tr("Véhicule déjà sorti", "Vehicle already exited")
                                session.status == SessionStatus.NEEDS_VERIFICATION.name -> tr("Vérification requise", "Verification required")
                                else -> tr("Ticket valide", "Valid ticket")
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${session.referenceCode} • ${formatDateTime(session.entryTime)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth().parkoCardOutline(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${session.vehicleLabel} (${session.vehicleColor})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${tr("Plaque", "Plate")} : ${session.plateNumber}", fontFamily = JetBrainsMonoFamily, color = ParkoBlue, fontWeight = FontWeight.Bold)
                    Text("${tr("Propriétaire", "Owner")} : ${session.ownerName} • ${session.ownerPhone.ifBlank { tr("Non renseigné", "N/A") }}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("${tr("Gardien", "Attendant")} : ${session.attendantName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(PaymentMethodLabel(session.paymentMethod), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(formatFcfa(session.feeFcfa), fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold)
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
                Surface(
                    onClick = { physicalChecked = !physicalChecked },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, if (physicalChecked) ParkoEmerald else MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = physicalChecked, onCheckedChange = { physicalChecked = it })
                        Spacer(Modifier.width(8.dp))
                        Text(
                            tr("Contrôle physique du véhicule effectué", "Physical vehicle check completed"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (withoutQr) {
                    OutlinedTextField(
                        value = auditReason,
                        onValueChange = { auditReason = it },
                        label = { Text(tr("Motif sortie sans QR *", "Reason without QR *")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = { onAuthorizeExit(physicalChecked, withoutQr, auditReason) },
                    enabled = physicalChecked && paymentConfirmed,
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
            }
            Spacer(modifier = Modifier.height(24.dp))
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
                        minLines = 2
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = tab == "TOUS", onClick = { tab = "TOUS" }, label = { Text("${tr("Tous", "All")} (${sessions.size})", fontWeight = FontWeight.Bold) })
            FilterChip(selected = tab == "PRESENTS", onClick = { tab = "PRESENTS" }, label = { Text(tr("Présents", "Parked"), fontWeight = FontWeight.Bold) })
            FilterChip(selected = tab == "SORTIS", onClick = { tab = "SORTIS" }, label = { Text(tr("Sortis", "Exited"), fontWeight = FontWeight.Bold) })
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
                            Text(formatFcfa(s.feeFcfa), fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
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
                    FilterChip(selected = period == key, onClick = { period = key }, label = { Text(label, fontWeight = FontWeight.Bold) })
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
                                fontFamily = JetBrainsMonoFamily
                            )
                            Text(tr("Revenus ($period)", "Revenue ($period)"), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusPill("+12%", ParkoEmeraldLight, ParkoEmeraldDark, Icons.Filled.TrendingUp)
                    }

                    RevenueBarChart(values = chartData, modifier = Modifier.fillMaxWidth())

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Payments, contentDescription = null, tint = ParkoAmber, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(tr("Espèces", "Cash"), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(formatFcfa(cashTotal), fontWeight = FontWeight.Bold, fontFamily = JetBrainsMonoFamily)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = ParkoBlue, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Mobile Money", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(formatFcfa(momoTotal), fontWeight = FontWeight.Bold, fontFamily = JetBrainsMonoFamily)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("En attente", "Pending"), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ParkoAmberDark)
                        Text(formatFcfa(pendingTotal), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, fontFamily = JetBrainsMonoFamily, color = ParkoAmberDark)
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
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(tr("Nom complet *", "Full name *")) })
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(tr("Téléphone (+228...) *", "Phone (+228...) *")) })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = role == UserRole.ATTENDANT,
                            onClick = { role = UserRole.ATTENDANT },
                            label = { Text(UserRole.ATTENDANT.localizedLabel(en), fontWeight = FontWeight.Bold) }
                        )
                        FilterChip(
                            selected = role == UserRole.SUPERVISOR,
                            onClick = { role = UserRole.SUPERVISOR },
                            label = { Text(UserRole.SUPERVISOR.localizedLabel(en), fontWeight = FontWeight.Bold) }
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
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description *") })
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
