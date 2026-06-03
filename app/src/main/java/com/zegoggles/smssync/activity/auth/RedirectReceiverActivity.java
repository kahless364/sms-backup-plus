package com.zegoggles.smssync.activity.auth;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.zegoggles.smssync.App;
import com.zegoggles.smssync.auth.OAuth2Client;
import com.zegoggles.smssync.service.state.SyncEvent;

import static com.zegoggles.smssync.App.TAG;

// U-020: App.post(new BrowserAuthResult(code, error)) replaced by
// repository.tryEmitEvent(new SyncEvent.BrowserAuthResult(code, error)).
// The inner BrowserAuthResult class is deleted — SyncEvent.BrowserAuthResult is used instead.
public class RedirectReceiverActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: " +savedInstanceState);
        super.onCreate(savedInstanceState);
        handleRedirectIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleRedirectIntent(intent);
    }

    private void handleRedirectIntent(Intent intent) {
        if (OAuth2Client.REDIRECT_URL.getScheme().equals(intent.getScheme())) {
            final String code = intent.getData().getQueryParameter("code");
            final String error = intent.getData().getQueryParameter("error");

            // U-020: App.post(new BrowserAuthResult(code, error)) replaced (AC-17)
            if (App.syncStateRepository() != null) {
                boolean emitted = App.syncStateRepository().tryEmitEvent(
                    new SyncEvent.BrowserAuthResult(code, error));
                if (!emitted) {
                    Log.w(TAG, "RedirectReceiverActivity: tryEmitEvent(BrowserAuthResult) returned false");
                }
            }
        }
        finish();
    }
}
