/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject;
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
    private static final Log log = Log.getLog(PostgreTableForeignKey.class);

    public enum MatchType implements DBPNamedObject {
        f("FULL"),
        p("PARTIAL"),
        s("SIMPLE"),
        u("UNKNOWN");

        private final String title;

        MatchType(String title) {
            this.title = title;
        }

        @Override
        public String getName() {
            return title;
        }
    }

    private MatchType matchType;
    private DBSForeignKeyModifyRule updateRule;
    private DBSForeignKeyModifyRule deleteRule;
    private DBSEntityConstraint refConstraint;
    private final List<PostgreTableForeignKeyColumn> columns = new ArrayList<>();
    private final PostgreTableBase refTable;

    public PostgreTableForeignKey(
        @NotNull PostgreTableBase table,
        @NotNull String name,
        @NotNull JDBCResultSet resultSet) throws DBException
    {
        super(table, name, DBSEntityConstraintType.FOREIGN_KEY, resultSet);
        try {
            this.matchType = MatchType.valueOf(JDBCUtils.safeGetString(resultSet, "confmatchtype"));
        } catch (Throwable e) {
            log.debug("Error reading FK match type: " + e.getMessage());
        }
        this.updateRule = getRuleFromAction(JDBCUtils.safeGetString(resultSet, "confupdtype"));
        this.deleteRule = getRuleFromAction(JDBCUtils.safeGetString(resultSet, "confdeltype"));

        final DBRProgressMonitor monitor = resultSet.getSession().getProgressMonitor();
        final long refSchemaId = JDBCUtils.safeGetLong(resultSet, "refnamespace");
        final long refTableId = JDBCUtils.safeGetLong(resultSet, "confrelid");
        refTable = table.getDatabase().findTable(
            monitor,
            refSchemaId,
            refTableId);
        if (refTable == null) {
            throw new DBException("Reference table " + refTableId + " not found");
        }
    }

    public PostgreTableForeignKey(
        @NotNull PostgreTableBase table,
        @NotNull DBSEntityConstraint refConstraint,
        @NotNull DBSForeignKeyModifyRule deleteRule,
        @NotNull DBSForeignKeyModifyRule updateRule)
    {
        super(table, null, DBSEntityConstraintType.FOREIGN_KEY);
        this.refConstraint = refConstraint;
        this.refTable = (PostgreTableBase) refConstraint.getParentObject();
        this.matchType = MatchType.s;
        this.updateRule = updateRule;
        this.deleteRule = deleteRule;
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
    @Property(viewable = true, specific = true, order = 50)
    public PostgreTableBase getAssociatedEntity() {
        return refTable;
    }

    @Nullable
    @Override
    @Property(id = "reference", viewable = true, specific = true, order = 51)
    public DBSEntityConstraint getReferencedConstraint() {
        return refConstraint;
    }

    @Property(viewable = true, specific = true, order = 54)
    public MatchType getMatchType() {
        return matchType;
    }

    @NotNull
    @Override
    @Property(viewable = true, specific = true, order = 55)
    public DBSForeignKeyModifyRule getDeleteRule() {
        return deleteRule;
    }

    @NotNull
    @Override
    @Property(viewable = true, specific = true, order = 56)
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
        for (PostgreTableConstraintColumn c : children) {
            lst.add(((PostgreTableForeignKeyColumn)c).getReferencedColumn());
        }
        try {
            refConstraint = DBUtils.findEntityConstraint(monitor, refTable, lst);
        } catch (DBException e) {
            log.error("Error finding reference constraint", e);
        }
        if (refConstraint == null) {
            log.warn("Can't find reference constraint for foreign key '" + getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
        }
    }

    public void addColumn(PostgreTableForeignKeyColumn column) {
        this.columns.add(column);
    }
}
