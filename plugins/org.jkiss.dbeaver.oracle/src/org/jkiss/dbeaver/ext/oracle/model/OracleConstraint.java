/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCConstraint;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * OracleConstraint
 */
public class OracleConstraint extends JDBCConstraint<OracleTable> {

    static final Log log = LogFactory.getLog(OracleConstraint.class);

    private String searchCondition;
    private OracleObjectStatus status;
    private List<OracleConstraintColumn> columns;

    public OracleConstraint(OracleTable oracleTable, String name, DBSConstraintType constraintType, String searchCondition, OracleObjectStatus status)
    {
        super(oracleTable, name, null, constraintType, false);
        this.searchCondition = searchCondition;
        this.status = status;
    }

    public OracleConstraint(OracleTable table, ResultSet dbResult)
    {
        super(
            table,
            JDBCUtils.safeGetString(dbResult, "CONSTRAINT_NAME"),
            null,
            getConstraintType(JDBCUtils.safeGetString(dbResult, "CONSTRAINT_TYPE")),
            true);
        this.searchCondition = JDBCUtils.safeGetString(dbResult, "SEARCH_CONDITION");
        final String constraintStatus = JDBCUtils.safeGetString(dbResult, "STATUS");
        this.status = CommonUtils.isEmpty(constraintStatus) ?
            null :
            OracleObjectStatus.valueOf(constraintStatus);
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
    public OracleObjectStatus getStatus()
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

    public static DBSConstraintType getConstraintType(String type)
    {
        if ("C".equals(type)) {
            return DBSConstraintType.CHECK;
        } else if ("P".equals(type)) {
            return DBSConstraintType.PRIMARY_KEY;
        } else if ("U".equals(type)) {
            return DBSConstraintType.UNIQUE_KEY;
        } else if ("К".equals(type)) {
            return DBSConstraintType.FOREIGN_KEY;
        } else {
            log.debug("Unsupported constraint type: " + type);
            return DBSConstraintType.CHECK;
        }
    }
}
