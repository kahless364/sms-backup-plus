package com.fsck.k9.mail.store.imap;

import android.text.TextUtils;
import com.fsck.k9.mail.AuthenticationFailedException;
import org.json.JSONObject;

public class XOAuth2AuthenticationFailedException extends AuthenticationFailedException {
    private JSONObject error;

    public XOAuth2AuthenticationFailedException(String message, JSONObject error) {
        super(message);
        this.error = error;
    }

    public int getStatus() {
        if (error != null) {
            String status = error.optString("status", null);
            if (!TextUtils.isEmpty(status)) {
                try {
                    return Integer.parseInt(status);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return -1;
    }

    public String getScope() {
        if (error != null) {
            return error.optString("scope", null);
        } else {
            return null;
        }
    }
}
