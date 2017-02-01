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

package org.jkiss.dbeaver.model.impl.jdbc.cache;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.AbstractObjectCache;
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

    private static final Log log = Log.getLog(JDBCBasicDataTypeCache.class);

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
        int valueType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DATA_TYPE);
        // Check for bad value type for strings: #494
        if (valueType == Types.BINARY && (name.contains("varchar") || name.contains("VARCHAR"))) {
            log.warn("Inconsistent string data type name/id: " + name + "(" + valueType + "). Setting to " + Types.VARCHAR);
            valueType = Types.VARCHAR;
        }

        return new JDBCDataType(
            this.owner,
            valueType,
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
