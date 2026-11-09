package com.feurstagram.extension;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ListView;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

/** Runs before drawing, including cached content and viewers reached inside DMs. */
public final class ContentGuard {
    private ContentGuard() {}
    private static final WeakHashMap<View, Boolean> INSTALLED = new WeakHashMap<>();
    private static final Set<String> HIDDEN_IDS = new HashSet<>(Arrays.asList(
            "feed_tab", "clips_tab", "creation_tab", "profile_tabs_container",
            "profile_tabs_top_divider", "profile_tabs_bottom_divider",
            "highlights_tray", "highlights_reel_tray_recycler_view", "reel_tray", "reels_tray", "stories_tray",
            "reels_tray_container", "overlay_stories_tray_container", "clips_stories_tray_container",
            "mid_feed_stories_tray_container_stub", "reel_ring", "top_serp_story_ring",
            "clips_viewer_container", "clips_viewer_fragment_container", "clips_viewer_view_pager",
            "reel_viewer_container", "reel_viewer_root",
            "message_content_media_share_bubble_container", "reel_share_item_view",
            "direct_reel_share_legibility_gradient_footer", "direct_private_share_media_container_view",
            "search_grid_ads_debug_row_container", "visual_search_icon_container"));

    public static void onFragmentViewCreated(Object fragment) {
        if (fragment == null) return;
        try {
            View view = (View) fragment.getClass().getMethod("getView").invoke(fragment);
            if (view == null) return;
            String name = fragment.getClass().getName();
            String module = "";
            try {
                java.lang.reflect.Method getter = fragment.getClass().getMethod("getModuleName");
                getter.setAccessible(true);
                module = String.valueOf(getter.invoke(fragment));
            }
            catch (ReflectiveOperationException ignored) {}
            ChatControls.observe(fragment, view, module);
            boolean blocked = MinimalPolicy.blocksModule(module)
                    || name.equals("com.instagram.profile.fragment.ProfileMediaTabFragment")
                    || name.contains("ReelViewerFragment") || name.contains("ClipsViewerFragment")
                    || name.contains("ShortUrlReelLoadingFragment");
            install(view, blocked, "search_typeahead".equals(module) || "search_serp".equals(module));
            if (blocked && (module.startsWith("clips_viewer") || module.startsWith("reel_feed")
                    || module.startsWith("feed_contextual") || module.equals("reel_viewer"))) {
                // Leave viewers promptly, so an already cached video cannot keep playing.
                view.post(() -> {
                    try {
                        Object activity = fragment.getClass().getMethod("getActivity").invoke(fragment);
                        if (activity instanceof android.app.Activity) ((android.app.Activity) activity).onBackPressed();
                    } catch (ReflectiveOperationException | RuntimeException ignored) {}
                });
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {}
    }

    public static void installWindow(View view) { install(view.getRootView(), false, false); }

    private static void install(View view, boolean blocked, boolean accountsOnly) {
        if (INSTALLED.containsKey(view)) return;
        INSTALLED.put(view, Boolean.TRUE);
        // A weak reference prevents the observer retaining detached fragment views.
        WeakReference<View> reference = new WeakReference<>(view);
        ViewTreeObserver.OnPreDrawListener watcher = () -> {
            View root = reference.get();
            if (root == null) return true;
            if (blocked) {
                root.setVisibility(View.INVISIBLE);
                root.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
                root.setOnTouchListener((v, event) -> true);
            } else scrub(root, accountsOnly);
            return true;
        };
        view.getViewTreeObserver().addOnPreDrawListener(watcher);
        if (blocked) view.setVisibility(View.INVISIBLE); else scrub(view, accountsOnly);
    }

    private static void scrub(View view, boolean accountsOnly) {
        if (SharedMessages.isHidden(view)) {
            view.setVisibility(View.GONE);
            return;
        }
        String id = resourceName(view);
        if (SharedMediaGuard.scrub(view, id)) return;
        if (HIDDEN_IDS.contains(id) || (accountsOnly && isSearchMedia(view, id))) {
            hide(view);
            return;
        }
        if (id.equals("profile_header_avatar_container") || id.equals("row_profile_header_imageview_frame_layout")) {
            disableStoryTouch(view);
        }
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        boolean results = accountsOnly && (view instanceof ListView
                || isRecyclerView(view));
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            // Search result rows must contain a native account-row identifier.
            // Headers, hashtags, places, media tiles, and unknown mixed rows stay hidden.
            // Rows can be recycled into account results. GONE already prevents
            // interaction; keep their native click/accessibility state for reuse.
            if (results && !hasAccountRow(child)) child.setVisibility(View.GONE);
            else {
                if (results) child.setVisibility(View.VISIBLE);
                scrub(child, accountsOnly);
            }
        }
    }

    private static boolean isRecyclerView(View view) {
        for (Class<?> type = view.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getName().equals("androidx.recyclerview.widget.RecyclerView")) return true;
        }
        return false;
    }

    private static void disableStoryTouch(View view) {
        view.setOnClickListener(null);
        view.setOnLongClickListener(null);
        view.setClickable(false);
        view.setLongClickable(false);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) disableStoryTouch(group.getChildAt(i));
        }
    }

    private static boolean isSearchMedia(View view, String id) {
        return view.getClass().getName().contains("IgMultiImageButton")
                || id.startsWith("row_hashtag") || id.startsWith("row_place")
                || id.startsWith("row_search_keyword") || id.contains("search_grid")
                || id.equals("search_tab_bar_layout");
    }

    private static boolean hasAccountRow(View view) {
        String id = resourceName(view);
        if (id.equals("row_search_user_container") || id.equals("row_search_user_username")
                || id.equals("row_search_user_vertical_container")) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) if (hasAccountRow(group.getChildAt(i))) return true;
        }
        return false;
    }

    private static String resourceName(View view) {
        if (view.getId() == View.NO_ID) return "";
        try { return view.getResources().getResourceEntryName(view.getId()); }
        catch (RuntimeException ignored) { return ""; }
    }

    private static void hide(View view) {
        view.setVisibility(View.GONE);
        view.setClickable(false);
        view.setLongClickable(false);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }
}
