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
package org.jkiss.dbeaver.ext.exasol.editors;

import org.jkiss.dbeaver.ext.exasol.model.ExasolTableColumn;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.struct.DBSDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides a list of Exasol Table Column for Exasol Table editors
 *
 * @author Karl Griesser
 */
public class ExasolColumnDataTypeListProvider implements IPropertyValueListProvider<ExasolTableColumn> {

    @Override
    public boolean allowCustomValue() {
        return false;
    }

    @Override
    public Object[] getPossibleValues(ExasolTableColumn column) {
        List<DBSDataType> dataTypes = new ArrayList<DBSDataType>(column.getTable().getDataSource().getLocalDataTypes());
        if (!dataTypes.contains(column.getDataType())) {
            dataTypes.add(column.getDataType());
        }
        return dataTypes.toArray(new DBSDataType[dataTypes.size()]);
    }
}