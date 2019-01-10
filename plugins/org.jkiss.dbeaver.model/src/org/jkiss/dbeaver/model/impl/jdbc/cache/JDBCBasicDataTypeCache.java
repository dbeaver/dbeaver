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

package org.jkiss.dbeaver.model.impl.jdbc.cache;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

public class JDBCBasicDataTypeCache<OWNER extends DBSObject, OBJECT extends JDBCDataType> extends JDBCObjectCache<OWNER, OBJECT> {

    private static final Log log = Log.getLog(JDBCBasicDataTypeCache.class);

    protected final OWNER owner;
    protected final Set<String> ignoredTypes = new HashSet<>();

    public JDBCBasicDataTypeCache(OWNER owner)
    {
        this.owner = owner;
        setCaseSensitive(false);
    }

    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OWNER owner) throws SQLException
    {
        return session.getMetaData().getTypeInfo().getSourceStatement();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected OBJECT fetchObject(@NotNull JDBCSession session, @NotNull OWNER owner, @NotNull JDBCResultSet dbResult) throws SQLException, DBException
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

        return makeDataType(dbResult, name, valueType);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    protected OBJECT makeDataType(@NotNull JDBCResultSet dbResult, String name, int valueType) {
        return (OBJECT) new JDBCDataType(
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

    @SuppressWarnings("unchecked")
    private OBJECT makeDataType(OWNER owner, int valueType, String name, @Nullable String remarks, boolean unsigned, boolean searchable, int precision, int minScale, int maxScale) {
        return (OBJECT) new JDBCDataType(
                this.owner,
                valueType,
                name,
                remarks,
                unsigned,
                searchable,
                precision,
                minScale,
                maxScale);
    }

    // SQL-92 standard types
    // plus a few de-facto standard types
    @SuppressWarnings("unchecked")
    public void fillStandardTypes(OWNER owner)
    {
        List<OBJECT> standardTypes = new ArrayList<>();
        Collections.addAll(standardTypes,
            makeDataType(owner, Types.INTEGER, "INTEGER", null, false, true, 0, 0, 0),
            makeDataType(owner, Types.FLOAT, "FLOAT", null, false, true, 0, 0, 0),
            makeDataType(owner, Types.REAL, "REAL", null, false, true, 0, 0, 0),
            makeDataType(owner, Types.DOUBLE, "DOUBLE PRECISION", null, false, true, 0, 0, 0),
            makeDataType(owner, Types.NUMERIC, "NUMBER", null, false, true, 0, 0, 0),
            makeDataType(owner, Types.DECIMAL, "DECIMAL", null, false, true, 0, 0, 0),
            makeDataType(owner, Types.SMALLINT, "SMALLINT", null, false, true, 0, 0, 0),
            makeDataType(owner, Types.BIGINT, "BIGINT", null, false, true, 0, 0, 0),
            makeDataType(owner, Types.BIT, "BIT", null, false, true, 0, 0, 0),
            makeDataType(owner, Types.VARCHAR, "VARCHAR", null, false, true, 0, 0, 0),
            makeDataType(owner, Types.VARBINARY, "VARBINARY", null, false, true, 0, 0, 0),
            makeDataType(owner, Types.DATE, "DATE", null, false, true, 0, 0, 0),
            makeDataType(owner, Types.TIME, "TIME", null, false, true, 0, 0, 0),
            makeDataType(owner, Types.TIMESTAMP, "TIMESTAMP", null, false, true, 0, 0, 0),
            makeDataType(owner, Types.BLOB, "BLOB", null, false, true, 0, 0, 0),
            makeDataType(owner, Types.CLOB, "CLOB", null, false, true, 0, 0, 0),
            makeDataType(owner, Types.BOOLEAN, "BOOLEAN", null, false, true, 0, 0, 0),
            makeDataType(owner, Types.OTHER, "OBJECT", null, false, true, 0, 0, 0));
        setCache(standardTypes);
    }

    public DBSDataType getCachedObject(int typeID) {
        for (JDBCDataType type : getCachedObjects()) {
            if (type.getTypeID() == typeID) {
                return type;
            }
        }
        return null;
    }

}
