/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.db2.editors;

import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.struct.DBSDataType;

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