package org.jkiss.dbeaver.tools.transfer.stream.exporter;

public enum QuoteStrategy {

    DISABLED("disabled"), ALL("all"), STRINGS("strings"), ALL_BUT_NUMBERS("all but numbers");

    private final String value;

    QuoteStrategy(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static QuoteStrategy fromValue(String v) {
        for (QuoteStrategy s: QuoteStrategy.values()) {
            if (s.value.equals(v)) {
                return s;
            }
        }
        // backward compability
        if ("true".equalsIgnoreCase(v)) {
            return ALL;
        } else if ("false".equalsIgnoreCase(v)) {
            return DISABLED;
        }
        throw new IllegalArgumentException(v);
    }
}
