package hu.montlikadani.api;

public enum TicksPerSecondType {

    SECONDS_5("5sec"), SECONDS_15("15sec"), MINUTES_1("1min"), MINUTES_5("5min"), MINUTES_15("15min");

    public static final TicksPerSecondType[] VALUES = values();

    public final String type;

    TicksPerSecondType(String type) {
        this.type = type;
    }
}
