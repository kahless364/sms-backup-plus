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

    // U-005 / U-001: minSdk raised to 21; SDK 16 (JELLY_BEAN) and SDK 19 (KITKAT) are below
    // the new minSdk floor and are no longer reachable. Tests updated to use SDK 21 (LOLLIPOP)
    // which is the minimum supported API level after U-001.

    @Test @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
    public void testOnReceivePreKitKat() {
        subject.onReceive(RuntimeEnvironment.application, new Intent());
    }

    @Test @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
    public void testOnReceiveKitKat() {
        subject.onReceive(RuntimeEnvironment.application, new Intent());
    }

    @Test @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
    public void testIsSmsBackupDefaultSmsAppPreKitKat() {
        assertThat(SmsReceiver.isSmsBackupDefaultSmsApp(RuntimeEnvironment.application)).isFalse();
    }

    @Test @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
    public void testIsSmsBackupDefaultSmsAppKitKat() {
        assertThat(SmsReceiver.isSmsBackupDefaultSmsApp(RuntimeEnvironment.application)).isFalse();
    }
}
