/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import net.sf.jkiss.utils.CommonUtils;

/**
* Parameter/argument mode
*/
public enum OracleParameterMode {
    IN,
    OUT,
    INOUT;

    public static OracleParameterMode getMode(String modeName)
    {
        if (CommonUtils.isEmpty(modeName)) {
            return null;
        } else if ("IN".equals(modeName)) {
            return IN;
        } else if ("OUT".equals(modeName)) {
            return OracleParameterMode.OUT;
        } else {
            return OracleParameterMode.INOUT;
        }
    }
}
