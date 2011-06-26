/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCForeignKey;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.properties.IPropertyValueListProvider;

import java.sql.ResultSet;

/**
 * OracleForeignKey
 */
public class OracleForeignKey extends OracleConstraint implements DBSForeignKey
{
    private OracleConstraint referencedKey;
    private DBSConstraintModifyRule deleteRule;

    public OracleForeignKey(
        OracleTableBase oracleTable,
        String name,
        OracleObjectStatus status,
        OracleConstraint referencedKey,
        DBSConstraintModifyRule deleteRule)
    {
        super(oracleTable, name, DBSConstraintType.FOREIGN_KEY, null, status);
        this.referencedKey = referencedKey;
        this.deleteRule = deleteRule;
    }

    public OracleForeignKey(
        DBRProgressMonitor monitor,
        OracleTable table,
        ResultSet dbResult)
        throws DBException
    {
        super(table, dbResult);

        String refName = JDBCUtils.safeGetString(dbResult, "R_CONSTRAINT_NAME");
        OracleTableBase refTable = OracleTableBase.findTable(
            monitor,
            table.getDataSource(),
            JDBCUtils.safeGetString(dbResult, "R_OWNER"),
            JDBCUtils.safeGetString(dbResult, "R_TABLE_NAME"));
        referencedKey = refTable.getConstraint(monitor, refName);
        if (referencedKey == null) {
            log.warn("Referenced constraint '" + refName + "' not found in table '" + refTable.getFullQualifiedName() + "'");
        }

        String deleteRuleName = JDBCUtils.safeGetString(dbResult, "DELETE_RULE");
        this.deleteRule = "CASCADE".equals(deleteRuleName) ? DBSConstraintModifyRule.CASCADE : DBSConstraintModifyRule.NO_ACTION;
    }

    @Property(name = "Ref Table", viewable = true, order = 3)
    public OracleTableBase getReferencedTable()
    {
        return referencedKey.getTable();
    }

    @Property(id = "reference", name = "Ref Object", viewable = true, order = 4)
    public OracleConstraint getReferencedKey()
    {
        return referencedKey;
    }

    @Property(name = "On Delete", viewable = true, editable = true, listProvider = ConstraintModifyRuleListProvider.class, order = 5)
    public DBSConstraintModifyRule getDeleteRule()
    {
        return deleteRule;
    }

    // Update rule is not supported by Oracle
    public DBSConstraintModifyRule getUpdateRule()
    {
        return DBSConstraintModifyRule.NO_ACTION;
    }

    public DBSForeignKeyColumn getColumn(DBRProgressMonitor monitor, DBSTableColumn tableColumn)
    {
        return (DBSForeignKeyColumn)super.getColumn(monitor, tableColumn);
    }

    public DBSEntity getAssociatedEntity()
    {
        return getReferencedTable();
    }

    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getContainer(),
            getTable(),
            this);
    }

    public static class ConstraintModifyRuleListProvider implements IPropertyValueListProvider<JDBCForeignKey> {

        public boolean allowCustomValue()
        {
            return false;
        }

        public Object[] getPossibleValues(JDBCForeignKey foreignKey)
        {
            return new DBSConstraintModifyRule[] {
                DBSConstraintModifyRule.NO_ACTION,
                DBSConstraintModifyRule.CASCADE,
                DBSConstraintModifyRule.RESTRICT,
                DBSConstraintModifyRule.SET_NULL,
                DBSConstraintModifyRule.SET_DEFAULT };
        }
    }
}
