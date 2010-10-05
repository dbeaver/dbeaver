/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.qm;

import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.runtime.qm.QMMetaEvent;
import org.jkiss.dbeaver.runtime.qm.QMMetaListener;
import org.jkiss.dbeaver.ui.controls.querylog.QueryLogViewer;

import java.util.Collection;

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

    public static void registerMetaListener(QMMetaListener metaListener)
    {
        DBeaverCore.getInstance().getQueryManager().registerMetaListener(metaListener);
    }

    public static void unregisterMetaListener(QMMetaListener metaListener)
    {
        DBeaverCore.getInstance().getQueryManager().unregisterMetaListener(metaListener);
    }

    public static Collection<QMMetaEvent> getPastMetaEvents()
    {
        return DBeaverCore.getInstance().getQueryManager().getPastMetaEvents();
    }
}
