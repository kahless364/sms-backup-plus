package com.zegoggles.smssync.di

import com.zegoggles.smssync.auth.ContactsPort
import com.zegoggles.smssync.auth.PeopleApiContactsAdapter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds ContactsPort -> PeopleApiContactsAdapter for the SingletonComponent.
 * CalendarPort is stubbed (no adapter exists yet — deferred to DES-011).
 *
 * U-022: AC-5 — ContactsModule is PARTIALLY ACTIVE.
 * - ContactsPort: ACTIVE — PeopleApiContactsAdapter delivered by U-029 / DES-011 exists.
 * - CalendarPort: STUBBED — no CalendarPort interface or adapter exists yet (DES-011).
 *
 * PeopleApiContactsAdapter has an implicit no-arg constructor (no @Inject annotation yet;
 * U-023 will add it). Using @Provides for now.
 *
 * TODO(U-023): convert @Provides to @Binds once PeopleApiContactsAdapter has @Inject constructor.
 * TODO(U-029): activate CalendarPort binding when DES-011 delivers CalendarPort interface + adapter.
 *
 * DES-MODERNIZATION-008 §Module layout: ContactsModule | @Binds ContactsPort <- PeopleApiContactsAdapter
 * / @Provides CalendarPort (impl from DES-011).
 */
@Module
@InstallIn(SingletonComponent::class)
object ContactsModule {

    @Provides
    @Singleton
    fun provideContactsPort(): ContactsPort = PeopleApiContactsAdapter()

    // TODO(U-029): activate when DES-011 delivers CalendarPort interface + adapter
    // @Binds
    // abstract fun bindCalendarPort(impl: CalendarPortAdapter): CalendarPort
}
