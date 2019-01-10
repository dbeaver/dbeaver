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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostgreTableForeign
 */
public class PostgreTableForeign extends PostgreTable implements DBPForeignObject, DBPImageProvider
{
    private long foreignServerId;
    private String[] foreignOptions;

    public PostgreTableForeign(PostgreSchema catalog)
    {
        super(catalog);
    }

    public PostgreTableForeign(
        PostgreSchema catalog,
        ResultSet dbResult)
    {
        super(catalog, dbResult);
    }

    @Property(viewable = false, order = 200)
    public PostgreForeignServer getForeignServer(DBRProgressMonitor monitor) throws DBException {
        readForeignInfo(monitor);
        return PostgreUtils.getObjectById(monitor, getDatabase().foreignServerCache, getDatabase(), foreignServerId);
    }

    @Property(viewable = false, order = 201)
    public String[] getForeignOptions(DBRProgressMonitor monitor) throws DBException {
        readForeignInfo(monitor);
        return foreignOptions;
    }

    private void readForeignInfo(DBRProgressMonitor monitor) throws DBException {
        if (foreignServerId > 0) {
            return;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read foreign table info")) {
            try (JDBCPreparedStatement stat = session.prepareStatement("SELECT * FROM pg_catalog.pg_foreign_table WHERE ftrelid=?")) {
                stat.setLong(1, getObjectId());
                try (JDBCResultSet result = stat.executeQuery()) {
                    if (result.next()) {
                        foreignServerId = JDBCUtils.safeGetLong(result, "ftserver");
                        foreignOptions = JDBCUtils.safeGetArray(result, "ftoptions");
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, getDataSource());
        }
    }

    @Nullable
    @Override
    public DBPImage getObjectImage() {
        return DBIcon.TREE_TABLE_LINK;
    }

    @Override
    public boolean isForeignObject() {
        return true;
    }
}
