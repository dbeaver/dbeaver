/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

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
