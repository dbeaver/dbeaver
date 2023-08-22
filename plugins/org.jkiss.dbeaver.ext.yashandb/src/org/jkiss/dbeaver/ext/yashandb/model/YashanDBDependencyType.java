package org.jkiss.dbeaver.ext.yashandb.model;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public enum YashanDBDependencyType {
    HARD("HARD"),
    REF("REF");

    private final String type;

    YashanDBDependencyType(String type) {
        this.type = type;
    }

    public static YashanDBDependencyType getByType(String type) {
        if ("HARD".equals(type)) {
            return HARD;
        }
        if ("REF".equals(type)) {
            return REF;
        }
        return null;
    }

    public String getType() {
        return type;
    }
}
