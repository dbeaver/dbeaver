/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Meta object
 */
public interface DBSObject extends DBPNamedObject
{
    /**
     * Object name
     *
     * @return object name
     */
    String getName();

    /**
     * Object description
     *
     * @return object description or null
     */
    String getDescription();

    /**
     * Parent object
     *
     * @return parent object or null
     */
	DBSObject getParentObject();

    /**
     * Datasource which this object belongs
     * @return datasource reference
     */
    DBPDataSource getDataSource();

}
