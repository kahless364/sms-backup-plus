package com.zegoggles.smssync.activity.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
// U-020: import com.squareup.otto.Subscribe removed
import com.zegoggles.smssync.App;
import com.zegoggles.smssync.activity.ThemeActivity;
import com.zegoggles.smssync.service.state.SyncEvent;

// U-020: @Subscribe onBrowserAuthResult replaced by lifecycle-aware
// SyncEvent.BrowserAuthResult collection from repository.events.
// App.register/unregister removed.
public class OAuth2WebAuthActivity extends ThemeActivity {
    public static final String EXTRA_CODE = "code";
    private static final String EXTRA_ERROR = "error";

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        final Uri urlToLoad = getIntent().getData();
        // U-020: App.register(this) removed

        startActivity(new Intent(Intent.ACTION_VIEW, urlToLoad));

        // U-020: start collecting SyncEvent.BrowserAuthResult in lifecycle-aware scope
        if (App.syncStateRepository() != null) {
            OAuth2WebAuthFlowHelper.collectBrowserAuthResult(this, App.syncStateRepository(), this);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // user navigated back, cancel auth flow
        setResult(RESULT_CANCELED);
        finish();
    }

    // U-020: replaces @Subscribe onBrowserAuthResult(RedirectReceiverActivity.BrowserAuthResult event)
    void onBrowserAuthResult(SyncEvent.BrowserAuthResult event) {
        if (!TextUtils.isEmpty(event.getCode())) {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_CODE, event.getCode()));
        } else {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_ERROR, event.getError()));
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // U-020: App.unregister(this) removed — lifecycle handles cleanup
    }
}
