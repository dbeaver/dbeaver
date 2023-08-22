package org.jkiss.dbeaver.ext.yashandb.model;

public enum YashanDBSourceType {
    TYPE(false),
    PROCEDURE(false),
    UDF(false),
//    FUNCTION(false),
    PACKAGE(false),
    TRIGGER(false),
    VIEW(true),
    JOB(false),
    PROFILE(false),

    MATERIALIZED_VIEW(true);


    private final boolean isCustom;

    YashanDBSourceType(boolean custom) {
        isCustom = custom;
    }

    public boolean isCustom() {
        return isCustom;
    }
}
