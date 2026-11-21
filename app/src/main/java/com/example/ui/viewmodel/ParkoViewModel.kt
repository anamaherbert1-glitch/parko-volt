package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.repository.ParkoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ParkoViewModel(private val repository: ParkoRepository) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _lastCreatedPlainCode = MutableStateFlow<String?>(null)
    val lastCreatedPlainCode: StateFlow<String?> = _lastCreatedPlainCode.asStateFlow()

    private val _isOnline = MutableStateFlow(repository.isOnlineProvider())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            _isReady.value = true
        }
    }

    fun refreshConnectivity(): Boolean {
        val online = repository.isOnlineProvider()
        _isOnline.value = online
        return online
    }

    val loggedInUserId: StateFlow<String?> = repository.loggedInUserId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeSpace: StateFlow<AppSpace> = repository.activeSpace
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSpace.PERSONAL)

    val language: StateFlow<String> = repository.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Français")

    val themeMode: StateFlow<ThemeMode> = repository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val currentUser: StateFlow<UserEntity?> = loggedInUserId
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) flowOf(null) else repository.observeUser(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userVehicles: StateFlow<List<VehicleEntity>> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else repository.observeUserVehicles(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSessions: StateFlow<List<ParkingSessionEntity>> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else repository.observeUserSessions(user.id, user.phone)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userPayments: StateFlow<List<PaymentRecordEntity>> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else repository.observeUserPayments(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userNotifications: StateFlow<List<NotificationEntity>> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else repository.observeUserNotifications(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userApplication: StateFlow<ManagerApplicationEntity?> = currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(null) else repository.observeUserApplication(user.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allParkings: StateFlow<List<ParkingLocationEntity>> = repository.observeAllParkings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAttendants: StateFlow<List<AttendantEntity>> = repository.observeAllAttendants()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Strictly resolves the user's assigned parking. Never falls back to an arbitrary parking.
     */
    val activeParking: StateFlow<ParkingLocationEntity?> = combine(
        currentUser,
        allParkings,
        allAttendants
    ) { user, parkings, attendants ->
        if (user == null || user.isSuspended) return@combine null
        when (UserRole.entries.firstOrNull { it.name == user.role } ?: UserRole.USER) {
            UserRole.MANAGER -> parkings.firstOrNull { it.managerUserId == user.id }
            UserRole.ATTENDANT, UserRole.SUPERVISOR -> {
                val activeAssignment = attendants.firstOrNull { it.userId == user.id && it.isActive }
                activeAssignment?.let { att -> parkings.firstOrNull { it.id == att.parkingId } }
            }
            UserRole.SUPER_ADMIN, UserRole.USER -> null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val proSessions: StateFlow<List<ParkingSessionEntity>> = activeParking
        .flatMapLatest { parking ->
            if (parking == null) flowOf(emptyList()) else repository.observeParkingSessions(parking.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val proPayments: StateFlow<List<PaymentRecordEntity>> = activeParking
        .flatMapLatest { parking ->
            if (parking == null) flowOf(emptyList()) else repository.observeParkingPayments(parking.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val proAttendants: StateFlow<List<AttendantEntity>> = activeParking
        .flatMapLatest { parking ->
            if (parking == null) flowOf(emptyList()) else repository.observeAttendantsForParking(parking.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val proIncidents: StateFlow<List<IncidentEntity>> = activeParking
        .flatMapLatest { parking ->
            if (parking == null) flowOf(emptyList()) else repository.observeParkingIncidents(parking.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val proRecoveries: StateFlow<List<ExceptionalRecoveryEntity>> = activeParking
        .flatMapLatest { parking ->
            if (parking == null) flowOf(emptyList()) else repository.observeParkingRecoveries(parking.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Super-Admin streams
    val allUsers: StateFlow<List<UserEntity>> = repository.observeAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allSessions: StateFlow<List<ParkingSessionEntity>> = repository.observeAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allPayments: StateFlow<List<PaymentRecordEntity>> = repository.observeAllPayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allApplications: StateFlow<List<ManagerApplicationEntity>> = repository.observeAllApplications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allIncidents: StateFlow<List<IncidentEntity>> = repository.observeAllIncidents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allAuditLogs: StateFlow<List<AuditLogEntity>> = repository.observeAllAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val canAccessProSpace: StateFlow<Boolean> = combine(
        currentUser,
        activeParking,
        allAttendants
    ) { user, parking, attendants ->
        if (user == null || user.isSuspended || parking == null || parking.isSuspended) {
            return@combine false
        }
        val role = UserRole.entries.firstOrNull { it.name == user.role } ?: UserRole.USER
        when (role) {
            UserRole.ATTENDANT, UserRole.SUPERVISOR -> {
                attendants.any { it.userId == user.id && it.parkingId == parking.id && it.isActive } &&
                    parking.subscriptionActive
            }
            UserRole.MANAGER -> {
                parking.managerUserId == user.id &&
                    parking.subscriptionActive &&
                    parking.subscriptionExpiresAt > System.currentTimeMillis()
            }
            UserRole.SUPER_ADMIN, UserRole.USER -> false
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun clearMessage() {
        _toastMessage.value = null
    }

    fun showMessage(msg: String) {
        _toastMessage.value = msg
    }

    fun clearCreatedSecretCode() {
        _lastCreatedPlainCode.value = null
    }

    fun touchProSession() {
        viewModelScope.launch {
            if (repository.checkProSessionExpired(currentUser.value)) {
                _toastMessage.value = "Session professionnelle expirée. Veuillez la rouvrir."
            } else {
                repository.refreshProActivity()
            }
        }
    }

    fun switchSpace(space: AppSpace) {
        viewModelScope.launch {
            val en = language.value.equals("English", ignoreCase = true)
            if (space == AppSpace.PRO && !canAccessProSpace.value) {
                _toastMessage.value = if (en) {
                    "Pro access unavailable: an approved manager with active subscription or active assignment is required."
                } else {
                    "Accès Pro indisponible : un abonnement actif ou une affectation active est requis."
                }
                return@launch
            }
            if (space == AppSpace.ADMIN && currentUser.value?.role != UserRole.SUPER_ADMIN.name) {
                _toastMessage.value = if (en) {
                    "Access reserved for PARKO administration."
                } else {
                    "Accès réservé à l'administration PARKO."
                }
                return@launch
            }
            repository.setActiveSpace(space)
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            repository.setLanguage(lang)
            val en = lang.equals("English", ignoreCase = true)
            _toastMessage.value = if (en) "Language set to English" else "Langue définie sur Français"
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun login(identifier: String, password: String, rememberMe: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.login(identifier, password, rememberMe)
                .onSuccess { onSuccess() }
                .onFailure { _toastMessage.value = it.message }
        }
    }

    fun loginDemo(userId: String = SeedData.DEMO_USER_ID) {
        viewModelScope.launch {
            repository.loginDemo(userId)
                .onFailure { _toastMessage.value = it.message }
        }
    }

    fun register(
        firstName: String,
        lastName: String,
        phone: String,
        email: String,
        password: String,
        confirmPassword: String,
        onSuccess: () -> Unit
    ) {
        if (password != confirmPassword) {
            _toastMessage.value = "Les mots de passe ne correspondent pas."
            return
        }
        viewModelScope.launch {
            repository.register(firstName, lastName, phone, email, password)
                .onSuccess { onSuccess() }
                .onFailure { _toastMessage.value = it.message }
        }
    }

    fun resetPassword(phone: String, newPassword: String, otpCode: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.resetPasswordByPhone(phone, newPassword, otpCode)
                .onSuccess {
                    _toastMessage.value = "Mot de passe réinitialisé avec succès."
                    onSuccess()
                }
                .onFailure { _toastMessage.value = it.message }
        }
    }

    fun logout() {
        _lastCreatedPlainCode.value = null
        viewModelScope.launch { repository.logout() }
    }

    fun updateProfile(firstName: String, lastName: String, city: String, neighborhood: String, notificationsEnabled: Boolean) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.updateProfile(user, firstName, lastName, city, neighborhood, notificationsEnabled)
            _toastMessage.value = "Profil mis à jour"
        }
    }

    fun saveVehicle(
        vehicleId: String?,
        type: VehicleType,
        plate: String,
        brand: String,
        model: String,
        color: String,
        photoUri: String?,
        onSuccess: () -> Unit
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.saveVehicle(user, vehicleId, type, plate, brand, model, color, photoUri)
                .onSuccess {
                    _toastMessage.value = "Véhicule enregistré"
                    onSuccess()
                }
                .onFailure { _toastMessage.value = it.message }
        }
    }

    fun deleteVehicle(vehicleId: String) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicleId)
            _toastMessage.value = "Véhicule supprimé"
        }
    }

    fun markNotificationsRead() {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch { repository.markNotificationsRead(uid) }
    }

    fun submitManagerApplication(
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
        consentAccepted: Boolean,
        onSuccess: () -> Unit
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.submitManagerApplication(
                user, fullName, birthDate, phone, email, city, neighborhood,
                parkingName, parkingAddress, parkingCity, parkingNeighborhood,
                parkingType, acceptedVehicleTypes, dailyVolume, attendantsCount,
                applicantStatus, idFrontUri, idBackUri, selfieUri, consentAccepted
            ).onSuccess {
                _toastMessage.value = "Candidature transmise à PARKO"
                onSuccess()
            }.onFailure { _toastMessage.value = it.message }
        }
    }

    fun reviewApplication(application: ManagerApplicationEntity, newStatus: ApplicationStatus, note: String) {
        val admin = currentUser.value ?: return
        viewModelScope.launch {
            repository.reviewApplication(admin, application, newStatus, note)
                .onSuccess { _toastMessage.value = "Décision enregistrée : ${newStatus.label}" }
                .onFailure { _toastMessage.value = it.message }
        }
    }

    fun activateSubscription(
        tier: SubscriptionTier,
        method: PaymentMethod,
        providerConfirmed: Boolean,
        switchToProOnActivate: Boolean = true,
        onActivated: () -> Unit
    ) {
        val user = currentUser.value ?: return
        val parking = activeParking.value
        val en = language.value.equals("English", ignoreCase = true)
        if (user.role != UserRole.MANAGER.name || parking == null) {
            _toastMessage.value = if (en) {
                "Only approved parking managers can subscribe after completing the manager form."
            } else {
                "Seuls les gestionnaires de parking validés peuvent souscrire après avoir rempli le formulaire."
            }
            return
        }
        viewModelScope.launch {
            repository.activateSubscription(user, parking, tier, method, providerConfirmed)
                .onSuccess { record ->
                    if (record != null && record.status == PaymentStatus.PENDING.name) {
                        _toastMessage.value = if (en) {
                            "${method.label} payment awaiting Admin/provider confirmation."
                        } else {
                            "Paiement ${method.label} enregistré en attente de validation PARKO."
                        }
                    } else {
                        _toastMessage.value = if (en) {
                            "${tier.title} plan activated!"
                        } else {
                            "Offre ${tier.title} activée !"
                        }
                        if (switchToProOnActivate) {
                            repository.setActiveSpace(AppSpace.PRO)
                        }
                        onActivated()
                    }
                }
                .onFailure { _toastMessage.value = it.message }
        }
    }

    fun confirmSubscriptionPayment(payment: PaymentRecordEntity) {
        val admin = currentUser.value ?: return
        val parking = allParkings.value.firstOrNull { it.id == payment.parkingId }
        viewModelScope.launch {
            repository.confirmPendingSubscriptionPayment(admin, payment, parking)
                .onSuccess { _toastMessage.value = "Paiement d'abonnement confirmé" }
                .onFailure { _toastMessage.value = it.message }
        }
    }

    fun registerVehicleEntry(
        vehicleType: VehicleType,
        plateNumber: String,
        brandModel: String,
        color: String,
        ownerName: String,
        ownerPhone: String,
        feeFcfa: Int,
        paymentMethod: PaymentMethod,
        photoUri: String?,
        onSuccess: (ParkingSessionEntity) -> Unit
    ) {
        val actor = currentUser.value ?: return
        val parking = activeParking.value ?: return
        viewModelScope.launch {
            repository.registerVehicleEntry(
                actor, parking, vehicleType, plateNumber, brandModel,
                color, ownerName, ownerPhone, feeFcfa, paymentMethod, photoUri
            ).onSuccess { result ->
                repository.refreshProActivity()
                _lastCreatedPlainCode.value = result.plainWithdrawalCode
                if (result.session.ownerUserId != null) {
                    _toastMessage.value = "Ticket QR envoyé directement au compte de ${result.session.ownerName}"
                }
                onSuccess(result.session)
            }.onFailure { _toastMessage.value = it.message }
        }
    }

    fun sendTicketToClient(
        session: ParkingSessionEntity,
        clientPhone: String,
        onUpdated: (ParkingSessionEntity) -> Unit
    ) {
        val actor = currentUser.value ?: return
        viewModelScope.launch {
            repository.sendTicketToClient(actor, session, clientPhone)
                .onSuccess { updated ->
                    _toastMessage.value = if (updated.ownerUserId != null) {
                        "Ticket envoyé ! Stocké dans Mes tickets de ${updated.ownerName}"
                    } else {
                        "Ticket associé au numéro ${updated.ownerPhone}"
                    }
                    onUpdated(updated)
                }
                .onFailure { _toastMessage.value = it.message }
        }
    }

    fun claimTicketByCode(code: String, verifyingPlate: String, onSuccess: (ParkingSessionEntity) -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.claimTicketByCode(user, code, verifyingPlate)
                .onSuccess { updated ->
                    val en = language.value.equals("English", ignoreCase = true)
                    _toastMessage.value = if (en) {
                        "Ticket ${updated.referenceCode} saved to My Tickets!"
                    } else {
                        "Ticket ${updated.referenceCode} stocké dans Mes tickets !"
                    }
                    onSuccess(updated)
                }
                .onFailure { _toastMessage.value = it.message }
        }
    }

    fun scanSessionByQuery(
        query: String,
        onFound: (ParkingSessionEntity) -> Unit,
        onNotFound: () -> Unit
    ) {
        val actor = currentUser.value ?: return
        val parking = activeParking.value ?: return
        viewModelScope.launch {
            repository.lookupParkingSessionForScan(actor, parking.id, query)
                .onSuccess { found ->
                    repository.refreshProActivity()
                    onFound(found)
                }
                .onFailure {
                    _toastMessage.value = it.message
                    onNotFound()
                }
        }
    }

    fun beginExitVerification(
        session: ParkingSessionEntity,
        onReady: (ParkingSessionEntity) -> Unit,
        onBlocked: (String) -> Unit
    ) {
        val actor = currentUser.value ?: return
        viewModelScope.launch {
            repository.beginExitVerification(actor, session)
                .onSuccess { locked -> onReady(locked) }
                .onFailure {
                    val msg = it.message ?: "Sortie déjà en cours"
                    _toastMessage.value = msg
                    onBlocked(msg)
                }
        }
    }

    fun cancelExitVerification(session: ParkingSessionEntity) {
        val actor = currentUser.value ?: return
        viewModelScope.launch {
            repository.cancelExitVerification(actor, session)
        }
    }

    fun verifyWithdrawalCode(
        session: ParkingSessionEntity,
        inputCode: String,
        onUpdated: (ParkingSessionEntity) -> Unit
    ) {
        val actor = currentUser.value ?: return
        viewModelScope.launch {
            repository.verifyWithdrawalCode(actor, session, inputCode)
                .onSuccess { updated ->
                    repository.refreshProActivity()
                    _toastMessage.value = "Code secret de retrait vérifié"
                    onUpdated(updated)
                }
                .onFailure { _toastMessage.value = it.message }
        }
    }

    fun unlockBlockedWithdrawal(
        session: ParkingSessionEntity,
        reason: String,
        onUpdated: (ParkingSessionEntity) -> Unit
    ) {
        val actor = currentUser.value ?: return
        viewModelScope.launch {
            repository.unlockBlockedWithdrawal(actor, session, reason)
                .onSuccess { updated ->
                    _toastMessage.value = "Blocage levé par le responsable"
                    onUpdated(updated)
                }
                .onFailure { _toastMessage.value = it.message }
        }
    }

    fun reportPhysicalMismatch(
        session: ParkingSessionEntity,
        description: String,
        onUpdated: (ParkingSessionEntity) -> Unit
    ) {
        val actor = currentUser.value ?: return
        viewModelScope.launch {
            repository.reportPhysicalMismatch(actor, session, description)
                .onSuccess { updated ->
                    _toastMessage.value = "Attention : non-conformité enregistrée. Sortie refusée."
                    onUpdated(updated)
                }
                .onFailure { _toastMessage.value = it.message }
        }
    }

    fun submitExceptionalRecovery(
        session: ParkingSessionEntity,
        reasonType: String,
        claimantName: String,
        declaredPlateNumber: String,
        description: String,
        physicalCheckConfirmed: Boolean,
        onSubmitted: () -> Unit
    ) {
        val actor = currentUser.value ?: return
        viewModelScope.launch {
            repository.submitExceptionalRecovery(
                actor, session, reasonType, claimantName, declaredPlateNumber, description, physicalCheckConfirmed
            ).onSuccess {
                _toastMessage.value = "Demande de récupération exceptionnelle envoyée au gestionnaire."
                onSubmitted()
            }.onFailure { _toastMessage.value = it.message }
        }
    }

    fun reviewExceptionalRecovery(
        recovery: ExceptionalRecoveryEntity,
        approve: Boolean,
        decisionReason: String,
        onDone: () -> Unit = {}
    ) {
        val reviewer = currentUser.value ?: return
        viewModelScope.launch {
            repository.reviewExceptionalRecovery(reviewer, recovery, approve, decisionReason)
                .onSuccess {
                    _toastMessage.value = if (approve) {
                        "Récupération exceptionnelle approuvée"
                    } else {
                        "Récupération exceptionnelle refusée"
                    }
                    onDone()
                }
                .onFailure { _toastMessage.value = it.message }
        }
    }

    fun verifySessionMobileMoney(
        session: ParkingSessionEntity,
        method: PaymentMethod,
        onUpdated: (ParkingSessionEntity) -> Unit
    ) {
        val actor = currentUser.value ?: return
        viewModelScope.launch {
            repository.verifyMobileMoneySessionPayment(actor, session, method)
                .onSuccess { updated ->
                    _toastMessage.value = "Paiement ${method.label} vérifié"
                    onUpdated(updated)
                }
                .onFailure { _toastMessage.value = it.message }
        }
    }

    fun authorizeExit(
        session: ParkingSessionEntity,
        physicalCheckConfirmed: Boolean,
        auditReason: String,
        onSuccess: (ParkingSessionEntity) -> Unit
    ) {
        val actor = currentUser.value ?: return
        refreshConnectivity()
        viewModelScope.launch {
            repository.authorizeExit(actor, session, physicalCheckConfirmed, auditReason)
                .onSuccess { updated ->
                    repository.refreshProActivity()
                    _toastMessage.value = "Sortie validée pour ${updated.plateNumber}"
                    onSuccess(updated)
                }
                .onFailure { _toastMessage.value = it.message }
        }
    }

    fun reportIncident(
        session: ParkingSessionEntity?,
        type: IncidentType,
        description: String,
        onSuccess: () -> Unit
    ) {
        val actor = currentUser.value ?: return
        val parking = activeParking.value ?: return
        viewModelScope.launch {
            repository.reportIncident(actor, parking, session, type, description)
                .onSuccess {
                    _toastMessage.value = "Incident signalé au gestionnaire"
                    onSuccess()
                }
                .onFailure { _toastMessage.value = it.message }
        }
    }

    fun resolveIncident(incident: IncidentEntity, note: String) {
        val actor = currentUser.value ?: return
        viewModelScope.launch {
            repository.resolveIncident(actor, incident, note)
                .onSuccess { _toastMessage.value = "Incident marqué comme résolu" }
                .onFailure { _toastMessage.value = it.message }
        }
    }

    fun inviteAttendant(fullName: String, phone: String, role: UserRole, onSuccess: () -> Unit) {
        val manager = currentUser.value ?: return
        val parking = activeParking.value ?: return
        viewModelScope.launch {
            repository.inviteAttendant(manager, parking, proAttendants.value, fullName, phone, role)
                .onSuccess {
                    _toastMessage.value = "Gardien invité avec succès"
                    onSuccess()
                }
                .onFailure { _toastMessage.value = it.message }
        }
    }

    fun toggleAttendantStatus(attendant: AttendantEntity) {
        val manager = currentUser.value ?: return
        viewModelScope.launch {
            repository.toggleAttendantStatus(manager, attendant)
                .onFailure { _toastMessage.value = it.message }
        }
    }

    fun toggleParkingSuspension(parking: ParkingLocationEntity) {
        val admin = currentUser.value ?: return
        viewModelScope.launch {
            repository.toggleParkingSuspension(admin, parking)
                .onFailure { _toastMessage.value = it.message }
        }
    }

    fun toggleUserSuspension(user: UserEntity) {
        val admin = currentUser.value ?: return
        viewModelScope.launch {
            repository.toggleUserSuspension(admin, user)
                .onFailure { _toastMessage.value = it.message }
        }
    }

    class Factory(private val repository: ParkoRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ParkoViewModel(repository) as T
        }
    }
}
