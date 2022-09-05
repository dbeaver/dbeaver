package org.jkiss.dbeaver.parser.common;

public class TermPatternInfo {
    public static final TermPatternInfo EOF = new TermPatternInfo(-1, null, TermPatternCaps.FIXED);

    public final int id;
    public final String pattern;
    public final TermPatternCaps caps;

    public TermPatternInfo(int id, String pattern, TermPatternCaps caps) {
        this.id = id;
        this.pattern = pattern;
        this.caps = caps;
    }

    public boolean isEOF() {
        return this.pattern == null;
    }

    public String makeRegexGroup() {
        return "(?<g" + id + ">(" + pattern + "))";
    }

    public String makeRegexGroupName() {
        return "g" + id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.id);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TermPatternInfo && this.id == ((TermPatternInfo) obj).id;
    }

    @Override
    public String toString() {
        return "[$" + this.id + ": " + this.pattern + "]";
    }
}
