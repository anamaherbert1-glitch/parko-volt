package com.example.util

import com.example.BuildConfig

/**
 * Centralized, configurable security policy constants for PARKO.
 * Values can be overridden in unit tests to verify lockouts, expirations, and production mode.
 */
object ParkoSecurityConfig {
    /** Demo shortcuts & demo data seeding are active ONLY in debug/test builds, never in production. */
    @Volatile
    var isDemoModeEnabled: Boolean = BuildConfig.DEBUG

    /** Configurable thresholds for secret withdrawal code verification. */
    @Volatile
    var withdrawalTempLockAttempts: Int = 3

    @Volatile
    var withdrawalManagerBlockAttempts: Int = 5

    @Volatile
    var withdrawalTempLockDurationMs: Long = 60_000L

    @Volatile
    var withdrawalCodeValidityMs: Long = 7L * 24L * 3_600_000L

    /** Configurable lock duration for EXIT_PENDING session checkout. */
    @Volatile
    var exitLockDurationMs: Long = 5L * 60_000L

    /** Configurable login rate limiting thresholds. */
    @Volatile
    var loginDelayThresholdAttempts: Int = 3

    @Volatile
    var loginDelayDurationMs: Long = 30_000L

    @Volatile
    var loginHardLockThresholdAttempts: Int = 5

    @Volatile
    var loginHardLockDurationMs: Long = 5L * 60_000L

    /** Maximum inactivity / lifetime before a professional session expires. */
    @Volatile
    var proSessionTimeoutMs: Long = 8L * 3_600_000L
}
