/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IAction;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;


public abstract class SessionAction extends DataSourceAction
{
    static final Log log = LogFactory.getLog(SessionAction.class);

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