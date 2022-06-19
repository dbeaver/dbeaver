/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSNamespace;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.utils.ArrayUtils;

import java.sql.SQLException;
import java.util.List;

/**
 * PostgreNamespace
 */
public class PostgreNamespace implements DBSNamespace  {

    private static final Log log = Log.getLog(PostgreNamespace.class);

    public static final DBSObjectType[] SUPPORTED_TYPES = {
        RelationalObjectType.TYPE_TABLE,
        RelationalObjectType.TYPE_VIEW,
        RelationalObjectType.TYPE_DATA_TYPE,
        RelationalObjectType.TYPE_INDEX,
        RelationalObjectType.TYPE_SEQUENCE
    };

    public static boolean supportsObjectType(DBSObjectType objectType) {
        return ArrayUtils.contains(SUPPORTED_TYPES, objectType);
    }

    private final PostgreSchema schema;

    public PostgreNamespace(PostgreSchema schema) {
        this.schema = schema;
    }

    @NotNull
    @Override
    public DBSObjectType[] getNamespaceObjectTypes() {
        return SUPPORTED_TYPES;
    }

    @Nullable
    @Override
    public DBSObject getObjectByName(@NotNull DBRProgressMonitor monitor, @NotNull String name) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, schema, "Search PG class")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT oid,relkind FROM pg_catalog.pg_class WHERE relname = ?")) {
                dbStat.setString(1, name);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        long oid = JDBCUtils.safeGetLong(dbResult, "oid");
                        String relKind = JDBCUtils.safeGetString(dbResult, "relkind");
                        if (relKind == null) {
                            log.debug("NULL relkind for class " + name);
                            return null;
                        }
                        switch (relKind) {
                            case "r":
                            case "v":
                            case "m":
                            case "p":
                            case "f":
                                return schema.getTable(monitor, oid);
                            case "i":
                            case "I":
                                return schema.getIndex(monitor, oid);
                            case "S":
                                return schema.getSequence(monitor, name);
                            case "c":
                                schema.getDataTypeCache().getAllObjects(monitor, schema);
                                return schema.getDataTypeCache().getDataType(oid);
                            default:
                                log.debug("Unknown relkind: " + relKind);
                                return null;
                        }
                    } else {
                        return null;
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBCException("Error reading class info", e);
        }
    }

    @NotNull
    @Override
    public List<? extends DBSObject> getObjectsByType(@NotNull DBRProgressMonitor monitor, @NotNull DBSObjectType objectType) throws DBException {
        if (objectType == RelationalObjectType.TYPE_TABLE ||
            objectType == RelationalObjectType.TYPE_VIEW)
        {
            return schema.getTables(monitor);
        } else if (objectType == RelationalObjectType.TYPE_SEQUENCE) {
            return schema.getSequences(monitor);
        } else if (objectType == RelationalObjectType.TYPE_DATA_TYPE) {
            return schema.getDataTypes(monitor);
        } else if (objectType == RelationalObjectType.TYPE_INDEX) {
            return schema.getIndexes(monitor);
        }
        throw new DBException("Unsupported object type: " + objectType.getTypeName());
    }
}
