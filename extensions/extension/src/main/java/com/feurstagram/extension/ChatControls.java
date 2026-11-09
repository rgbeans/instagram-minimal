package com.feurstagram.extension;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.WeakHashMap;

/** One controller per window: inbox notifications and capture access for ordinary chats. */
public final class ChatControls {
    private ChatControls() {}
    private static final WeakHashMap<View, Controller> WINDOWS = new WeakHashMap<>();
    private static final WeakHashMap<View, Boolean> OBSERVED = new WeakHashMap<>();

    public static boolean isOrdinaryChat(Window window) {
        View root = window.peekDecorView();
        Controller controller = root == null ? null : WINDOWS.get(root);
        return controller != null && controller.screen() != Screen.OTHER;
    }

    private enum Screen { OTHER, INBOX, THREAD }

    public static void observe(Object fragment, View view, String module) {
        if (OBSERVED.put(view, Boolean.TRUE) != null) return;
        Entry entry = new Entry(fragment, view, module);
        view.addOnAttachStateChangeListener(entry);
        if (view.isAttachedToWindow()) entry.onViewAttachedToWindow(view);
    }

    private static final class Entry implements View.OnAttachStateChangeListener {
        final WeakReference<Object> fragment;
        final WeakReference<View> view;
        final String module;
        WeakReference<Controller> controller = new WeakReference<>(null);
        Entry(Object fragment, View view, String module) {
            this.fragment = new WeakReference<>(fragment);
            this.view = new WeakReference<>(view);
            this.module = module.toLowerCase(Locale.ROOT);
        }
        public void onViewAttachedToWindow(View view) {
            Context context = Settings.getActivityContext(view);
            if (!(context instanceof Activity)) return;
            Window window = ((Activity) context).getWindow();
            View root = view.getRootView();
            if (root != window.getDecorView()) return;
            Controller current = WINDOWS.get(root);
            if (current == null) {
                current = new Controller(root, window);
                WINDOWS.put(root, current);
            }
            current.entries.put(view, this);
            controller = new WeakReference<>(current);
            current.update();
        }
        public void onViewDetachedFromWindow(View view) {
            Controller current = controller.get();
            if (current != null) { current.entries.remove(view); current.update(); }
        }
        boolean active() {
            View root = view.get(); Object owner = fragment.get();
            if (root == null || owner == null || !visible(root)) return false;
            try {
                Method resumed = owner.getClass().getMethod("isResumed");
                resumed.setAccessible(true);
                if (!Boolean.TRUE.equals(resumed.invoke(owner))) return false;
            } catch (ReflectiveOperationException | RuntimeException ignored) { return false; }
            try {
                if (Boolean.FALSE.equals(owner.getClass().getMethod("getUserVisibleHint").invoke(owner))) return false;
            } catch (ReflectiveOperationException ignored) {}
            return true;
        }
    }

    private static final class Controller implements ViewTreeObserver.OnPreDrawListener {
        final WeakReference<View> root;
        final WeakReference<Window> window;
        final WeakHashMap<View, Entry> entries = new WeakHashMap<>();
        final HashMap<String, Integer> ids = new HashMap<>();
        WeakReference<ImageButton> button = new WeakReference<>(null);
        Controller(View root, Window window) {
            this.root = new WeakReference<>(root); this.window = new WeakReference<>(window);
            root.getViewTreeObserver().addOnPreDrawListener(this);
        }
        public boolean onPreDraw() { update(); return true; }
        Screen screen() {
            View decor = root.get();
            if (decor == null) return Screen.OTHER;
            boolean inbox = false, thread = false, sensitive = false;
            for (Entry entry : entries.values()) if (entry.active()) {
                String module = entry.module;
                View content = entry.view.get();
                if (content == null) continue;
                inbox |= module.equals("direct_inbox") || named(content, "direct_inbox_action_bar") != null;
                thread |= module.equals("direct_thread") || module.equals("csc_direct_thread")
                        || module.equals("broadcast_chat_thread") || module.equals("direct_thread_toggle")
                        || named(content, "direct_thread_header") != null
                        || named(content, "direct_thread_content_below_action_bar") != null;
                sensitive |= module.contains("expiring") || module.contains("vanish")
                        || module.contains("disappearing") || module.contains("visual_message_viewer")
                        || module.contains("visual_reply");
            }
            if (!inbox && !thread) return Screen.OTHER;
            sensitive |= named(decor, "direct_expiring_media_viewer_container") != null
                    || named(decor, "direct_expiring_media_viewer_content") != null
                    || named(decor, "disappearing_mode_indicator_placeholder") != null
                    || named(decor, "disappearing_mode_indicator_stub") != null;
            return sensitive ? Screen.OTHER : thread ? Screen.THREAD : Screen.INBOX;
        }
        View named(View view, String name) {
            int id = ids.computeIfAbsent(name, key -> resource(view.getContext(), key, "id"));
            return id == 0 ? null : findVisible(view, id);
        }
        void update() {
            View decor = root.get(); Window host = window.get();
            if (decor == null || host == null) return;
            Screen screen = screen();
            ScreenCapture.setChatActive(host, screen != Screen.OTHER);
            if (screen == Screen.INBOX) {
                View inboxBar = named(decor, "direct_inbox_action_bar");
                View scope = inboxBar != null ? inboxBar : decor;
                View target = named(scope, "action_bar_buttons_container_right");
                if (target == null && inboxBar != null) target = named(decor, "action_bar_buttons_container_right");
                if (!(target instanceof LinearLayout)) target = actionGroup(target != null ? target : inboxBar);
                if (target instanceof LinearLayout) addButton((LinearLayout) target);
                else removeButton();
            } else removeButton();
        }
        void addButton(ViewGroup parent) {
            ImageButton current = button.get();
            if (current != null && current.getParent() == parent) return;
            removeButton();
            Context context = parent.getContext();
            ImageButton icon = new ImageButton(context);
            icon.setId(View.generateViewId());
            icon.setContentDescription("Notifications");
            icon.setTooltipText("Notifications");
            int drawable = resource(context, "fb_ic_bell_outline_24", "drawable");
            icon.setImageResource(drawable != 0 ? drawable : android.R.drawable.ic_popup_reminder);
            icon.setImageTintList(ColorStateList.valueOf(iconColor(context)));
            icon.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            int padding = dp(context, 12);
            icon.setPadding(padding, padding, padding, padding);
            TypedValue background = new TypedValue();
            if (context.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, background, true)
                    && background.resourceId != 0) icon.setBackgroundResource(background.resourceId);
            else icon.setBackgroundColor(Color.TRANSPARENT);
            icon.setOnClickListener(v -> {
                Context activity = Settings.getActivityContext(v);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("instagram://news"));
                intent.setPackage(activity.getPackageName());
                if (!(activity instanceof Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try { activity.startActivity(intent); }
                catch (RuntimeException error) { Toast.makeText(activity, "Notifications are unavailable here", Toast.LENGTH_SHORT).show(); }
            });
            int size = dp(context, 48);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(size, size);
            if (parent instanceof LinearLayout) {
                LinearLayout.LayoutParams linear = new LinearLayout.LayoutParams(size, size);
                linear.gravity = Gravity.CENTER_VERTICAL; params = linear;
            }
            parent.addView(icon, params);
            button = new WeakReference<>(icon);
        }
        void removeButton() {
            ImageButton current = button.get();
            if (current != null && current.getParent() instanceof ViewGroup)
                ((ViewGroup) current.getParent()).removeView(current);
            button.clear();
        }
    }

    private static int resource(Context context, String name, String type) {
        int id = context.getResources().getIdentifier(name, type, context.getPackageName());
        return id != 0 ? id : context.getResources().getIdentifier(name, type, "com.instagram.android");
    }
    private static int iconColor(Context context) {
        int id = resource(context, "igds_primary_icon", "color");
        if (id != 0) try { return context.getResources().getColor(id, context.getTheme()); } catch (RuntimeException ignored) {}
        return (context.getResources().getConfiguration().uiMode & 0x30) == 0x20 ? Color.WHITE : Color.BLACK;
    }
    private static int dp(Context context, int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
    private static LinearLayout actionGroup(View root) {
        if (root == null || !visible(root)) return null;
        LinearLayout best = null;
        if (root instanceof LinearLayout && ((LinearLayout) root).getOrientation() == LinearLayout.HORIZONTAL
                && (root.getLayoutParams() == null || root.getLayoutParams().width < 0)) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof android.widget.ImageView || child.isClickable()) { best = (LinearLayout) root; break; }
            }
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                LinearLayout candidate = actionGroup(group.getChildAt(i));
                if (candidate == null) continue;
                Rect a = new Rect(), b = new Rect(); candidate.getGlobalVisibleRect(a);
                if (best != null) best.getGlobalVisibleRect(b);
                boolean rtl = root.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
                if (best == null || (rtl ? a.left < b.left : a.right > b.right)
                        || ((rtl ? a.left == b.left : a.right == b.right) && a.width() < b.width())) best = candidate;
            }
        }
        return best;
    }
    private static boolean visible(View view) { return view.isShown() && view.getGlobalVisibleRect(new Rect()); }
    private static View findVisible(View root, int id) {
        if (root.getVisibility() != View.VISIBLE) return null;
        if (root.getId() == id && visible(root)) return root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findVisible(group.getChildAt(i), id);
                if (found != null) return found;
            }
        }
        return null;
    }
}
