package com.feurstagram.extension;

import android.view.Window;
import android.view.WindowManager;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** The pinned Instagram build requests secure windows with a named reason. */
public final class ScreenCapture {
    private ScreenCapture() {}
    private static final int SECURE = WindowManager.LayoutParams.FLAG_SECURE;
    private static final WeakHashMap<Window, State> WINDOWS = new WeakHashMap<>();
    private static final class State {
        final Map<String, Integer> reasons = new HashMap<>();
        boolean relaxed;
        boolean onlyOrdinaryReasons() {
            if (reasons.isEmpty()) return false;
            for (String reason : reasons.keySet()) if (!ordinaryReason(reason)) return false;
            return true;
        }
    }

    private static boolean ordinaryReason(String reason) {
        return "DirectInboxController".equals(reason) || "DirectThreadController".equals(reason);
    }

    /** Invoked before Instagram's secure-window manager records a request. */
    public static boolean skipRequest(Window window, String reason) {
        if (window == null || reason == null) return false;
        if (ordinaryReason(reason) && ChatControls.isOrdinaryChat(window)) return true;
        State state = WINDOWS.computeIfAbsent(window, ignored -> new State());
        state.reasons.merge(reason, 1, Integer::sum);
        if (state.relaxed && !state.onlyOrdinaryReasons()) {
            // A protected viewer can open on top of a chat between two draws.
            window.addFlags(SECURE);
            state.relaxed = false;
        }
        return false;
    }

    /** An ordinary request that never reached Instagram must not consume a protected request. */
    public static boolean skipRelease(Window window, String reason) {
        State state = WINDOWS.get(window);
        Integer count = state == null ? null : state.reasons.get(reason);
        if (count == null) return ordinaryReason(reason);
        if (count == 1) state.reasons.remove(reason);
        else state.reasons.put(reason, count - 1);
        if (state.reasons.isEmpty()) WINDOWS.remove(window);
        return false;
    }

    public static void setChatActive(Window window, boolean ordinary) {
        State state = WINDOWS.get(window);
        if (state == null) return;
        if (ordinary && state.onlyOrdinaryReasons()) {
            if ((window.getAttributes().flags & SECURE) != 0) {
                window.clearFlags(SECURE);
                state.relaxed = true;
            }
        } else if (state.relaxed) {
            state.relaxed = false;
            if (!state.reasons.isEmpty()) window.addFlags(SECURE);
        }
    }
}
