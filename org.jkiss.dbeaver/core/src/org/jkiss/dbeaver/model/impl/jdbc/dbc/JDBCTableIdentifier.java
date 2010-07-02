/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.dbc;

import org.jkiss.dbeaver.model.dbc.DBCTableIdentifier;
import org.jkiss.dbeaver.model.struct.DBSConstraint;
import org.jkiss.dbeaver.model.struct.DBSIndex;

import java.util.List;

/**
 * JDBC Table MetaData
 */
public class JDBCTableIdentifier implements DBCTableIdentifier {

    private DBSConstraint constraint;
    private DBSIndex index;
    private List<JDBCColumnMetaData> columns;

    public JDBCTableIdentifier(DBSConstraint constraint, List<JDBCColumnMetaData> columns)
    {
        this.constraint = constraint;
        this.columns = columns;
    }

    public JDBCTableIdentifier(DBSIndex index, List<JDBCColumnMetaData> columns)
    {
        this.index = index;
        this.columns = columns;
    }

    public DBSConstraint getConstraint()
    {
        return constraint;
    }

    public DBSIndex getIndex()
    {
        return index;
    }

    public List<JDBCColumnMetaData> getResultSetColumns()
    {
        return columns;
    }

}