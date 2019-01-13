/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.sqlite.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SQLiteDataSource extends GenericDataSource {

    public SQLiteDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, GenericMetaModel metaModel)
        throws DBException
    {
        super(monitor, container, metaModel, new SQLiteSQLDialect());
    }

    @Override
    public DBSDataType getLocalDataType(String typeName) {
        // Resolve type name according to https://www.sqlite.org/datatype3.html
        typeName = typeName.toUpperCase(Locale.ENGLISH);
        SQLiteAffinity affinity;
        if (typeName.contains("INT")) {
            affinity = SQLiteAffinity.INTEGER;
        } else if (typeName.contains("CHAR") || typeName.contains("CLOB") || typeName.contains("TEXT")) {
            affinity = SQLiteAffinity.TEXT;
        } else if (typeName.contains("BLOB")) {
            affinity = SQLiteAffinity.BLOB;
        } else if (typeName.contains("REAL") || typeName.contains("FLOA") || typeName.contains("DOUB")) {
            affinity = SQLiteAffinity.REAL;
        } else {
            affinity = SQLiteAffinity.NUMERIC;
        }
        return super.getLocalDataType(affinity.name());
    }

    @Override
    protected boolean isConnectionReadOnlyBroken() {
        return true;
    }

    @Override
    protected Map<String, String> getInternalConnectionProperties(DBRProgressMonitor monitor, DBPDriver driver, String purpose, DBPConnectionConfiguration connectionInfo) throws DBCException {
        Map<String, String> connectionsProps = new HashMap<>();
        if (getContainer().isConnectionReadOnly()) {
            // Read-only prop
            connectionsProps.put("open_mode", "1");  //1 == readonly
        }
        return connectionsProps;
    }

}
