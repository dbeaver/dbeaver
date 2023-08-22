package org.jkiss.dbeaver.ext.yashandb.model;


/**
 * Data type modifier
 */
public enum YashanDBDataTypeModifier {
    REF,
    POINTER;

    public static YashanDBDataTypeModifier resolveTypeModifier(String typeMod) {
        if (typeMod == null || typeMod.length() == 0) {
            return null;
        } else if (typeMod.equals("REF")) {
            return REF;
        } else {
            return POINTER;
        }
    }
}