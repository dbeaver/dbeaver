/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPNamedObject;

/**
 * Meta object
 */
public interface DBSObject extends DBPNamedObject
{

    /**
     * Object ID. ID must be unique within object's datasource.
     * @return object ID
     */
    String getObjectId();

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

    /**
     * Object's persisted flag
     * @return true if object is persisted in external data source
     */
    boolean isPersisted();

}
