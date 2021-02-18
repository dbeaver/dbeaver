/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.debug.internal;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.debug.DBGResolver;
import org.jkiss.dbeaver.ext.postgresql.debug.core.PostgreSqlDebugCore;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.HashMap;
import java.util.Map;

public class PostgreResolver implements DBGResolver {

    private final DBPDataSourceContainer dataSource;

    public PostgreResolver(DBPDataSourceContainer dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public DBSObject resolveObject(Map<String, Object> context, Object identifier, DBRProgressMonitor monitor)
            throws DBException {
        return PostgreSqlDebugCore.resolveFunction(monitor, dataSource, context);
    }

    @Override
    public Map<String, Object> resolveContext(DBSObject databaseObject) {
        HashMap<String, Object> context = new HashMap<>();
        if (databaseObject instanceof PostgreProcedure) {
            PostgreSqlDebugCore.saveFunction((PostgreProcedure)databaseObject, context);
        }
        return context;
    }

}
