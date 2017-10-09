/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLiteDataTypeCache
 */
public class FireBirdDataTypeCache extends JDBCBasicDataTypeCache<GenericStructContainer, FireBirdDataType>
{
    private static final Log log = Log.getLog(FireBirdDataTypeCache.class);

    public FireBirdDataTypeCache(GenericStructContainer owner) {
        super(owner);
    }

    @Override
    protected synchronized void loadObjects(DBRProgressMonitor monitor, GenericStructContainer container) throws DBException {
        FireBirdDataSource dataSource = (FireBirdDataSource) container.getDataSource();

        if (dataSource == null) {
            throw new DBException("Not connected to database");
        }
        // Load domain types
        List<FireBirdDataType> tmpObjectList = new ArrayList<>();

        for (FireBirdFieldType fieldType : FireBirdFieldType.values()) {
            FireBirdDataType dataType = new FireBirdDataType(dataSource, fieldType);
            tmpObjectList.add(dataType);
        }

        try {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource.getDataSource(), "Load FireBird domain types")) {
                try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT F.* FROM RDB$FIELDS F ORDER BY RDB$FIELD_NAME"))
                {
                    monitor.subTask("Load FireBird domain types");
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        while (dbResult.next()) {
                            if (monitor.isCanceled()) {
                                break;
                            }
                            String typeName = JDBCUtils.safeGetString(dbResult, "RDB$FIELD_NAME");
                            if (typeName == null) {
                                continue;
                            }
                            int fieldLength = JDBCUtils.safeGetInt(dbResult, "RDB$FIELD_LENGTH");
                            int fieldScale = JDBCUtils.safeGetInt(dbResult, "RDB$FIELD_SCALE");
                            int fieldPrecision = JDBCUtils.safeGetInt(dbResult, "RDB$FIELD_PRECISION");
                            int fieldType = JDBCUtils.safeGetInt(dbResult, "RDB$FIELD_TYPE");
                            int fieldSubType = JDBCUtils.safeGetInt(dbResult, "RDB$FIELD_SUB_TYPE");
                            int charLength = JDBCUtils.safeGetInt(dbResult, "RDB$CHARACTER_LENGTH");
                            int collationId = JDBCUtils.safeGetInt(dbResult, "RDB$COLLATION_ID");
                            int charsetId = JDBCUtils.safeGetInt(dbResult, "RDB$CHARACTER_SET_ID");
                            String validationSource = JDBCUtils.safeGetString(dbResult, "RDB$VALIDATION_SOURCE");
                            String computedSource = JDBCUtils.safeGetString(dbResult, "RDB$COMPUTED_SOURCE");
                            String typeDescription = JDBCUtils.safeGetString(dbResult, "RDB$DESCRIPTION");
                            String defaultSource = JDBCUtils.safeGetString(dbResult, "RDB$DEFAULT_SOURCE");

                            FireBirdFieldType fieldDT = FireBirdFieldType.getById(fieldType);
                            if (fieldDT == null) {
                                log.error("Field type '" + fieldType + "' not found");
                                continue;
                            }
                            String charsetName = dataSource.getMetaFieldValue(FireBirdConstants.TYPE_CHARACTER_SET_NAME, charsetId);
                            boolean notNull = JDBCUtils.safeGetInt(dbResult, "RDB$NULL_FLAG") == 1;

                            FireBirdDataType dataType = new FireBirdDataType(
                                dataSource, fieldDT, fieldSubType, typeName.trim(), typeDescription, false, true, fieldPrecision, fieldScale, fieldScale,
                                fieldLength, charLength,
                                computedSource, validationSource, defaultSource,
                                charsetName,
                                notNull);
                            tmpObjectList.add(dataType);
                        }
                    }
                }
            } catch (SQLException ex) {
                throw new DBException(ex, dataSource);
            }
        } catch (DBException e) {
            if (!handleCacheReadError(e)) {
                throw e;
            }
        }

        mergeCache(tmpObjectList);
    }

}
