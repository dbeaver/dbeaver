package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

import java.util.List;

/**
 * DBPRegistry
 */
public interface DBPRegistry
{
    
    public List<? extends DBSDataSourceContainer> getDataSources();
}
