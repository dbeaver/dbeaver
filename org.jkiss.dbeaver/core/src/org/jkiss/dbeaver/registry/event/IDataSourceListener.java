package org.jkiss.dbeaver.registry.event;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * IDataSourceListener
 */
public interface IDataSourceListener
{
    void dataSourceChanged(DataSourceEvent event, IProgressMonitor monitor);
}
