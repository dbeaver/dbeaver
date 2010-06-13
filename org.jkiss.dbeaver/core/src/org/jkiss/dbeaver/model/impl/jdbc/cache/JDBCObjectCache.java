/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.cache;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Various objects cache
 */
public abstract class JDBCObjectCache<OBJECT extends DBSObject> {

    private List<OBJECT> objectList;
    private final String objectListName;

    protected JDBCObjectCache(String objectListName)
    {
        this.objectListName = objectListName;
    }

    abstract protected PreparedStatement prepareObjectsStatement(DBRProgressMonitor monitor)
        throws SQLException, DBException;

    abstract protected OBJECT fetchObject(DBRProgressMonitor monitor, ResultSet resultSet)
        throws SQLException, DBException;

    public List<OBJECT> getObjects(DBRProgressMonitor monitor)
        throws DBException
    {
        if (objectList == null) {
            cacheObjects(monitor);
        }
        return objectList;
    }

    public void clearCache()
    {
        this.objectList = null;
    }

    private void cacheObjects(DBRProgressMonitor monitor)
        throws DBException
    {
        if (objectList != null) {
            return;
        }

        List<OBJECT> tmpProcedureList = new ArrayList<OBJECT>();

        try {
            PreparedStatement dbStat = prepareObjectsStatement(monitor);
            monitor.startBlock(JDBCUtils.makeBlockingObject(dbStat), "Load " + this.objectListName);
            try {

                // Load objectList
                ResultSet dbResult = dbStat.executeQuery();
                try {
                    while (dbResult.next()) {
                        OBJECT object = fetchObject(monitor, dbResult);
                        if (object == null) {
                            continue;
                        }
                        tmpProcedureList.add(object);
                    }
                }
                finally {
                    JDBCUtils.safeClose(dbResult);
                }
            }
            finally {
                JDBCUtils.closeStatement(monitor, dbStat);
            }
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
        this.objectList = tmpProcedureList;
    }

}
