---
scr_id: SCR-ADV-001
screen: AdvancedSettings (Backup, Restore, Server, Main, CallLog sub-screens)
source_files:
  - app/src/main/java/com/zegoggles/smssync/activity/fragments/AdvancedSettings.java
  - app/src/main/java/com/zegoggles/smssync/activity/fragments/SMSBackupPreferenceFragment.java
  - app/src/main/java/com/zegoggles/smssync/activity/fragments/MainSettings.java
  - app/src/main/java/com/zegoggles/smssync/activity/fragments/AutoBackupSettings.java
  - app/src/main/java/com/zegoggles/smssync/preferences/Preferences.java
  - app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java
  - app/src/main/java/com/zegoggles/smssync/preferences/DataTypePreferences.java
  - app/src/main/java/com/zegoggles/smssync/mail/DataType.java
  - app/src/main/java/com/zegoggles/smssync/mail/BackupImapStore.java
  - app/src/main/java/com/zegoggles/smssync/calendar/CalendarAccessor.java
  - app/src/main/java/com/zegoggles/smssync/contacts/ContactAccessor.java
  - app/src/main/res/xml/preferences.xml
  - app/src/main/res/values/strings.xml
  - app/src/main/res/values/arrays.xml
complexity: High — 5 nested preference sub-screens with runtime permission checks, inline business logic for IMAP folder validation, calendar enumeration, contact group enumeration, DataType-keyed enable/disable cascade, and legacy XOAuth2 connect toggle
priority: P1 Critical
generated: 2026-05-29
refactor_recommendation: REFACTOR-IN-PLACE — extract inline business logic to ViewModel; no rebuild warranted
---

# Screen Specification: AdvancedSettings

**SCR ID:** SCR-ADV-001
**Screen:** AdvancedSettings — Five Nested Preference Sub-Screens
**Source Application:** SMS Backup+
**Complexity:** High
**Priority:** P1 Critical
**Last Updated:** 2026-05-29

---

## Section 1: Screen Identity & User Story

**As a** device owner configuring SMS Backup+,
**I want to** set all backup, restore, server, and scheduling preferences, including which data types to back up, which IMAP folder to use, whether to sync call logs to calendar, and how to authenticate,
**So that** the app backs up exactly what I want, where I want it, and according to my schedule.

```gherkin
Feature: AdvancedSettings Preference Sub-Screens

  Background:
    Given the user is on the main settings screen
    And has navigated into a settings sub-screen

  # --- AdvancedSettings.Main ---

  Scenario: Connected account summary reflects OAuth2 username
    Given the user is in the "Advanced settings" screen
    And OAuth2 tokens are stored for user "test@gmail.com"
    Then the "Connect" toggle shows summary "Connected as test@gmail.com."

  Scenario: Connected toggle shows needs-connecting when no tokens
    Given no OAuth2 tokens stored
    Then the "Connect" toggle shows summary "Connect your Gmail account (unsupported)."

  Scenario: User toggles Connect on — opens AccountManager flow
    Given the user is in Advanced settings
    When the user toggles "Connect" on
    Then AccountConnectionChangedEvent(connected=true) is posted

  Scenario: User toggles Connect off — shows disconnect dialog
    When the user toggles "Connect" off
    Then AccountConnectionChangedEvent(connected=false) is posted
    And the DISCONNECT dialog appears

  Scenario: Account added/removed refreshes connected toggle
    When AccountAddedEvent or AccountRemovedEvent is received
    Then the "Connect" toggle checked state reflects hasOAuth2Tokens()

  # --- AdvancedSettings.Backup ---

  Scenario: Last backup time shown per data type
    Given the user is in Backup settings
    When onResume fires
    Then SMS checkbox summary shows "Last item backed up: {date}" or "never"
    And MMS checkbox summary shows same pattern
    And Call log checkbox summary shows same pattern

  Scenario: Max items per sync displayed in title
    Given max_items_per_sync is set to 100
    When onResume fires
    Then the "Items per backup" preference title shows "100"
    When the user changes it to "All" (value -1)
    Then the title shows "All"

  Scenario: Call log backup requires permissions
    Given the call log backup checkbox is unchecked
    When the user checks "Backup Call log"
    And READ_CALL_LOG permission is not granted
    Then a permission request is sent for READ_CALL_LOG
    When permission is granted
    Then the call log checkbox is checked

  Scenario: Call log settings sub-screen enabled only when call log enabled
    Given backup_calllog DataType is enabled
    Then the "Call log settings" sub-screen preference is enabled
    Given backup_calllog DataType is disabled
    Then the "Call log settings" sub-screen preference is disabled

  Scenario: IMAP folder name validated
    Given the user edits the SMS IMAP folder
    When the user enters a folder name with leading or trailing spaces
    Then the INVALID_IMAP_FOLDER dialog is shown
    And "The label name may not contain spaces (at the beginning or end)."
    And the preference is NOT saved

  Scenario: Contact group selector requires READ_CONTACTS
    Given READ_CONTACTS permission is NOT granted
    When onResume fires
    Then the "Contacts to backup" preference is disabled

  Scenario: Contact group selector populated from device contacts
    Given READ_CONTACTS permission IS granted
    When onResume fires
    Then the contact group list is populated with groups from ContactAccessor
    And the preference title shows the currently selected group name

  # --- AdvancedSettings.Backup.CallLog ---

  Scenario: Calendar sync requires WRITE_CALENDAR permission
    Given the user enables "Calendar sync"
    And WRITE_CALENDAR is not granted
    Then permission is requested for WRITE_CALENDAR
    When granted
    Then "Calendar sync" becomes checked

  Scenario: Calendar sync disabled when WRITE_CALENDAR revoked
    Given "Calendar sync" was enabled
    And user revokes WRITE_CALENDAR in system settings
    When onResume fires
    Then "Calendar sync" is automatically unchecked (permission re-check)

  Scenario: Calendar list populated with device calendars
    Given WRITE_CALENDAR is granted
    When onResume fires
    Then the calendar ListPreference is populated with device calendars via CalendarAccessor

  Scenario: Call log IMAP folder validated
    Given the user edits the call log IMAP folder
    When an invalid name is entered
    Then INVALID_IMAP_FOLDER dialog shown

  # --- AdvancedSettings.Restore ---

  Scenario: Max items per restore displayed in title
    Given max_items_per_restore is 500
    When onResume fires
    Then "Items per restore" preference title shows "500"

  # --- AdvancedSettings.Server ---

  Scenario: IMAP password change saved to credentials
    Given the user is in IMAP settings
    When the user changes the password field
    Then authPreferences.setImapPassword() is called with the new value

  Scenario: Server screen summary in MainSettings
    Given auth mode is PLAIN and credentials are set
    Then the server screen preference in MainSettings shows "username@server" or "username (Gmail)"
    Given auth mode is PLAIN and credentials are NOT set
    Then shows "Not configured"
```

---

## Section 2: Screen Layout

### AdvancedSettings.Main sub-screen

```
+---------------------------------------------+
| Toolbar: Advanced settings                  |
+---------------------------------------------+
| [>] Restore settings                        |
| [ ] Notifications                           |
| [ ] Confirm actions                         |
| [ ] Sync log                                |
| [ ] Extra debug information  (dep: app_log) |
| [switch] Dark theme                         |
| --- Legacy settings ---                     |
| (summary: XOAuth2 is no longer supported)  |
| [list] Authentication         IMAP user/pwd |
| [switch] Connect             (un)connected  |
+---------------------------------------------+
```

### AdvancedSettings.Backup sub-screen

```
+---------------------------------------------+
| Toolbar: Backup settings                    |
+---------------------------------------------+
| [x] Backup SMS           Last backed up:... |
| [x] Backup MMS           Last backed up:... |
| [ ] Backup Call log      Last backed up:... |
| [>] Call log settings    (enabled if above) |
| [list] Items per backup  100 / All / ...    |
| [list] Mark as read (emails)                |
| [edit] Gmail label / IMAP folder    SMS     |
| [list] Contacts to backup  Everybody / ...  |
| [list] Email address style Name / ...       |
| [ ] Email subject prefix                    |
+---------------------------------------------+
```

### AdvancedSettings.Backup.CallLog sub-screen

```
+---------------------------------------------+
| Toolbar: Call log settings                  |
+---------------------------------------------+
| [edit] Call log label       Call_log        |
| [list] Call types           Everything/...  |
| [ ] Calendar sync           add to calendar |
| [list] Calendar             (dep: above)    |
| [ ] Backup after call                       |
+---------------------------------------------+
```

### AdvancedSettings.Restore sub-screen

```
+---------------------------------------------+
| Toolbar: Restore settings                   |
+---------------------------------------------+
| [x] Restore SMS                             |
| [x] Restore call log                        |
| [ ] Starred items only                      |
| [list] Items per restore    500 / All / ... |
| [x] Mark as read (SMS)                      |
+---------------------------------------------+
```

### AdvancedSettings.Server sub-screen (IMAP settings)

```
+---------------------------------------------+
| Toolbar: IMAP settings                      |
+---------------------------------------------+
| [edit] Server address       imap.gmail.com:993 |
| [edit] Username             email address   |
| [edit] Password             (masked)        |
| [list] Security             TLS / STARTTLS / None |
| [ ] Trust all certificates  (dangerous)    |
+---------------------------------------------+
```

**Layout semantics:**

All sub-screens are `PreferenceFragmentCompat` instances rendered as scrollable RecyclerViews of preference items. Layout is entirely managed by the AndroidX Preference library.

| Section | Layout Type | Scroll Behavior |
|---------|-------------|-----------------|
| Each sub-screen | RecyclerView (Preference library) | Vertically scrollable |
| Header | Inherited from MainActivity toolbar | Fixed |

---

## Section 3: Field Inventory

### 3.1 Input Fields — All Sub-Screens

#### AdvancedSettings.Main

| Field ID | Label (exact) | Type | Key | Required | Validation | Default |
|----------|--------------|------|-----|----------|------------|---------|
| server_authentication | "Authentication" | ListPreference | server_authentication | No | Enum: plain, xoauth | plain |
| connected | "Connect" | SwitchPreferenceCompat | connected | No | N/A | off |
| notifications | "Notifications" | CheckBoxPreference | notifications | No | N/A | false |
| confirm_action | "Confirm actions" | CheckBoxPreference | confirm_action | No | N/A | false |
| app_log | "Sync log" | CheckBoxPreference | app_log | No | N/A | false |
| app_log_debug | "Extra debug information" | CheckBoxPreference | app_log_debug | No | N/A (dependency: app_log) | false |
| dark_theme | "Dark theme" | SwitchPreferenceCompat | dark_theme | No | N/A | off |

#### AdvancedSettings.Backup

| Field ID | Label (exact) | Type | Key | Required | Validation | Default |
|----------|--------------|------|-----|----------|------------|---------|
| backup_sms | "Backup SMS" | CheckBoxPreference | backup_sms | No | N/A | true |
| backup_mms | "Backup MMS" | CheckBoxPreference | backup_mms | No | N/A | true |
| backup_calllog | "Backup Call log" | CheckBoxPreference | backup_calllog | No | Permission: READ_CALL_LOG | false |
| max_items_per_sync | "Items per backup" | ListPreference | max_items_per_sync | No | From array entries | -1 (All) |
| mark_as_read_types | "Mark as read (emails)" | ListPreference | mark_as_read_types | No | Enum: read/unread/message_status | read |
| imap_folder | "Gmail label / IMAP folder" | EditTextPreference | imap_folder | Yes | BackupImapStore.isValidImapFolder() — no leading/trailing spaces | "SMS" |
| backup_contact_group | "Contacts to backup" | ListPreference | backup_contact_group | No | Dynamic from ContactAccessor; requires READ_CONTACTS | (all) |
| email_address_style | "Email address style" | ListPreference | email_address_style | No | Enum: name/name_and_number/number | name |
| mail_subject_prefix | "Email subject prefix" | CheckBoxPreference | mail_subject_prefix | No | N/A | false |

#### AdvancedSettings.Backup.CallLog

| Field ID | Label (exact) | Type | Key | Required | Validation | Default |
|----------|--------------|------|-----|----------|------------|---------|
| imap_folder_calllog | "Call log label" | EditTextPreference | imap_folder_calllog | Yes | BackupImapStore.isValidImapFolder() | "Call_log" |
| backup_calllog_types | "Call types" | ListPreference | backup_calllog_types | No | Enum: everything/incoming/outgoing/etc. | everything |
| backup_calllog_sync_calendar_enabled | "Calendar sync" | CheckBoxPreference | backup_calllog_sync_calendar_enabled | No | Permission: WRITE_CALENDAR | false |
| backup_calllog_sync_calendar | "Calendar used for call logs" | ListPreference | backup_calllog_sync_calendar | No | Dynamic from CalendarAccessor; dependency: above enabled | -1 (none) |
| backup_calllog_after_call | "Backup after call" | CheckBoxPreference | backup_calllog_after_call | No | N/A | false |

#### AdvancedSettings.Restore

| Field ID | Label (exact) | Type | Key | Required | Validation | Default |
|----------|--------------|------|-----|----------|------------|---------|
| restore_sms | "Restore SMS" | CheckBoxPreference | restore_sms | No | N/A | true |
| restore_calllog | "Restore call log" | CheckBoxPreference | restore_calllog | No | N/A | true |
| restore_starred_only | "Starred items" | CheckBoxPreference | restore_starred_only | No | N/A | false |
| max_items_per_restore | "Items per restore" | ListPreference | max_items_per_restore | No | From array | 500 |
| mark_as_read_on_restore | "Mark as read (SMS)" | CheckBoxPreference | mark_as_read_on_restore | No | N/A | true |

#### AdvancedSettings.Server (IMAP settings)

| Field ID | Label (exact) | Type | Key | Required | Validation | Default |
|----------|--------------|------|-----|----------|------------|---------|
| server_address | "Server address" | EditTextPreference | server_address | Yes | Non-empty | imap.gmail.com:993 |
| login_user | "Username" | EditTextPreference | login_user | Yes | Non-empty; email input type | — |
| login_password | "Password" | EditTextPreference | login_password | Yes | Non-empty; password input type; persistent=false | — |
| server_protocol | "Security" | ListPreference | server_protocol | No | Enum: +ssl+, +tls+, "" | +ssl+ |
| server_trust_all_certificates | "Trust all certificates" | CheckBoxPreference | server_trust_all_certificates | No | N/A | false |

### 3.2 Display Fields

| Field ID | Label | Data Type | Source | Format | Update Trigger |
|----------|-------|-----------|--------|--------|----------------|
| connected summary | — | string | authPreferences.getOauth2Username() | "Connected as {username}." or "Connect your Gmail account (unsupported)." | onResume, AccountAddedEvent, AccountRemovedEvent, SettingsResetEvent |
| backup_sms summary | — | string | DataTypePreferences.getMaxSyncedDate(SMS) | "Last item backed up: {date}" or "never" | onResume |
| backup_mms summary | — | string | DataTypePreferences.getMaxSyncedDate(MMS) | "Last item backed up: {date}" | onResume |
| backup_calllog summary | — | string | DataTypePreferences.getMaxSyncedDate(CALLLOG) | "Last item backed up: {date}" | onResume |
| max_items_per_sync title | — | string | preferences.getMaxItemsPerSync() | numeric or "All" (for -1) | onResume, preference change |
| max_items_per_restore title | — | string | preferences.getMaxItemsPerRestore() | numeric or "All" | onResume, preference change |
| backup_contact_group title | — | string | Selected group entry | group name or "Contacts to backup" | onResume |
| backup_calllog_sync_calendar title | — | string | Selected calendar entry | calendar name or "None" | onResume |
| server screen summary (in MainSettings) | — | string | authPreferences.toString() or "Not configured" | "user@server" or "username (Gmail)" | onCreatePreferences |
| autoBackup summary (in MainSettings) | — | string | Enabled data types + schedule | "Automatically backup SMS, MMS." or "No item type to backup selected." | onResume, events |
| autoBackup schedule summary (in MainSettings) | — | string | Preferences schedule values | "Regular schedule: 2 h, Incoming schedule: 1 min (Require Wifi)" | onResume |

### 3.3 Validation Rules

| Field ID | Rule Type | Rule (exact) | Error Message (exact) | Trigger |
|----------|-----------|-------------|----------------------|---------|
| imap_folder | custom | BackupImapStore.isValidImapFolder(value) — no leading/trailing spaces | Dialog title: "Invalid label name" / message: "The label name may not contain spaces (at the beginning or end)." | onPreferenceChange; returns false to block save |
| imap_folder_calllog | custom | BackupImapStore.isValidImapFolder(value) | Same as above | onPreferenceChange; returns false to block save |
| backup_calllog | permission | READ_CALL_LOG must be granted to enable | System permission dialog (Android OS message) | onPreferenceChange; returns false if permission denied |
| backup_calllog_sync_calendar_enabled | permission | WRITE_CALENDAR must be granted to enable | System permission dialog | onPreferenceChange; returns false if permission denied |

### 3.4 State Controllers

```yaml
state_controllers:
  - field_id: connected (SwitchPreferenceCompat)
    trigger: onResume, AccountAddedEvent, AccountRemovedEvent, SettingsResetEvent
    trigger_source: Otto bus + lifecycle
    conditions:
      - when: authPreferences.hasOAuth2Tokens() == true
        effects:
          value: checked = true
      - when: authPreferences.hasOAuth2Tokens() == false
        effects:
          value: checked = false
    pseudocode: |
      FUNCTION updateConnected():
        connected.setChecked(authPreferences.hasOAuth2Tokens())

  - field_id: connected summary
    trigger: provideSummary() call (lazy)
    trigger_source: Preference rendering
    conditions:
      - when: preference.isEnabled() AND preference.isChecked() AND username not empty
        effects:
          value: "Connected as {username}."
      - when: preference.isEnabled() AND (NOT checked OR username empty)
        effects:
          value: "Connect your Gmail account (unsupported)."
      - when: NOT preference.isEnabled()
        effects:
          value: null
    pseudocode: |
      FUNCTION provideSummary(preference: TwoStatePreference): String
        username = authPreferences.getOauth2Username()
        IF preference.isEnabled():
          IF preference.isChecked() AND username is not empty:
            RETURN "Connected as {username}."
          ELSE:
            RETURN "Connect your Gmail account (unsupported)."
        RETURN null

  - field_id: calllog_settings_subscreen
    trigger: DataTypePreferences.DataTypeListener onChanged(CALLLOG, ...)
    trigger_source: DataTypePreferences listener registered in onResume
    conditions:
      - when: preferences.isBackupEnabled(CALLLOG) == true
        effects:
          enabled: true
      - when: preferences.isBackupEnabled(CALLLOG) == false
        effects:
          enabled: false
    pseudocode: |
      ON dataTypeChanged(CALLLOG, prefs):
        calllogSettingsScreen.setEnabled(prefs.isBackupEnabled(CALLLOG))

  - field_id: backup_contact_group
    trigger: onResume
    trigger_source: READ_CONTACTS permission check
    conditions:
      - when: READ_CONTACTS permission granted
        effects:
          enabled: true
          options: ContactAccessor.getGroups() result
      - when: READ_CONTACTS permission NOT granted
        effects:
          enabled: false
    pseudocode: |
      FUNCTION initGroups():
        IF checkSelfPermission(READ_CONTACTS) == GRANTED:
          groups = ContactAccessor().getGroups(contentResolver, resources)
          initListPreference(groupPref, groups, false)
        ELSE:
          groupPref.setEnabled(false)

  - field_id: backup_calllog_sync_calendar_enabled
    trigger: onResume (permission re-check) OR user toggles
    trigger_source: WRITE_CALENDAR permission
    conditions:
      - when: WRITE_CALENDAR not granted AND preference is checked
        effects:
          value: unchecked (auto-reverted on onResume)
      - when: user enables AND WRITE_CALENDAR not granted
        effects:
          value: permission requested; preference blocked (returns false)
      - when: user enables AND permission granted
        effects:
          value: checked = true
    pseudocode: |
      ON toggleCalendarSyncEnabled(newValue):
        IF newValue == TRUE AND WRITE_CALENDAR not granted:
          requestPermissions([WRITE_CALENDAR], REQUEST_CALENDAR_ACCESS)
          RETURN false  // block preference save
        RETURN true  // allow save

      ON onResume:
        IF needCalendarPermission() AND enabledPreference.isChecked():
          enabledPreference.setChecked(false)

  - field_id: backup_calllog_sync_calendar (ListPreference)
    trigger: XML dependency attribute
    trigger_source: AndroidX Preference library dependency mechanism
    conditions:
      - when: backup_calllog_sync_calendar_enabled is checked
        effects:
          enabled: true
      - when: backup_calllog_sync_calendar_enabled is unchecked
        effects:
          enabled: false
    pseudocode: |
      // Handled by AndroidX Preference library via android:dependency="backup_calllog_sync_calendar_enabled"

  - field_id: max_items_per_sync title (and max_items_per_restore)
    trigger: onResume OR onPreferenceChange
    trigger_source: Value change or initial bind
    conditions:
      - when: value == "-1"
        effects:
          title: "All"
      - when: value != "-1"
        effects:
          title: numeric string value
    pseudocode: |
      FUNCTION updateMaxItems(pref: Preference, currentValue: Int, newValue: String?):
        displayValue = newValue ?? String(currentValue)
        IF displayValue == "-1":
          pref.setTitle("All")
        ELSE:
          pref.setTitle(displayValue)

  - field_id: autoBackup enable/summary (MainSettings)
    trigger: onResume, AccountAddedEvent, AccountRemovedEvent, AutoBackupSettingsChangedEvent, SettingsResetEvent
    trigger_source: Otto bus + lifecycle
    conditions:
      - when: useXOAuth() AND NOT hasOAuth2Tokens()
        effects:
          enabled: false (autoBackup disabled — no valid XOAuth tokens)
      - when: NOT useXOAuth() OR hasOAuth2Tokens()
        effects:
          enabled: true
      - when: autoBackup.isEnabled() AND autoBackup.isChecked()
        effects:
          backup_settings_screen enabled: true
          backup_settings_screen summary: schedule summary string
      - when: NOT (autoBackup.isEnabled() AND autoBackup.isChecked())
        effects:
          backup_settings_screen enabled: false
    pseudocode: |
      FUNCTION updateAutoBackupPreferences():
        autoBackup.setSummary(summarizeAutoBackupSettings())
        autoBackup.setEnabled(NOT useXOAuth() OR hasOAuth2Tokens())
        autoBackupSettings.setSummary(summarizeBackupScheduleSettings(autoBackup.isChecked()))
        autoBackupSettings.setEnabled(autoBackup.isEnabled() AND autoBackup.isChecked())
```

---

## Section 4: Actions & Behaviors

### 4.1 User Actions (per sub-screen)

| Action ID | Element | Label (exact) | Behavior | Post-Action |
|-----------|---------|--------------|----------|-------------|
| ACT-ADV-001 | connected toggle (on) | "Connect" | Post AccountConnectionChangedEvent(true) | MainActivity opens AccountManagerAuthActivity |
| ACT-ADV-002 | connected toggle (off) | "Connect" | Post AccountConnectionChangedEvent(false) | MainActivity shows DISCONNECT dialog |
| ACT-ADV-003 | dark_theme toggle | "Dark theme" | Post ThemeChangedEvent | MainActivity calls recreate() |
| ACT-ADV-004 | backup_calllog toggle (enable) | "Backup Call log" | checkCallLogPermissions() | If denied: OS dialog; if granted: checked=true, AutoBackupSettingsChangedEvent |
| ACT-ADV-005 | backup_calllog_sync_calendar_enabled toggle | "Calendar sync" | If no WRITE_CALENDAR: request permission | If granted: checked=true |
| ACT-ADV-006 | imap_folder edit | — | checkValidImapFolder(newValue) | If invalid: INVALID_IMAP_FOLDER dialog; preference NOT saved |
| ACT-ADV-007 | imap_folder_calllog edit | — | checkValidImapFolder(newValue) | Same |
| ACT-ADV-008 | login_password edit | — | authPreferences.setImapPassword(newValue) | Password saved to "credentials" prefs |
| ACT-ADV-009 | max_items_per_sync change | — | updateMaxItemsPerSync(newValue) | Title updates to new value or "All" |
| ACT-ADV-010 | max_items_per_restore change | — | updateMaxItemsPerRestore(newValue) | Title updates |
| ACT-ADV-011 | Any preference change (SMS/MMS enable, wifi_only, etc.) | — | Post AutoBackupSettingsChangedEvent via handler.post | MainSettings.updateAutoBackupPreferences() |

### 4.2 System Actions

| Action ID | Trigger | Behavior | Frequency |
|-----------|---------|----------|-----------|
| SYS-ADV-001 | Main.onStart / MainSettings.onStart | App.register(this) | Each start |
| SYS-ADV-002 | Main.onStop / MainSettings.onDestroy | App.unregister(this) | Each stop |
| SYS-ADV-003 | Backup.onResume | updateLastBackupTimes(), updateBackupContactGroupLabelFromPref(), initGroups(), registerValidImapFolderCheck(), updateMaxItemsPerSync(null) | Each resume |
| SYS-ADV-004 | Backup.onStop | preferences.getDataTypePreferences().registerDataTypeListener(null) | Each stop |
| SYS-ADV-005 | CallLog.onResume | initCalendars(), updateCallLogCalendarLabelFromPref(), registerValidCallLogFolderCheck(), registerCalendarSyncEnabledCallback(), permission re-check | Each resume |
| SYS-ADV-006 | Restore.onResume | updateMaxItemsPerRestore(null) | Each resume |
| SYS-ADV-007 | MainSettings.onResume | checkUserDonationStatus(), updateAutoBackupPreferences() | Each resume |

---

## Section 5: State Management

### 5.1 Screen States

| State | Entry Condition | Visual | Exit Condition |
|-------|-----------------|--------|----------------|
| LOADED | onResume completes | All preferences populated with current values | User navigates away or changes preference |
| PERMISSION_REQUESTING | Permission request initiated | System permission dialog overlay | onRequestPermissionsResult() |
| DIALOG_SHOWN | INVALID_IMAP_FOLDER dialog triggered | Modal dialog overlay | User dismisses |

### 5.2 Local State Variables

| Variable | Type | Initial Value | Purpose | Persisted |
|----------|------|---------------|---------|-----------|
| authPreferences | AuthPreferences | Constructed in onCreatePreferences | Read OAuth2 tokens | No |
| connected (Main) | TwoStatePreference | Found in onCreatePreferences | Reference for updateConnected() | No |
| callLogPreference (Backup) | CheckBoxPreference | Found in onViewCreated | Reference for permission listener | No |
| enabledPreference (CallLog) | CheckBoxPreference | Found in onViewCreated | Calendar sync toggle reference | No |
| calendarPreference (CallLog) | ListPreference | Found in onViewCreated | Calendar selector | No |
| folderPreference (CallLog) | Preference | Found in onViewCreated | Call log IMAP folder | No |
| preferences (base) | Preferences | Constructed in onCreatePreferences | All shared preferences | No |
| handler (base) | Handler | Constructed in onCreatePreferences | Deferred event posting | No |

### 5.3 Global State Dependencies

| State Path | Read/Write | Purpose |
|------------|------------|---------|
| SharedPreferences (default) | Read/Write | All preference values via Preferences facade |
| SharedPreferences "credentials" | Read/Write | IMAP password via AuthPreferences |
| App.bus (Otto) | Read/Write | Subscribe to AccountAddedEvent, AccountRemovedEvent, SettingsResetEvent, AutoBackupSettingsChangedEvent, ThemeChangedEvent; post AccountConnectionChangedEvent, AutoBackupSettingsChangedEvent |
| Device calendar content provider | Read | CalendarAccessor.getCalendars() |
| Device contacts content provider | Read | ContactAccessor.getGroups() |

---

## Section 6: Business Logic

### 6.1 Business Rules

| Rule ID | Name | Description | Applies To |
|---------|------|-------------|------------|
| BR-ADV-001 | IMAP folder validation | Folder name may not have leading/trailing spaces | imap_folder, imap_folder_calllog |
| BR-ADV-002 | Call log permission gate | READ_CALL_LOG must be granted before enabling call log backup | backup_calllog toggle |
| BR-ADV-003 | Calendar permission gate | WRITE_CALENDAR must be granted before enabling calendar sync | backup_calllog_sync_calendar_enabled |
| BR-ADV-004 | Calendar permission revocation | If WRITE_CALENDAR was revoked while calendar sync was enabled, auto-uncheck on resume | CallLog.onResume |
| BR-ADV-005 | Max items display | Value of -1 displays as "All messages" | max_items_per_sync, max_items_per_restore |
| BR-ADV-006 | AutoBackup enabled state | autoBackup preference disabled if useXOAuth() AND NOT hasOAuth2Tokens() | MainSettings.updateAutoBackupPreferences |
| BR-ADV-007 | Schedule summary | Summary string for schedule screen shows regular and incoming intervals plus wifi-only if set | summarizeBackupScheduleSettings() |
| BR-ADV-008 | Donation status hides donate item | If user has donated or billing is unavailable, remove donate preference from screen | MainSettings.checkUserDonationStatus |

### 6.2 Calculation Logic

```
// Rule: updateMaxItems (BR-ADV-005)
FUNCTION updateMaxItems(preference: Preference, currentValue: Int, newValue: String?):
  displayValue: String = newValue ?? String(currentValue)
  IF displayValue == "-1":
    preference.setTitle("All")
  ELSE:
    preference.setTitle(displayValue)
END FUNCTION

// Rule: checkValidImapFolder (BR-ADV-001)
FUNCTION checkValidImapFolder(fragmentManager: FragmentManager, imapFolder: String): Boolean
  IF BackupImapStore.isValidImapFolder(imapFolder):
    RETURN true
  ELSE:
    showDialog(INVALID_IMAP_FOLDER, fragmentManager)
    RETURN false   // preference not saved
END FUNCTION

// Rule: summarizeAutoBackupSettings (BR-ADV-006)
FUNCTION summarizeAutoBackupSettings(): String
  enabled = dataTypePreferences.enabled().map { getString(it.resId) }
  IF enabled is empty:
    RETURN "No item type to backup selected."
  summary = "Automatically backup {join(enabled, ", ")}."
  IF App.isInstalledOnSDCard():
    summary += " Auto backup might not work reliably when app is installed on SD card."
  RETURN summary
END FUNCTION

// Rule: summarizeBackupScheduleSettings (BR-ADV-007)
FUNCTION summarizeBackupScheduleSettings(isEnabled: Boolean): String?
  IF NOT isEnabled: RETURN null
  regSchedule.setValue(String(preferences.getRegularTimeoutSecs()))
  incomingSchedule.setValue(String(preferences.getIncomingTimeoutSecs()))
  summary = "{regSchedule.title}: {regSchedule.entry}, {incomingSchedule.title}: {incomingSchedule.entry}"
  IF preferences.isWifiOnly():
    summary += " ({wifiOnlyPref.title})"
  RETURN summary
END FUNCTION

// Rule: checkCallLogPermissions (BR-ADV-002)
FUNCTION checkCallLogPermissions(): Boolean
  required = CALLLOG.checkPermissions(context)
  IF required is empty:
    RETURN true
  requestPermissions(required.toArray(), REQUEST_CALL_LOG_PERMISSIONS)
  RETURN false
END FUNCTION
```

### 6.3 Portability Assessment

| Logic Area | Portability | Notes |
|------------|-------------|-------|
| IMAP folder validation rule | High | Pure string trim/check; no platform dependency |
| Max items "-1" = All mapping | High | Pure value mapping |
| Schedule summary string building | High | String formatting with platform-provided values |
| Permission checks | Low | Android-specific; iOS equivalent is permission authorization status |
| Calendar/Contact provider queries | Low | Android ContentProvider; iOS uses EventKit/CNContactStore |
| Otto bus subscriptions | Low | Framework-specific |
| DataTypeListener registration pattern | Medium | Observer pattern; straightforward to translate |

---

## Section 7: Data Contracts

No external API calls. Data sources:

| Source | Access Method | Data |
|--------|---------------|------|
| Android Calendar Provider | CalendarAccessor.getCalendars() via ContentResolver | Map<Long, String> calendar IDs to names |
| Android Contacts Provider | ContactAccessor.getGroups() via ContentResolver | Map<Integer, Group> group IDs to names |
| SharedPreferences (default) | Preferences facade | All user settings |
| SharedPreferences "credentials" | AuthPreferences.getCredentials() | IMAP password, OAuth2 tokens |

---

## Section 8: Navigation

### 8.1 Entry Points

| From | Trigger | Fragment loaded |
|------|---------|----------------|
| MainSettings | User taps "Advanced settings" | AdvancedSettings.Main |
| MainSettings | User taps "IMAP settings" | AdvancedSettings.Server |
| MainSettings | User taps "Backup settings" | AdvancedSettings.Backup |
| MainSettings | User taps "Auto backup settings" | AutoBackupSettings |
| AdvancedSettings.Backup | User taps "Call log settings" | AdvancedSettings.Backup.CallLog |
| AdvancedSettings.Main | User taps "Restore settings" | AdvancedSettings.Restore |

### 8.2 Exit Points

All sub-screens exit via fragment back-stack pop (Android back or ActionBar up button).

### 8.3 Back Stack Behavior

| Scenario | Behavior |
|----------|----------|
| Any sub-screen | Back press pops fragment, ActionBar subtitle updated |

---

## Section 9: Conditional Display Logic

### 9.1 Visibility Rules

| Element ID | Condition | When True | When False |
|------------|-----------|-----------|------------|
| backup_contact_group | READ_CONTACTS granted | Enabled, populated with groups | Disabled (greyed out) |
| calllog_settings_subscreen | backup_calllog DataType enabled | Enabled | Disabled |
| backup_calllog_sync_calendar | backup_calllog_sync_calendar_enabled checked | Enabled (via XML dependency) | Disabled |
| app_log_debug | app_log checked | Enabled (via XML dependency) | Disabled |
| donate preference | BillingClient status == NOT_DONATED AND available | Visible | Removed from preference screen |
| autoBackup checkbox | NOT useXOAuth() OR hasOAuth2Tokens() | Enabled | Disabled |
| backup_settings_screen | autoBackup enabled AND checked | Enabled | Disabled |

### 9.2 Permission Matrix

Single-user local app; no RBAC. Android permissions:

| Feature | Permission Required |
|---------|-------------------|
| Call log backup | READ_CALL_LOG |
| Call log backup | PROCESS_OUTGOING_CALLS (checked in DataType.CALLLOG.checkPermissions) |
| Contact group filter | READ_CONTACTS |
| Calendar sync | WRITE_CALENDAR |

---

## Section 10: Error Handling

### 10.1 Error States

| Error Type | Trigger | User Message (exact) | Recovery |
|------------|---------|----------------------|----------|
| Invalid IMAP folder | Non-empty folder with leading/trailing spaces | Dialog title: "Invalid label name" / message: "The label name may not contain spaces (at the beginning or end)." | User corrects folder name |
| Call log permission denied | User enables backup_calllog, denies READ_CALL_LOG | System permission denial (OS message) | Grant in settings |
| Calendar permission denied | User enables calendar sync, denies WRITE_CALENDAR | System permission denial | Grant in settings |

### 10.2 Error Display Patterns

| Error Type | Display | Style | Duration |
|------------|---------|-------|----------|
| Invalid IMAP folder | AlertDialog (INVALID_IMAP_FOLDER) | Modal | Until dismissed |
| Permission denied | System dialog → preference not toggled | N/A | N/A |

---

## Section 11: Loading States

No loading states in these screens — all data is loaded synchronously from ContentProvider and SharedPreferences on `onResume`.

**Exception:** Calendar and contact group queries run on the main thread in `onResume` via `ContentResolver`. These are not wrapped in async loaders, so slow devices may show momentary UI freeze. This is a known technical debt.

---

## Section 12: Accessibility Requirements

**Finding:** No explicit accessibility attributes in AdvancedSettings.java or preferences.xml beyond what the AndroidX Preference library provides by default.

| Requirement | Implementation |
|-------------|----------------|
| Preference items | AndroidX Preference library provides accessible item labels from android:title |
| Toggle states | Checked/unchecked state announced by accessibility services via TwoStatePreference |
| Dependency-disabled items | Greyed-out items should have "not available" announced; AndroidX handles this for android:dependency |

---

## Section 13: Analytics Events

N/A — no analytics in source.

---

## Section 14: Test Scenarios

### 14.1 Happy Path Tests

```gherkin
Scenario: IMAP folder saved with valid name
  Given the user is in Backup settings
  When the user enters "SMS-Archive" in the IMAP folder field
  Then the preference is saved
  And no error dialog is shown

Scenario: Call log backup enabled with permissions
  Given READ_CALL_LOG is not granted
  When the user enables "Backup Call log"
  Then the system permission dialog appears for READ_CALL_LOG
  When the user grants the permission
  Then the call log checkbox becomes checked

Scenario: Calendar sync auto-disabled after permission revoked
  Given "Calendar sync" was enabled
  And the user revokes WRITE_CALENDAR in system settings
  When the user returns to the Call log settings screen
  Then "Calendar sync" is automatically unchecked
```

### 14.2 Validation Tests

```gherkin
Scenario: IMAP folder with leading space rejected
  Given the user enters " SMS" (space then SMS) in the IMAP folder field
  Then the INVALID_IMAP_FOLDER dialog is shown
  And title is "Invalid label name"
  And message is "The label name may not contain spaces (at the beginning or end)."
  And the preference is NOT saved

Scenario: Max items displays "All" for -1 value
  Given max_items_per_sync is -1
  When onResume fires
  Then the "Items per backup" preference title shows "All"
```

### 14.3 Edge Cases

| Case | Setup | Action | Expected Result |
|------|-------|--------|-----------------|
| READ_CONTACTS denied mid-session | Permission was granted; user revokes via settings; returns to app | onResume in Backup screen | backup_contact_group preference disabled |
| Empty contacts group list | No groups exist in device contacts | onResume with READ_CONTACTS granted | Preference enabled but list shows only default "Everybody" entry |
| No device calendars | Device has no calendars added | onResume with WRITE_CALENDAR granted | Calendar ListPreference populated empty or with default -1 entry |
| IMAP folder all spaces | User enters "   " | onPreferenceChange | Treated as invalid (trim produces empty); INVALID_IMAP_FOLDER dialog |
| Network cut mid onResume | N/A — no network calls in onResume | — | All data is local; no network dependency |
| AutoBackupSettingsChangedEvent before view ready | Event fired before onCreatePreferences completes | Event received | findPreference() would return null; NullPointerException risk; handler.post defers until view ready |
| Setting reset (SettingsResetEvent) | User resets settings | SettingsResetEvent received in Main | updateConnected() refreshes toggle; preferences.reset() clears sms_default_package state |

---

## Section 15: Source References

| File | Role |
|------|------|
| `app/src/main/java/com/zegoggles/smssync/activity/fragments/AdvancedSettings.java` | All five sub-screen fragments |
| `app/src/main/java/com/zegoggles/smssync/activity/fragments/SMSBackupPreferenceFragment.java` | Base class |
| `app/src/main/java/com/zegoggles/smssync/activity/fragments/MainSettings.java` | Root fragment (donation check, auto-backup summary) |
| `app/src/main/java/com/zegoggles/smssync/preferences/Preferences.java` | Preferences facade |
| `app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java` | Auth preferences |
| `app/src/main/java/com/zegoggles/smssync/preferences/DataTypePreferences.java` | Per-type enable/max-synced-date |
| `app/src/main/java/com/zegoggles/smssync/mail/DataType.java` | Type-object enum with preference keys |
| `app/src/main/java/com/zegoggles/smssync/mail/BackupImapStore.java` | isValidImapFolder() validation logic |
| `app/src/main/java/com/zegoggles/smssync/calendar/CalendarAccessor.java` | Device calendar enumeration |
| `app/src/main/java/com/zegoggles/smssync/contacts/ContactAccessor.java` | Device contact group enumeration |
| `app/src/main/res/xml/preferences.xml` | Complete preference hierarchy |
| `app/src/main/res/values/strings.xml` | All string resources |

---

## Section 16: Feature Flags & Configuration

| Flag/Config | Source | Default | Effect |
|-------------|--------|---------|--------|
| server_trust_all_certificates | SharedPreferences | false | Enables trust-all TLS socket factory (SEC-001 risk) |
| use_old_scheduler | SharedPreferences | false | Forces AlarmManager scheduler over Firebase JobDispatcher |
| third_party_integration | SharedPreferences | false | Allows external apps to trigger backup via broadcast |
| app_log / app_log_debug | SharedPreferences | false | Enables rotating file log; debug adds verbose entries |

---

## Section 17: Modals, Drawers & Overlays

| Modal ID | Type | Trigger | Dismissal | Content |
|----------|------|---------|-----------|---------|
| INVALID_IMAP_FOLDER | dialog | Bad IMAP folder name entered | OK | "Invalid label name" / "The label name may not contain spaces (at the beginning or end)." |
| OS permission dialog (call log) | System | backup_calllog enabled without READ_CALL_LOG | Grant/Deny | Android system dialog |
| OS permission dialog (calendar) | System | calendar sync enabled without WRITE_CALENDAR | Grant/Deny | Android system dialog |

---

## Section 18: Local Storage & Caching

All settings are stored in the default SharedPreferences file. See field inventory for individual keys. The "credentials" SharedPreferences file stores login_password separately (not in the default prefs backup).

---

## Section 19: State Machine Diagrams

These screens have no complex state machine; each sub-screen is stateless (loads on resume, saves on preference change). The only state flow is the permission request loop:

```
[Preference disabled / unchecked]
  --> [User enables]
  --> [Permission check]
  --> IF granted: [Preference checked/enabled]
  --> IF denied:  [Preference reverted to unchecked/false]
       --> [Permission problem displayed in StatusPreference]
```

---

## Section 20: Implementation Notes

### 20.1 Known Complexity Areas
- **Inline business logic:** `AdvancedSettings.Backup` has `onPreferenceChangeListener` lambdas containing real business rules (permission checks, IMAP validation). These belong in a ViewModel.
- **Synchronous ContentProvider queries on main thread:** `initGroups()` and `initCalendars()` call ContentResolver on `onResume` without coroutines or AsyncTaskLoader. On slow devices with many calendar/contact entries this blocks the UI thread.
- **DataTypeListener pattern:** `DataTypePreferences.registerDataTypeListener()` uses a single-slot listener (not a list), cleared in `onStop`. This means only one listener can be registered at a time — adequate for the current single-fragment use but fragile.

### 20.2 Recommended Approach (Refactor-in-place)
- Move IMAP validation, permission check, and max-items display logic into a `SettingsViewModel`. Fragments observe via LiveData / StateFlow.
- Replace synchronous ContentProvider queries with `viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO)` with `withContext(Dispatchers.Main)` for UI update.

### 20.3 Dependencies
- `MainSettings` is the root entry point into all AdvancedSettings sub-screens
- `Dialogs.INVALID_IMAP_FOLDER` used for folder validation errors
- `BackupImapStore.isValidImapFolder()` — the validation predicate
- `DataTypePreferences` — per-type backup enable/date tracking
- `CalendarAccessor`, `ContactAccessor` — device data queries

### 20.4 Technical Debt
- `AdvancedSettings.Backup.initGroups()` calls `new ContactAccessor()` directly; should be injected
- `CalendarAccessor.Get.instance()` is a factory on `ContentResolver`; should be injected
- `AuthPreferences` constructed with `new AuthPreferences(getContext())` in multiple places; should be injected
- ContentProvider queries block main thread

### 20.5 Migration Considerations
- 25 locale translations exist; any label changes must propagate to all locales
- The android:dependency attribute (XML) provides automatic enable/disable for dependent preferences; any rebuild must replicate this behavior
- `backup_calllog_types` array entries/values are defined in `res/values/arrays.xml` — must be ported

---

## Screen Metrics

| Metric | Count |
|--------|-------|
| Input Fields | 24 (across all 5 sub-screens) |
| Display Fields | 12 (computed summaries) |
| Actions (User) | 11 |
| Actions (System) | 7 |
| Screen States | 3 |
| State Controllers | 7 |
| Validation Rules | 4 |
| API Endpoints | 0 |
| Business Rules | 8 |
| Visibility Rules | 7 |
| Error Types | 3 |
| Loading States | 0 (synchronous) |
| Analytics Events | 0 |
| Test Scenarios | 9 |
| Modals/Drawers | 3 |

---

<!-- SELF-CHECK
Date: 2026-05-29
Checklist: 59/66
Gaps Found:
- Loading states: synchronous ContentProvider queries; documented as technical debt
- Analytics: none; documented N/A
- arrays.xml not fully read (schedule entry values not listed); documented as resource dependency
Result: READY FOR AUDIT
-->
