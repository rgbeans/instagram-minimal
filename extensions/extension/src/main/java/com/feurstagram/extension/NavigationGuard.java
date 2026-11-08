package com.feurstagram.extension;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/** Sanitizes incoming intents before Instagram's URL handlers consume them. */
public final class NavigationGuard {
    private NavigationGuard() {}
    private static final String SAFE = "instagram://direct-inbox";
    private static final String[] URL_KEYS = {"original_url", "url", "uri", "extra_url", "deep_link", "deeplink", "intent_data"};

    public static Intent filterIntent(Intent intent) {
        if (intent == null) return null;
        try {
            boolean blocked = intent.getData() != null && MinimalPolicy.blocksLink(intent.getDataString());
            // Sanitize known media data before touching foreign, possibly unreadable extras.
            if (blocked) intent.setData(Uri.parse(SAFE));
            Bundle extras = intent.getExtras();
            if (extras != null) for (String key : URL_KEYS) {
                Object value = extras.get(key);
                if (value != null && MinimalPolicy.blocksLink(value.toString())) {
                    blocked = true;
                    intent.setData(Uri.parse(SAFE));
                    intent.putExtra(key, SAFE);
                }
            }
            if (blocked) {
                intent.setData(Uri.parse(SAFE));
                intent.removeExtra("media_id");
                intent.removeExtra("reel_id");
                intent.removeExtra("story_id");
            }
        } catch (RuntimeException ignored) { /* foreign unparcelable extras */ }
        return intent;
    }
}
