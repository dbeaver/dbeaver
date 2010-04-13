/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.dbc.DBCTableIdentifier;
import org.jkiss.dbeaver.model.struct.DBSConstraint;

import java.util.List;

/**
 * JDBC Table MetaData
 */
public class JDBCTableIdentifier implements DBCTableIdentifier {

    private DBSConstraint constraint;
    private List<JDBCColumnMetaData> columns;

    public JDBCTableIdentifier(DBSConstraint constraint, List<JDBCColumnMetaData> columns)
    {
        this.constraint = constraint;
        this.columns = columns;
    }

    public DBSConstraint getConstraint()
    {
        return constraint;
    }

    public List<JDBCColumnMetaData> getResultSetColumns()
    {
        return columns;
    }

}