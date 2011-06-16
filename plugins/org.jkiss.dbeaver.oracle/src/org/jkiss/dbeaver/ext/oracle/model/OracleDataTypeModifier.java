/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import net.sf.jkiss.utils.CommonUtils;

/**
* Data type modifier
*/
public enum OracleDataTypeModifier {
    REF,
    POINTER;

    public static OracleDataTypeModifier resolveTypeModifier(String typeMod)
    {
        if (typeMod == null || typeMod.length() == 0) {
            return null;
        } else if (typeMod.equals("REF")) {
            return REF;
        } else {
            return POINTER;
        }
    }
}
