/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.cache;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;

/**
 * Abstract objects cache
 */
public interface JDBCAbstractCache<OBJECT extends DBSObject> {

    Collection<OBJECT> getObjects(DBRProgressMonitor monitor, JDBCDataSource dataSource)
        throws DBException;

    Collection<OBJECT> getCachedObjects();

    OBJECT getObject(DBRProgressMonitor monitor, JDBCDataSource dataSource, String name)
        throws DBException;

    OBJECT getCachedObject(String name);

    void clearCache();
}
