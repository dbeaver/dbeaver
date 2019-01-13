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
package org.jkiss.dbeaver.ext.mssql.model.generic;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.model.DBPOverloadedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.SQLException;

/**
* SQL Server table
*/
public class SQLServerGenericTable extends GenericTable implements DBPOverloadedObject {

    public SQLServerGenericTable(GenericStructContainer container, String tableName, String tableType, JDBCResultSet dbResult) {
        super(container, tableName, tableType, dbResult);
    }

    @Override
    public String getOverloadedName() {
        //return getSchema().getName() + "." + getName();
        return getName();
    }

    @Override
    public String getDescription() {
        return super.getDescription();
    }

    @Property(viewable = true, multiline = true, order = 100)
    public String getDescription(DBRProgressMonitor monitor) throws DBException {
        String description = getDescription();
        if (description != null || !isSqlServer()) {
            return description;
        }
        // Query row count
        try (JDBCSession session = DBUtils.openUtilSession(monitor, this, "Read table description")) {
            DBSObject defaultDatabase = getDataSource().getDefaultObject();
            boolean switchSchema = getCatalog() != null && defaultDatabase != null && defaultDatabase != getCatalog();
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                (switchSchema ? "USE " + DBUtils.getQuotedIdentifier(getCatalog()) + ";\n" : "") +
                "SELECT cast(value as varchar(8000)) as value " +
                    "FROM fn_listextendedproperty ('MS_DESCRIPTION', 'schema', ?, 'table', ?, default, default);\n" +
                    (switchSchema ? "USE "+ DBUtils.getQuotedIdentifier(defaultDatabase) + ";\n" : "")))
            {
                dbStat.setString(1, getSchema().getName());
                dbStat.setString(2, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        description = dbResult.getString(1);
                    } else {
                        description = "";
                    }
                }

            } catch (SQLException e) {
                throw new DBException(e, getDataSource());
            }
        }

        setDescription(description);
        return description;
    }

    @Override
    public SQLServerGenericDataSource getDataSource() {
        return (SQLServerGenericDataSource)super.getDataSource();
    }

    private boolean isSqlServer() {
        return ((SQLServerMetaModel)getDataSource().getMetaModel()).isSqlServer();
    }

    @Override
    protected boolean isTruncateSupported() {
        return true;
    }
}
