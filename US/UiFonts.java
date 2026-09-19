import java.awt.*;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves a UI font family once at startup (Segoe UI on Windows, DejaVu
 * Sans on most Linux systems, falling back to the platform's SansSerif),
 * and hands out cached Font instances instead of allocating a new Font
 * object on every paint call.
 */
public final class UiFonts {
    private static final String FAMILY = resolveFamily();
    private static final Map<Long, Font> CACHE = new ConcurrentHashMap<>();

    private UiFonts() {}

    private static String resolveFamily() {
        Set<String> available = new HashSet<>();
        for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            available.add(name);
        }
        for (String candidate : new String[]{"Segoe UI", "DejaVu Sans", "Noto Sans", "Arial"}) {
            if (available.contains(candidate)) {
                return candidate;
            }
        }
        return Font.SANS_SERIF;
    }

    public static Font bold(int size) {
        return cached(Font.BOLD, size);
    }

    public static Font plain(int size) {
        return cached(Font.PLAIN, size);
    }

    private static Font cached(int style, int size) {
        long key = ((long) style << 32) | (size & 0xffffffffL);
        return CACHE.computeIfAbsent(key, k -> new Font(FAMILY, style, size));
    }
}
