/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
        for (DBSObjectType ot : SUPPORTED_TYPES) {
            if (ot == objectType) {
                return true;
            }
        }
        for (DBSObjectType ot : SUPPORTED_TYPES) {
            if (ot.isCompatibleWith(objectType)) {
                return true;
            }
        }
        return false;
    }

    private final PostgreSchema schema;

    public PostgreNamespace(@NotNull PostgreSchema schema) {
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
            // To find object we select from both pg_class and pg_type because
            // enums are (surprise!) are not classes
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT oid,relkind," + (schema.getDataSource().isSupportsReltypeColumn() ? "" : "0 as ") + "reltype " +
                    "FROM pg_catalog.pg_class WHERE relnamespace=? AND relname=?\n" +
                    "UNION ALL\n" +
                    "SELECT oid,'c',oid FROM pg_catalog.pg_type WHERE typnamespace=? AND typname=?")) {
                dbStat.setLong(1, schema.getObjectId());
                dbStat.setString(2, name);
                dbStat.setLong(3, schema.getObjectId());
                dbStat.setString(4, name);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        long oid = JDBCUtils.safeGetLong(dbResult, "oid");
                        String relKind = JDBCUtils.safeGetString(dbResult, "relkind");
                        long reltype = JDBCUtils.safeGetLong(dbResult, "reltype");
                        if (relKind == null) {
                            log.debug("NULL relkind for class " + name);
                            return null;
                        }
                        return switch (relKind) {
                            case "r", "v", "m", "p", "f" -> schema.getTable(monitor, oid);
                            case "i", "I" -> schema.getIndex(monitor, oid);
                            case "S" -> schema.getSequence(monitor, name);
                            case "c" -> {
                                schema.getDataTypeCache().getAllObjects(monitor, schema);
                                yield schema.getDataTypeCache().getDataType(reltype);
                            }
                            default -> {
                                log.debug("Unknown relkind: " + relKind);
                                yield null;
                            }
                        };
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
