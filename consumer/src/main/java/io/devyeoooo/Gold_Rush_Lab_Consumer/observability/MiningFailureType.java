package io.devyeoooo.Gold_Rush_Lab_Consumer.observability;

public enum MiningFailureType {
    DESERIALIZATION("deserialization"),
    USER_NOT_FOUND("user_not_found"),
    MINE_DEPLETED("mine_depleted"),
    CANNOT_ACQUIRE_LOCK("cannot_acquire_lock"),
    LOCK_TIMEOUT("lock_timeout"),
    DEADLOCK("deadlock"),
    DATABASE("database"),
    KAFKA_PUBLISH("kafka_publish"),
    UNKNOWN("unknown");

    private final String tagValue;

    MiningFailureType(String tagValue) {
        this.tagValue = tagValue;
    }

    public String tagValue() {
        return tagValue;
    }
}
