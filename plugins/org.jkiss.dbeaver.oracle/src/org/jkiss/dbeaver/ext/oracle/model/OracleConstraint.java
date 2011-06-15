/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.ext.oracle.OracleConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCConstraint;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;

import java.util.ArrayList;
import java.util.List;

/**
 * GenericPrimaryKey
 */
public class OracleConstraint extends JDBCConstraint<OracleTable> {
    private String searchCondition;
    private OracleConstants.ObjectStatus status;
    private List<OracleConstraintColumn> columns;

    public OracleConstraint(OracleTable table, String name, DBSConstraintType constraintType, String searchCondition, OracleConstants.ObjectStatus status, boolean persisted)
    {
        super(table, name, null, constraintType, persisted);
        this.searchCondition = searchCondition;
        this.status = status;
    }

    public OracleDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Property(name = "Type", viewable = true, editable = true, valueTransformer = JDBCObjectNameCaseTransformer.class, order = 3)
    @Override
    public DBSConstraintType getConstraintType()
    {
        return constraintType;
    }

    @Property(name = "Condition", viewable = true, editable = true, order = 4)
    public String getSearchCondition()
    {
        return searchCondition;
    }

    @Property(name = "Status", viewable = true, editable = false, order = 5)
    public OracleConstants.ObjectStatus getStatus()
    {
        return status;
    }

    public List<OracleConstraintColumn> getColumns(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public void addColumn(OracleConstraintColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<OracleConstraintColumn>();
        }
        this.columns.add(column);
    }

    void setColumns(List<OracleConstraintColumn> columns)
    {
        this.columns = columns;
    }

    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getContainer(),
            getTable(),
            this);
    }

}
