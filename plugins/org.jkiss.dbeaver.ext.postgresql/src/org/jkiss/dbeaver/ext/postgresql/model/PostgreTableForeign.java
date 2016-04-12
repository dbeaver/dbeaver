/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPImageProvider;
import org.jkiss.dbeaver.model.DBUtils;
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
public class PostgreTableForeign extends PostgreTable implements DBPImageProvider
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
        try (JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Read foreign table info")) {
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

}
