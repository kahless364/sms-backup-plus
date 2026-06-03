package com.zegoggles.smssync.di

import javax.inject.Qualifier

/**
 * Qualifier for the IO CoroutineDispatcher (Dispatchers.IO).
 *
 * U-022: AC-7 — qualifier file created alongside DispatcherModule.
 * Usage: annotate parameters or @Provides methods with @IoDispatcher to distinguish
 * the IO dispatcher from any other dispatchers that may be provided in future modules.
 *
 * Retention.BINARY: retained in bytecode but not at runtime — the recommended
 * retention for Dagger/Hilt qualifiers (compile-time only).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class IoDispatcher
