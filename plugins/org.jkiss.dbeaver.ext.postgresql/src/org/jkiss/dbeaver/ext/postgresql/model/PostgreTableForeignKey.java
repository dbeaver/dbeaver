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
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;

import java.util.ArrayList;
import java.util.Collection;
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
    private final PostgreTableBase refTable;

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
        refTable = table.getDatabase().findTable(
            monitor,
            table.getSchema().getObjectId(),
            refTableId);
        if (refTable == null) {
            log.warn("Reference table " + refTableId + " not found");
            return;
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
    public PostgreTableBase getAssociatedEntity() {
        return refTable;
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

    void cacheAttributes(DBRProgressMonitor monitor, List<? extends PostgreTableConstraintColumn> children, boolean secondPass) {
        if (!secondPass) {
            return;
        }
        columns.clear();
        columns.addAll((Collection<? extends PostgreTableForeignKeyColumn>) children);

        final List<PostgreAttribute> lst = new ArrayList<>(children.size());
        for (PostgreTableConstraintColumn c : children) lst.add(((PostgreTableForeignKeyColumn)c).getReferencedColumn());
        try {
            refConstraint = DBUtils.findEntityConstraint(monitor, refTable, lst);
        } catch (DBException e) {
            log.error("Error finding reference constraint", e);
        }
        if (refConstraint == null) {
            log.warn("Can't find reference constraint for foreign key '" + getFullQualifiedName() + "'");
        }
    }

}
