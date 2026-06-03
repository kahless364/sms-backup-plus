package com.zegoggles.smssync.service.state

import com.zegoggles.smssync.activity.AppPermission
import com.zegoggles.smssync.activity.events.PerformAction
import com.zegoggles.smssync.tasks.OAuth2CallbackTask

/**
 * Sealed class absorbing the transient Otto POJOs. Each variant maps 1:1 to a current
 * event so no consumer behavior is lost. Member signatures are fixed by
 * CNTR-MODERNIZATION-006 §Type: SyncEvent.
 *
 * U-019: Step 1 (Facade) — these events are still dispatched via App.bus / Otto in this
 * step. The Flow-backed dispatch is wired in Step 2 (U-020).
 *
 * Implementation notes on ThemeChanged:
 *   Source ThemeChangedEvent.java (activity/events/ThemeChangedEvent.java) is payload-less
 *   (no fields). CNTR-MODERNIZATION-006 specifies "data class ThemeChanged(val themeId: Int)
 *   — payload TBD at impl; see Notes". After reading the source POJO, the correct
 *   verbatim mapping is a payload-less object. Implemented as `object ThemeChanged` to
 *   match the source. The contract note is satisfied: "payload type confirmed at
 *   implementation; this AC is satisfied when the field matches the source theme event's
 *   payload verbatim". The source has no payload, so no field is the correct verbatim match.
 */
sealed class SyncEvent {

    /**
     * Replaces service/CancelEvent.java (CancelEvent.java:5-19).
     * The USER vs SYSTEM distinction is LOAD-BEARING: it drives mayInterruptIfRunning(),
     * which returns true ONLY for SYSTEM.
     * Default origin is USER (matches CancelEvent()'s no-arg ctor, CancelEvent.java:9-11).
     * NOTE: Origin is promoted from package-private to public — a deliberate minimal
     * public-API surface change (CNTR-MODERNIZATION-006 §Notes).
     */
    data class Cancel(val origin: Origin = Origin.USER) : SyncEvent() {
        enum class Origin { USER, SYSTEM }
        fun mayInterruptIfRunning(): Boolean = origin == Origin.SYSTEM
    }

    /**
     * Replaces activity/events/PerformAction.java (PerformAction.java:3-17).
     * Both the Actions enum {Backup, BackupSkip, Restore} AND the `confirm` flag are
     * preserved verbatim — `confirm` gates the confirmation dialog in
     * Dialogs.performAction (Dialogs.java:331-343).
     */
    data class PerformActionRequested(
        val action: PerformAction.Actions,
        val confirm: Boolean
    ) : SyncEvent()

    /**
     * Replaces activity/events/MissingPermissionsEvent.java (MissingPermissionsEvent.java:7-12).
     * Payload is List<AppPermission> (NOT List<String>). MUST remain one-shot (non-sticky).
     */
    data class MissingPermissions(val permissions: List<AppPermission>) : SyncEvent()

    /**
     * Replaces OAuth2CallbackTask.OAuth2CallbackEvent.
     * Payload shape confirmed at implementation by reading OAuth2CallbackTask.java:42-51:
     *   OAuth2CallbackEvent has a single field: final OAuth2Token token.
     * Carried verbatim as OAuth2CallbackTask.OAuth2CallbackEvent payload.
     */
    data class OAuth2Callback(val payload: OAuth2CallbackTask.OAuth2CallbackEvent) : SyncEvent()

    // --- Account / settings / theme events absorbed from transient Otto POJOs.
    // Each maps 1:1 to an existing activity/events/* POJO. Payload-less members are objects.

    /** Replaces activity/events/AccountAddedEvent.java */
    object AccountAdded : SyncEvent()

    /** Replaces activity/events/AccountRemovedEvent.java */
    object AccountRemoved : SyncEvent()

    /** Replaces activity/events/AccountConnectionChangedEvent.java */
    object AccountConnectionChanged : SyncEvent()

    /** Replaces activity/events/AutoBackupSettingsChangedEvent.java */
    object AutoBackupSettingsChanged : SyncEvent()

    /** Replaces activity/events/FallbackAuthEvent.java */
    object FallbackAuth : SyncEvent()

    /** Replaces activity/events/SettingsResetEvent.java */
    object SettingsReset : SyncEvent()

    /**
     * Replaces activity/events/ThemeChangedEvent.java.
     * Source POJO is payload-less (ThemeChangedEvent.java has no fields).
     * Implemented as object to match the source payload verbatim.
     * CNTR-MODERNIZATION-006 specifies "themeId: Int — payload TBD at impl";
     * reading the source reveals no payload, so object is the correct form.
     */
    object ThemeChanged : SyncEvent()

    /**
     * U-020: Replaces RedirectReceiverActivity.BrowserAuthResult inner class.
     * Carries the OAuth2 browser redirect result (code/error) from
     * RedirectReceiverActivity to OAuth2WebAuthActivity.
     * The inner BrowserAuthResult class is deleted; consumers collect this event.
     */
    data class BrowserAuthResult(val code: String?, val error: String?) : SyncEvent()
}
