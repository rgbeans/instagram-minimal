package com.feurstagram.extension;

import java.net.URI;
import java.net.URLDecoder;
import java.util.Locale;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Pure policy, shared by network/navigation hooks and host regression tests. */
public final class MinimalPolicy {
    private MinimalPolicy() {}
    private static final Set<String> SHARED_MEDIA = new HashSet<>(Arrays.asList(
            "MEDIA_SHARE", "XMA_MEDIA_SHARE", "MEDIA_MENTION", "REEL_SHARE", "XMA_REEL_SHARE",
            "XMA_REEL_MENTION", "STORY_SHARE", "XMA_STORY_SHARE", "XMA_STORY_MENTION",
            "CLIPS_SHARE", "XMA_CLIPS_SHARE", "CLIPS_MENTION", "CLIPS_PREVIEW_SHARE",
            "FELIX_SHARE", "XMA_FELIX_SHARE", "REPOST_SHARE", "REPOST_REPLY", "COMMENT_RESHARE"));

    public static boolean blocksSharedMessage(String type) { return SHARED_MEDIA.contains(type); }

    public static boolean blocksRequest(URI uri) {
        if (uri == null) return false;
        String host = lower(uri.getHost());
        String path = lower(uri.normalize().getPath());
        // Do not block upload, push registration, presence, read receipts, login,
        // account security, user info, or friendships merely because they log events.
        if (host.equals("graph.facebook.com") && (path.equals("/activities") || path.endsWith("/activities"))) return true;
        if (host.equals("graph.instagram.com") && path.startsWith("/logging")) return true;
        if (path.contains("/logging/") || path.contains("/analytics/")
                || path.contains("/falco/") || path.contains("/client_events/")
                || path.contains("/qp/batch_fetch/") || path.contains("/ads/")) return true;
        if (!isInstagramApiHost(host)) return false;
        return path.startsWith("/api/v1/feed/")
                || path.startsWith("/api/v1/clips/")
                || path.startsWith("/api/v1/stories/")
                || path.startsWith("/api/v1/highlights/")
                || path.startsWith("/api/v1/archive/reel/")
                || path.startsWith("/api/v1/usertags/")
                || path.startsWith("/api/v1/tags/")
                || path.startsWith("/api/v1/locations/")
                || path.startsWith("/api/v1/discover/topical_explore")
                || path.startsWith("/api/v1/discover/explore")
                || path.startsWith("/api/v1/discover/stream/")
                || path.startsWith("/api/v1/fbsearch/serp/")
                || path.startsWith("/api/v1/fbsearch/media_search/")
                || path.startsWith("/api/v1/fbsearch/reels_search/")
                || path.matches("/api/v1/media/[0-9_]+/(info|comments|likers|permalink|viewable|carousel_media)/?.*")
                || path.startsWith("/api/v1/commerce/")
                || path.startsWith("/api/v1/shopping/")
                || path.startsWith("/api/v1/stats/");
    }

    private static boolean isInstagramApiHost(String host) {
        return host.equals("i.instagram.com") || host.equals("b.i.instagram.com")
                || host.equals("www.instagram.com") || host.equals("instagram.com");
    }

    /** Restrict Instagram media URLs, including wrapped and percent-encoded links. */
    public static boolean blocksLink(String text) { return blocksLink(text, 0); }

    /** Web previews must not provide a second route to shared posts. */
    public static String filterWebUrl(String text) {
        return blocksLink(text) ? "about:blank" : text;
    }

    private static boolean blocksLink(String text, int depth) {
        if (text == null || depth > 4) return false;
        try {
            URI uri = new URI(text.trim());
            String scheme = lower(uri.getScheme());
            String host = lower(uri.getHost());
            String path = lower(uri.normalize().getPath());
            if ("instagram".equals(scheme)) {
                if (host.matches("media|p|reel|reels|clips|story|stories|tv|explore|camera|create|saved|collection|collections")) return true;
                // Notification/native routing sometimes puts the action in the path.
                if (path.matches("/?(media|reels?|clips|stories|story|tv|explore|create)(/.*)?")) return true;
            }
            if (host.equals("instagram.com") || host.endsWith(".instagram.com") || host.equals("instagr.am") || host.endsWith(".instagr.am")) {
                if (path.matches("/(p|reels?|stories|tv|explore|share|s|reel|clips|saved|collections?)(/.*)?")) return true;
                // instagram.com/<account>/reels, tagged, and other profile media tabs.
                if (path.matches("/[^/]+/(reels|tagged|reposts|channel)(/.*)?")) return true;
                String query = uri.getRawQuery();
                if (query != null) for (String pair : query.split("&")) {
                    int eq = pair.indexOf('=');
                    if (eq < 0) continue;
                    String value = URLDecoder.decode(pair.substring(eq + 1), "UTF-8");
                    if (blocksLink(value, depth + 1)) return true;
                }
            }
        } catch (Exception ignored) { /* malformed input is handled by Instagram */ }
        return false;
    }

    public static boolean blocksModule(String name) {
        String module = lower(name);
        return module.equals("feed_timeline") || module.startsWith("clips_viewer")
                || module.startsWith("reel_feed") || module.equals("reel_viewer")
                || module.startsWith("feed_contextual") || module.equals("feed_contextual_self_profile")
                || module.equals("feed_contextual_profile") || module.equals("feed_photos_of_you")
                || module.equals("feed_hashtag") || module.equals("feed_location")
                || module.equals("archive_feed") || module.equals("shorturlfeedfragment")
                || module.equals("short_url_reel_loading_fragment")
                || module.equals("profile_media_grid") || module.equals("profile_clips")
                || module.equals("profile_reposts") || module.equals("profile_tagged_media_photos_of_you");
    }

    private static String lower(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT); }
}
