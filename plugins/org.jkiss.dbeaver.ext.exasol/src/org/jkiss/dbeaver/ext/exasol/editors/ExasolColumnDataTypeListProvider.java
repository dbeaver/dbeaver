/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
        return dataTypes.toArray(new DBSDataType[0]);
    }
}