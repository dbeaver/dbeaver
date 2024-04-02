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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericFunctionResultType;
import org.jkiss.dbeaver.ext.generic.model.GenericPackage;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CubridProcedure extends GenericProcedure
{
    private List<CubridProcedureParameter> proColumns;
    private String returnType;

    public CubridProcedure(
            @NotNull GenericStructContainer container,
            @NotNull String procedureName,
            @Nullable String description,
            @NotNull DBSProcedureType procedureType,
            @NotNull String returnType) {
        super(container, procedureName, description, procedureType, null, true);
        this.returnType = returnType;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return super.getName();
    }

    @NotNull
    @Property(viewable = true, order = 2)
    public GenericSchema getSchema() {
        return super.getSchema();
    }

    @Nullable
    @Override
    public GenericCatalog getCatalog() {
        return null;
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
            throw new DBException(e, getDataSource());
        }
    }
}
