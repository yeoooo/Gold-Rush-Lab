package io.devyeoooo.Gold_Rush_Lab.observability;

public enum MiningFailureType {
    CANNOT_ACQUIRE_LOCK("cannot_acquire_lock", true),
    LOCK_TIMEOUT("lock_timeout", true),
    DEADLOCK("deadlock", false),
    OPTIMISTIC_LOCK("optimistic_lock", false),
    UNKNOWN("unknown", false);

    private final String tagValue;
    private final boolean timeout;

    MiningFailureType(String tagValue, boolean timeout) {
        this.tagValue = tagValue;
        this.timeout = timeout;
    }

    public String tagValue() {
        return tagValue;
    }

    public boolean isTimeout() {
        return timeout;
    }
}
