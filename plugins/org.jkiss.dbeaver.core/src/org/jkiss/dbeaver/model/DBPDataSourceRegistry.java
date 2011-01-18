/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import org.eclipse.core.resources.IProject;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

import java.util.List;

/**
 * DBPDataSourceRegistry
 */
public interface DBPDataSourceRegistry {

    IProject getProject();

    DBSDataSourceContainer getDataSource(String id);

    DBSDataSourceContainer getDataSource(DBPDataSource dataSource);

    DBSDataSourceContainer findDataSourceByName(String name);

    public List<? extends DBSDataSourceContainer> getDataSources();

    void addDataSourceListener(DBPEventListener listener);

    boolean removeDataSourceListener(DBPEventListener listener);

}
