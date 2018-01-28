/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.DBPOverloadedObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;

/**
* SQL Server table
*/
public abstract class MSSQLTable extends JDBCTable<MSSQLDataSource, MSSQLSchema> implements DBPOverloadedObject {

    public MSSQLTable(MSSQLSchema schema, String tableName) {
        super(schema, tableName, true);
    }

    public MSSQLSchema getSchema() {
        return getContainer();
    }

    @Override
    public String getOverloadedName() {
        //return getSchema().getName() + "." + getName();
        return getName();
    }
}
