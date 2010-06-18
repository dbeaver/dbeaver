/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.qm;

import org.jkiss.dbeaver.core.DBeaverCore;

/**
 * Query Manager utils
 */
public class QMUtils {

    public static QMExecutionHandler getHandler()
    {
        return DBeaverCore.getInstance().getQMHandler();
    }

}
