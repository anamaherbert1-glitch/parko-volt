package com.example.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.example.data.model.*

enum class AppLanguage(val storageValue: String, val displayLabel: String, val shortCode: String) {
    FR("Français", "Français", "FR"),
    EN("English", "English", "EN");

    companion object {
        fun fromStorage(raw: String?): AppLanguage {
            return if (raw.equals("English", ignoreCase = true) || raw.equals("EN", ignoreCase = true)) {
                EN
            } else {
                FR
            }
        }
    }
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.FR }

@Composable
fun tr(fr: String, en: String): String {
    return if (LocalAppLanguage.current == AppLanguage.EN) en else fr
}

fun isEn(language: String): Boolean = AppLanguage.fromStorage(language) == AppLanguage.EN

fun ThemeMode.localizedLabel(en: Boolean): String = when (this) {
    ThemeMode.SYSTEM -> if (en) "System" else "Système"
    ThemeMode.LIGHT -> if (en) "Light" else "Clair"
    ThemeMode.DARK -> if (en) "Dark" else "Sombre"
}

fun UserRole.localizedLabel(en: Boolean): String = when (this) {
    UserRole.USER -> if (en) "User" else "Utilisateur"
    UserRole.ATTENDANT -> if (en) "Attendant" else "Gardien"
    UserRole.SUPERVISOR -> if (en) "Supervisor" else "Superviseur"
    UserRole.MANAGER -> if (en) "Manager" else "Gestionnaire"
    UserRole.SUPER_ADMIN -> if (en) "Super Admin" else "Super administrateur"
}

fun VehicleType.localizedLabel(en: Boolean): String = when (this) {
    VehicleType.MOTO -> if (en) "Motorcycle" else "Moto"
    VehicleType.VOITURE -> if (en) "Car" else "Voiture"
    VehicleType.TRICYCLE -> "Tricycle"
    VehicleType.CAMION -> if (en) "Truck" else "Camion"
    VehicleType.AUTRE -> if (en) "Other" else "Autre"
}

fun SessionStatus.localizedLabel(en: Boolean): String = when (this) {
    SessionStatus.ACTIVE -> if (en) "Valid" else "Valide"
    SessionStatus.EXIT_PENDING -> if (en) "Exit in progress" else "Sortie en cours"
    SessionStatus.COMPLETED -> if (en) "Completed" else "Terminée"
    SessionStatus.EXPIRED -> if (en) "Expired" else "Expirée"
    SessionStatus.NEEDS_VERIFICATION -> if (en) "Check required" else "Vérification requise"
}

fun RecoveryStatus.localizedLabel(en: Boolean): String = when (this) {
    RecoveryStatus.PENDING -> if (en) "Pending" else "En attente"
    RecoveryStatus.APPROVED -> if (en) "Approved" else "Approuvée"
    RecoveryStatus.REJECTED -> if (en) "Rejected" else "Refusée"
}

fun PaymentMethod.localizedLabel(en: Boolean): String = when (this) {
    PaymentMethod.CASH -> if (en) "Cash" else "Espèces"
    PaymentMethod.MIXX_BY_YAS -> "Mixx by Yas"
    PaymentMethod.FLOOZ -> "Flooz"
}

fun PaymentStatus.localizedLabel(en: Boolean): String = when (this) {
    PaymentStatus.PENDING -> if (en) "Pending" else "En attente"
    PaymentStatus.CONFIRMED -> if (en) "Confirmed" else "Confirmé"
    PaymentStatus.FAILED -> if (en) "Failed" else "Échoué"
    PaymentStatus.CANCELLED -> if (en) "Cancelled" else "Annulé"
    PaymentStatus.REFUNDED -> if (en) "Refunded" else "Remboursé"
}

fun ApplicationStatus.localizedLabel(en: Boolean): String = when (this) {
    ApplicationStatus.PENDING -> if (en) "Pending" else "En attente"
    ApplicationStatus.UNDER_REVIEW -> if (en) "Under review" else "En cours d'examen"
    ApplicationStatus.MORE_INFO_REQUIRED -> if (en) "Info required" else "Infos requises"
    ApplicationStatus.APPROVED -> if (en) "Approved" else "Approuvée"
    ApplicationStatus.REJECTED -> if (en) "Rejected" else "Rejetée"
}

fun SubscriptionTier.localizedTitle(en: Boolean): String = when (this) {
    SubscriptionTier.FREE_TRIAL -> if (en) "Free Trial" else "Essai gratuit"
    SubscriptionTier.ESSENTIEL -> if (en) "Essential" else "Essentiel"
    SubscriptionTier.STANDARD -> "Standard"
    SubscriptionTier.PRO_UNLIMITED -> "Pro Unlimited"
}

fun SubscriptionTier.localizedPeriod(en: Boolean): String = when (this) {
    SubscriptionTier.FREE_TRIAL -> "0 FCFA"
    SubscriptionTier.ESSENTIEL -> if (en) "500 FCFA/mo" else "500 FCFA/mois"
    SubscriptionTier.STANDARD -> if (en) "3,000 FCFA/mo" else "3 000 FCFA/mois"
    SubscriptionTier.PRO_UNLIMITED -> if (en) "5,000 FCFA/mo" else "5 000 FCFA/mois"
}

fun SubscriptionTier.localizedHighlights(en: Boolean): List<String> {
    if (!en) return highlights
    return when (this) {
        SubscriptionTier.FREE_TRIAL -> listOf(
            "50 new parkings / month",
            "1 attendant",
            "QR tickets & exit scanning",
            "Basic history"
        )
        SubscriptionTier.ESSENTIEL -> listOf(
            "15 new parkings / day",
            "1 attendant • Motorcycles & cars",
            "QR tickets & entry/exit validation",
            "Basic history"
        )
        SubscriptionTier.STANDARD -> listOf(
            "50 vehicles / day • Up to 3 attendants",
            "Vehicle photos included",
            "Daily reports & monthly stats",
            "Full payment management"
        )
        SubscriptionTier.PRO_UNLIMITED -> listOf(
            "Unlimited parkings & QR tickets",
            "Up to 7 connected attendants",
            "Rankings, badges & incident tracking",
            "CSV export & advanced dashboard"
        )
    }
}

fun IncidentType.localizedLabel(en: Boolean): String = when (this) {
    IncidentType.INVALID_QR -> if (en) "Invalid QR code" else "QR code invalide"
    IncidentType.ALREADY_USED_QR -> if (en) "QR code already used" else "QR code déjà utilisé"
    IncidentType.LOST_TICKET -> if (en) "Lost ticket" else "Perte du ticket"
    IncidentType.VEHICLE_MISMATCH -> if (en) "Vehicle mismatch" else "Véhicule non conforme"
    IncidentType.PAYMENT_DISPUTE -> if (en) "Payment dispute" else "Désaccord sur le paiement"
    IncidentType.REGISTRATION_ERROR -> if (en) "Registration error" else "Erreur d'enregistrement"
    IncidentType.CONTESTED_EXIT -> if (en) "Contested exit" else "Sortie contestée"
    IncidentType.ATTENDANT_ISSUE -> if (en) "Attendant issue" else "Incident impliquant un gardien"
}

fun IncidentStatus.localizedLabel(en: Boolean): String = when (this) {
    IncidentStatus.OPEN -> if (en) "Open" else "Ouvert"
    IncidentStatus.IN_REVIEW -> if (en) "In review" else "En examen"
    IncidentStatus.RESOLVED -> if (en) "Resolved" else "Résolu"
}

fun ParkingSizePreset.localizedTitle(en: Boolean): String = if (en) titleEn else titleFr
fun ParkingSizePreset.localizedCapacity(en: Boolean): String = if (en) capacityLabelEn else capacityLabelFr
