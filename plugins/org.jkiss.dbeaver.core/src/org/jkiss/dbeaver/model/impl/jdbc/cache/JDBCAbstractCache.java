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
public interface JDBCAbstractCache<OWNER extends DBSObject, OBJECT extends DBSObject> {

    Collection<OBJECT> getObjects(DBRProgressMonitor monitor, OWNER owner)
        throws DBException;

    Collection<OBJECT> getCachedObjects();

    OBJECT getObject(DBRProgressMonitor monitor, OWNER owner, String name)
        throws DBException;

    OBJECT getCachedObject(String name);

    void cacheObject(OBJECT object);

    void clearCache();

}
