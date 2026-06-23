package com.zegoggles.smssync.di

import com.zegoggles.smssync.auth.ContactsPort
import com.zegoggles.smssync.auth.PeopleApiContactsAdapter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds ContactsPort -> PeopleApiContactsAdapter for the SingletonComponent.
 *
 * U-048: Converted from @Provides-new to @Binds abstract. PeopleApiContactsAdapter now
 * has an @Inject constructor (added by U-048), so Hilt constructs the single @Singleton
 * instance — no manual `new PeopleApiContactsAdapter()` in module code.
 *
 * CalendarPort is deferred to DES-011/U-029 (no interface or adapter exists yet).
 *
 * DES-MODERNIZATION-008 §Module layout:
 *   ContactsModule | @Binds ContactsPort <- PeopleApiContactsAdapter
 *                  | @Provides CalendarPort (impl from DES-011, deferred to U-029).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ContactsModule {

    /**
     * Binds [ContactsPort] to [PeopleApiContactsAdapter] as a @Singleton.
     * Hilt constructs [PeopleApiContactsAdapter] via its @Inject constructor
     * and returns the same instance for every injection point in the graph.
     */
    @Binds
    @Singleton
    abstract fun bindContactsPort(impl: PeopleApiContactsAdapter): ContactsPort

    // TODO(U-029): activate when DES-011 delivers CalendarPort interface + adapter
    // @Binds
    // abstract fun bindCalendarPort(impl: CalendarPortAdapter): CalendarPort
}
