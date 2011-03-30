/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPPersistedObject;

/**
 * Meta object
 */
public interface DBSObject extends DBPNamedObject, DBPPersistedObject
{

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
