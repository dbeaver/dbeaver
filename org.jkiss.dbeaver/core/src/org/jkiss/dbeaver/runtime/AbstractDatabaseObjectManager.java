/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabaseObjectManager;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * AbstractDatabaseObjectManager
 */
public abstract class AbstractDatabaseObjectManager<OBJECT_TYPE extends DBPObject> implements IDatabaseObjectManager<OBJECT_TYPE> {

    private DBPDataSource dataSource;
    private OBJECT_TYPE object;

    public DBPDataSource getDataSource() {
        return dataSource;
    }

    public OBJECT_TYPE getObject() {
        return object;
    }

    @SuppressWarnings("unchecked")
    public void init(DBPDataSource dataSource, OBJECT_TYPE object) {
        if (object == null) {
            throw new IllegalArgumentException("Object can't be NULL");
        }
        this.dataSource = dataSource;
        this.object = object;
    }

    public boolean supportsEdit() {
        return false;
    }

    public void saveChanges(DBRProgressMonitor monitor) throws DBException {
        // do nothing
    }

    public void resetChanges(DBRProgressMonitor monitor) {
        // do nothing
    }
}
