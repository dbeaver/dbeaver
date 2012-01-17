/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCConstraint;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * OracleConstraint
 */
public class OracleConstraint extends JDBCConstraint<OracleTableBase> {

    static final Log log = LogFactory.getLog(OracleConstraint.class);

    private String searchCondition;
    private OracleObjectStatus status;
    private List<OracleConstraintColumn> columns;

    public OracleConstraint(OracleTableBase oracleTable, String name, DBSEntityConstraintType constraintType, String searchCondition, OracleObjectStatus status)
    {
        super(oracleTable, name, null, constraintType, false);
        this.searchCondition = searchCondition;
        this.status = status;
    }

    public OracleConstraint(OracleTableBase table, ResultSet dbResult)
    {
        super(
            table,
            JDBCUtils.safeGetString(dbResult, "CONSTRAINT_NAME"),
            null,
            getConstraintType(JDBCUtils.safeGetString(dbResult, "CONSTRAINT_TYPE")),
            true);
        this.searchCondition = JDBCUtils.safeGetString(dbResult, "SEARCH_CONDITION");
        this.status = CommonUtils.valueOf(OracleObjectStatus.class, JDBCUtils.safeGetStringTrimmed(dbResult, "STATUS"));
    }

    public OracleDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Property(name = "Type", viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 3)
    @Override
    public DBSEntityConstraintType getConstraintType()
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

    public static DBSEntityConstraintType getConstraintType(String code)
    {
        if ("C".equals(code)) {
            return DBSEntityConstraintType.CHECK;
        } else if ("P".equals(code)) {
            return DBSEntityConstraintType.PRIMARY_KEY;
        } else if ("U".equals(code)) {
            return DBSEntityConstraintType.UNIQUE_KEY;
        } else if ("R".equals(code)) {
            return DBSEntityConstraintType.FOREIGN_KEY;
        } else if ("V".equals(code)) {
            return OracleView.CONSTRAINT_WITH_CHECK_OPTION;
        } else if ("O".equals(code)) {
            return OracleView.CONSTRAINT_WITH_READ_ONLY;
        } else {
            log.debug("Unsupported constraint type: " + code);
            return DBSEntityConstraintType.CHECK;
        }
    }
}
