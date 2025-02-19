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
package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectWithScript;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CubridProcedure extends GenericProcedure implements DBSObjectWithScript, DBPRefreshableObject
{
    private List<CubridProcedureParameter> proColumns;
    private DBSProcedureType procedureType;
    private String returnType;
    private String source;

    public CubridProcedure(
            @NotNull GenericStructContainer container,
            @NotNull String procedureName,
            @Nullable String description,
            @NotNull DBSProcedureType procedureType,
            @NotNull String returnType) {
        super(container, procedureName, description, procedureType, null, true);
        this.procedureType = procedureType;
        this.returnType = returnType;
    }

    public CubridProcedure(@NotNull GenericStructContainer container, DBSProcedureType procedureType) {
        super(container, null, null, procedureType, null, false);
        this.procedureType = procedureType;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return super.getName();
    }

    @NotNull
    @Property(viewable = true, order = 2, labelProvider = GenericSchema.SchemaNameTermProvider.class)
    public GenericSchema getSchema() {
        return super.getSchema();
    }

    @Nullable
    @Override
    public GenericCatalog getCatalog() {
        return null;
    }

    @Override
    @Property(order = 2)
    public DBSProcedureType getProcedureType() {
        return procedureType;
    }

    public void setProcedureType(DBSProcedureType procedureType) {
        this.procedureType = procedureType;
    }

    @Nullable
    @Override
    public GenericFunctionResultType getFunctionResultType() {
        return null;
    }

    @Nullable
    @Override
    public GenericPackage getPackage() {
        return null;
    }

    @NotNull
    @Property(viewable = true, order = 20)
    public String getReturnType() {
        return returnType;
    }

    @Nullable
    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 100)
    public String getDescription() {
        return super.getDescription();
    }

    public void addColumn(@NotNull CubridProcedureParameter column) {
        if (this.proColumns == null) {
            this.proColumns = new ArrayList<>();
        }
        this.proColumns.add(column);
    }

    @Nullable
    public List<CubridProcedureParameter> getParams(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (proColumns == null) {
            loadProcedureColumns(monitor);
            if (proColumns == null) {
                proColumns = new ArrayList<>();
            }
        }
        return proColumns;
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (source == null) {
            if (!persisted) {
                this.source = "CREATE OR REPLACE " + getProcedureType().name() + " " + getName() + "()";
                this.source += (getProcedureType() == DBSProcedureType.FUNCTION) ? " RETURN int" : "";
                this.source += "\nAS LANGUAGE JAVA NAME";
            } else {
                this.source = "-- Procedure definition not available";
            }
        }
        return source;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public void setObjectDefinitionText(String source) {
        setSource(source);
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(@NotNull DBPEvaluationContext context) {
        return getName();
    }

    @Override
    public void loadProcedureColumns(@NotNull DBRProgressMonitor monitor) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Read procedure parameter")) {
            String stmt = "select * from db_stored_procedure_args where sp_name = ?";
            try (JDBCPreparedStatement dbStat = session.prepareStatement(stmt)) {
                dbStat.setString(1, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String argName = JDBCUtils.safeGetString(dbResult, "arg_name");
                        String dataType = JDBCUtils.safeGetString(dbResult, "data_type");
                        String mode = JDBCUtils.safeGetString(dbResult, "mode");
                        String comment = JDBCUtils.safeGetString(dbResult, CubridConstants.COMMENT);
                        addColumn(new CubridProcedureParameter(this, getName(), argName, dataType, mode, comment));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, getDataSource());
        }
    }
}
