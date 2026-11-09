package com.feurstagram.extension;

import android.view.View;
import android.view.ViewGroup;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

/** Newer shared-post renderers are XMA cards, not direct-photo attachments. */
public final class SharedMediaGuard {
    private SharedMediaGuard() {}
    private static final Set<String> CARDS = new HashSet<>(Arrays.asList(
            "message_content_generic_xma_container", "message_content_portrait_xma_container",
            "standard_dxma_message_container", "layered_xma_item_view", "xma_bubble_container"));
    private static final Set<String> POST_MEDIA = new HashSet<>(Arrays.asList(
            "message_content_media_share_bubble_container", "reel_share_item_view",
            "direct_private_share_media_container_view", "xma_content_repost_note_avatar_view"));
    private static final WeakHashMap<View, Integer> HIDDEN = new WeakHashMap<>();

    public static boolean scrub(View view, String id) {
        if (!CARDS.contains(id)) return false;
        boolean blocked = contains(view, POST_MEDIA);
        if (blocked) {
            HIDDEN.putIfAbsent(view, view.getVisibility());
            // GONE removes drawing, touches and accessibility without corrupting a recycled row.
            view.setVisibility(View.GONE);
        } else {
            Integer previous = HIDDEN.remove(view);
            if (previous != null && view.getVisibility() == View.GONE) view.setVisibility(previous);
        }
        return blocked;
    }

    private static boolean contains(View view, Set<String> names) {
        if (names.contains(resourceName(view))) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) if (contains(group.getChildAt(i), names)) return true;
        }
        return false;
    }

    private static String resourceName(View view) {
        if (view.getId() == View.NO_ID) return "";
        try { return view.getResources().getResourceEntryName(view.getId()); }
        catch (RuntimeException ignored) { return ""; }
    }
}
