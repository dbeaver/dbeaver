/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.utils.CommonUtils;

/**
* Object status
*/
public enum OracleObjectStatus {
    ENABLED,
    DISABLED;

    public static OracleObjectStatus getByName(String name)
    {
        if (CommonUtils.isEmpty(name)) {
            return null;
        } else {
            return OracleObjectStatus.valueOf(name);
        }
    }
}
