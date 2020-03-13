/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableForeignKey;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * OracleTableForeignKey
 */
public class OracleTableForeignKey extends OracleTableConstraintBase implements DBSTableForeignKey
{
    private static final Log log = Log.getLog(OracleTableForeignKey.class);

    private OracleTableConstraint referencedKey;
    private DBSForeignKeyModifyRule deleteRule;

    public OracleTableForeignKey(
        @NotNull OracleTableBase oracleTable,
        @Nullable String name,
        @Nullable OracleObjectStatus status,
        @Nullable OracleTableConstraint referencedKey,
        @NotNull DBSForeignKeyModifyRule deleteRule)
    {
        super(oracleTable, name, DBSEntityConstraintType.FOREIGN_KEY, status, false);
        this.referencedKey = referencedKey;
        this.deleteRule = deleteRule;
    }

    public OracleTableForeignKey(
        DBRProgressMonitor monitor,
        OracleTable table,
        ResultSet dbResult)
        throws DBException
    {
        super(
            table,
            JDBCUtils.safeGetString(dbResult, "CONSTRAINT_NAME"),
            DBSEntityConstraintType.FOREIGN_KEY,
            CommonUtils.notNull(
                CommonUtils.valueOf(OracleObjectStatus.class, JDBCUtils.safeGetStringTrimmed(dbResult, "STATUS")),
                OracleObjectStatus.ENABLED),
            true);

        String refName = JDBCUtils.safeGetString(dbResult, "R_CONSTRAINT_NAME");
        String refOwnerName = JDBCUtils.safeGetString(dbResult, "R_OWNER");
        String refTableName = JDBCUtils.safeGetString(dbResult, "R_TABLE_NAME");
        OracleTableBase refTable = OracleTableBase.findTable(
            monitor,
            table.getDataSource(),
            refOwnerName,
            refTableName);
        if (refTable == null) {
            log.warn("Referenced table '" + DBUtils.getSimpleQualifiedName(refOwnerName, refTableName) + "' not found");
        } else {
            referencedKey = refTable.getConstraint(monitor, refName);
            if (referencedKey == null) {
                log.warn("Referenced constraint '" + refName + "' not found in table '" + refTable.getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
                referencedKey = new OracleTableConstraint(refTable, "refName", DBSEntityConstraintType.UNIQUE_KEY, null, OracleObjectStatus.ERROR);
            }
        }

        String deleteRuleName = JDBCUtils.safeGetString(dbResult, "DELETE_RULE");
        this.deleteRule = "CASCADE".equals(deleteRuleName) ? DBSForeignKeyModifyRule.CASCADE : DBSForeignKeyModifyRule.NO_ACTION;
    }

    @Property(viewable = true, order = 3)
    public OracleTableBase getReferencedTable()
    {
        return referencedKey == null ? null : referencedKey.getTable();
    }

    @Nullable
    @Override
    @Property(id = "reference", viewable = true, order = 4)
    public OracleTableConstraint getReferencedConstraint()
    {
        return referencedKey;
    }

    public void setReferencedConstraint(OracleTableConstraint referencedKey) {
        this.referencedKey = referencedKey;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, listProvider = ConstraintModifyRuleListProvider.class, order = 5)
    public DBSForeignKeyModifyRule getDeleteRule()
    {
        return deleteRule;
    }

    public void setDeleteRule(DBSForeignKeyModifyRule deleteRule) {
        this.deleteRule = deleteRule;
    }

    // Update rule is not supported by Oracle
    @NotNull
    @Override
    public DBSForeignKeyModifyRule getUpdateRule()
    {
        return DBSForeignKeyModifyRule.NO_ACTION;
    }

    @Override
    public OracleTableBase getAssociatedEntity()
    {
        return getReferencedTable();
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getContainer(),
            getTable(),
            this);
    }

    public static class ConstraintModifyRuleListProvider implements IPropertyValueListProvider<JDBCTableForeignKey> {

        @Override
        public boolean allowCustomValue()
        {
            return false;
        }

        @Override
        public Object[] getPossibleValues(JDBCTableForeignKey foreignKey)
        {
            return new DBSForeignKeyModifyRule[] {
                DBSForeignKeyModifyRule.NO_ACTION,
                DBSForeignKeyModifyRule.CASCADE,
                DBSForeignKeyModifyRule.RESTRICT,
                DBSForeignKeyModifyRule.SET_NULL,
                DBSForeignKeyModifyRule.SET_DEFAULT };
        }
    }
}
