/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.jdbc.dbc;

import org.jkiss.dbeaver.model.exec.DBCEntityIdentifier;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.util.ArrayList;
import java.util.List;

/**
 * JDBC Table MetaData
 */
public class JDBCTableIdentifier implements DBCEntityIdentifier {

    private DBSTableConstraint constraint;
    private DBSTableIndex index;
    private List<JDBCColumnMetaData> columns;
    private List<DBSTableColumn> tableColumns;

    public JDBCTableIdentifier(DBRProgressMonitor monitor, DBSTableConstraint constraint, List<JDBCColumnMetaData> columns)
    {
        this.constraint = constraint;
        this.columns = columns;
        this.tableColumns = new ArrayList<DBSTableColumn>();
        for (DBSTableConstraintColumn cColumn : constraint.getColumns(monitor)) {
            tableColumns.add(cColumn.getAttribute());
        }
    }

    public JDBCTableIdentifier(DBRProgressMonitor monitor, DBSTableIndex index, List<JDBCColumnMetaData> columns)
    {
        this.index = index;
        this.columns = columns;
        this.tableColumns = new ArrayList<DBSTableColumn>();
        for (DBSTableIndexColumn cColumn : index.getColumns(monitor)) {
            tableColumns.add(cColumn.getTableColumn());
        }
    }

//    public JDBCTableIdentifier(DBRProgressMonitor monitor, DBSObject object, DBSEntityReferrer referrer)
//    {
//        this.index = (DBSTableIndex) object;
//        this.columns = columns;
//        this.tableColumns = new ArrayList<DBSTableColumn>();
//        for (DBSEntityAttributeRef cColumn : referrer.getAttributeReferences(monitor)) {
//            tableColumns.add(cColumn.getAttribute());
//        }
//    }

    @Override
    public DBSTableConstraint getConstraint()
    {
        return constraint;
    }

    @Override
    public DBSTableIndex getIndex()
    {
        return index;
    }

    @Override
    public List<JDBCColumnMetaData> getResultSetColumns()
    {
        return columns;
    }

    @Override
    public List<? extends DBSTableColumn> getTableColumns()
    {
        return tableColumns;
    }

}