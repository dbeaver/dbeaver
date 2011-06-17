/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.OracleConstants;
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
        OracleTable oracleTable,
        String name,
        OracleConstants.ObjectStatus status,
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

        String refOwner = JDBCUtils.safeGetString(dbResult, "R_OWNER");
        String refTableName = JDBCUtils.safeGetString(dbResult, "R_TABLE_NAME");
        String refName = JDBCUtils.safeGetString(dbResult, "R_CONSTRAINT_NAME");
        String deleteRuleName = JDBCUtils.safeGetString(dbResult, "DELETE_RULE");
        OracleSchema refSchema = getTable().getDataSource().getSchema(monitor, refOwner);
        if (refSchema == null) {
            log.warn("Referenced schema '" + refOwner + "' not found for foreign key '" + getName() + "'");
        } else {
            OracleTable refTable = refSchema.getTable(monitor, refTableName);
            if (refTable == null) {
                log.warn("Referenced table '" + refTable + "' not found in schema '" + refOwner + "' for foreign key '" + getName() + "'");
            } else {
                referencedKey = refTable.getConstraint(monitor, refName);
                if (referencedKey == null) {
                    log.warn("Referenced constraint '" + refName + "' not found in table '" + refTable.getFullQualifiedName() + "'");
                }
            }
        }

        this.deleteRule = "CASCADE".equals(deleteRuleName) ? DBSConstraintModifyRule.CASCADE : DBSConstraintModifyRule.NO_ACTION;
    }

    @Property(name = "Ref Table", viewable = true, order = 3)
    public OracleTable getReferencedTable()
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
