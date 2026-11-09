package com.feurstagram.extension;

import android.view.View;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Filters the rendered DM card, leaving the native message and attachment data intact. */
public final class SharedMessages {
    private SharedMessages() {}
    private static final Map<Class<?>, Field> STYLES = new HashMap<>();
    private static final Map<Class<?>, Field> KINDS = new HashMap<>();
    private static final Map<Class<?>, Method> VIEWS = new HashMap<>();
    private static final WeakHashMap<View, Integer> HIDDEN = new WeakHashMap<>();

    public static void onBind(Object holder, Object data) {
        if (holder == null || data == null) return;
        try {
            boolean legacyPost = "X.0OkG".equals(holder.getClass().getName());
            String type = legacyPost ? null : messageType(data);
            if (!legacyPost && type == null) return;
            boolean block = legacyPost || MinimalPolicy.blocksSharedMessage(type);
            if (!block && ("XMA_LINK".equals(type) || "GENERIC_XMA".equals(type) || "FB_XAC_XMA".equals(type))) {
                for (Field field : data.getClass().getDeclaredFields()) {
                    if (field.getType() != String.class || java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                    field.setAccessible(true);
                    if (MinimalPolicy.blocksLink((String) field.get(data))) { block = true; break; }
                }
            }
            Method getView = VIEWS.get(holder.getClass());
            if (getView == null) {
                getView = holder.getClass().getMethod("Cd3");
                getView.setAccessible(true);
                VIEWS.put(holder.getClass(), getView);
            }
            update((View) getView.invoke(holder), block);
        } catch (ReflectiveOperationException | RuntimeException ignored) { /* preserve an unknown native card */ }
    }

    private static String messageType(Object data) throws ReflectiveOperationException {
        Field styleField = styleField(data.getClass());
        Object style = styleField == null ? null : styleField.get(data);
        if (style == null) return null;
        Class<?> styleClass = style.getClass();
        Field kindField = KINDS.get(styleClass);
        if (kindField == null) {
            kindField = styleClass.getDeclaredField("A04");
            kindField.setAccessible(true);
            KINDS.put(styleClass, kindField);
        }
        Object kind = kindField.get(style);
        return kind instanceof Enum ? ((Enum<?>) kind).name() : null;
    }

    private static Field styleField(Class<?> type) {
        if (STYLES.containsKey(type)) return STYLES.get(type);
        for (Class<?> cls = type; cls != null; cls = cls.getSuperclass()) {
            for (Field field : cls.getDeclaredFields()) if ("X.07Ej".equals(field.getType().getName())) {
                field.setAccessible(true);
                STYLES.put(type, field);
                return field;
            }
        }
        STYLES.put(type, null);
        return null;
    }

    public static void update(View view, boolean blocked) {
        if (view == null) return;
        if (blocked) {
            HIDDEN.putIfAbsent(view, view.getVisibility());
            view.setVisibility(View.GONE);
        } else {
            Integer previous = HIDDEN.remove(view);
            if (previous != null && view.getVisibility() == View.GONE) view.setVisibility(previous);
        }
    }

    static boolean isHidden(View view) { return HIDDEN.containsKey(view); }
}
