/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.cache;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;

/**
 * Abstract objects cache
 */
public interface JDBCAbstractCache<OBJECT extends DBSObject> {

    Collection<OBJECT> getObjects(DBRProgressMonitor monitor)
        throws DBException;

    OBJECT getObject(DBRProgressMonitor monitor, String name)
        throws DBException;

}
