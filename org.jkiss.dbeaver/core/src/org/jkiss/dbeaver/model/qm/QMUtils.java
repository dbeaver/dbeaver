/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.qm;

import org.jkiss.dbeaver.core.DBeaverCore;

/**
 * Query Manager utils
 */
public class QMUtils {

    private static QMExecutionHandler defaultHandler; 

    public static QMExecutionHandler getDefaultHandler()
    {
        if (defaultHandler == null) {
            defaultHandler = DBeaverCore.getInstance().getQueryManager().getDefaultHandler();
        }
        return defaultHandler;
    }

    public static void registerHandler(QMExecutionHandler handler)
    {
        DBeaverCore.getInstance().getQueryManager().registerHandler(handler);
    }

    public static void unregisterHandler(QMExecutionHandler handler)
    {
        DBeaverCore.getInstance().getQueryManager().unregisterHandler(handler);
    }

}
