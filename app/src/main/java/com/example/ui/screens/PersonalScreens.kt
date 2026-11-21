package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.*
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.i18n.*
import com.example.ui.theme.*
import com.example.util.TicketPrinter

@Composable
fun PersonalHomeScreen(
    user: UserEntity,
    themeMode: ThemeMode,
    activeTicketsCount: Int,
    recentSessions: List<ParkingSessionEntity>,
    unreadNotifications: Int,
    canAccessPro: Boolean,
    application: ManagerApplicationEntity?,
    isAdmin: Boolean,
    onToggleTheme: () -> Unit,
    onOpenVehicles: () -> Unit,
    onOpenTickets: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenTicketDetail: (ParkingSessionEntity) -> Unit,
    onOpenSubscriptions: () -> Unit,
    onSwitchToPro: () -> Unit,
    onSwitchToAdmin: () -> Unit
) {
    val en = LocalAppLanguage.current == AppLanguage.EN
    LazyColumn(
        modifier = Modifier.fillMaxSize().parkoAtmosphericBackground(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF062A7A), ParkoBlueDark, ParkoBlue, Color(0xFF1D6EF5))
                        )
                    )
                    .drawBehind {
                        val w = size.width
                        val h = size.height
                        drawCircle(
                            color = Color.White.copy(alpha = 0.09f),
                            radius = size.maxDimension * 0.44f,
                            center = Offset(w * 0.92f, h * 0.14f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.14f),
                            radius = size.maxDimension * 0.56f,
                            center = Offset(w * 0.92f, h * 0.14f),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawCircle(
                            color = ParkoEmerald.copy(alpha = 0.18f),
                            radius = size.minDimension * 0.28f,
                            center = Offset(w * 0.06f, h * 0.90f)
                        )
                        val wave = androidx.compose.ui.graphics.Path().apply {
                            moveTo(-w * 0.05f, h * 0.72f)
                            cubicTo(w * 0.35f, h * 0.52f, w * 0.65f, h * 0.95f, w * 1.05f, h * 0.68f)
                        }
                        drawPath(
                            path = wave,
                            color = Color.White.copy(alpha = 0.14f),
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    }
                    .statusBarsPadding()
                    .padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (canAccessPro) {
                            FilledTonalButton(
                                onClick = onSwitchToPro,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.24f),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("switch_pro_chip")
                            ) {
                                Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(7.dp))
                                Text(tr("Espace Pro", "Pro Space"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        } else if (isAdmin) {
                            FilledTonalButton(
                                onClick = onSwitchToAdmin,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.24f),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("switch_admin_icon")
                            ) {
                                Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(7.dp))
                                Text("Admin", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ParkoLogoBadge(size = 32.dp, inverted = true)
                                Text(
                                    text = "PARKO",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isAdmin && canAccessPro) {
                                IconButton(
                                    onClick = onSwitchToAdmin,
                                    modifier = Modifier.minimumInteractiveComponentSize().testTag("switch_admin_icon")
                                ) {
                                    Icon(Icons.Filled.Shield, contentDescription = tr("Super Admin", "Super Admin"), tint = Color.White, modifier = Modifier.size(25.dp))
                                }
                            }
                            IconButton(
                                onClick = onToggleTheme,
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("home_theme_toggle")
                            ) {
                                Icon(
                                    imageVector = if (themeMode == ThemeMode.DARK) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                    contentDescription = tr("Changer de thème clair ou sombre", "Switch light or dark theme"),
                                    tint = Color.White,
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                            Box {
                                IconButton(
                                    onClick = onOpenNotifications,
                                    modifier = Modifier.minimumInteractiveComponentSize().testTag("home_notif_button")
                                ) {
                                    Icon(Icons.Filled.Notifications, contentDescription = tr("Notifications", "Notifications"), tint = Color.White, modifier = Modifier.size(25.dp))
                                }
                                if (unreadNotifications > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(ParkoEmerald)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.28f))
                                    .clickable(onClick = onOpenProfile),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.firstName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                    ) {
                        Text(
                            text = tr("Bonjour,", "Hello,"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.90f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = user.firstName,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Surface(
                        onClick = onOpenTickets,
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White.copy(alpha = 0.22f),
                        contentColor = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.3.dp, Color.White.copy(alpha = 0.38f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_active_tickets_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr("Mes tickets", "My tickets"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.95f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tr(
                                        "$activeTicketsCount actif${if (activeTicketsCount > 1) "s" else ""}",
                                        "$activeTicketsCount active"
                                    ),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.QrCode2,
                                    contentDescription = tr("Voir mes tickets QR", "View my QR tickets"),
                                    tint = ParkoBlue,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!canAccessPro && application?.status == ApplicationStatus.APPROVED.name) {
            val recTier = SubscriptionTier.recommendedFor(
                application.estimatedDailyVolume,
                application.plannedAttendantsCount
            )
            item {
                Surface(
                    onClick = onOpenSubscriptions,
                    shape = RoundedCornerShape(20.dp),
                    color = ParkoEmeraldLight,
                    contentColor = ParkoCharcoal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 14.dp)
                        .parkoCardOutline(20.dp)
                        .testTag("activate_pro_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ParkoEmerald),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                tr("Activer votre espace Pro", "Activate your Pro space"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ParkoEmeraldDark
                            )
                            Text(
                                tr("Offre conseillée : ${recTier.localizedTitle(en)}", "Recommended: ${recTier.localizedTitle(en)}"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ParkoCharcoal
                            )
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ParkoEmeraldDark, modifier = Modifier.size(26.dp))
                    }
                }
            }
        }

        // 2x2 Quick Action Grid (No payment mode for simple user)
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickActionTile(
                        icon = Icons.Filled.DirectionsCar,
                        title = tr("Mes véhicules", "My vehicles"),
                        tint = ParkoBlue,
                        onClick = onOpenVehicles,
                        modifier = Modifier.weight(1f).testTag("tile_vehicles")
                    )
                    QuickActionTile(
                        icon = Icons.Filled.History,
                        title = tr("Historique", "History"),
                        tint = ParkoBlueMid,
                        onClick = onOpenHistory,
                        modifier = Modifier.weight(1f).testTag("tile_history")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickActionTile(
                        icon = Icons.Filled.QrCode2,
                        title = tr("Tickets QR", "QR Tickets"),
                        badgeCount = activeTicketsCount,
                        tint = ParkoEmerald,
                        onClick = onOpenTickets,
                        modifier = Modifier.weight(1f).testTag("tile_tickets")
                    )
                    QuickActionTile(
                        icon = Icons.Filled.NotificationsActive,
                        title = tr("Notifications", "Notifications"),
                        badgeCount = unreadNotifications,
                        tint = ParkoAmber,
                        onClick = onOpenNotifications,
                        modifier = Modifier.weight(1f).testTag("tile_notifications")
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    tr("Dernières activités", "Recent activity"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onOpenHistory) {
                    Text(tr("Voir tout", "See all"), fontWeight = FontWeight.Bold)
                }
            }
        }

        if (recentSessions.isEmpty()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    EmptyStateCard(
                        icon = Icons.Filled.LocalParking,
                        title = tr("Aucun stationnement récent", "No recent parking")
                    )
                }
            }
        } else {
            items(recentSessions.take(4), key = { it.id }) { session ->
                Surface(
                    onClick = { onOpenTicketDetail(session) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 5.dp)
                        .parkoCardOutline(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(
                                    if (session.status == SessionStatus.COMPLETED.name) ParkoEmeraldLight else ParkoBlueLight
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (session.status == SessionStatus.COMPLETED.name)
                                    Icons.Filled.CheckCircle else Icons.Filled.LocalParking,
                                contentDescription = null,
                                tint = if (session.status == SessionStatus.COMPLETED.name)
                                    ParkoEmeraldDark else ParkoBlue,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = session.parkingName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${session.vehicleLabel} • ${session.plateNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        SessionStatusBadge(session.status)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    title: String,
    tint: Color,
    onClick: () -> Unit,
    badgeCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier.parkoCardOutline(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(tint.copy(alpha = 0.18f))
                        .parkoCardOutline(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp))
                }
                if (badgeCount > 0) {
                    Badge(modifier = Modifier.align(Alignment.TopEnd)) {
                        Text("$badgeCount", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun VehiclesScreen(
    vehicles: List<VehicleEntity>,
    onBack: () -> Unit,
    onSaveVehicle: (String?, VehicleType, String, String, String, String, String?, () -> Unit) -> Unit,
    onDeleteVehicle: (String) -> Unit
) {
    val en = LocalAppLanguage.current == AppLanguage.EN
    var editingVehicle by remember { mutableStateOf<VehicleEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().parkoAtmosphericBackground()) {
        ParkoTopBar(
            title = tr("Mes véhicules", "My vehicles"),
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = { editingVehicle = null; showAddDialog = true },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("add_vehicle_top_button")
                ) {
                    Icon(Icons.Filled.AddCircle, contentDescription = tr("Ajouter un véhicule", "Add vehicle"), tint = ParkoBlue, modifier = Modifier.size(28.dp))
                }
            }
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (vehicles.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.DirectionsCar,
                        title = tr("Aucun véhicule enregistré", "No registered vehicles"),
                        actionLabel = tr("Ajouter un véhicule", "Add a vehicle"),
                        onAction = { editingVehicle = null; showAddDialog = true }
                    )
                }
            } else {
                items(vehicles, key = { it.id }) { vehicle ->
                    Surface(
                        onClick = { editingVehicle = vehicle; showAddDialog = true },
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .parkoCardOutline(18.dp)
                            .testTag("vehicle_card_${vehicle.plateNumber}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VehicleThumbnail(
                                photoUri = vehicle.photoUri,
                                drawableKey = vehicle.drawableKey,
                                vehicleType = vehicle.type,
                                modifier = Modifier.size(width = 86.dp, height = 64.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = vehicle.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = vehicle.plateNumber,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = ParkoBlue
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val vType = VehicleType.entries.firstOrNull { it.name == vehicle.type } ?: VehicleType.AUTRE
                                StatusPill(
                                    label = "${vType.localizedLabel(en)} • ${vehicle.color.ifBlank { "Standard" }}",
                                    containerColor = ParkoBlueLight,
                                    contentColor = ParkoBlueDark
                                )
                            }
                            IconButton(onClick = { onDeleteVehicle(vehicle.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = tr("Supprimer", "Delete"), tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { editingVehicle = null; showAddDialog = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("add_vehicle_bottom_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Ajouter un véhicule", "Add a vehicle"), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showAddDialog) {
        AddEditVehicleDialog(
            initial = editingVehicle,
            onDismiss = { showAddDialog = false },
            onSave = { type, plate, brand, model, color, photoUri ->
                onSaveVehicle(editingVehicle?.id, type, plate, brand, model, color, photoUri) {
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
private fun AddEditVehicleDialog(
    initial: VehicleEntity?,
    onDismiss: () -> Unit,
    onSave: (VehicleType, String, String, String, String, String?) -> Unit
) {
    val en = LocalAppLanguage.current == AppLanguage.EN
    var selectedType by remember {
        mutableStateOf(VehicleType.entries.firstOrNull { it.name == initial?.type } ?: VehicleType.MOTO)
    }
    var plate by remember { mutableStateOf(initial?.plateNumber ?: "") }
    var brand by remember { mutableStateOf(initial?.brand ?: "") }
    var model by remember { mutableStateOf(initial?.model ?: "") }
    var color by remember { mutableStateOf(initial?.color ?: "") }
    var photoUri by remember { mutableStateOf(initial?.photoUri) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) photoUri = uri.toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) tr("Ajouter un véhicule", "Add a vehicle")
                else tr("Modifier le véhicule", "Edit vehicle"),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(tr("Type de véhicule", "Vehicle type"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VehicleType.entries.take(3).forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.localizedLabel(en), maxLines = 1, fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VehicleType.entries.drop(3).forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.localizedLabel(en), maxLines = 1, fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it.uppercase() },
                    label = { Text(tr("Immatriculation * (ex: TG-1234-AA)", "License plate * (e.g. TG-1234-AA)")) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text(tr("Marque", "Brand")) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text(tr("Modèle", "Model")) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text(tr("Couleur", "Color")) },
                    singleLine = true
                )
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (photoUri == null) tr("Ajouter une photo", "Add a photo")
                        else tr("Photo sélectionnée ✓", "Photo selected ✓"),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selectedType, plate, brand, model, color, photoUri) }) {
                Text(tr("Enregistrer", "Save"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("Annuler", "Cancel"), fontWeight = FontWeight.Bold) }
        }
    )
}

@Composable
fun TicketsScreen(
    sessions: List<ParkingSessionEntity>,
    onOpenTicketDetail: (ParkingSessionEntity) -> Unit,
    onClaimTicket: (String, () -> Unit) -> Unit
) {
    var filter by remember { mutableStateOf("ACTIFS") }
    var showClaimDialog by remember { mutableStateOf(false) }

    val filtered = remember(sessions, filter) {
        when (filter) {
            "ACTIFS" -> sessions.filter { it.status == SessionStatus.ACTIVE.name || it.status == SessionStatus.NEEDS_VERIFICATION.name }
            "TERMINES" -> sessions.filter { it.status == SessionStatus.COMPLETED.name }
            else -> sessions
        }
    }

    Column(modifier = Modifier.fillMaxSize().parkoAtmosphericBackground()) {
        ParkoTopBar(
            title = tr("Mes tickets QR", "My QR Tickets"),
            actions = {
                FilledTonalButton(
                    onClick = { showClaimDialog = true },
                    modifier = Modifier.testTag("claim_ticket_button")
                ) {
                    Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(tr("Recevoir un ticket", "Receive ticket"), fontWeight = FontWeight.Bold)
                }
            }
        )

        Surface(
            onClick = { showClaimDialog = true },
            shape = RoundedCornerShape(16.dp),
            color = ParkoBlueLight,
            contentColor = ParkoCharcoal,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .parkoCardOutline(16.dp)
                .testTag("receive_ticket_banner")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Key, contentDescription = null, tint = ParkoBlue, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    tr("Saisir un code ticket (ex : PK-...)", "Enter a ticket code (e.g. PK-...)"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ParkoBlueDark,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ParkoBlueDark, modifier = Modifier.size(24.dp))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = filter == "ACTIFS", onClick = { filter = "ACTIFS" }, label = { Text(tr("Actifs", "Active"), fontWeight = FontWeight.Bold) })
            FilterChip(selected = filter == "TERMINES", onClick = { filter = "TERMINES" }, label = { Text(tr("Terminés", "Completed"), fontWeight = FontWeight.Bold) })
            FilterChip(selected = filter == "TOUS", onClick = { filter = "TOUS" }, label = { Text(tr("Tous", "All"), fontWeight = FontWeight.Bold) })
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.QrCode2,
                        title = tr("Aucun ticket reçu", "No ticket received"),
                        actionLabel = tr("Recevoir un ticket", "Receive a ticket"),
                        onAction = { showClaimDialog = true }
                    )
                }
            } else {
                items(filtered, key = { it.id }) { ticket ->
                    Surface(
                        onClick = { onOpenTicketDetail(ticket) },
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .parkoCardOutline(18.dp)
                            .testTag("ticket_card_${ticket.referenceCode}")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(ParkoBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.QrCode2, contentDescription = null, tint = ParkoBlue, modifier = Modifier.size(34.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ticket.vehicleLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "${ticket.plateNumber} • ${ticket.referenceCode}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = ParkoBlue
                                )
                                Text(
                                    "${ticket.parkingName} • ${formatDateTime(ticket.entryTime)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            SessionStatusBadge(ticket.status)
                        }
                    }
                }
            }
        }
    }

    if (showClaimDialog) {
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showClaimDialog = false },
            title = { Text(tr("Recevoir un ticket", "Receive a ticket"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase() },
                        label = { Text(tr("Code du ticket (ex: PK-4420GM) *", "Ticket code (e.g. PK-4420GM) *")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("claim_code_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { onClaimTicket(code) { showClaimDialog = false } },
                    modifier = Modifier.testTag("claim_confirm_button")
                ) {
                    Text(tr("Valider & afficher le QR", "Claim & show QR"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClaimDialog = false }) { Text(tr("Annuler", "Cancel"), fontWeight = FontWeight.Bold) }
            }
        )
    }
}

@Composable
fun TicketDetailScreen(
    session: ParkingSessionEntity,
    isProSender: Boolean = false,
    onBack: () -> Unit,
    onSendToClient: ((String) -> Unit)? = null,
    onSavedNotice: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val en = LocalAppLanguage.current == AppLanguage.EN

    var clientPhoneInput by remember(session.ownerPhone) {
        mutableStateOf(session.ownerPhone.ifBlank { "+228 " })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .parkoAtmosphericBackground()
            .verticalScroll(rememberScrollState())
    ) {
        ParkoTopBar(
            title = if (isProSender) tr("Ticket client", "Client ticket") else tr("Mon Ticket QR", "My QR Ticket"),
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isProSender) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ParkoBlueLight,
                    contentColor = ParkoCharcoal,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp).parkoCardOutline(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            tr("Code client", "Client code"),
                            style = MaterialTheme.typography.titleSmall,
                            color = ParkoBlueDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = session.referenceCode,
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            color = ParkoBlueDark,
                            modifier = Modifier.testTag("generated_claim_code")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(session.referenceCode))
                                    onSavedNotice(
                                        if (en) "Code ${session.referenceCode} copied"
                                        else "Code ${session.referenceCode} copié"
                                    )
                                },
                                modifier = Modifier.weight(1f).testTag("copy_claim_code_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(tr("Copier", "Copy"), fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    TicketPrinter.printTicket(context, session, en, onSavedNotice)
                                },
                                modifier = Modifier.weight(1f).testTag("print_ticket_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(tr("Imprimer", "Print"), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth().parkoCardOutline(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    QrCodeCanvas(
                        payload = session.qrToken,
                        modifier = Modifier.size(220.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    SessionStatusBadge(session.status)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = session.vehicleLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = session.plateNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        color = ParkoBlue
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = session.ownerName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${session.parkingName} • ${formatDateTime(session.entryTime)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    StatusPill(
                        label = "${tr("Code", "Code")} : ${session.referenceCode}",
                        containerColor = ParkoBlueLight,
                        contentColor = ParkoBlueDark
                    )
                    if (session.ownerUserId != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        StatusPill(
                            label = if (isProSender) {
                                tr("Stocké • ${session.ownerName}", "Stored • ${session.ownerName}")
                            } else {
                                tr("Stocké dans vos Tickets", "Stored in your Tickets")
                            },
                            containerColor = ParkoEmeraldLight,
                            contentColor = ParkoEmeraldDark,
                            icon = Icons.Filled.CheckCircle
                        )
                    }
                }
            }

            if (isProSender && onSendToClient != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth().parkoCardOutline(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            tr("Envoi direct au client", "Direct send to client"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = clientPhoneInput,
                            onValueChange = { clientPhoneInput = it },
                            label = { Text(tr("Téléphone du client (+228...)", "Client phone (+228...)")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("send_qr_phone_input")
                        )
                        Button(
                            onClick = { onSendToClient(clientPhoneInput) },
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("send_qr_client_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(tr("Envoyer vers les Tickets du client", "Send to client's Tickets"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val shareText = if (en) {
                        "PARKO Ticket\nCode: ${session.referenceCode}\nVehicle: ${session.vehicleLabel} (${session.plateNumber})\nParking: ${session.parkingName}\nToken: ${session.qrToken}"
                    } else {
                        "Ticket PARKO\nCode: ${session.referenceCode}\nVéhicule: ${session.vehicleLabel} (${session.plateNumber})\nParking: ${session.parkingName}\nJeton: ${session.qrToken}"
                    }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, if (en) "Share PARKO ticket" else "Partager le ticket PARKO"))
                },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("share_ticket_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(tr("Partager le ticket", "Share ticket"), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    onSavedNotice(
                        if (en) "Ticket ${session.referenceCode} saved offline for exit."
                        else "Ticket ${session.referenceCode} sauvegardé hors ligne pour votre sortie."
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(tr("Conserver hors ligne", "Save offline"), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PersonalHistoryScreen(
    sessions: List<ParkingSessionEntity>,
    onBack: (() -> Unit)? = null,
    onOpenTicket: (ParkingSessionEntity) -> Unit
) {
    var filter by remember { mutableStateOf("TOUS") }
    val filtered = remember(sessions, filter) {
        when (filter) {
            "ENTREES" -> sessions.filter { it.status != SessionStatus.COMPLETED.name }
            "SORTIES" -> sessions.filter { it.status == SessionStatus.COMPLETED.name }
            else -> sessions
        }
    }

    Column(modifier = Modifier.fillMaxSize().parkoAtmosphericBackground()) {
        ParkoTopBar(title = tr("Historique", "History"), onBack = onBack)

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = filter == "TOUS", onClick = { filter = "TOUS" }, label = { Text(tr("Tous", "All"), fontWeight = FontWeight.Bold) })
            FilterChip(selected = filter == "ENTREES", onClick = { filter = "ENTREES" }, label = { Text(tr("Entrées", "Entries"), fontWeight = FontWeight.Bold) })
            FilterChip(selected = filter == "SORTIES", onClick = { filter = "SORTIES" }, label = { Text(tr("Sorties", "Exits"), fontWeight = FontWeight.Bold) })
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.History,
                        title = tr("Aucun historique", "No history")
                    )
                }
            } else {
                items(filtered, key = { it.id }) { session ->
                    val isExit = session.status == SessionStatus.COMPLETED.name
                    Surface(
                        onClick = { onOpenTicket(session) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth().parkoCardOutline(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isExit) ParkoEmeraldLight else ParkoBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isExit) Icons.Filled.CheckCircle else Icons.Filled.LocalParking,
                                    contentDescription = null,
                                    tint = if (isExit) ParkoEmeraldDark else ParkoBlue
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isExit) tr("Sortie parking", "Parking exit") else tr("Entrée parking", "Parking entry"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${session.parkingName} • ${session.vehicleLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatDateTime(session.exitTime ?: session.entryTime),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            SessionStatusBadge(session.status)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    notifications: List<NotificationEntity>,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit
) {
    LaunchedEffect(Unit) { onMarkAllRead() }

    Column(modifier = Modifier.fillMaxSize().parkoAtmosphericBackground()) {
        ParkoTopBar(title = tr("Notifications", "Notifications"), onBack = onBack)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (notifications.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.NotificationsNone,
                        title = tr("Aucune notification", "No notifications")
                    )
                }
            } else {
                items(notifications, key = { it.id }) { notif ->
                    val (icon, tint) = when (notif.category) {
                        "TICKET" -> Icons.Filled.LocalParking to ParkoEmerald
                        "BADGE" -> Icons.Filled.EmojiEvents to ParkoAmber
                        "PAYMENT" -> Icons.Filled.CheckCircle to ParkoEmerald
                        "INCIDENT", "QUOTA" -> Icons.Filled.WarningAmber to ParkoAmber
                        else -> Icons.Filled.Notifications to ParkoBlue
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth().parkoCardOutline(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(tint.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(notif.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(formatTimeShort(notif.createdAt), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(notif.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsProfileScreen(
    user: UserEntity,
    language: String,
    themeMode: ThemeMode,
    canAccessPro: Boolean,
    application: ManagerApplicationEntity?,
    onOpenVehicles: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenBecomeManager: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onSwitchToPro: () -> Unit,
    onSwitchToAdmin: () -> Unit,
    onUpdateProfile: (String, String, String, String, Boolean) -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onLogout: () -> Unit
) {
    val currentAppLang = AppLanguage.fromStorage(language)
    val en = currentAppLang == AppLanguage.EN
    var showEditProfile by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .parkoAtmosphericBackground()
            .verticalScroll(rememberScrollState())
    ) {
        ParkoTopBar(title = tr("Paramètres", "Settings"))

        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                onClick = { showEditProfile = true },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth().parkoCardOutline(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(ParkoBlueLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.firstName.take(1).uppercase() + user.lastName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ParkoBlue
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(user.phone, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${user.city} • ${user.neighborhood}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ParkoBlue
                        )
                    }
                    Icon(Icons.Filled.Edit, contentDescription = tr("Modifier profil", "Edit profile"), tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                }
            }

            // Light / Dark / System Theme Selector
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth().parkoCardOutline(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (themeMode == ThemeMode.DARK) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                            contentDescription = null,
                            tint = ParkoBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            tr("Thème", "Theme"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { onSetThemeMode(mode) },
                                label = { Text(mode.localizedLabel(en), maxLines = 1, fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    val icon = when (mode) {
                                        ThemeMode.LIGHT -> Icons.Filled.LightMode
                                        ThemeMode.DARK -> Icons.Filled.DarkMode
                                        ThemeMode.SYSTEM -> Icons.Filled.SettingsBrightness
                                    }
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                modifier = Modifier.weight(1f).testTag("theme_chip_${mode.name}")
                            )
                        }
                    }
                }
            }

            // French / English Language Selector
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth().parkoCardOutline(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Language, contentDescription = null, tint = ParkoBlue, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            tr("Langue", "Language"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppLanguage.entries.forEach { langOption ->
                            FilterChip(
                                selected = currentAppLang == langOption,
                                onClick = { onSetLanguage(langOption.storageValue) },
                                label = { Text(langOption.displayLabel, maxLines = 1, fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    if (currentAppLang == langOption) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("lang_chip_${langOption.name}")
                            )
                        }
                    }
                }
            }

            // Personal menu section (no payment mode for simple user)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth().parkoCardOutline(20.dp)
            ) {
                Column {
                    ParkoMenuRow(
                        icon = Icons.Filled.Person,
                        title = tr("Mon profil", "My profile"),
                        onClick = { showEditProfile = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ParkoMenuRow(
                        icon = Icons.Filled.DirectionsCar,
                        title = tr("Mes véhicules", "My vehicles"),
                        onClick = onOpenVehicles
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ParkoMenuRow(
                        icon = Icons.Filled.Notifications,
                        title = tr("Notifications", "Notifications"),
                        trailingBadge = if (user.notificationsEnabled) tr("Activées", "On") else tr("Désactivées", "Off"),
                        onClick = onOpenNotifications
                    )
                }
            }

            Text(
                tr("Espace professionnel", "Professional space"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 4.dp)
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth().parkoCardOutline(20.dp)
            ) {
                Column {
                    if (canAccessPro) {
                        ParkoMenuRow(
                            icon = Icons.Filled.Storefront,
                            title = tr("Ouvrir l'espace professionnel", "Open professional space"),
                            iconTint = ParkoEmerald,
                            trailingBadge = tr("Actif", "Active"),
                            onClick = onSwitchToPro,
                            testTag = "settings_open_pro"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    }
                    ParkoMenuRow(
                        icon = Icons.Filled.WorkspacePremium,
                        title = tr("Devenir gestionnaire de parking", "Become a parking manager"),
                        subtitle = application?.let {
                            val st = ApplicationStatus.entries.firstOrNull { s -> s.name == it.status }
                            st?.localizedLabel(en) ?: it.status
                        },
                        iconTint = ParkoAmber,
                        trailingBadge = if (application?.status == ApplicationStatus.APPROVED.name) tr("Validée", "Approved") else null,
                        onClick = onOpenBecomeManager,
                        testTag = "settings_become_manager"
                    )
                    if (application?.status == ApplicationStatus.APPROVED.name || user.role == UserRole.MANAGER.name) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ParkoMenuRow(
                            icon = Icons.Filled.CreditScore,
                            title = tr("Abonnements PARKO", "PARKO Subscriptions"),
                            iconTint = ParkoBlue,
                            trailingBadge = if (canAccessPro) tr("Actif", "Active") else tr("À activer", "Activate"),
                            onClick = onOpenSubscriptions,
                            testTag = "settings_subscriptions"
                        )
                    }
                    if (user.role == UserRole.SUPER_ADMIN.name) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ParkoMenuRow(
                            icon = Icons.Filled.AdminPanelSettings,
                            title = tr("Administration PARKO", "PARKO Administration"),
                            iconTint = MaterialTheme.colorScheme.onSurface,
                            onClick = onSwitchToAdmin,
                            testTag = "settings_open_admin"
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth().parkoCardOutline(20.dp)
            ) {
                Column {
                    ParkoMenuRow(
                        icon = Icons.Filled.Info,
                        title = tr("À propos & Sécurité", "About & Security"),
                        onClick = { showAbout = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ParkoMenuRow(
                        icon = Icons.Filled.Logout,
                        title = tr("Déconnexion", "Sign out"),
                        iconTint = ParkoRed,
                        onClick = onLogout,
                        testTag = "logout_button"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showEditProfile) {
        var first by remember { mutableStateOf(user.firstName) }
        var last by remember { mutableStateOf(user.lastName) }
        var city by remember { mutableStateOf(user.city) }
        var neighborhood by remember { mutableStateOf(user.neighborhood) }
        var notifEnabled by remember { mutableStateOf(user.notificationsEnabled) }

        AlertDialog(
            onDismissRequest = { showEditProfile = false },
            title = { Text(tr("Modifier mon profil", "Edit profile"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = first, onValueChange = { first = it }, label = { Text(tr("Prénom", "First name")) })
                    OutlinedTextField(value = last, onValueChange = { last = it }, label = { Text(tr("Nom", "Last name")) })
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text(tr("Ville", "City")) })
                    OutlinedTextField(value = neighborhood, onValueChange = { neighborhood = it }, label = { Text(tr("Quartier", "Neighborhood")) })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = notifEnabled, onCheckedChange = { notifEnabled = it })
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Recevoir les notifications", "Receive notifications"), fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateProfile(first, last, city, neighborhood, notifEnabled)
                    showEditProfile = false
                }) { Text(tr("Enregistrer", "Save"), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfile = false }) { Text(tr("Annuler", "Cancel"), fontWeight = FontWeight.Bold) }
            }
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text(tr("À propos de PARKO", "About PARKO"), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    tr(
                        "PARKO digitalise et sécurise les parkings de motos et de voitures. " +
                            "Chaque véhicule enregistré génère un ticket QR unique sans exposer vos données personnelles, " +
                            "et chaque sortie fait l'objet d'un contrôle physique et d'une traçabilité complète.",
                        "PARKO digitizes and secures motorcycle and car parking lots. " +
                            "Every registered vehicle generates a unique QR ticket without exposing personal data, " +
                            "and every exit undergoes physical verification and full audit logging."
                    )
                )
            },
            confirmButton = {
                Button(onClick = { showAbout = false }) { Text(tr("Fermer", "Close"), fontWeight = FontWeight.Bold) }
            }
        )
    }
}
