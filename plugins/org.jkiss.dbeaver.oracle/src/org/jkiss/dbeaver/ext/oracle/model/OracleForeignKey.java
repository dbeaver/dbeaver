/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.OracleConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCForeignKey;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.properties.IPropertyValueListProvider;

/**
 * OracleForeignKey
 */
public class OracleForeignKey extends OracleConstraint implements DBSForeignKey
{
    static final Log log = LogFactory.getLog(OracleForeignKey.class);

    private Object referencedKey;
    private DBSConstraintModifyRule deleteRule;

    public OracleForeignKey(
        OracleTable table,
        String name,
        OracleConstants.ObjectStatus status,
        Object referencedKey,
        DBSConstraintModifyRule deleteRule,
        boolean persisted)
    {
        super(table, name, DBSConstraintType.FOREIGN_KEY, null, status, persisted);
        this.referencedKey = referencedKey;
        this.deleteRule = deleteRule;
    }

    boolean resolveLazyReference(DBRProgressMonitor monitor)
    {
        if (referencedKey instanceof OracleConstraint) {
            return true;
        } else if (referencedKey instanceof OracleLazyReference) {
            try {
                final OracleLazyReference lazyReference = (OracleLazyReference) referencedKey;
                OracleSchema refSchema = getTable().getDataSource().getSchema(monitor, lazyReference.schemaName);
                if (refSchema == null) {
                    log.warn("Referenced schema '" + lazyReference.schemaName + "' not found for foreign key '" + getName() + "'");
                    return false;
                }
                referencedKey = refSchema.getConstraintCache().getObject(monitor, getDataSource(), lazyReference.objectName);
                if (referencedKey == null) {
                    log.warn("Referenced constraint '" + lazyReference.objectName + "' not found in schema '" + lazyReference.schemaName + "'");
                    return false;
                }
                return true;
            } catch (DBException e) {
                log.error(e);
            }
        }
        return false;
    }

    @Property(name = "Ref Table", viewable = true, order = 3)
    public OracleTable getReferencedTable()
    {
        return getReferencedKey().getTable();
    }

    @Property(id = "reference", name = "Ref Object", viewable = true, order = 4)
    public OracleConstraint getReferencedKey()
    {
        return (OracleConstraint)referencedKey;
    }

    @Property(name = "On Delete", viewable = true, editable = true, listProvider = ConstraintModifyRuleListProvider.class, order = 5)
    public DBSConstraintModifyRule getDeleteRule()
    {
        return deleteRule;
    }

    public void setDeleteRule(DBSConstraintModifyRule deleteRule)
    {
        this.deleteRule = deleteRule;
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
