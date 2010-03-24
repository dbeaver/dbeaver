package org.jkiss.dbeaver.registry.event;

import org.jkiss.dbeaver.model.DBPProgressMonitor;

/**
 * IDataSourceListener
 */
public interface IDataSourceListener
{
    void dataSourceChanged(DataSourceEvent event, DBPProgressMonitor monitor);
}
