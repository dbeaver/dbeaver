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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreTableConstraint
 */
public class PostgreTableConstraint extends PostgreTableConstraintBase {

    private List<PostgreTableConstraintColumn> columns = new ArrayList<>();

    public PostgreTableConstraint(PostgreTableBase table, String name, DBSEntityConstraintType constraintType, JDBCResultSet resultSet) throws DBException {
        super(table, name, constraintType, resultSet);

        Object keyNumbers = JDBCUtils.safeGetArray(resultSet, "conkey");
        if (keyNumbers != null) {
            List<PostgreAttribute> attributes = table.getAttributes(resultSet.getSession().getProgressMonitor());
            assert attributes != null;
            int colCount = Array.getLength(keyNumbers);
            for (int i = 0; i < colCount; i++) {
                Number colNumber = (Number) Array.get(keyNumbers, i); // Column number - 1-based
                if (colNumber.intValue() <= 0 || colNumber.intValue() > attributes.size()) {
                    log.warn("Bad constraint attribute index: " + colNumber);
                } else {
                    PostgreAttribute attr = attributes.get(colNumber.intValue() - 1);
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

}
