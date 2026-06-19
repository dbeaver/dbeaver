/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.informix.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericExecutionContext;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericUtils;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCStructureAssistant;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractObjectReference;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.List;

public class InformixStructureAssistant extends JDBCStructureAssistant<GenericExecutionContext> {

    private final InformixDataSource dataSource;
    private final DBSStructureAssistant<GenericExecutionContext> genericAssistant;

    public InformixStructureAssistant(
        @NotNull InformixDataSource dataSource,
        @NotNull DBSStructureAssistant<GenericExecutionContext> genericAssistant
    ) {
        this.dataSource = dataSource;
        this.genericAssistant = genericAssistant;
    }

    @Override
    protected InformixDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    @Override
    public DBSObjectType[] getSupportedObjectTypes() {
        return genericAssistant.getSupportedObjectTypes();
    }

    @NotNull
    @Override
    public DBSObjectType[] getSearchObjectTypes() {
        return genericAssistant.getSearchObjectTypes();
    }

    @NotNull
    @Override
    public DBSObjectType[] getHyperlinkObjectTypes() {
        return genericAssistant.getHyperlinkObjectTypes();
    }

    @NotNull
    @Override
    public DBSObjectType[] getAutoCompleteObjectTypes() {
        return genericAssistant.getAutoCompleteObjectTypes();
    }

    @Override
    protected void findObjectsByMask(
        @NotNull GenericExecutionContext executionContext,
        @NotNull JDBCSession session,
        @NotNull DBSObjectType objectType,
        @NotNull ObjectsSearchParams params,
        @NotNull List<DBSObjectReference> references
    ) throws DBException, SQLException {
        int remainingResults = params.getMaxResults() - references.size();
        if (remainingResults <= 0) {
            return;
        }

        ObjectsSearchParams objectParams = copyParams(params, objectType, remainingResults);
        if (objectType == RelationalObjectType.TYPE_PROCEDURE) {
            DBSObject parentObject = objectParams.getParentObject();
            boolean globalSearch = objectParams.isGlobalSearch();
            GenericSchema schema = parentObject instanceof GenericSchema ? (GenericSchema) parentObject :
                globalSearch ? null : executionContext.getDefaultSchema();
            GenericCatalog catalog = parentObject instanceof GenericCatalog ? (GenericCatalog) parentObject :
                schema == null ? (globalSearch ? null : executionContext.getDefaultCatalog()) : schema.getCatalog();

            DBPIdentifierCase identifierCase = objectParams.isCaseSensitive() ?
                dataSource.getSQLDialect().storesQuotedCase() :
                dataSource.getSQLDialect().storesUnquotedCase();
            String procedureNameMask = identifierCase.transform(objectParams.getMask());
            findProceduresByMask(session, catalog, schema, procedureNameMask, objectParams, references);
        } else {
            references.addAll(genericAssistant.findObjectsByMask(session.getProgressMonitor(), executionContext, objectParams));
        }
    }

    private void findProceduresByMask(
        @NotNull JDBCSession session,
        GenericCatalog catalog,
        GenericSchema schema,
        @NotNull String procedureNameMask,
        @NotNull ObjectsSearchParams params,
        @NotNull List<DBSObjectReference> references
    ) throws SQLException, DBException {
        String schemaName = schema == null || DBUtils.isVirtualObject(schema) ? null : schema.getName();
        boolean likeSearch = params.isLikeCondition() || procedureNameMask.contains("%");
        StringBuilder query = new StringBuilder(
            "SELECT procid, procname, owner FROM informix.sysprocedures WHERE "
        );
        query.append(likeSearch ? "procname LIKE ?" : "procname = ?");
        if (!CommonUtils.isEmpty(schemaName)) {
            query.append(" AND owner = ?");
        }
        query.append(" ORDER BY procname, procid");

        DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(query.toString())) {
            dbStat.setString(1, procedureNameMask);
            if (!CommonUtils.isEmpty(schemaName)) {
                dbStat.setString(2, schemaName);
            }

            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next()) {
                    String procedureName = JDBCUtils.safeGetStringTrimmed(dbResult, "procname");
                    if (CommonUtils.isEmpty(procedureName)) {
                        continue;
                    }
                    long procId = JDBCUtils.safeGetLong(dbResult, "procid");
                    String ownerName = JDBCUtils.safeGetStringTrimmed(dbResult, "owner");
                    procedureName = GenericUtils.normalizeProcedureName(procedureName);

                    references.add(new ProcedureReference(
                        findContainer(session.getProgressMonitor(), catalog, schema, ownerName),
                        procedureName,
                        procId
                    ));
                    if (references.size() >= params.getMaxResults()) {
                        break;
                    }
                }
            }
        }
    }

    private GenericStructContainer findContainer(
        @NotNull DBRProgressMonitor monitor,
        GenericCatalog parentCatalog,
        GenericSchema parentSchema,
        String schemaName
    ) throws DBException {
        if (parentSchema != null) {
            return parentSchema;
        }

        GenericCatalog catalog = parentCatalog;
        if (catalog == null && !CommonUtils.isEmpty(dataSource.getCatalogs()) && dataSource.getCatalogs().size() == 1) {
            catalog = dataSource.getCatalogs().getFirst();
        }

        if (!CommonUtils.isEmpty(schemaName)) {
            GenericSchema schema = catalog == null ? dataSource.getSchema(schemaName) : catalog.getSchema(monitor, schemaName);
            if (schema != null) {
                return schema;
            }
        }

        return catalog == null ? dataSource : catalog;
    }

    @NotNull
    private static ObjectsSearchParams copyParams(
        @NotNull ObjectsSearchParams params,
        @NotNull DBSObjectType objectType,
        int maxResults
    ) {
        ObjectsSearchParams copy = new ObjectsSearchParams(new DBSObjectType[] {objectType}, params.getMask());
        copy.setParentObject(params.getParentObject());
        copy.setMaxResults(maxResults);
        copy.setCaseSensitive(params.isCaseSensitive());
        copy.setSearchInComments(params.isSearchInComments());
        copy.setSearchInDefinitions(params.isSearchInDefinitions());
        copy.setGlobalSearch(params.isGlobalSearch());
        copy.setLikeCondition(params.isLikeCondition());
        return copy;
    }

    private class ProcedureReference extends AbstractObjectReference<GenericStructContainer> {
        private final long procId;

        private ProcedureReference(
            @NotNull GenericStructContainer container,
            @NotNull String procedureName,
            long procId
        ) {
            super(procedureName, container, null, GenericProcedure.class, RelationalObjectType.TYPE_PROCEDURE);
            this.procId = procId;
        }

        @Override
        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
            String uniqueName = String.valueOf(procId);
            GenericProcedure procedure = getContainer().getProcedure(monitor, uniqueName);
            if (procedure == null) {
                throw new DBException(
                    "Can't find procedure '" + getName() + "' (" + uniqueName + ")' in '" +
                        DBUtils.getFullQualifiedName(dataSource, getContainer()) + "'");
            }
            return procedure;
        }
    }
}
