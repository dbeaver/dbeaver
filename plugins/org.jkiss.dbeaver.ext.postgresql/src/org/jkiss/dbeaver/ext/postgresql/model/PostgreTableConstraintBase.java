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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreTableConstraintBase
 */
public class PostgreTableConstraintBase extends JDBCTableConstraint<PostgreTableBase> implements PostgreObject {
    static final Log log = Log.getLog(PostgreTableConstraintBase.class);

    private int oid;
    private List<PostgreTableConstraintColumn> columns = new ArrayList<>();

    public PostgreTableConstraintBase(PostgreTableBase table, String name, JDBCResultSet resultSet) throws DBException {
        super(table, name, null, null, true);

        this.oid = JDBCUtils.safeGetInt(resultSet, "oid");
        Object keyNumbers = JDBCUtils.safeGetArray(resultSet, "conkey");
        if (keyNumbers != null) {
            List<PostgreAttribute> attributes = table.getAttributes(resultSet.getSession().getProgressMonitor());
            int colCount = Array.getLength(keyNumbers);
            for (int i = 0; i < colCount; i++) {
                Number colNumber = (Number) Array.get(keyNumbers, i);
                if (colNumber.intValue() < 0 || colNumber.intValue() >= attributes.size()) {
                    log.warn("Bad constraint attribute index: " + colNumber);
                } else {
                    PostgreAttribute attr = attributes.get(colNumber.intValue());
                    PostgreTableConstraintColumn cCol = new PostgreTableConstraintColumn(this, attr, i);
                    columns.add(cCol);
                }
            }
        }
    }

    @Override
    public List<PostgreTableConstraintColumn> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public void addColumn(PostgreTableConstraintColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<>();
        }
        this.columns.add(column);
    }

    void setColumns(List<PostgreTableConstraintColumn> columns)
    {
        this.columns = columns;
    }

    @NotNull
    @Override
    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getContainer(),
            getTable(),
            this);
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Override
    public PostgreDatabase getDatabase() {
        return getParentObject().getDatabase();
    }

    @Override
    public int getObjectId() {
        return oid;
    }
}
