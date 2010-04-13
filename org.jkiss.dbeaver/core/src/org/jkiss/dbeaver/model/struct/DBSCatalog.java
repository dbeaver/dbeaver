/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;

import java.util.Collection;

/**
 * DBSCatalog
 */
public interface DBSCatalog extends DBSStructureObject, DBSStructureContainer
{
    Collection<? extends DBSSchema> getSchemas() throws DBException;
}
