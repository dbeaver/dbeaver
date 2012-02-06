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

    /**
     * Adds specified object to cache
     * @param object object to cache
     */
    void cacheObject(OBJECT object);

    /**
     * Removes specified object from cache
     * @param object object to remove
     */
    void removeObject(OBJECT object);

    void clearCache();

}
