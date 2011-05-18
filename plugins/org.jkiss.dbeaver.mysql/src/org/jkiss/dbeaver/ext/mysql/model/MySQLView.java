/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSConstraint;
import org.jkiss.dbeaver.model.struct.DBSForeignKey;
import org.jkiss.dbeaver.model.struct.DBSIndex;

import java.sql.ResultSet;
import java.util.List;

/**
 * MySQLView
 */
public class MySQLView extends MySQLTableBase
{

    public MySQLView(MySQLCatalog catalog)
    {
        super(catalog);
    }

    public MySQLView(
        MySQLCatalog catalog,
        ResultSet dbResult)
    {
        super(catalog, dbResult);
    }

    @Property(name = "View Name", viewable = true, editable = true, valueTransformer = JDBCObjectNameCaseTransformer.class, order = 1)
    @Override
    public String getName()
    {
        return super.getName();
    }

    public boolean isView()
    {
        return true;
    }

    public List<? extends DBSIndex> getIndexes(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    public List<? extends DBSConstraint> getConstraints(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    public List<? extends DBSForeignKey> getForeignKeys(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    public List<? extends DBSForeignKey> getReferences(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Override
    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException
    {
        super.refreshEntity(monitor);
        return true;
    }

    public String getDescription()
    {
        return null;
    }


}
