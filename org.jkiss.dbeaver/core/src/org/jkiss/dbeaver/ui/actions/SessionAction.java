/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.utils.DBeaverUtils;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.actions.sql.AbstractSQLAction;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.eclipse.jface.action.IAction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public abstract class SessionAction extends DataSourceAction
{
    static Log log = LogFactory.getLog(SessionAction.class);

    @Override
    protected void updateAction(IAction action)
    {
        action.setEnabled(isConnected());
    }

    protected boolean isConnected()
    {
        DBSDataSourceContainer dataSourceContainer = getDataSourceContainer(false);
        return dataSourceContainer != null && dataSourceContainer.isConnected();
    }

}