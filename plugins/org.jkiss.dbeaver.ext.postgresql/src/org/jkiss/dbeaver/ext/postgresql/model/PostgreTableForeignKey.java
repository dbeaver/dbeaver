/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreTableForeignKey
 */
public class PostgreTableForeignKey extends PostgreTableConstraintBase implements DBSTableForeignKey
{
    private DBSForeignKeyModifyRule updateRule;
    private DBSForeignKeyModifyRule deleteRule;
    private DBSEntityConstraint refConstraint;
    private final List<PostgreTableForeignKeyColumn> columns = new ArrayList<>();

    public PostgreTableForeignKey(
        PostgreTableBase table,
        String name,
        JDBCResultSet resultSet) throws DBException
    {
        super(table, name, DBSEntityConstraintType.FOREIGN_KEY, resultSet);
        updateRule = getRuleFromAction(JDBCUtils.safeGetString(resultSet, "confupdtype"));
        deleteRule = getRuleFromAction(JDBCUtils.safeGetString(resultSet, "confdeltype"));

        final DBRProgressMonitor monitor = resultSet.getSession().getProgressMonitor();
        final int refTableId = JDBCUtils.safeGetInt(resultSet, "confrelid");
        PostgreTableBase refTable = table.getDatabase().findTable(
            monitor,
            table.getSchema().getObjectId(),
            refTableId);
        if (refTable == null) {
            log.warn("Reference table " + refTableId + " not found");
            return;
        }
        Object keyNumbers = JDBCUtils.safeGetArray(resultSet, "conkey");
        Object keyRefNumbers = JDBCUtils.safeGetArray(resultSet, "confkey");
        if (keyNumbers != null && keyRefNumbers != null) {
            List<PostgreAttribute> attributes = table.getAttributes(monitor);
            List<PostgreAttribute> refAttributes = refTable.getAttributes(monitor);
            List<PostgreAttribute> matchedRefAttributes = new ArrayList<>();
            assert attributes != null && refAttributes != null;
            int colCount = Array.getLength(keyNumbers);
            for (int i = 0; i < colCount; i++) {
                Number colNumber = (Number) Array.get(keyNumbers, i); // Column number - 1-based
                Number colRefNumber = (Number) Array.get(keyRefNumbers, i);
                if (colNumber.intValue() <= 0 || colNumber.intValue() > attributes.size()) {
                    log.warn("Bad constraint attribute index: " + colNumber);
                } else {
                    PostgreAttribute attr = attributes.get(colNumber.intValue() - 1);
                    PostgreAttribute refAttr = refAttributes.get(colRefNumber.intValue() - 1);
                    PostgreTableForeignKeyColumn cCol = new PostgreTableForeignKeyColumn(this, attr, i, refAttr);
                    columns.add(cCol);
                    matchedRefAttributes.add(refAttr);
                }
            }
            refConstraint = DBUtils.findEntityConstraint(monitor, refTable, matchedRefAttributes);
            if (refConstraint == null) {
                log.warn("Can't find reference constraint for foreign key '" + getFullQualifiedName() + "'");
            }
        }
    }

    @NotNull
    private DBSForeignKeyModifyRule getRuleFromAction(String action) {
        switch (action) {
            case "a": return DBSForeignKeyModifyRule.NO_ACTION;
            case "r": return DBSForeignKeyModifyRule.RESTRICT;
            case "c": return DBSForeignKeyModifyRule.CASCADE;
            case "n": return DBSForeignKeyModifyRule.SET_NULL;
            case "d": return DBSForeignKeyModifyRule.SET_DEFAULT;
            default:
                log.warn("Unsupported constraint action: " + action);
                return DBSForeignKeyModifyRule.NO_ACTION;
        }
    }

    @Override
    @Property(viewable = true)
    public DBSEntity getAssociatedEntity() {
        return refConstraint == null ? null : refConstraint.getParentObject();
    }

    @NotNull
    @Override
    @Property(id = "reference", viewable = true)
    public DBSEntityConstraint getReferencedConstraint() {
        return refConstraint;
    }

    @NotNull
    @Override
    @Property(viewable = true)
    public DBSForeignKeyModifyRule getDeleteRule() {
        return deleteRule;
    }

    @NotNull
    @Override
    @Property(viewable = true)
    public DBSForeignKeyModifyRule getUpdateRule() {
        return updateRule;
    }

    @Nullable
    @Override
    public List<PostgreTableForeignKeyColumn> getAttributeReferences(DBRProgressMonitor monitor) throws DBException {
        return columns;
    }
}
