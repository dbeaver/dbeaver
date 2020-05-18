/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractObjectReference;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SQLServerStructureAssistant
 */
public class SQLServerStructureAssistant implements DBSStructureAssistant<SQLServerExecutionContext>
{
    static protected final Log log = Log.getLog(SQLServerStructureAssistant.class);

    private final SQLServerDataSource dataSource;

    public SQLServerStructureAssistant(SQLServerDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    public DBSObjectType[] getSupportedObjectTypes()
    {
        return new DBSObjectType[] {
            SQLServerObjectType.S,
            SQLServerObjectType.U,
            SQLServerObjectType.IT,
            SQLServerObjectType.V,
            SQLServerObjectType.SN,
            SQLServerObjectType.P,
            SQLServerObjectType.FN,
            SQLServerObjectType.FT,
            SQLServerObjectType.FS,
            SQLServerObjectType.X,
            };
    }

    @Override
    public DBSObjectType[] getSearchObjectTypes() {
        return new DBSObjectType[] {
            RelationalObjectType.TYPE_TABLE,
            RelationalObjectType.TYPE_VIEW,
            SQLServerObjectType.SN,
            RelationalObjectType.TYPE_PROCEDURE,
        };
    }

    @Override
    public DBSObjectType[] getHyperlinkObjectTypes()
    {
        return new DBSObjectType[] {
            SQLServerObjectType.S,
            SQLServerObjectType.U,
            SQLServerObjectType.IT,
            SQLServerObjectType.V,
            RelationalObjectType.TYPE_PROCEDURE,
        };
    }

    @Override
    public DBSObjectType[] getAutoCompleteObjectTypes()
    {
        return new DBSObjectType[] {
            SQLServerObjectType.U,
            SQLServerObjectType.V,
            SQLServerObjectType.P,
            };
    }

    @NotNull
    @Override
    public List<DBSObjectReference> findObjectsByMask(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SQLServerExecutionContext executionContext,
        DBSObject parentObject,
        DBSObjectType[] objectTypes,
        String objectNameMask,
        boolean caseSensitive,
        boolean globalSearch, int maxResults)
        throws DBException
    {
        SQLServerDatabase database = parentObject instanceof SQLServerDatabase ?
            (SQLServerDatabase) parentObject :
            (parentObject instanceof SQLServerSchema ? ((SQLServerSchema) parentObject).getDatabase() : null);
        if (database == null) {
            database = executionContext.getContextDefaults().getDefaultCatalog();
        }
        if (database == null) {
            database = executionContext.getDataSource().getDefaultDatabase(monitor);
        }
        if (database == null) {
            return Collections.emptyList();
        }
        SQLServerSchema schema = parentObject instanceof SQLServerSchema ? (SQLServerSchema) parentObject : null;

        try (JDBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.META, "Find objects by name")) {
            List<DBSObjectReference> objects = new ArrayList<>();

            // Search all objects
            searchAllObjects(session, database, schema, objectNameMask, objectTypes, caseSensitive, maxResults, objects);

            return objects;
        }
    }

    private void searchAllObjects(final JDBCSession session, final SQLServerDatabase database, final SQLServerSchema schema, String objectNameMask, DBSObjectType[] objectTypes, boolean caseSensitive, int maxResults, List<DBSObjectReference> objects)
        throws DBException
    {
        final List<SQLServerObjectType> supObjectTypes = new ArrayList<>(objectTypes.length + 2);
        for (DBSObjectType objectType : objectTypes) {
            if (objectType instanceof SQLServerObjectType) {
                supObjectTypes.add((SQLServerObjectType) objectType);
            } else if (objectType == RelationalObjectType.TYPE_PROCEDURE) {
                supObjectTypes.addAll(SQLServerObjectType.getTypesForClass(SQLServerProcedure.class));
            } else if (objectType == RelationalObjectType.TYPE_TABLE) {
                supObjectTypes.addAll(SQLServerObjectType.getTypesForClass(SQLServerTable.class));
            } else if (objectType == RelationalObjectType.TYPE_CONSTRAINT) {
                supObjectTypes.addAll(SQLServerObjectType.getTypesForClass(SQLServerTableCheckConstraint.class));
                supObjectTypes.addAll(SQLServerObjectType.getTypesForClass(SQLServerTableForeignKey.class));
            } else if (objectType == RelationalObjectType.TYPE_VIEW) {
                supObjectTypes.addAll(SQLServerObjectType.getTypesForClass(SQLServerView.class));
            }
        }
        if (supObjectTypes.isEmpty()) {
            return;
        }
        StringBuilder objectTypeClause = new StringBuilder(100);
        for (SQLServerObjectType objectType : supObjectTypes) {
            if (objectTypeClause.length() > 0) objectTypeClause.append(",");
            objectTypeClause.append("'").append(objectType.getTypeID()).append("'");
        }
        if (objectTypeClause.length() == 0) {
            return;
        }

        // Seek for objects (join with public synonyms)
        try (JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT * FROM " + SQLServerUtils.getSystemTableName(database, "all_objects") + " o " +
                "\nWHERE o.type IN (" + objectTypeClause.toString() + ") AND o.name LIKE ?" +
                (schema == null ? "" : " AND o.schema_id=? ") +
                "\nORDER BY o.name"))
        {
            dbStat.setString(1, objectNameMask);
            if (schema != null) {
                dbStat.setLong(2, schema.getObjectId());
            }
            dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (objects.size() < maxResults && dbResult.next()) {
                    if (session.getProgressMonitor().isCanceled()) {
                        break;
                    }
                    final long schemaId = JDBCUtils.safeGetLong(dbResult, "schema_id");
                    final String objectName = JDBCUtils.safeGetString(dbResult, "name");
                    final String objectTypeName = JDBCUtils.safeGetStringTrimmed(dbResult, "type");
                    final SQLServerObjectType objectType = SQLServerObjectType.valueOf(objectTypeName);
                    {
                        SQLServerSchema objectSchema = schemaId == 0 ? null : database.getSchema(session.getProgressMonitor(), schemaId);
                        objects.add(new AbstractObjectReference(
                            objectName,
                            objectSchema != null ? objectSchema : database,
                            null,
                            objectType.getTypeClass(),
                            objectType)
                        {
                            @Override
                            public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                                DBSObject object = objectType.findObject(session.getProgressMonitor(), database, objectSchema, objectName);
                                if (object == null) {
                                    throw new DBException(objectTypeName + " '" + objectName + "' not found");
                                }
                                return object;
                            }
                        });
                    }
                }
            }
        } catch (Throwable e) {
            throw new DBException("Error while searching in system catalog", e, dataSource);
        }
    }


}
