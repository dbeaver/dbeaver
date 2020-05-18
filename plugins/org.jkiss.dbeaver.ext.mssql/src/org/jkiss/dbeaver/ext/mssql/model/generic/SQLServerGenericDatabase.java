/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mssql.model.generic;

import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;

/**
* SQL Server database
*/
public class SQLServerGenericDatabase extends GenericCatalog {

    SQLServerGenericDatabase(GenericDataSource dataSource, String catalogName) {
        super(dataSource, catalogName);
    }
/*
    @Override
    public Collection<GenericSchema> getSchemas(DBRProgressMonitor monitor) throws DBException {
        // Do not read schemas
        return null;
    }


    @Override
    public Collection<GenericTable> getTables(DBRProgressMonitor monitor) throws DBException {
        // Read all tables from all schemas
        List<GenericTable> allTables = new ArrayList<>();
        for (GenericSchema schema : super.getSchemas(monitor)) {
            Collection<GenericTable> tables = schema.getTables(monitor);
            if (!CommonUtils.isEmpty(tables)) {
                allTables.addAll(tables);
            }
        }
        return allTables;
    }
*/
}
