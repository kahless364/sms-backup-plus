package com.zegoggles.smssync.compat;

import android.content.Intent;
import android.os.Build;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
public class SmsReceiverTest {
    private SmsReceiver subject;

    @Before
    public void setUp() throws Exception {
        subject = new SmsReceiver();
    }

    // U-068: minSdk raised to 24. isSmsBackupDefaultSmsApp keeps two branches: the role check from
    // Q (API 29) and the default-SMS-package check below Q (API 24 to 28, BUG-009). The tests run
    // each branch: N (API 24, the floor) for the package check and Q for the role check.

    @Test @Config(sdk = Build.VERSION_CODES.N)
    public void testOnReceive_belowQ() {
        subject.onReceive(RuntimeEnvironment.application, new Intent());
    }

    @Test @Config(sdk = Build.VERSION_CODES.Q)
    public void testOnReceive_q() {
        subject.onReceive(RuntimeEnvironment.application, new Intent());
    }

    @Test @Config(sdk = Build.VERSION_CODES.N)
    public void testIsSmsBackupDefaultSmsApp_belowQ_usesDefaultSmsPackage() {
        assertThat(SmsReceiver.isSmsBackupDefaultSmsApp(RuntimeEnvironment.application)).isFalse();
    }

    @Test @Config(sdk = Build.VERSION_CODES.Q)
    public void testIsSmsBackupDefaultSmsApp_q_usesRoleManager() {
        assertThat(SmsReceiver.isSmsBackupDefaultSmsApp(RuntimeEnvironment.application)).isFalse();
    }
}
