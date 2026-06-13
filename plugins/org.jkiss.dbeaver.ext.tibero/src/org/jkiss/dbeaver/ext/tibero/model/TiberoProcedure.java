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
package org.jkiss.dbeaver.ext.tibero.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.tibero.model.source.TiberoSourceObject;
import org.jkiss.dbeaver.ext.tibero.model.source.TiberoSourcePersistAction;
import org.jkiss.dbeaver.ext.tibero.model.source.TiberoSourceType;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractProcedure;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TiberoProcedure extends AbstractProcedure<TiberoDataSource, TiberoSchema> implements TiberoSourceObject {

    private final DBSProcedureType procedureType;
    private boolean valid;

    private List<TiberoProcedureParameter> parameters;
    private String sourceDeclaration;

    public TiberoProcedure(@NotNull TiberoSchema schema, @NotNull JDBCResultSet dbResult) {
        super(schema, true, JDBCUtils.safeGetString(dbResult, "OBJECT_NAME"), null);
        String objectType = JDBCUtils.safeGetStringTrimmed(dbResult, "OBJECT_TYPE");
        this.procedureType = "FUNCTION".equalsIgnoreCase(objectType)
            ? DBSProcedureType.FUNCTION
            : DBSProcedureType.PROCEDURE;
        this.valid = "VALID".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "STATUS"));
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 2)
    public DBSProcedureType getProcedureType() {
        return procedureType;
    }

    @Property(viewable = true, order = 3)
    public boolean isValid() {
        return valid;
    }

    @NotNull
    @Override
    public TiberoSchema getSchema() {
        return container;
    }

    @NotNull
    @Override
    public TiberoSourceType getSourceType() {
        return procedureType == DBSProcedureType.FUNCTION ? TiberoSourceType.FUNCTION : TiberoSourceType.PROCEDURE;
    }

    @Nullable
    @Override
    @Association
    public synchronized Collection<TiberoProcedureParameter> getParameters(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (parameters == null) {
            parameters = loadParameters(monitor);
        }
        return parameters;
    }

    @NotNull
    private List<TiberoProcedureParameter> loadParameters(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<TiberoProcedureParameter> result = new ArrayList<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Tibero procedure parameters")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT ARGUMENT_NAME, POSITION, DATA_LEVEL, IN_OUT, " +
                    "       DATA_TYPE, DATA_LENGTH, DATA_PRECISION, DATA_SCALE " +
                    "FROM ALL_ARGUMENTS " +
                    "WHERE OWNER = ? AND PACKAGE_NAME IS NULL AND OBJECT_NAME = ? " +
                    "ORDER BY POSITION, DATA_LEVEL"
            )) {
                dbStat.setString(1, getSchema().getName());
                dbStat.setString(2, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        result.add(new TiberoProcedureParameter(this, dbResult));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, getDataSource());
        }
        return result;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(@NotNull DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(), getSchema(), this);
    }

    @NotNull
    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(
        @NotNull DBRProgressMonitor monitor,
        @Nullable Map<String, Object> options
    ) throws DBException {
        if (sourceDeclaration == null || (options != null && Boolean.TRUE.equals(options.get(OPTION_REFRESH)))) {
            String objectType = procedureType == DBSProcedureType.FUNCTION ? "FUNCTION" : "PROCEDURE";
            sourceDeclaration = TiberoUtils.loadObjectSource(monitor, this, objectType, getSchema().getName());
        }
        return sourceDeclaration;
    }

    @Override
    public void setObjectDefinitionText(String sourceDeclaration) {
        this.sourceDeclaration = sourceDeclaration;
    }

    @NotNull
    @Override
    public DBEPersistAction[] getCompileActions(@NotNull DBRProgressMonitor monitor) throws DBCException {
        String objectType = procedureType == DBSProcedureType.FUNCTION ? "FUNCTION" : "PROCEDURE";
        return new DBEPersistAction[] {
            new TiberoSourcePersistAction(
                objectType,
                "Compile " + objectType.toLowerCase(),
                "ALTER " + objectType + " " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " COMPILE"
            )
        };
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState() {
        return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {
        String objectType = procedureType == DBSProcedureType.FUNCTION ? "FUNCTION" : "PROCEDURE";
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Refresh Tibero " + objectType + " state")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT STATUS FROM ALL_OBJECTS WHERE OBJECT_TYPE = ? AND OWNER = ? AND OBJECT_NAME = ?"
            )) {
                dbStat.setString(1, objectType);
                dbStat.setString(2, getSchema().getName());
                dbStat.setString(3, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        this.valid = "VALID".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "STATUS"));
                    }
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }
    }
}
