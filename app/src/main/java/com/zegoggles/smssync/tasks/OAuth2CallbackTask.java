package com.zegoggles.smssync.tasks;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import com.zegoggles.smssync.App;
import com.zegoggles.smssync.auth.OAuth2Client;
import com.zegoggles.smssync.auth.OAuth2Token;
import com.zegoggles.smssync.service.state.SyncEvent;

import java.io.IOException;
import java.util.Arrays;

import static com.zegoggles.smssync.App.TAG;

// U-020: App.post(new OAuth2CallbackEvent(token)) replaced by
// repository.tryEmitEvent(new SyncEvent.OAuth2Callback(event)) (AC-17).
// OAuth2CallbackEvent class is retained for backward-compat since SyncEvent.OAuth2Callback
// wraps it per CNTR-MODERNIZATION-006.
@SuppressWarnings("deprecation")
public class OAuth2CallbackTask extends AsyncTask<String, Void, OAuth2Token> {

    private final OAuth2Client oauth2Client;

    public OAuth2CallbackTask(OAuth2Client oauth2Client) {
        this.oauth2Client = oauth2Client;
    }

    @Override
    protected OAuth2Token doInBackground(String... code) {
        if (code == null || code.length == 0 || TextUtils.isEmpty(code[0])) {
            Log.w(TAG, "invalid input: "+ Arrays.toString(code));
            return null;
        }
        try {
            return oauth2Client.getToken(code[0]);
        } catch (IOException e) {
            Log.w(TAG, e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(OAuth2Token token) {
        // U-020: App.post(new OAuth2CallbackEvent(token)) replaced by tryEmitEvent
        OAuth2CallbackEvent event = new OAuth2CallbackEvent(token);
        if (App.syncStateRepository() != null) {
            boolean emitted = App.syncStateRepository().tryEmitEvent(
                new SyncEvent.OAuth2Callback(event));
            if (!emitted) {
                Log.w(TAG, "OAuth2CallbackTask: tryEmitEvent returned false");
            }
        }
    }

    public static class OAuth2CallbackEvent {
        public final OAuth2Token token;

        OAuth2CallbackEvent(OAuth2Token token) {
            this.token = token;
        }

        public boolean valid() {
            return token != null;
        }
    }
}
