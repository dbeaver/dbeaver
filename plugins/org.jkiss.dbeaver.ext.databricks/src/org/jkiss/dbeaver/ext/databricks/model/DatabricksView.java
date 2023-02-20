/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.databricks.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.databricks.DatabricksConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

public class DatabricksView extends GenericView {

    private static final Log log = Log.getLog(DatabricksView.class);

    private volatile boolean additionalInfoLoaded = false;
    private String owner;
    private String createdTime;
    private String tableProperties;
    private String storageProperties;

    public DatabricksView(
        GenericStructContainer container,
        @Nullable String tableName,
        @Nullable String tableType,
        @Nullable JDBCResultSet dbResult
    ) {
        super(container, tableName, tableType, dbResult);
    }

    @Property(viewable = true, order = 2)
    public String getOwner(@NotNull DBRProgressMonitor monitor) {
        checkExtraInfo(monitor);
        return owner;
    }

    @Property(viewable = true, order = 3)
    public String getCreatedTime(@NotNull DBRProgressMonitor monitor) {
        checkExtraInfo(monitor);
        return createdTime;
    }

    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 98)
    public String getTableProperties(@NotNull DBRProgressMonitor monitor) {
        checkExtraInfo(monitor);
        return tableProperties;
    }

    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 99)
    public String getStorageProperties(@NotNull DBRProgressMonitor monitor) {
        checkExtraInfo(monitor);
        return storageProperties;
    }

    @Nullable
    @Override
    public String getDescription() {
        // Not supported
        return null;
    }

    private void checkExtraInfo(@NotNull DBRProgressMonitor monitor) {
        if (!additionalInfoLoaded) {
            loadAdditionalInfo(monitor);
        }
    }

    private void loadAdditionalInfo(@NotNull DBRProgressMonitor monitor) {
        if (!isPersisted()) {
            additionalInfoLoaded = true;
            return;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load view extra info")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "DESCRIBE TABLE EXTENDED " + getFullyQualifiedName(DBPEvaluationContext.DDL))) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        // This is table of metadata, where column name is in the col_name column
                        String key = JDBCUtils.safeGetString(dbResult, "col_name");
                        String value = JDBCUtils.safeGetString(dbResult, "data_type");
                        if (CommonUtils.isNotEmpty(key)) {
                            switch (key) {
                                case DatabricksConstants.PROP_OWNER:
                                    owner = value;
                                    break;
                                case DatabricksConstants.PROP_CREATED_TIME:
                                    createdTime = value;
                                    break;
                                case DatabricksConstants.PROP_TABLE_PROPERTIES:
                                    tableProperties = value;
                                    break;
                                case DatabricksConstants.PROP_STORAGE_PROPERTIES:
                                    storageProperties = value;
                                    break;
                            }
                        }
                    }
                    additionalInfoLoaded = true;
                }
            } catch (SQLException e) {
                log.error("Can't read additional table info", e);
            }
        } catch (DBCException e) {
            log.error("Can't read additional table info", e);
        }
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(
            getDataSource(),
            getCatalog(),
            getSchema(),
            this);
    }

    @Override
    public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        additionalInfoLoaded = false;
        return super.refreshObject(monitor);
    }
}
