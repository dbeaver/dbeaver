/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry.event;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * IDataSourceListener
 */
public interface IDataSourceListener
{
    void handleDataSourceEvent(DataSourceEvent event);
}
