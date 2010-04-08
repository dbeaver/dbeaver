package org.jkiss.dbeaver.registry.event;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * IDataSourceListener
 */
public interface IDataSourceListener
{
    void dataSourceChanged(DataSourceEvent event);
}
