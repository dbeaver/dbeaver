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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * AltibaseDataTypeCache
 */
public class AltibaseDataTypeCache extends JDBCBasicDataTypeCache<GenericStructContainer, AltibaseDataType> {

    public AltibaseDataTypeCache(GenericStructContainer owner) {
        super(owner);
    }

    @Override
    protected synchronized void loadObjects(DBRProgressMonitor monitor, GenericStructContainer container) throws DBException {
        AltibaseDataSource dataSource = (AltibaseDataSource) container.getDataSource();

        if (dataSource == null) {
            throw new DBException(ModelMessages.error_not_connected_to_database);
        }

        // Load domain types
        List<AltibaseDataType> tmpObjectList = new ArrayList<>();

        try {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Load Altibase data types")) {
                try (JDBCPreparedStatement dbStat = session.prepareStatement(
                        "SELECT * FROM V$DATATYPE ORDER BY TYPE_NAME")) {
                    monitor.subTask("Load Altibase domain types");
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        while (dbResult.next()) {
                            if (monitor.isCanceled()) {
                                break;
                            }
                            String typeName = JDBCUtils.safeGetString(dbResult, "TYPE_NAME");
                            if (typeName == null) {
                                continue;
                            }
                            boolean searchabel = (JDBCUtils.safeGetInt(dbResult, "SEARCHABLE") > 0);
                            int precision = JDBCUtils.safeGetInt(dbResult, "COLUMN_SIZE");
                            boolean unsinged = (JDBCUtils.safeGetInt(dbResult, "UNSIGNED_ATTRIBUTE") == 1);
                            String remarks = JDBCUtils.safeGetString(dbResult, "LOCAL_TYPE_NAME");
                            int minScale = JDBCUtils.safeGetInt(dbResult, "MINIMUM_SCALE");
                            int maxScale = JDBCUtils.safeGetInt(dbResult, "MAXIMUM_SCALE");

                            AltibaseDataTypeDomain fieldType = AltibaseDataTypeDomain.getByTypeName(typeName);
                            if (fieldType == null) {
                                // Internal type
                                continue;
                            }

                            AltibaseDataType dataType = new AltibaseDataType(
                                    dataSource, fieldType, typeName, remarks, unsinged, searchabel, precision, minScale, maxScale);
                            tmpObjectList.add(dataType);
                        }
                    }
                }
            } catch (SQLException ex) {
                throw new DBDatabaseException(ex, dataSource);
            }
        } catch (DBException e) {
            if (!handleCacheReadError(e)) {
                throw e;
            }
        }

        mergeCache(tmpObjectList);
    }
}
