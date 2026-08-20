package util;

public final class AuthContext {
    private static final ThreadLocal<String> CURRENT_USER_ID = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(String userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static String userId() {
        return CURRENT_USER_ID.get();
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}