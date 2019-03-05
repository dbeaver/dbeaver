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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.gis.GisAttribute;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.Collection;

/**
 * PostgreTableColumn
 */
public class PostgreTableColumn extends PostgreAttribute<PostgreTableBase> implements PostgrePrivilegeOwner, GisAttribute
{
    private static final Log log = Log.getLog(PostgreTableColumn.class);

    private int srid = -1;

    public PostgreTableColumn(PostgreTableBase table) {
        super(table);
    }

    public PostgreTableColumn(DBRProgressMonitor monitor, PostgreTableBase table, JDBCResultSet dbResult) throws DBException {
        super(monitor, table, dbResult);
    }

    @Override
    public PostgreSchema getSchema() {
        return getTable().getSchema();
    }

    @Override
    public PostgreRole getOwner(DBRProgressMonitor monitor) throws DBException {
        return getTable().getOwner(monitor);
    }

    @Override
    public Collection<PostgrePrivilege> getPrivileges(DBRProgressMonitor monitor, boolean includeNestedObjects) throws DBException {
        return PostgreUtils.extractPermissionsFromACL(monitor, this, getAcl());
    }

    @Override
    public String generateChangeOwnerQuery(String owner) {
        return null;
    }

    @Override
    public int getAttributeSRID(DBRProgressMonitor monitor) {
        if (srid == -1) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table inheritance info")) {
                Object sridValue = JDBCUtils.queryObject(session, "SELECT Find_SRID('" + getSchema().getName() + "', '" + getTable().getName() + "', '" + getName() + "')");
                if (sridValue instanceof Number) {
                    srid = ((Number) sridValue).intValue();
                }
            } catch (SQLException e) {
                log.debug("Error reading attribute " + getName() + " SRID", e);
                srid = 0;
            }
        }
        return srid;
    }
}
