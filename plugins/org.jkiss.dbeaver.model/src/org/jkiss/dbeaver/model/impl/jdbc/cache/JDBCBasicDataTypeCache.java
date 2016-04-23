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

package org.jkiss.dbeaver.model.impl.jdbc.cache;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

public class JDBCBasicDataTypeCache extends JDBCObjectCache<JDBCDataSource, DBSDataType> {
    private final DBSObject owner;
    protected final Set<String> ignoredTypes = new HashSet<>();

    public JDBCBasicDataTypeCache(DBSObject owner)
    {
        this.owner = owner;
        setCaseSensitive(false);
    }

    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull JDBCDataSource owner) throws SQLException
    {
        return session.getMetaData().getTypeInfo().getSourceStatement();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected JDBCDataType fetchObject(@NotNull JDBCSession session, @NotNull JDBCDataSource owner, @NotNull JDBCResultSet dbResult) throws SQLException, DBException
    {
        String name = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_NAME);
        if (CommonUtils.isEmpty(name)) {
            return null;
        }
        if (ignoredTypes.contains(name.toUpperCase(Locale.ENGLISH))) {
            return null;
        }

        return new JDBCDataType(
            this.owner,
            JDBCUtils.safeGetInt(dbResult, JDBCConstants.DATA_TYPE),
            name,
            JDBCUtils.safeGetString(dbResult, JDBCConstants.LOCAL_TYPE_NAME),
            JDBCUtils.safeGetBoolean(dbResult, JDBCConstants.UNSIGNED_ATTRIBUTE),
            JDBCUtils.safeGetInt(dbResult, JDBCConstants.SEARCHABLE) != 0,
            JDBCUtils.safeGetInt(dbResult, JDBCConstants.PRECISION),
            JDBCUtils.safeGetInt(dbResult, JDBCConstants.MINIMUM_SCALE),
            JDBCUtils.safeGetInt(dbResult, JDBCConstants.MAXIMUM_SCALE));
    }

    // SQL-92 standard types
    // plus a few de-facto standard types
    @SuppressWarnings("unchecked")
    public void fillStandardTypes(DBSObject owner)
    {
        List<DBSDataType> standardTypes = new ArrayList<>();
        Collections.addAll(standardTypes,
            new JDBCDataType(owner, Types.INTEGER, "INTEGER", null, false, true, 0, 0, 0),
            new JDBCDataType(owner, Types.FLOAT, "FLOAT", null, false, true, 0, 0, 0),
            new JDBCDataType(owner, Types.REAL, "REAL", null, false, true, 0, 0, 0),
            new JDBCDataType(owner, Types.DOUBLE, "DOUBLE PRECISION", null, false, true, 0, 0, 0),
            new JDBCDataType(owner, Types.NUMERIC, "NUMBER", null, false, true, 0, 0, 0),
            new JDBCDataType(owner, Types.DECIMAL, "DECIMAL", null, false, true, 0, 0, 0),
            new JDBCDataType(owner, Types.SMALLINT, "SMALLINT", null, false, true, 0, 0, 0),
            new JDBCDataType(owner, Types.BIGINT, "BIGINT", null, false, true, 0, 0, 0),
            new JDBCDataType(owner, Types.BIT, "BIT", null, false, true, 0, 0, 0),
            new JDBCDataType(owner, Types.VARCHAR, "VARCHAR", null, false, true, 0, 0, 0),
            new JDBCDataType(owner, Types.VARBINARY, "VARBINARY", null, false, true, 0, 0, 0),
            new JDBCDataType(owner, Types.DATE, "DATE", null, false, true, 0, 0, 0),
            new JDBCDataType(owner, Types.TIME, "TIME", null, false, true, 0, 0, 0),
            new JDBCDataType(owner, Types.TIMESTAMP, "TIMESTAMP", null, false, true, 0, 0, 0),
            new JDBCDataType(owner, Types.BLOB, "BLOB", null, false, true, 0, 0, 0),
            new JDBCDataType(owner, Types.CLOB, "CLOB", null, false, true, 0, 0, 0),
            new JDBCDataType(owner, Types.BOOLEAN, "BOOLEAN", null, false, true, 0, 0, 0),
            new JDBCDataType(owner, Types.OTHER, "OBJECT", null, false, true, 0, 0, 0));
        setCache(standardTypes);
    }

}
