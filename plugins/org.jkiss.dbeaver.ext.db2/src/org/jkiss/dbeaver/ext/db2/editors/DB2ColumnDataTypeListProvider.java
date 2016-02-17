/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.editors;

import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides a list of DB2 Table Column for DB2 Table editors
 * 
 * @author Denis Forveille
 * 
 */
public class DB2ColumnDataTypeListProvider implements IPropertyValueListProvider<DB2TableColumn> {

    @Override
    public boolean allowCustomValue()
    {
        return false;
    }

    @Override
    public Object[] getPossibleValues(DB2TableColumn column)
    {
        List<DBSDataType> dataTypes = new ArrayList<DBSDataType>(column.getTable().getDataSource().getLocalDataTypes());
        if (!dataTypes.contains(column.getDataType())) {
            dataTypes.add(column.getDataType());
        }
        return dataTypes.toArray(new DBSDataType[dataTypes.size()]);
    }
}