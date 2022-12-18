/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.generator;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataTypeProvider;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class SQLGeneratorColumnNameType extends SQLGenerator<DBSEntityAttribute> {

    @Override
    public boolean isDDLOption() {
        return true;
    }

    @Override
    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        super.run(monitor);
    }

    @Override
    public void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, DBSEntityAttribute object) throws DBException {
        if (sql.length() > 0) {
            if (isCompactSQL()) {
                sql.append(", ");
            } else {
                sql.append(",\n");
            }
        }        
        String columnName = DBUtils.getQuotedIdentifier(object.getDataSource(), object.getName());
        sql.append(columnName);
        final String typeName = object.getTypeName();
        sql.append(' ').append(typeName);
        DBPDataTypeProvider dataTypeProvider = DBUtils.getParentOfType(DBPDataTypeProvider.class, object);
        if (dataTypeProvider != null) {
            DBSDataType dataType = dataTypeProvider.getLocalDataType(typeName);
            if (dataType != null) {
                DBPDataKind dataKind = dataType.getDataKind();
                String modifiers = SQLUtils.getColumnTypeModifiers(object.getDataSource(), object, typeName, dataKind);
                if (modifiers != null) {
                    sql.append(modifiers);
                }
            } 
        }        
    }

    @Override
    protected void addOptions(Map<String, Object> options) {
        super.addOptions(options);
    }
}