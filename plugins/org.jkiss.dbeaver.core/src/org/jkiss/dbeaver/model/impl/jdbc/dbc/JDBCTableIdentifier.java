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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCEntityIdentifier;
import org.jkiss.dbeaver.model.exec.DBCEntityMetaData;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * JDBC Table MetaData
 */
public class JDBCTableIdentifier implements DBCEntityIdentifier {

    private DBSEntityReferrer referrer;

    //private DBSTableIndex index;
    private List<DBCAttributeMetaData> columns;
    private List<DBSEntityAttribute> tableColumns;

    public JDBCTableIdentifier(DBRProgressMonitor monitor, DBSEntityReferrer referrer, JDBCTableMetaData metaData) throws DBException
    {
        this.referrer = referrer;
        reloadAttributes(monitor, metaData);
    }

    public void reloadAttributes(DBRProgressMonitor monitor, DBCEntityMetaData metaData) throws DBException
    {
        this.columns = new ArrayList<DBCAttributeMetaData>();
        this.tableColumns = new ArrayList<DBSEntityAttribute>();
        for (DBSEntityAttributeRef cColumn : referrer.getAttributeReferences(monitor)) {
            DBCAttributeMetaData rsColumn = metaData.getColumnMetaData(monitor, cColumn.getAttribute());
            if (rsColumn != null) {
                columns.add(rsColumn);
                tableColumns.add(cColumn.getAttribute());
            }
        }
    }

    public DBSEntityReferrer getReferrer()
    {
        return referrer;
    }

    @Override
    public Collection<DBCAttributeMetaData> getResultSetColumns()
    {
        return columns;
    }

    @Override
    public Collection<? extends DBSEntityAttribute> getAttributes()
    {
        return tableColumns;
    }

}