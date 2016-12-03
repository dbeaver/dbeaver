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
import org.jkiss.dbeaver.ext.exasol.ExasolConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;

import java.sql.ResultSet;
import java.util.List;

/**
 * @author Karl Griesser
 */
public class ExasolTableUniqueKey extends JDBCTableConstraint<ExasolTable> implements DBSEntityReferrer {

    private String owner;
    private Boolean enabled;

    private List<ExasolTableKeyColumn> columns;


    // CONSTRUCTOR

    public ExasolTableUniqueKey(DBRProgressMonitor monitor, ExasolTable table, ResultSet dbResult, DBSEntityConstraintType type)
        throws DBException {
        super(table, JDBCUtils.safeGetString(dbResult, "CONSTRAINT_NAME"), null, type, true);
        this.owner = JDBCUtils.safeGetString(dbResult, "CONSTRAINT_OWNER");
        this.enabled = JDBCUtils.safeGetBoolean(dbResult, "CONSTRAINT_ENABLED");

    }

    public ExasolTableUniqueKey(ExasolTable exasolTable, DBSEntityConstraintType constraintType) {
        super(exasolTable, null, null, constraintType, false);
    }

    // -----------------
    // Business Contract
    // -----------------

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(), getTable().getContainer(), getTable(), this);
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return getTable().getDataSource();
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
    @Override
    @Property(viewable = true, editable = false, order = 2)
    public ExasolTable getTable() {
        return super.getTable();
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = false, order = 3)
    public DBSEntityConstraintType getConstraintType() {
        return super.getConstraintType();
    }

    @Nullable
    @Override
    @Property(viewable = false, editable = false, order = 4)
    public String getDescription() {
        return null;
    }

    @Property(viewable = false, editable = false, category = ExasolConstants.CAT_OWNER)
    public String getOwner() {
        return owner;
    }

    @Property(viewable = true, editable = false)
    public Boolean getEnabled() {
        return enabled;
    }

	public boolean hasColumn(ExasolTableColumn column)
	{
        if (this.columns != null) {
            for (ExasolTableKeyColumn constColumn : columns) {
                if (constColumn.getAttribute() == column) {
                    return true;
                }
            }
        }
        return false;
	}


}
