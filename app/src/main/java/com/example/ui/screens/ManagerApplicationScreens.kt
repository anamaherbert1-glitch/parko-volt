package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.ManagerApplicationEntity
import com.example.data.local.ParkingLocationEntity
import com.example.data.local.UserEntity
import com.example.data.model.ApplicationStatus
import com.example.data.model.ParkingSizePreset
import com.example.data.model.PaymentMethod
import com.example.data.model.SubscriptionTier
import com.example.ui.components.ParkoTopBar
import com.example.ui.components.StatusPill
import com.example.ui.components.formatDateOnly
import com.example.ui.components.formatFcfa
import com.example.ui.components.parkoAtmosphericBackground
import com.example.ui.components.parkoCardOutline
import com.example.ui.i18n.*
import com.example.ui.theme.*

@Composable
fun ManagerApplicationScreen(
    user: UserEntity,
    existingApplication: ManagerApplicationEntity?,
    onBack: () -> Unit,
    onSubmit: (
        fullName: String,
        birthDate: String,
        phone: String,
        email: String,
        city: String,
        neighborhood: String,
        parkingName: String,
        parkingAddress: String,
        parkingCity: String,
        parkingNeighborhood: String,
        parkingType: String,
        acceptedVehicleTypes: String,
        dailyVolume: Int,
        attendantsCount: Int,
        applicantStatus: String,
        idFrontUri: String?,
        idBackUri: String?,
        selfieUri: String?,
        consentAccepted: Boolean
    ) -> Unit,
    onGoToSubscriptions: () -> Unit
) {
    val en = LocalAppLanguage.current == AppLanguage.EN
    var forceNewForm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().parkoAtmosphericBackground()) {
        ParkoTopBar(title = tr("Devenir gestionnaire", "Become a manager"), onBack = onBack)

        if (existingApplication != null && !forceNewForm) {
            val status = ApplicationStatus.entries.firstOrNull { it.name == existingApplication.status }
                ?: ApplicationStatus.PENDING
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth().parkoCardOutline(22.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(tr("Suivi de candidature", "Application status"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            val (bg, fg) = when (status) {
                                ApplicationStatus.APPROVED -> ParkoEmeraldLight to ParkoEmeraldDark
                                ApplicationStatus.REJECTED -> ParkoRedLight to ParkoRedDark
                                ApplicationStatus.MORE_INFO_REQUIRED -> ParkoAmberLight to ParkoAmberDark
                                else -> ParkoBlueLight to ParkoBlueDark
                            }
                            StatusPill(status.localizedLabel(en), bg, fg)
                        }
                        Text(existingApplication.parkingName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${existingApplication.parkingNeighborhood}, ${existingApplication.parkingCity}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${existingApplication.applicantStatus} • ${formatDateOnly(existingApplication.submittedAt)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)

                        existingApplication.reviewNote?.let {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${tr("Note PARKO", "PARKO Note")} : $it",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                if (status == ApplicationStatus.APPROVED) {
                    val recTier = SubscriptionTier.recommendedFor(
                        existingApplication.estimatedDailyVolume,
                        existingApplication.plannedAttendantsCount
                    )
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = ParkoBlueLight,
                        contentColor = ParkoCharcoal,
                        modifier = Modifier.fillMaxWidth().parkoCardOutline(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = ParkoBlue, modifier = Modifier.size(28.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    tr("Offre conseillée : ${recTier.localizedTitle(en)}", "Recommended plan: ${recTier.localizedTitle(en)}"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ParkoBlueDark
                                )
                                Text(
                                    tr(
                                        "${existingApplication.estimatedDailyVolume} véh./j • ${existingApplication.plannedAttendantsCount} gardien(s)",
                                        "${existingApplication.estimatedDailyVolume} veh./day • ${existingApplication.plannedAttendantsCount} attendant(s)"
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ParkoBlueDark
                                )
                            }
                        }
                    }
                    Button(
                        onClick = onGoToSubscriptions,
                        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("proceed_subscriptions_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.CreditScore, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Choisir & activer mon abonnement", "Choose & activate my subscription"), fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = { forceNewForm = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Mettre à jour la demande", "Update application"), fontWeight = FontWeight.Bold)
                    }
                }
            }
            return
        }

        var step by remember { mutableIntStateOf(1) }

        var fullName by remember { mutableStateOf(user.fullName) }
        var birthDate by remember { mutableStateOf("14/06/1990") }
        var phone by remember { mutableStateOf(user.phone) }
        var email by remember { mutableStateOf(user.email) }
        var city by remember { mutableStateOf(user.city) }
        var neighborhood by remember { mutableStateOf(user.neighborhood) }

        var parkingName by remember { mutableStateOf("") }
        var parkingAddress by remember { mutableStateOf("") }
        var parkingCity by remember { mutableStateOf("Lomé") }
        var parkingNeighborhood by remember { mutableStateOf("") }
        var parkingType by remember { mutableStateOf("Parking commercial mixte") }
        var acceptedTypes by remember { mutableStateOf("Moto, Voiture") }
        var dailyVolume by remember { mutableStateOf("30") }
        var attendantsCount by remember { mutableStateOf("2") }
        var applicantStatus by remember { mutableStateOf("Propriétaire") }

        var idFrontUri by remember { mutableStateOf<String?>(null) }
        var idBackUri by remember { mutableStateOf<String?>(null) }
        var selfieUri by remember { mutableStateOf<String?>(null) }
        var consentAccepted by remember { mutableStateOf(false) }

        val pickFront = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) idFrontUri = uri.toString()
        }
        val pickBack = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) idBackUri = uri.toString()
        }
        val pickSelfie = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) selfieUri = uri.toString()
        }

        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Étape $step sur 3", "Step $step of 3"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = ParkoBlue)
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { step / 3f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                    color = ParkoBlue,
                    trackColor = ParkoBlueLight
                )
            }

            when (step) {
                1 -> {
                    Text(tr("Informations personnelles", "Personal information"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text(tr("Nom complet *", "Full name *")) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = birthDate, onValueChange = { birthDate = it }, label = { Text(tr("Date de naissance", "Date of birth")) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(tr("Téléphone (+228...) *", "Phone (+228...) *")) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(tr("E-mail", "Email")) }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text(tr("Ville", "City")) }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = neighborhood, onValueChange = { neighborhood = it }, label = { Text(tr("Quartier", "Neighborhood")) }, modifier = Modifier.weight(1f))
                    }
                    Button(
                        onClick = { step = 2 },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(tr("Suivant →", "Next →"), fontWeight = FontWeight.Bold)
                    }
                }
                2 -> {
                    val volInt = dailyVolume.toIntOrNull() ?: 30
                    val attInt = attendantsCount.toIntOrNull() ?: 2
                    val liveRecommended = SubscriptionTier.recommendedFor(volInt, attInt)
                    Text(tr("Informations sur le parking", "Parking details"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(tr("Taille du parking", "Parking size"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ParkingSizePreset.entries.forEach { preset ->
                            val isChosen = when (preset) {
                                ParkingSizePreset.SMALL -> volInt <= 15 && attInt <= 1
                                ParkingSizePreset.MEDIUM -> (volInt in 16..50) && attInt <= 3
                                ParkingSizePreset.LARGE -> volInt > 50 || attInt > 3
                            }
                            Surface(
                                onClick = {
                                    dailyVolume = preset.defaultDailyVolume.toString()
                                    attendantsCount = preset.defaultAttendants.toString()
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isChosen) ParkoBlueLight else MaterialTheme.colorScheme.surface,
                                contentColor = if (isChosen) ParkoCharcoal else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        width = if (isChosen) 2.dp else 1.2.dp,
                                        color = if (isChosen) ParkoBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = when (preset) {
                                            ParkingSizePreset.SMALL -> Icons.Filled.TwoWheeler
                                            ParkingSizePreset.MEDIUM -> Icons.Filled.DirectionsCar
                                            ParkingSizePreset.LARGE -> Icons.Filled.Domain
                                        },
                                        contentDescription = null,
                                        tint = if (isChosen) ParkoBlue else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(preset.localizedTitle(en), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    Text(
                                        preset.localizedCapacity(en),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isChosen) ParkoBlueDark else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(value = parkingName, onValueChange = { parkingName = it }, label = { Text(tr("Nom du parking *", "Parking name *")) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = parkingAddress, onValueChange = { parkingAddress = it }, label = { Text(tr("Adresse *", "Address *")) }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = parkingCity, onValueChange = { parkingCity = it }, label = { Text(tr("Ville *", "City *")) }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = parkingNeighborhood, onValueChange = { parkingNeighborhood = it }, label = { Text(tr("Quartier *", "Neighborhood *")) }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = parkingType, onValueChange = { parkingType = it }, label = { Text(tr("Type de parking", "Parking type")) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = acceptedTypes, onValueChange = { acceptedTypes = it }, label = { Text(tr("Véhicules acceptés", "Accepted vehicles")) }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = dailyVolume,
                            onValueChange = { dailyVolume = it.filter { c -> c.isDigit() } },
                            label = { Text(tr("Véhicules / jour", "Vehicles / day")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = attendantsCount,
                            onValueChange = { attendantsCount = it.filter { c -> c.isDigit() } },
                            label = { Text(tr("Nb gardiens", "Attendants")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ParkoEmeraldLight,
                        contentColor = ParkoCharcoal,
                        modifier = Modifier.fillMaxWidth().parkoCardOutline(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = ParkoEmeraldDark, modifier = Modifier.size(22.dp))
                            Text(
                                tr(
                                    "Abonnement conseillé (après validation) : ${liveRecommended.localizedTitle(en)} (${liveRecommended.localizedPeriod(en)})",
                                    "Recommended after approval: ${liveRecommended.localizedTitle(en)} (${liveRecommended.localizedPeriod(en)})"
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = ParkoEmeraldDark
                            )
                        }
                    }
                    Text(tr("Votre statut", "Your role"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Propriétaire" to tr("Propriétaire", "Owner"), "Responsable" to tr("Responsable", "Supervisor"), "Gestionnaire" to tr("Gestionnaire", "Manager")).forEach { (value, label) ->
                            FilterChip(
                                selected = applicantStatus == value,
                                onClick = { applicantStatus = value },
                                label = { Text(label, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { step = 1 }, modifier = Modifier.weight(1f).height(52.dp)) {
                            Text(tr("← Précédent", "← Back"), fontWeight = FontWeight.Bold)
                        }
                        Button(onClick = { step = 3 }, modifier = Modifier.weight(1f).height(52.dp)) {
                            Text(tr("Suivant →", "Next →"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                3 -> {
                    Text(tr("Vérification d'identité", "Identity verification"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    OutlinedButton(
                        onClick = { pickFront.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Filled.Badge, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (idFrontUri == null) tr("Pièce d'identité (Recto)", "ID document (Front)") else tr("Recto sélectionné ✓", "Front selected ✓"), fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { pickBack.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Filled.CreditCard, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (idBackUri == null) tr("Pièce d'identité (Verso)", "ID document (Back)") else tr("Verso sélectionné ✓", "Back selected ✓"), fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { pickSelfie.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Filled.Face, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (selfieUri == null) tr("Selfie de vérification", "Verification selfie") else tr("Selfie sélectionné ✓", "Selfie selected ✓"), fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = consentAccepted, onCheckedChange = { consentAccepted = it })
                        Text(
                            tr("Confirmer la vérification de mes documents", "Confirm verification of my documents"),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { step = 2 }, modifier = Modifier.weight(1f).height(52.dp)) {
                            Text(tr("← Précédent", "← Back"), fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                onSubmit(
                                    fullName, birthDate, phone, email, city, neighborhood,
                                    parkingName, parkingAddress, parkingCity, parkingNeighborhood,
                                    parkingType, acceptedTypes,
                                    dailyVolume.toIntOrNull() ?: 25,
                                    attendantsCount.toIntOrNull() ?: 1,
                                    applicantStatus, idFrontUri, idBackUri, selfieUri, consentAccepted
                                )
                            },
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) {
                            Text(tr("Soumettre", "Submit"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SubscriptionsScreen(
    currentParking: ParkingLocationEntity?,
    application: ManagerApplicationEntity? = null,
    isManagerEligible: Boolean = true,
    onBack: () -> Unit,
    onSelectTier: (SubscriptionTier) -> Unit
) {
    val en = LocalAppLanguage.current == AppLanguage.EN
    val isCurrentlyActive = currentParking?.subscriptionActive == true
    val activeTier = SubscriptionTier.entries.firstOrNull { it.name == currentParking?.subscriptionTier }
    val recommendedTier = remember(application, currentParking) {
        when {
            application != null -> SubscriptionTier.recommendedFor(
                application.estimatedDailyVolume,
                application.plannedAttendantsCount
            )
            currentParking != null -> when {
                currentParking.quotaLimit == Int.MAX_VALUE -> SubscriptionTier.PRO_UNLIMITED
                currentParking.quotaLimit > 15 -> SubscriptionTier.STANDARD
                else -> SubscriptionTier.ESSENTIEL
            }
            else -> SubscriptionTier.STANDARD
        }
    }
    var selectedTier by remember(activeTier, recommendedTier, isCurrentlyActive) {
        mutableStateOf(if (isCurrentlyActive && activeTier != null) activeTier else recommendedTier)
    }

    Column(modifier = Modifier.fillMaxSize().parkoAtmosphericBackground()) {
        ParkoTopBar(title = tr("Abonnements", "Subscriptions"), onBack = onBack)

        Column(
            modifier = Modifier
                .widthIn(max = 580.dp)
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ParkoBlueLight,
                contentColor = ParkoCharcoal,
                modifier = Modifier.fillMaxWidth().parkoCardOutline(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ParkoBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(26.dp))
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            tr("Conseillé : ${recommendedTier.localizedTitle(en)}", "Recommended: ${recommendedTier.localizedTitle(en)}"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ParkoBlueDark
                        )
                        if (application != null) {
                            Text(
                                "${application.parkingName} • ${application.estimatedDailyVolume} ${tr("véh./j", "veh./day")} • ${application.plannedAttendantsCount} ${tr("gardien(s)", "attendant(s)")}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ParkoCharcoal
                            )
                        }
                    }
                }
            }

            Text(tr("Choisissez votre abonnement", "Choose your subscription"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            SubscriptionTier.entries.forEach { tier ->
                val isSelected = selectedTier == tier
                val isRecommended = tier == recommendedTier
                val isCurrentActive = isCurrentlyActive && activeTier == tier
                Surface(
                    onClick = { selectedTier = tier },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) ParkoBlueLight else MaterialTheme.colorScheme.surface,
                    contentColor = if (isSelected) ParkoCharcoal else MaterialTheme.colorScheme.onSurface,
                    tonalElevation = if (isSelected) 2.dp else 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 2.2.dp else 1.2.dp,
                            color = if (isSelected) ParkoBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(tier.localizedTitle(en), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    if (isRecommended) {
                                        StatusPill(tr("Conseillé", "Recommended"), ParkoEmeraldLight, ParkoEmeraldDark, Icons.Filled.AutoAwesome)
                                    }
                                    if (isCurrentActive) {
                                        StatusPill(tr("Actif", "Active"), ParkoBlue, androidx.compose.ui.graphics.Color.White)
                                    }
                                }
                                Text(tier.localizedPeriod(en), style = MaterialTheme.typography.titleSmall, color = ParkoBlue, fontWeight = FontWeight.Bold)
                            }
                            RadioButton(selected = isSelected, onClick = { selectedTier = tier })
                        }
                        tier.localizedHighlights(en).forEach { bullet ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ParkoEmerald, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(bullet, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            if (isManagerEligible) {
                Button(
                    onClick = { onSelectTier(selectedTier) },
                    modifier = Modifier.fillMaxWidth().height(54.dp).testTag("choose_plan_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (selectedTier == SubscriptionTier.FREE_TRIAL) tr("Activer l'essai gratuit", "Activate free trial")
                        else tr("Activer ${selectedTier.localizedTitle(en)} (${formatFcfa(selectedTier.priceFcfa)})", "Activate ${selectedTier.localizedTitle(en)} (${formatFcfa(selectedTier.priceFcfa)})"),
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ParkoAmberLight,
                    contentColor = ParkoCharcoal,
                    modifier = Modifier.fillMaxWidth().parkoCardOutline(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = ParkoAmberDark, modifier = Modifier.size(24.dp))
                        Text(
                            tr(
                                "Aucun abonnement requis pour un compte utilisateur simple. Réservé aux gestionnaires de parking validés.",
                                "No subscription required for personal users. Reserved for approved parking managers."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = ParkoAmberDark
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun SubscriptionPaymentScreen(
    tier: SubscriptionTier,
    onBack: () -> Unit,
    onPay: (PaymentMethod, Boolean) -> Unit
) {
    val en = LocalAppLanguage.current == AppLanguage.EN
    var selectedMethod by remember { mutableStateOf(PaymentMethod.MIXX_BY_YAS) }
    var awaitingProviderVerification by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().parkoAtmosphericBackground()) {
        ParkoTopBar(title = tr("Paiement", "Payment"), onBack = onBack)

        Column(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ParkoBlueLight,
                contentColor = ParkoCharcoal,
                modifier = Modifier.fillMaxWidth().parkoCardOutline(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("${tr("Plan", "Plan")} : ${tier.localizedTitle(en)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(formatFcfa(tier.priceFcfa), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ParkoBlue)
                }
            }

            Text(tr("Méthode de paiement", "Payment method"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            PaymentMethod.entries.forEach { method ->
                val isSelected = selectedMethod == method
                Surface(
                    onClick = { selectedMethod = method },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 2.2.dp else 1.2.dp,
                            color = if (isSelected) ParkoBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (method == PaymentMethod.CASH) Icons.Filled.Payments else Icons.Filled.PhoneAndroid,
                            contentDescription = null,
                            tint = if (method == PaymentMethod.CASH) ParkoAmber else ParkoBlue,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(method.localizedLabel(en), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        RadioButton(selected = isSelected, onClick = { selectedMethod = method })
                    }
                }
            }

            if (awaitingProviderVerification) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ParkoAmberLight,
                    contentColor = ParkoCharcoal,
                    modifier = Modifier.fillMaxWidth().parkoCardOutline(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            tr("Confirmation opérateur requise", "Provider confirmation required"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ParkoAmberDark
                        )
                        Button(
                            onClick = { onPay(selectedMethod, true) },
                            colors = ButtonDefaults.buttonColors(containerColor = ParkoEmerald),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(tr("Vérifier & activer", "Verify & activate"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Button(
                    onClick = {
                        if (selectedMethod == PaymentMethod.CASH || tier == SubscriptionTier.FREE_TRIAL) {
                            onPay(selectedMethod, true)
                        } else {
                            onPay(selectedMethod, false)
                            awaitingProviderVerification = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp).testTag("pay_subscription_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (tier == SubscriptionTier.FREE_TRIAL) tr("Activer l'essai gratuit", "Activate free trial")
                        else tr("Payer & activer", "Pay & activate"),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(tr("Annuler", "Cancel"), fontWeight = FontWeight.Bold)
            }
        }
    }
}
