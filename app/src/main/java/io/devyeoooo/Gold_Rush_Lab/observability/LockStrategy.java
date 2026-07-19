package io.devyeoooo.Gold_Rush_Lab.observability;

import java.util.Locale;

public enum LockStrategy {
    NONE,
    OPTIMISTIC,
    PESSIMISTIC,
    REDIS;

    public static LockStrategy from(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    public String tagValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
