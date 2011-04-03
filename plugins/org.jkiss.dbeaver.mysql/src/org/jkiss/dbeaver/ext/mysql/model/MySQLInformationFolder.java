/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;

/**
 * MySQL informational object
 */
public abstract class MySQLInformationFolder<OBJECT_TYPE extends DBSObject> implements DBSObject {

    private MySQLDataSource dataSource;
    private String name;

    public MySQLInformationFolder(MySQLDataSource dataSource, String name)
    {
        this.dataSource = dataSource;
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public boolean isPersisted()
    {
        return true;
    }

    public String getDescription()
    {
        return null;
    }

    public DBSObject getParentObject()
    {
        return dataSource.getContainer();
    }

    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    public abstract Collection<OBJECT_TYPE> getObjects(DBRProgressMonitor monitor) throws DBException;
}
