/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBPEventListener;
import org.jkiss.dbeaver.model.dbc.DBCStatement;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.runtime.qm.DefaultExecutionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Query manager execution handler implementation
 */
public class QMMExecutionHandler extends DefaultExecutionHandler implements DBPEventListener {

    private Map<DBSDataSourceContainer, QMMDataSourceInfo> dataSourcesInfo = new HashMap<DBSDataSourceContainer, QMMDataSourceInfo>();

    public QMMExecutionHandler()
    {
    	DBeaverCore.getInstance().getDataSourceRegistry().addDataSourceListener(this);
    }

    public void handleDataSourceEvent(DBPEvent event)
    {
        if (event.getObject() instanceof DBSDataSourceContainer && event.getAction() == DBPEvent.Action.OBJECT_REMOVE) {
            dataSourcesInfo.remove((DBSDataSourceContainer)event.getObject());
        }
    }

    public void dispose()
    {
        DBeaverCore.getInstance().getDataSourceRegistry().removeDataSourceListener(this);
    }

    public String getHandlerName()
    {
        return "Meta info collector";
    }

    @Override
    public void handleStatementExecuteBegin(DBCStatement statement)
    {

    }

}
