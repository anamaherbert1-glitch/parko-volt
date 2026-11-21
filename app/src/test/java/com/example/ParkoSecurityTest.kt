package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.repository.ParkoRepository
import com.example.ui.viewmodel.ParkoViewModel
import com.example.util.ParkoSecurityConfig
import com.example.util.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ParkoSecurityTest {

    private lateinit var context: Context
    private lateinit var db: ParkoDatabase
    private lateinit var dao: ParkoDao
    private lateinit var prefs: SessionPreferences
    private lateinit var repository: ParkoRepository
    private var virtualNow: Long = 0L

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        virtualNow = System.currentTimeMillis()
        ParkoSecurityConfig.isDemoModeEnabled = true
        ParkoSecurityConfig.withdrawalTempLockAttempts = 3
        ParkoSecurityConfig.withdrawalManagerBlockAttempts = 5
        ParkoSecurityConfig.withdrawalTempLockDurationMs = 60_000L
        ParkoSecurityConfig.withdrawalCodeValidityMs = 7L * 24L * 3_600_000L
        ParkoSecurityConfig.exitLockDurationMs = 5L * 60_000L
        ParkoSecurityConfig.loginDelayThresholdAttempts = 3
        ParkoSecurityConfig.loginDelayDurationMs = 30_000L
        ParkoSecurityConfig.loginHardLockThresholdAttempts = 5
        ParkoSecurityConfig.loginHardLockDurationMs = 5L * 60_000L
        ParkoSecurityConfig.proSessionTimeoutMs = 8L * 3_600_000L

        db = Room.inMemoryDatabaseBuilder(context, ParkoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.parkoDao()
        prefs = SessionPreferences(context)
        runBlocking { prefs.clearSession() }
        repository = ParkoRepository(dao = dao, prefs = prefs, nowProvider = { virtualNow })
        repository.isOnlineProvider = { true }
        repository.clearLoginRateLimitForTests()
    }

    @After
    fun tearDown() {
        runBlocking { prefs.clearSession() }
        db.close()
        ParkoSecurityConfig.isDemoModeEnabled = true
        ParkoSecurityConfig.proSessionTimeoutMs = 8L * 3_600_000L
        Dispatchers.resetMain()
    }

    private suspend fun seedDemo() {
        repository.ensureSeeded()
    }

    private suspend fun createUser(
        id: String,
        firstName: String,
        lastName: String,
        phone: String,
        role: UserRole,
        password: String = "Secret123!"
    ): UserEntity {
        val salt = SecurityUtils.generateSalt()
        val u = UserEntity(
            id = id,
            firstName = firstName,
            lastName = lastName,
            phone = SecurityUtils.normalizeTogoPhone(phone),
            email = "$id@parko.tg",
            passwordHash = SecurityUtils.hashPassword(password, salt),
            salt = salt,
            role = role.name,
            isAccountVerified = true,
            createdAt = virtualNow
        )
        dao.upsertUser(u)
        return u
    }

    @Test
    fun `01 - QR code alone cannot authorize vehicle exit without verified secret code or recovery`() = runBlocking {
        seedDemo()
        val guard = dao.getUserById("usr_koffi")!!
        val parking = dao.getParkingById(SeedData.PARKING_GRAND_MARCHE_ID)!!

        val entry = repository.registerVehicleEntry(
            actor = guard,
            parking = parking,
            vehicleType = VehicleType.VOITURE,
            plateNumber = "TG-9001-ZZ",
            brandModel = "Peugeot 308",
            color = "Bleu",
            ownerName = "Client Test",
            ownerPhoneRaw = "+228 90 99 88 77",
            feeFcfa = 500,
            paymentMethod = PaymentMethod.CASH,
            photoUri = null
        ).getOrThrow()

        // Guard scans the QR token (QR alone) and even checks the physical box,
        // but did NOT verify the 6-digit secret withdrawal code.
        val scanned = repository.lookupParkingSessionForScan(guard, parking.id, entry.session.qrToken).getOrThrow()
        assertEquals(entry.session.id, scanned.id)

        val exitResult = repository.authorizeExit(
            actor = guard,
            session = scanned,
            physicalCheckConfirmed = true
        )
        assertTrue("Exit MUST fail without verified secret withdrawal code", exitResult.isFailure)
        val after = dao.getSessionById(scanned.id)!!
        assertEquals(SessionStatus.ACTIVE.name, after.status)
        assertNull(after.exitTime)

        val logs = dao.observeAllAuditLogs().first()
        assertTrue(logs.any { it.action == "SORTIE_REFUSEE_SANS_CODE" && it.result == "DENIED" })
    }

    @Test
    fun `02 - Replay of already COMPLETED ticket or QR is blocked and logged`() = runBlocking {
        seedDemo()
        val guard = dao.getUserById("usr_koffi")!!
        val parking = dao.getParkingById(SeedData.PARKING_GRAND_MARCHE_ID)!!

        val entry = repository.registerVehicleEntry(
            actor = guard,
            parking = parking,
            vehicleType = VehicleType.MOTO,
            plateNumber = "TG-4510-XY",
            brandModel = "Haoujue",
            color = "Noir",
            ownerName = "Kodjo Replay",
            ownerPhoneRaw = "+228 91 00 11 22",
            feeFcfa = 200,
            paymentMethod = PaymentMethod.CASH,
            photoUri = null
        ).getOrThrow()

        val verified = repository.verifyWithdrawalCode(guard, entry.session, entry.plainWithdrawalCode).getOrThrow()
        val completed = repository.authorizeExit(guard, verified, physicalCheckConfirmed = true).getOrThrow()
        assertEquals(SessionStatus.COMPLETED.name, completed.status)

        // Replay QR scan + exit attempt
        val reScanned = repository.lookupParkingSessionForScan(guard, parking.id, completed.qrToken).getOrThrow()
        assertEquals(SessionStatus.COMPLETED.name, reScanned.status)
        val replayExit = repository.authorizeExit(guard, reScanned, physicalCheckConfirmed = true)
        assertTrue("Replayed ticket exit MUST fail", replayExit.isFailure)
        assertTrue(replayExit.exceptionOrNull()?.message?.contains("déjà été utilisé") == true)

        val logs = dao.observeAllAuditLogs().first()
        assertTrue(logs.any { it.action == "QR_REUTILISE" && it.sessionId == completed.id && it.result == "DENIED" })
    }

    @Test
    fun `03 - Wrong secret codes trigger temp delay at 3 and manager block at 5 until manager unlocks`() = runBlocking {
        seedDemo()
        val guard = dao.getUserById("usr_koffi")!!
        val manager = dao.getUserById(SeedData.DEMO_USER_ID)!!
        val parking = dao.getParkingById(SeedData.PARKING_GRAND_MARCHE_ID)!!

        val entry = repository.registerVehicleEntry(
            actor = guard,
            parking = parking,
            vehicleType = VehicleType.VOITURE,
            plateNumber = "TG-7777-AA",
            brandModel = "Kia Rio",
            color = "Blanc",
            ownerName = "Test Brute",
            ownerPhoneRaw = "+228 90 55 44 33",
            feeFcfa = 500,
            paymentMethod = PaymentMethod.CASH,
            photoUri = null
        ).getOrThrow()

        val wrongCode = if (entry.plainWithdrawalCode == "000000") "999999" else "000000"

        // 1st and 2nd failed attempts -> regular failure without lock
        assertTrue(repository.verifyWithdrawalCode(guard, entry.session, wrongCode).isFailure)
        assertTrue(repository.verifyWithdrawalCode(guard, entry.session, wrongCode).isFailure)
        var state = dao.getSessionById(entry.session.id)!!
        assertEquals(2, state.withdrawalAttempts)
        assertEquals(0L, state.withdrawalLockedUntil)
        assertFalse(state.withdrawalBlockedForManager)

        // 3rd failed attempt -> temporary delay activated
        assertTrue(repository.verifyWithdrawalCode(guard, state, wrongCode).isFailure)
        state = dao.getSessionById(entry.session.id)!!
        assertEquals(3, state.withdrawalAttempts)
        assertTrue(state.withdrawalLockedUntil > virtualNow)
        assertFalse(state.withdrawalBlockedForManager)

        // Even the RIGHT code is blocked while temporary delay is active
        val duringDelay = repository.verifyWithdrawalCode(guard, state, entry.plainWithdrawalCode)
        assertTrue(duringDelay.isFailure)
        assertTrue(duringDelay.exceptionOrNull()?.message?.contains("Délai de sécurité") == true)

        // Advance time past temporary delay, then fail 4th and 5th times -> manager block
        virtualNow += ParkoSecurityConfig.withdrawalTempLockDurationMs + 1_000L
        assertTrue(repository.verifyWithdrawalCode(guard, state, wrongCode).isFailure)
        assertTrue(repository.verifyWithdrawalCode(guard, state, wrongCode).isFailure)
        state = dao.getSessionById(entry.session.id)!!
        assertEquals(5, state.withdrawalAttempts)
        assertTrue(state.withdrawalBlockedForManager)

        // Right code must now remain blocked until manager/supervisor unlocks
        val whileManagerBlocked = repository.verifyWithdrawalCode(guard, state, entry.plainWithdrawalCode)
        assertTrue(whileManagerBlocked.isFailure)
        assertTrue(whileManagerBlocked.exceptionOrNull()?.message?.contains("Intervention du gestionnaire") == true)

        // Attendant cannot unlock
        val guardUnlock = repository.unlockBlockedWithdrawal(guard, state, "Client retrouvé")
        assertTrue(guardUnlock.isFailure)
        assertTrue(guardUnlock.exceptionOrNull() is SecurityException)

        // Manager unlocks with mandatory reason
        val unlocked = repository.unlockBlockedWithdrawal(manager, state, "Identité du propriétaire vérifiée").getOrThrow()
        assertFalse(unlocked.withdrawalBlockedForManager)
        assertEquals(0, unlocked.withdrawalAttempts)

        // Now the valid code succeeds
        val verified = repository.verifyWithdrawalCode(guard, unlocked, entry.plainWithdrawalCode).getOrThrow()
        assertNotNull(verified.withdrawalVerifiedAt)
    }

    @Test
    fun `04 - Valid secret code plus physical check and confirmed payment completes exit and wipes encrypted code`() = runBlocking {
        seedDemo()
        val guard = dao.getUserById("usr_koffi")!!
        val parking = dao.getParkingById(SeedData.PARKING_GRAND_MARCHE_ID)!!

        val entry = repository.registerVehicleEntry(
            actor = guard,
            parking = parking,
            vehicleType = VehicleType.VOITURE,
            plateNumber = "TG-1122-CC",
            brandModel = "Toyota RAV4",
            color = "Gris",
            ownerName = "Amadou Diallo",
            ownerPhoneRaw = "+228 90 12 34 56",
            feeFcfa = 500,
            paymentMethod = PaymentMethod.CASH,
            photoUri = null
        ).getOrThrow()

        assertNotNull(entry.session.encryptedWithdrawalCode)
        val locked = repository.beginExitVerification(guard, entry.session).getOrThrow()
        assertEquals(SessionStatus.EXIT_PENDING.name, locked.status)

        val verified = repository.verifyWithdrawalCode(guard, locked, entry.plainWithdrawalCode).getOrThrow()
        val completed = repository.authorizeExit(guard, verified, physicalCheckConfirmed = true).getOrThrow()

        assertEquals(SessionStatus.COMPLETED.name, completed.status)
        assertTrue(completed.physicalCheckConfirmed)
        assertNotNull(completed.exitTime)
        assertNull("Encrypted owner secret MUST be wiped upon exit", completed.encryptedWithdrawalCode)
    }

    @Test
    fun `05 - User B cannot claim a ticket already belonging to User A`() = runBlocking {
        seedDemo()
        val thief = createUser("usr_thief", "Malveillant", "Test", "+228 99 88 77 66", UserRole.USER)
        val s1 = dao.getSessionById("ses_1")!! // belongs to Amadou (DEMO_USER_ID)

        val claim = repository.claimTicketByCode(thief, s1.referenceCode, s1.plateNumber)
        assertTrue(claim.isFailure)
        assertEquals(
            "Ce ticket est déjà associé à un autre compte. Vous ne pouvez pas vous l'attribuer.",
            claim.exceptionOrNull()?.message
        )
        // Ensure ownership unchanged
        assertEquals(SeedData.DEMO_USER_ID, dao.getSessionById("ses_1")!!.ownerUserId)

        val logs = dao.observeAllAuditLogs().first()
        assertTrue(logs.any { it.action == "TENTATIVE_RATTACHEMENT_FRAUDULEUX" && it.actorUserId == thief.id })
    }

    @Test
    fun `06 - Claiming unowned ticket requires PK ref plus matching phone plus matching plate and rejects QR token`() = runBlocking {
        seedDemo()
        val s3 = dao.getSessionById("ses_3")!! // ownerUserId = null, phone = +228 97 55 10 20, plate = TG-3310-CC
        val wrongPhoneUser = createUser("usr_other", "Autre", "Client", "+228 90 11 11 11", UserRole.USER)
        val realOwner = createUser("usr_kossi", "Kossi", "Mawuli", "+228 97 55 10 20", UserRole.USER)

        // QR token (PKO-...) rejected for claiming
        assertTrue(repository.claimTicketByCode(realOwner, s3.qrToken, s3.plateNumber).isFailure)

        // Wrong phone rejected
        val wrongPhoneRes = repository.claimTicketByCode(wrongPhoneUser, s3.referenceCode, s3.plateNumber)
        assertTrue(wrongPhoneRes.isFailure)
        assertNull(dao.getSessionById("ses_3")!!.ownerUserId)

        // Wrong plate rejected even with matching phone
        val wrongPlateRes = repository.claimTicketByCode(realOwner, s3.referenceCode, "TG-0000-XX")
        assertTrue(wrongPlateRes.isFailure)
        assertNull(dao.getSessionById("ses_3")!!.ownerUserId)

        // Exact PK reference + matching phone + matching plate succeeds
        val successRes = repository.claimTicketByCode(realOwner, s3.referenceCode, s3.plateNumber)
        assertTrue(successRes.isSuccess)
        assertEquals(realOwner.id, dao.getSessionById("ses_3")!!.ownerUserId)
    }

    @Test
    fun `07 - Cross-parking isolation blocks guard from Parking A accessing or releasing Parking B ticket`() = runBlocking {
        seedDemo()
        val guardParkingA = dao.getUserById("usr_koffi")!! // Assigned strictly to Grand Marché
        val sessionParkingB = dao.getSessionById("ses_2")!! // Belongs to CHU Tokoin

        val scanCross = repository.lookupParkingSessionForScan(guardParkingA, SeedData.PARKING_TOKOIN_ID, sessionParkingB.qrToken)
        assertTrue(scanCross.isFailure)
        assertTrue(scanCross.exceptionOrNull() is SecurityException)

        val lockCross = repository.beginExitVerification(guardParkingA, sessionParkingB)
        assertTrue(lockCross.isFailure)
        assertTrue(lockCross.exceptionOrNull() is SecurityException)

        val exitCross = repository.authorizeExit(guardParkingA, sessionParkingB, physicalCheckConfirmed = true)
        assertTrue(exitCross.isFailure)
        assertTrue(exitCross.exceptionOrNull() is SecurityException)

        // ViewModel strictly resolves Guard A to Parking A only (never fallback to Parking B)
        prefs.saveSession(guardParkingA.id, true, AppSpace.PRO)
        val vm = ParkoViewModel(repository)
        val resolvedParking = vm.activeParking.first { it != null }
        assertEquals(SeedData.PARKING_GRAND_MARCHE_ID, resolvedParking?.id)

        val logs = dao.observeAllAuditLogs().first()
        assertTrue(logs.any { it.action == "ACCES_REFUSE" && it.parkingId == SeedData.PARKING_TOKOIN_ID })
    }

    @Test
    fun `08 - Physical mismatch blocks exit even when secret withdrawal code is verified`() = runBlocking {
        seedDemo()
        val guard = dao.getUserById("usr_koffi")!!
        val s1 = dao.getSessionById("ses_1")!! // already has confirmed payment

        val verified = repository.verifyWithdrawalCode(guard, s1, SeedData.DEMO_SECRET_SES_1).getOrThrow()
        assertNotNull(verified.withdrawalVerifiedAt)

        // Physical check detects mismatch -> creates incident and moves status to NEEDS_VERIFICATION
        val flagged = repository.reportPhysicalMismatch(
            actor = guard,
            session = verified,
            description = "Couleur rouge au lieu de gris argent"
        ).getOrThrow()
        assertTrue(flagged.physicalMismatchReported)
        assertEquals(SessionStatus.NEEDS_VERIFICATION.name, flagged.status)

        val exitAttempt = repository.authorizeExit(guard, flagged, physicalCheckConfirmed = true)
        assertTrue("Exit MUST stay blocked on physical mismatch", exitAttempt.isFailure)

        val openIncidents = dao.getOpenIncidentsForSession(s1.id)
        assertTrue(openIncidents.any { it.type == IncidentType.VEHICLE_MISMATCH.name })

        val logs = dao.observeAllAuditLogs().first()
        assertTrue(logs.any { it.action == "CONTROLE_PHYSIQUE_NON_CONFORME" && it.result == "DENIED" })
    }

    @Test
    fun `09 - Concurrent exit lock EXIT_PENDING blocks second attendant from opening or completing exit`() = runBlocking {
        seedDemo()
        val guard1 = dao.getUserById("usr_koffi")!!
        val guard2 = dao.getUserById("usr_dohou")!!
        val s1 = dao.getSessionById("ses_1")!!

        val locked = repository.beginExitVerification(guard1, s1).getOrThrow()
        assertEquals(SessionStatus.EXIT_PENDING.name, locked.status)
        assertEquals(guard1.id, locked.exitPendingByUserId)

        // Second guard tries to open verification or authorize exit simultaneously
        val secondBegin = repository.beginExitVerification(guard2, locked)
        assertTrue(secondBegin.isFailure)
        assertTrue(secondBegin.exceptionOrNull()?.message?.contains("déjà en cours") == true)

        val verifiedBy1 = repository.verifyWithdrawalCode(guard1, locked, SeedData.DEMO_SECRET_SES_1).getOrThrow()
        val secondAuthorize = repository.authorizeExit(guard2, verifiedBy1, physicalCheckConfirmed = true)
        assertTrue(secondAuthorize.isFailure)
        assertTrue(secondAuthorize.exceptionOrNull()?.message?.contains("déjà en cours") == true)

        val logs = dao.observeAllAuditLogs().first()
        assertTrue(logs.any { it.action == "SORTIE_CONCURRENTE_BLOQUEE" && it.result == "DENIED" })
    }

    @Test
    fun `10 - Exceptional recovery without code requires manager or supervisor approval and forbids attendant approval`() = runBlocking {
        seedDemo()
        val guard = dao.getUserById("usr_koffi")!!
        val manager = dao.getUserById(SeedData.DEMO_USER_ID)!!
        val s1 = dao.getSessionById("ses_1")!!

        // Missing physical check or blank details rejected
        assertTrue(
            repository.submitExceptionalRecovery(
                actor = guard,
                session = s1,
                reasonType = "Téléphone éteint",
                claimantName = "Amadou Diallo",
                declaredPlateNumber = s1.plateNumber,
                description = "CNI présentée",
                physicalCheckConfirmed = false
            ).isFailure
        )

        val recovery = repository.submitExceptionalRecovery(
            actor = guard,
            session = s1,
            reasonType = "Ticket perdu",
            claimantName = "Amadou Diallo",
            declaredPlateNumber = s1.plateNumber,
            description = "Carte grise + CNI vérifiées physiquement",
            physicalCheckConfirmed = true
        ).getOrThrow()
        assertEquals(RecoveryStatus.PENDING.name, recovery.status)

        // Pending recovery must NOT allow exit yet
        val beforeApproval = repository.authorizeExit(guard, dao.getSessionById(s1.id)!!, physicalCheckConfirmed = true)
        assertTrue(beforeApproval.isFailure)

        // Attendant tries to self-approve -> blocked!
        val guardSelfApprove = repository.reviewExceptionalRecovery(guard, recovery, approve = true, decisionReason = "OK")
        assertTrue(guardSelfApprove.isFailure)
        assertTrue(guardSelfApprove.exceptionOrNull() is SecurityException)

        // Manager approves with written reason -> exit succeeds cleanly and logs RECUPERATION_EXCEPTIONNELLE
        val approved = repository.reviewExceptionalRecovery(
            reviewer = manager,
            recovery = recovery,
            approve = true,
            decisionReason = "Carte grise concordante contrôlée"
        ).getOrThrow()
        assertEquals(RecoveryStatus.APPROVED.name, approved.status)

        val exited = repository.authorizeExit(
            actor = guard,
            session = dao.getSessionById(s1.id)!!,
            physicalCheckConfirmed = true
        ).getOrThrow()
        assertEquals(SessionStatus.COMPLETED.name, exited.status)
        assertTrue(exited.exitWithoutQr)

        val logs = dao.observeAllAuditLogs().first()
        assertTrue(logs.any { it.action == "RECUPERATION_EXCEPTIONNELLE" && it.sessionId == s1.id })
    }

    @Test
    fun `11 - Secret withdrawal code expires properly and is never stored in plaintext or QR payload`() = runBlocking {
        seedDemo()
        val guard = dao.getUserById("usr_koffi")!!
        val parking = dao.getParkingById(SeedData.PARKING_GRAND_MARCHE_ID)!!

        val entry = repository.registerVehicleEntry(
            actor = guard,
            parking = parking,
            vehicleType = VehicleType.VOITURE,
            plateNumber = "TG-3210-ZZ",
            brandModel = "Honda Civic",
            color = "Noire",
            ownerName = "Secret Check",
            ownerPhoneRaw = "+228 90 12 34 56",
            feeFcfa = 500,
            paymentMethod = PaymentMethod.CASH,
            photoUri = null
        ).getOrThrow()

        val plain = entry.plainWithdrawalCode
        assertEquals(6, plain.length)
        assertTrue(plain.all { it.isDigit() })
        assertNotEquals(plain, entry.session.withdrawalCodeHash)
        assertNotEquals(plain, entry.session.encryptedWithdrawalCode)
        assertFalse("QR payload must NOT contain withdrawal code", entry.session.qrToken.contains(plain))
        assertFalse("QR payload must NOT contain plate PII", entry.session.qrToken.contains("3210"))
        assertEquals(plain, SecurityUtils.decryptSensitiveText(entry.session.encryptedWithdrawalCode))

        // Expire code validity window
        virtualNow += ParkoSecurityConfig.withdrawalCodeValidityMs + 60_000L
        val expiredAttempt = repository.verifyWithdrawalCode(guard, entry.session, plain)
        assertTrue(expiredAttempt.isFailure)
        assertTrue(expiredAttempt.exceptionOrNull()?.message?.contains("expiré") == true)

        val logs = dao.observeAllAuditLogs().first()
        assertTrue(logs.any { it.action == "CODE_SECRET_EXPIRE" && it.sessionId == entry.session.id })
    }

    @Test
    fun `12 - Brute-force login rate limiting locks repeated failures and pro session expires on inactivity`() = runBlocking {
        seedDemo()
        val id = "+228 90 12 34 56"

        // 2 failures -> regular error
        assertTrue(repository.login(id, "Wrong1", false).isFailure)
        assertTrue(repository.login(id, "Wrong2", false).isFailure)

        // 3rd failure -> temporary delay
        val third = repository.login(id, "Wrong3", false)
        assertTrue(third.isFailure)
        assertTrue(third.exceptionOrNull()?.message?.contains("Délai de sécurité") == true)

        // Even right password blocked during delay
        val rightDuringDelay = repository.login(id, "Parko2026!", false)
        assertTrue(rightDuringDelay.isFailure)

        // Advance past delay, fail 4th and 5th -> hard lock
        virtualNow += ParkoSecurityConfig.loginDelayDurationMs + 1_000L
        assertTrue(repository.login(id, "Wrong4", false).isFailure)
        val fifth = repository.login(id, "Wrong5", false)
        assertTrue(fifth.isFailure)
        assertTrue(fifth.exceptionOrNull()?.message?.contains("verrouillé") == true)

        // Even right password blocked during hard lock
        assertTrue(repository.login(id, "Parko2026!", false).isFailure)

        // Advance past hard lock -> login succeeds
        virtualNow += ParkoSecurityConfig.loginHardLockDurationMs + 1_000L
        val loggedIn = repository.login(id, "Parko2026!", false).getOrThrow()

        // Pro session inactivity timeout forces return to PERSONAL
        repository.setActiveSpace(AppSpace.PRO)
        assertEquals(AppSpace.PRO, repository.activeSpace.first())
        virtualNow += ParkoSecurityConfig.proSessionTimeoutMs + 60_000L
        val expired = repository.checkProSessionExpired(loggedIn)
        assertTrue(expired)
        assertEquals(AppSpace.PERSONAL, repository.activeSpace.first())

        val logs = dao.observeAllAuditLogs().first()
        assertTrue(logs.any { it.action == "TENTATIVE_CONNEXION_REPETEE" })
        assertTrue(logs.any { it.action == "EXPIRATION_SESSION_PRO" })
    }

    @Test
    fun `13 - Offline mode blocks final exit validation and logs event`() = runBlocking {
        seedDemo()
        val guard = dao.getUserById("usr_koffi")!!
        val s1 = dao.getSessionById("ses_1")!!

        val verified = repository.verifyWithdrawalCode(guard, s1, SeedData.DEMO_SECRET_SES_1).getOrThrow()

        // Simulate offline
        repository.isOnlineProvider = { false }
        val offlineExit = repository.authorizeExit(guard, verified, physicalCheckConfirmed = true)
        assertTrue("Offline exit validation MUST be blocked", offlineExit.isFailure)
        assertTrue(offlineExit.exceptionOrNull()?.message?.contains("hors connexion") == true)
        assertEquals(SessionStatus.ACTIVE.name, dao.getSessionById(s1.id)!!.status)

        val logs = dao.observeAllAuditLogs().first()
        assertTrue(logs.any { it.action == "SORTIE_REFUSEE_HORS_LIGNE" && it.result == "DENIED" })

        // Back online -> succeeds
        repository.isOnlineProvider = { true }
        val onlineExit = repository.authorizeExit(guard, verified, physicalCheckConfirmed = true)
        assertTrue(onlineExit.isSuccess)
    }

    @Test
    fun `14 - Subscription payment cannot be self-confirmed client-side and non-managers cannot access PRO`() = runBlocking {
        seedDemo()
        val regularUser = dao.getUserById(SeedData.CLIENT_USER_ID)!!
        val admin = dao.getUserById(SeedData.ADMIN_USER_ID)!!

        // Create an approved manager whose parking does NOT yet have an active subscription
        val newManager = createUser("usr_newmgr", "Sena", "Kokou", "+228 94 10 20 30", UserRole.MANAGER)
        val newParking = ParkingLocationEntity(
            id = "pkg_new_test",
            name = "Parking Kégué",
            address = "Carrefour Kégué",
            city = "Lomé",
            neighborhood = "Kégué",
            parkingType = "Mixte",
            acceptedTypes = "Moto, Voiture",
            managerUserId = newManager.id,
            subscriptionTier = SubscriptionTier.FREE_TRIAL.name,
            subscriptionActive = false,
            subscriptionExpiresAt = 0L,
            quotaUsed = 0,
            quotaLimit = 50,
            createdAt = virtualNow
        )
        dao.upsertParking(newParking)

        // Regular user cannot activate subscription
        val userAttempt = repository.activateSubscription(
            user = regularUser,
            parking = newParking,
            tier = SubscriptionTier.STANDARD,
            method = PaymentMethod.MIXX_BY_YAS,
            providerConfirmed = true
        )
        assertTrue(userAttempt.isFailure)
        assertTrue(userAttempt.exceptionOrNull() is SecurityException)

        // Even with providerConfirmed = true, paid subscription stays PENDING until Super-Admin confirms
        val pendingRecord = repository.activateSubscription(
            user = newManager,
            parking = newParking,
            tier = SubscriptionTier.STANDARD,
            method = PaymentMethod.MIXX_BY_YAS,
            providerConfirmed = true
        ).getOrThrow()!!
        assertEquals(PaymentStatus.PENDING.name, pendingRecord.status)
        assertFalse("Parking subscription must NOT self-activate client-side", dao.getParkingById(newParking.id)!!.subscriptionActive)

        // Non-admin cannot confirm pending subscription payment
        assertTrue(repository.confirmPendingSubscriptionPayment(newManager, pendingRecord, newParking).isFailure)

        // Super-Admin confirms -> subscription activates
        repository.confirmPendingSubscriptionPayment(admin, pendingRecord, newParking).getOrThrow()
        val activatedParking = dao.getParkingById(newParking.id)!!
        assertTrue(activatedParking.subscriptionActive)
        assertEquals(SubscriptionTier.STANDARD.name, activatedParking.subscriptionTier)

        // Regular user cannot unlock PRO space in ViewModel
        prefs.saveSession(regularUser.id, true, AppSpace.PERSONAL)
        val vm = ParkoViewModel(repository)
        vm.switchSpace(AppSpace.PRO)
        assertEquals(AppSpace.PERSONAL, vm.activeSpace.first())
    }

    @Test
    fun `15 - Full security audit trail is append-only and persists all security events`() = runBlocking {
        seedDemo()
        val guard = dao.getUserById("usr_koffi")!!
        val s1 = dao.getSessionById("ses_1")!!

        // Generate multiple distinct security events
        repository.verifyWithdrawalCode(guard, s1, "111111") // CODE_SECRET_INCORRECT
        repository.authorizeExit(guard, s1, physicalCheckConfirmed = true) // SORTIE_REFUSEE_SANS_CODE
        val verified = repository.verifyWithdrawalCode(guard, dao.getSessionById(s1.id)!!, SeedData.DEMO_SECRET_SES_1).getOrThrow()
        repository.authorizeExit(guard, verified, physicalCheckConfirmed = true).getOrThrow() // SORTIE_VALIDEE

        val logs = dao.observeAllAuditLogs().first()
        assertTrue(logs.any { it.action == "CODE_SECRET_INCORRECT" && it.sessionId == s1.id && it.result == "DENIED" })
        assertTrue(logs.any { it.action == "SORTIE_REFUSEE_SANS_CODE" && it.sessionId == s1.id && it.result == "DENIED" })
        assertTrue(logs.any { it.action == "CODE_SECRET_VERIFIE" && it.sessionId == s1.id && it.result == "SUCCESS" })
        assertTrue(logs.any { it.action == "SORTIE_VALIDEE" && it.sessionId == s1.id && it.result == "SUCCESS" })

        // Verify append-only constraint (ABORT on conflict with same ID)
        val existingLog = logs.first()
        var aborted = false
        try {
            dao.insertAuditLog(existingLog.copy(details = "TAMPERED"))
        } catch (_: Throwable) {
            aborted = true
        }
        assertTrue("Audit log table must reject overwrite/modification", aborted)
    }

    @Test
    fun `16 - Production mode disables demo seeding, demo login, and local OTP reset`() = runBlocking {
        ParkoSecurityConfig.isDemoModeEnabled = false

        repository.ensureSeeded()
        assertEquals(0, dao.getUserCount())
        assertTrue(dao.observeAllParkings().first().isEmpty())

        val demoLogin = repository.loginDemo(SeedData.DEMO_USER_ID)
        assertTrue(demoLogin.isFailure)
        assertTrue(demoLogin.exceptionOrNull() is SecurityException)

        val realUser = createUser("usr_prod", "Prod", "User", "+228 90 12 34 56", UserRole.USER)
        val otpReset = repository.resetPasswordByPhone(realUser.phone, "NewPass99!", "1234")
        assertTrue("Local OTP 1234 reset MUST fail in production mode", otpReset.isFailure)
    }
}
