/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.Date;

/**
 * DB Link
 */
public class OracleDBLink extends OracleSchemaObject {

    private String userName;
    private String host;
    private Date created;

    protected OracleDBLink(DBRProgressMonitor progressMonitor, OracleSchema schema, ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "DB_LINK"), true);
        this.userName = JDBCUtils.safeGetString(dbResult, "USERNAME");
        this.host = JDBCUtils.safeGetString(dbResult, "HOST");
        this.created = JDBCUtils.safeGetTimestamp(dbResult, "CREATED");

    }

    @Property(viewable = true, editable = true, order = 2)
    public String getUserName()
    {
        return userName;
    }

    @Property(viewable = true, editable = true, order = 3)
    public String getHost()
    {
        return host;
    }

    @Property(viewable = true, order = 4)
    public Date getCreated()
    {
        return created;
    }

    public static Object resolveObject(DBRProgressMonitor monitor, OracleSchema schema, String dbLink) throws DBException
    {
        if (CommonUtils.isEmpty(dbLink)) {
            return null;
        }
        final OracleDBLink object = schema.dbLinkCache.getObject(monitor, schema, dbLink);
        if (object == null) {
            log.warn("DB Link '" + dbLink + "' not found in schema '" + schema.getName() + "'");
            return dbLink;
        }
        return object;
    }
}
