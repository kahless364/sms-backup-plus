"""U-069: check that every text this story's edits and mutations replace occurs exactly once.
usage (from the repository root): python -I -X utf8 oldtext_check.py start|fixed
start: before any edit (the worker as at <start>, the scheduler as U-068 leaves it, app/build.gradle, the
       scheduler test). fixed: after W1-W3 and T-F (the worker's mutation texts, the test factory line).
Files are read with universal newlines, so CRLF and LF working trees give the same result. Exit 1 if any count
is not 1."""
import sys
WRK = 'app/src/main/java/com/zegoggles/smssync/worker/BackupTriggerWorker.kt'
SCH = 'app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt'
BLD = 'app/build.gradle'
SXT = 'app/src/test/java/com/zegoggles/smssync/service/WorkManagerSchedulerTest.kt'
TST = 'app/src/test/java/com/zegoggles/smssync/worker/BackupTriggerWorkerTest.kt'
START = [
    ('W1', WRK, 'import androidx.work.OneTimeWorkRequest\nimport androidx.work.WorkManager\nimport androidx.work.WorkerParameters\n'),
    ('W2', WRK, 'class BackupTriggerWorker(\n    context: Context,\n    params: WorkerParameters\n) : Worker(context, params) {\n'),
    ('W2-doc', WRK, ' * U-015 dependency: once BackupWorker is a CoroutineWorker with full execution logic,\n'),
    ('W3', WRK, '        val prefs = Preferences(applicationContext)\n        val incomingTimeoutSecs = prefs.incomingTimeoutSecs.toLong()\n'),
    ('LOG', WRK, '        Log.d(TAG, "BackupTriggerWorker.doWork: content-URI trigger fired; " +\n'
                 '                "enqueueing delayed incoming backup with initialDelay=${incomingTimeoutSecs}s")\n'),
    ('M-T2', SCH, '        if (secretStore.isEncryptionDegraded()) {\n'
                  '            Log.w(TAG, "WorkManagerScheduler.scheduleIncoming: blocked — encryption degraded, credentials in plaintext")\n'),
    ('M-T3/M-T4', SCH, '        if (!preferences.isAutoBackupEnabled || timeoutSecs <= 0) {\n'),
    ('M-T5/M-T12', SCH, '            .setInitialDelay(timeoutSecs.toLong(), TimeUnit.SECONDS)\n'),
    ('M-T6', SCH, '            .enqueueUniqueWork(BackupType.INCOMING.name, ExistingWorkPolicy.REPLACE, request)\n'),
    ('AC13b-1', SCH, ' *   [BackupTriggerWorker] enqueues a delayed [BackupWorker] rather than backing up directly.\n'),
    ('AC13b-2', SCH, '     * INV-4: two-stage debounce — the triggered [BackupTriggerWorker] enqueues a delayed\n'
                     '     * incoming backup rather than backing up directly.\n'),
    ('NO-PRE24', SCH, 'override fun scheduleContentTrigger(): ScheduledJob? {\n'),
    ('G1', BLD, '        // BackupTriggerWorker has no unit tests; floor stays at 0%.\n'),
    ('G2', BLD, '                minimum = 0.00 // MEASURED 0% — BackupTriggerWorker untested; TODO raise after dedicated tests\n'),
    ('AC12c', SXT, '     * The content-URI trigger worker would ultimately call scheduleIncoming(), but the gate\n'),
    ('T-se002-incoming', SXT, '    fun se002_scheduleIncoming_encryptionDegraded_returnsNullNoWork() {\n'),
    ('T-autoBackupOff', SXT, '    fun ac5_scheduleIncoming_autoBackupDisabled_returnsNull() {\n'),
    ('T-zeroTimeout', SXT, '    fun ac5_scheduleIncoming_zeroTimeout_returnsNull() {\n'),
    ('T-atMinSdk24', SXT, '    fun inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger() {\n'),
]
ABSENT_AT_START = [
    ('pre24-branch', SCH, 'Build.VERSION.SDK_INT < Build.VERSION_CODES.N'),
    ('pre24-test', SXT, 'fun inv4_contentTrigger_pre24_returnsNull_fallbackToBroadcast'),
]
FIXED = [
    ('M-T1/M-T7', WRK, '        scheduler.scheduleIncoming()\n'),
    ('M-T8', WRK, '        scheduler.scheduleIncoming()\n\n        return Result.success()\n'),
    ('M-T9', WRK, '"enqueueing delayed incoming backup with initialDelay=${incomingTimeoutSecs}s")'),
    ('M-T10', WRK, '@HiltWorker\nclass BackupTriggerWorker @AssistedInject constructor(\n'),
    ('M-T11', WRK, 'class BackupTriggerWorker @AssistedInject constructor(\n'),
    ('T-F', TST, '                BackupTriggerWorker(appContext, workerParameters, scheduler, preferences)\n'),
]
mode = sys.argv[1]
checks = START if mode == 'start' else FIXED
bad = 0
for cid, path, text in checks:
    n = open(path, encoding='utf-8').read().count(text)
    bad += n != 1
    print(f'{cid} {path} occurrences={n}')
if mode == 'start':
    for cid, path, text in ABSENT_AT_START:
        n = open(path, encoding='utf-8').read().count(text)
        bad += n != 0
        print(f'{cid} {path} occurrences={n} (expected 0)')
print(f'RESULT mode={mode} mismatches={bad}')
sys.exit(1 if bad else 0)
