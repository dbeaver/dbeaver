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
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableBase;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableColumn;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableTrigger;
import org.jkiss.dbeaver.ext.oracle.model.OracleUtils;
import org.jkiss.dbeaver.ext.oracle.model.OracleView;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TiberoView extends OracleView {

    private static final VarHandle ORACLE_VIEW_ADDITIONAL_INFO_HANDLE;
    private static final VarHandle TABLE_ADDITIONAL_INFO_LOADED_HANDLE;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            ORACLE_VIEW_ADDITIONAL_INFO_HANDLE = MethodHandles.privateLookupIn(OracleView.class, lookup)
                .findVarHandle(OracleView.class, "additionalInfo", OracleView.AdditionalInfo.class);
            TABLE_ADDITIONAL_INFO_LOADED_HANDLE = MethodHandles.privateLookupIn(OracleTableBase.TableAdditionalInfo.class, lookup)
                .findVarHandle(OracleTableBase.TableAdditionalInfo.class, "loaded", boolean.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private volatile List<OracleTableColumn> columnsCache;
    private volatile String sourceText;

    public TiberoView(OracleSchema schema, String name) {
        super(schema, name);
    }

    public TiberoView(OracleSchema schema, ResultSet dbResult) {
        super(schema, dbResult);
    }

    /**
     * Routed through the schema so the Tibero-compatible trigger loading kicks in
     * instead of the per-table Oracle trigger query (Tibero has no ALL_TRIGGERS.BASE_OBJECT_TYPE).
     */
    @Nullable
    @Association
    @Override
    public List<OracleTableTrigger> getTriggers(@NotNull DBRProgressMonitor monitor) throws DBException {
        return ((TiberoSchema) getContainer()).getTableTriggers(monitor, this);
    }

    @NotNull
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    @Override
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull java.util.Map<String, Object> options) throws DBException {
        String definitionText = sourceText;
        if (definitionText != null) {
            return definitionText;
        }

        String viewText = loadViewText(monitor);
        if (viewText == null) {
            return "-- Tibero view definition is not available";
        }

        StringBuilder definition = new StringBuilder();
        definition.append("CREATE OR REPLACE VIEW ")
            .append(getFullyQualifiedName(org.jkiss.dbeaver.model.DBPEvaluationContext.DDL));

        List<OracleTableColumn> attributes = getAttributes(monitor);
        if (attributes != null && !attributes.isEmpty()) {
            definition.append("\n(");
            boolean first = true;
            for (OracleTableColumn column : attributes) {
                if (!first) {
                    definition.append(",");
                }
                definition.append(DBUtils.getQuotedIdentifier(column));
                first = false;
            }
            definition.append(")");
        }

        definition.append("\nAS\n").append(viewText);
        definition.append(";");
        definitionText = definition.toString();
        sourceText = definitionText;
        setObjectDefinitionText(definitionText);
        return definitionText;
    }

    @PropertyGroup
    @LazyProperty(cacheValidator = OracleTableBase.AdditionalInfoValidator.class)
    @Override
    public AdditionalInfo getAdditionalInfo(DBRProgressMonitor monitor) throws DBException {
        markAdditionalInfoLoaded();
        return super.getAdditionalInfo();
    }

    @NotNull
    @Override
    public List<OracleTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<OracleTableColumn> columns = columnsCache;
        if (columns != null) {
            return columns;
        }
        synchronized (this) {
            if (columnsCache == null) {
                columnsCache = loadAttributes(monitor);
            }
            return columnsCache;
        }
    }

    @Nullable
    @Override
    public OracleTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException {
        for (OracleTableColumn column : getAttributes(monitor)) {
            if (attributeName.equals(column.getName())) {
                return column;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public org.jkiss.dbeaver.model.struct.DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        columnsCache = null;
        sourceText = null;
        return super.refreshObject(monitor);
    }

    private void markAdditionalInfoLoaded() {
        OracleView.AdditionalInfo info = (OracleView.AdditionalInfo) ORACLE_VIEW_ADDITIONAL_INFO_HANDLE.get(this);
        if (info == null) {
            return;
        }
        TABLE_ADDITIONAL_INFO_LOADED_HANDLE.set(info, true);
    }

    private List<OracleTableColumn> loadAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<OracleTableColumn> columns = new ArrayList<>();
        final String colsView = getDataSource().isViewAvailable(monitor, OracleConstants.SCHEMA_SYS, "ALL_TAB_COLS")
            ? "ALL_TAB_COLS"
            : "TAB_COLS";
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Tibero view columns")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT c.*\n" +
                     ", c.TABLE_NAME AS OBJECT_NAME\n" +
                     ", NULL AS COMMENTS, 0 AS COMMENTS_LOADED\n" +
                     ", NULL AS DATA_TYPE_MOD, NULL AS HIDDEN_COLUMN\n" +
                "FROM " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), colsView) + " c\n" +
                "WHERE c.OWNER = ? AND c.TABLE_NAME = ?\n" +
                "ORDER BY c.COLUMN_ID"
            )) {
                dbStat.setString(1, getSchema().getName());
                dbStat.setString(2, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        columns.add(new OracleTableColumn(monitor, this, dbResult));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, getDataSource());
        }
        columns.sort(DBUtils.orderComparator());
        return Collections.unmodifiableList(columns);
    }

    private String loadViewText(@NotNull DBRProgressMonitor monitor) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Tibero view definition")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT TEXT\n" +
                "FROM " + OracleUtils.getAdminAllViewPrefix(monitor, getDataSource(), "VIEWS") + "\n" +
                "WHERE OWNER = ? AND VIEW_NAME = ?"
            )) {
                dbStat.setString(1, getSchema().getName());
                dbStat.setString(2, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        return JDBCUtils.safeGetString(dbResult, "TEXT");
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, getDataSource());
        }
        return null;
    }
}
