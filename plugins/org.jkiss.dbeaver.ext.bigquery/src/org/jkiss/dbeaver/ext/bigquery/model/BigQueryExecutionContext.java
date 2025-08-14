/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.bigquery.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericExecutionContext;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

public class BigQueryExecutionContext extends GenericExecutionContext {

    private String currentDataset;

    public BigQueryExecutionContext(@NotNull JDBCRemoteInstance instance, String purpose) {
        super(instance, purpose);
    }

    @Override
    public boolean supportsSchemaChange() {
        return true;
    }

    @Override
    public boolean supportsCatalogChange() {
        return true;
    }

    @Override
    public void setDefaultSchema(DBRProgressMonitor monitor, GenericSchema schema) throws DBCException {
        if (schema == null) {
            return;
        }

        GenericSchema oldSelectedSchema = getDefaultSchema();

        String projectId = null;
        GenericStructContainer c = schema.getCatalog();
        if (c != null) {
            projectId = c.getName();
        }

        try (
            JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Set active BigQuery dataset");
            JDBCStatement stmt = session.createStatement()
        ) {

            if (!CommonUtils.isEmpty(projectId)) {
                stmt.execute("SET @@dataset_project_id = " + DBUtils.getQuotedIdentifier(getDataSource(), projectId));
            }
            stmt.execute("SET @@dataset_id = " + DBUtils.getQuotedIdentifier(getDataSource(), schema.getName()));
        } catch (SQLException e) {
            throw new DBCException("Failed to switch BigQuery dataset to " + schema.getName(), e);
        }

        currentDataset = schema.getName();
        getDataSource().setSelectedEntityType(GenericConstants.ENTITY_TYPE_SCHEMA);
        if (oldSelectedSchema != null) {
            DBUtils.fireObjectSelect(oldSelectedSchema, false, this);
        }
        DBUtils.fireObjectSelect(schema, true, this);
    }


    @Override
    public GenericSchema getDefaultSchema() {
        if (currentDataset != null) {
            BigQueryDataSource dataSource = (BigQueryDataSource) getDataSource();
            return dataSource.getSchema(currentDataset);
        }
        return super.getDefaultSchema();
    }
}
