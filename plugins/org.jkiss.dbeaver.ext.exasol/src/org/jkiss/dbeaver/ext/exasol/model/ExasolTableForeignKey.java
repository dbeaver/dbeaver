/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;

import java.sql.ResultSet;
import java.util.List;


/**
 * @author Karl
 */
public class ExasolTableForeignKey extends JDBCTableConstraint<ExasolTable> implements DBSTableForeignKey {

    private ExasolTable refTable;

    private Boolean enabled;
    private List<ExasolTableKeyColumn> columns;

    private ExasolTableUniqueKey referencedKey;


    // -----------------
    // Constructor
    // -----------------

    public ExasolTableForeignKey(DBRProgressMonitor monitor, ExasolTable exasolTable, ResultSet dbResult) throws DBException {
        super(exasolTable, JDBCUtils.safeGetString(dbResult, "CONSTRAINT_NAME"), null, DBSEntityConstraintType.FOREIGN_KEY, true);

        String refSchemaName = JDBCUtils.safeGetString(dbResult, "REFERENCED_SCHEMA");
        String refTableName = JDBCUtils.safeGetString(dbResult, "REFERENCED_TABLE");
        String constName = JDBCUtils.safeGetString(dbResult, "REF_PK_NAME");
        refTable = ExasolUtils.findTableBySchemaNameAndName(monitor, exasolTable.getDataSource(), refSchemaName, refTableName);

        enabled = JDBCUtils.safeGetBoolean(dbResult, "CONSTRAINT_ENABLED");
        referencedKey = refTable.getConstraint(monitor, constName);

    }

    public ExasolTableForeignKey(ExasolTable exasolTable, ExasolTableUniqueKey referencedKey, Boolean enabled) {
        super(exasolTable, null, null, DBSEntityConstraintType.FOREIGN_KEY, true);
        this.referencedKey = referencedKey;
        this.enabled = enabled;

    }

    // -----------------
    // Business Contract
    // -----------------

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return getTable().getDataSource();
    }

    @Override
    public ExasolTable getAssociatedEntity() {
        return refTable;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(), getTable().getContainer(), getTable(), this);
    }

    @NotNull
    @Override
    public DBSForeignKeyModifyRule getUpdateRule() {
        if (this.enabled) {
            return DBSForeignKeyModifyRule.RESTRICT;
        } else {
            return DBSForeignKeyModifyRule.NO_ACTION;
        }
    }

    @NotNull
    @Override
    public DBSForeignKeyModifyRule getDeleteRule() {
        if (this.enabled) {
            return DBSForeignKeyModifyRule.RESTRICT;
        } else {
            return DBSForeignKeyModifyRule.NO_ACTION;
        }
    }

    // -----------------
    // Columns
    // -----------------
    @Override
    public List<? extends DBSEntityAttributeRef> getAttributeReferences(DBRProgressMonitor monitor) throws DBException {
        return columns;
    }

    public void setColumns(List<ExasolTableKeyColumn> columns) {
        this.columns = columns;
    }

    // -----------------
    // Properties
    // -----------------

    @Property(viewable = true, order = 3)
    public ExasolTable getReferencedTable() {
        return refTable;
    }

    @Nullable
    @NotNull
    @Override
    @Property(id = "reference", viewable = false)
    public ExasolTableUniqueKey getReferencedConstraint() {
        return referencedKey;
    }

    @Property(viewable = true, editable = true)
    public Boolean getEnabled() {
        return this.enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }


}
