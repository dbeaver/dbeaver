/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;

public class JDBCDataTypeCache extends JDBCObjectCache<JDBCDataSource, DBSDataType> {
    private final DBSObject owner;

    public JDBCDataTypeCache(DBSObject owner)
    {
        this.owner = owner;
        setCaseSensitive(false);
    }

    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, JDBCDataSource owner) throws SQLException
    {
        return context.getMetaData().getTypeInfo().getSource();
    }

    @Override
    protected JDBCDataType fetchObject(JDBCExecutionContext context, JDBCDataSource owner, ResultSet dbResult) throws SQLException, DBException
    {
        return new JDBCDataType(
            this.owner,
            JDBCUtils.safeGetInt(dbResult, JDBCConstants.DATA_TYPE),
            JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_NAME),
            JDBCUtils.safeGetString(dbResult, JDBCConstants.LOCAL_TYPE_NAME),
            JDBCUtils.safeGetBoolean(dbResult, JDBCConstants.UNSIGNED_ATTRIBUTE),
            JDBCUtils.safeGetInt(dbResult, JDBCConstants.SEARCHABLE) != 0,
            JDBCUtils.safeGetInt(dbResult, JDBCConstants.PRECISION),
            JDBCUtils.safeGetInt(dbResult, JDBCConstants.MINIMUM_SCALE),
            JDBCUtils.safeGetInt(dbResult, JDBCConstants.MAXIMUM_SCALE));
    }
}
