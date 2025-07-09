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
package org.jkiss.dbeaver.ext.cubrid.model;

import java.sql.SQLException;
import java.util.List;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.CubridConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCStructureAssistant;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractObjectReference;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

public class CubridStructureAssistant extends JDBCStructureAssistant<JDBCExecutionContext> {

    private final CubridDataSource dataSource;

    public CubridStructureAssistant(CubridDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public JDBCDataSource getDataSource() {
        return dataSource;
    }

    @Override
    protected void findObjectsByMask(
        JDBCExecutionContext executionContext,
        JDBCSession session,
        DBSObjectType objectType,
        ObjectsSearchParams params,
        List<DBSObjectReference> references
    ) throws DBException, SQLException {

        GenericSchema parentSchema = params.getParentObject() instanceof GenericSchema ? (GenericSchema) params.getParentObject() : null;
        if (objectType == RelationalObjectType.TYPE_TABLE) {
            findTablesByMask(session, parentSchema, params.getMask(), references);
        }
    }

    private void findTablesByMask(
        JDBCSession session,
        GenericSchema schema,
        String tableNameMask,
        List<DBSObjectReference> objects
    ) throws SQLException, DBException {

        String sql = "SELECT class_name, owner_name, comment FROM db_class WHERE class_name = ?";
        try {
            JDBCPreparedStatement dbStat = session.prepareStatement(sql);
            dbStat.setString(1, tableNameMask);
            JDBCResultSet dbResult = dbStat.executeQuery();
            while (dbResult.next()) {
                String schemaName = JDBCUtils.safeGetString(dbResult, CubridConstants.OWNER_NAME);
                String tableName = JDBCUtils.safeGetString(dbResult, CubridConstants.CLASS_NAME);
                String comment = JDBCUtils.safeGetString(dbResult, CubridConstants.COMMENT);
                GenericSchema resolvedSchema = dataSource.getSchema(schemaName);
                if (resolvedSchema != null) {
                    objects.add(new TableReference(resolvedSchema, tableName, comment));
                } else {
                    throw new DBException("Schema not found for name: " + schemaName);
                }
            }
        } catch (SQLException e) {
            throw new DBException("Failed to find tables matching: " + tableNameMask, e);
        }
    }

    private static final class TableReference extends AbstractObjectReference<GenericSchema> {
        private TableReference(GenericSchema container, String name, String comment) {
            super(name, container, comment, CubridTable.class, RelationalObjectType.TYPE_TABLE);
        }

        @Override
        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
            DBSObject table = getContainer().getTableCache().getObject(monitor, getContainer(), getName());
            if (table == null) {
                throw new DBException(String.format("Table '%s' not found in schema '%s'", getName(), getContainer().getName()));
            }
            return table;
        }
    }

}
