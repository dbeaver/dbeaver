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
package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

/**
 * Column value binding info
 */
public class DBDAttributeBinding {
    private final DBCAttributeMetaData attribute;
    private final DBDValueHandler valueHandler;
    private DBSEntityAttribute tableColumn;
    private DBDValueLocator valueLocator;

    public DBDAttributeBinding(DBCAttributeMetaData attribute, DBDValueHandler valueHandler) {
        this.attribute = attribute;
        this.valueHandler = valueHandler;
    }

    public String getColumnName()
    {
        return attribute.getName();
    }

    public int getColumnIndex()
    {
        return attribute.getIndex();
    }

    public DBCAttributeMetaData getAttribute() {
        return attribute;
    }

    public DBDValueHandler getValueHandler() {
        return valueHandler;
    }

    public DBSEntityAttribute getTableColumn()
    {
        return tableColumn;
    }

    public DBDValueLocator getValueLocator() {
        return valueLocator;
    }

    public void initValueLocator(DBSEntityAttribute tableColumn, DBDValueLocator valueLocator) {
        this.tableColumn = tableColumn;
        this.valueLocator = valueLocator;
    }
}
