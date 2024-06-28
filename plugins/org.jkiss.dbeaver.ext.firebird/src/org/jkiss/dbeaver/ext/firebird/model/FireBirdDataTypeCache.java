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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
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
 * FireBirdDataTypeCache
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
            throw new DBException(ModelMessages.error_not_connected_to_database);
        }
        // Load domain types
        List<FireBirdDataType> tmpObjectList = new ArrayList<>();

        for (FireBirdFieldType fieldType : FireBirdFieldType.values()) {
            FireBirdDataType dataType = new FireBirdDataType(dataSource, fieldType);
            tmpObjectList.add(dataType);
        }

        try {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Load Firebird domain types")) {
                // Use CAST to improve performance, binaries are too slow
                try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT RDB$FIELD_NAME,RDB$FIELD_LENGTH,RDB$FIELD_SCALE,RDB$FIELD_PRECISION,RDB$FIELD_TYPE,RDB$FIELD_SUB_TYPE,RDB$CHARACTER_LENGTH,RDB$CHARACTER_SET_ID,\n" +
                        "CAST(RDB$VALIDATION_SOURCE AS VARCHAR(512)) VALIDATION_SOURCE,SUBSTRING(RDB$COMPUTED_SOURCE FROM 1 FOR 512) COMPUTED_SOURCE,CAST(RDB$DEFAULT_SOURCE AS VARCHAR(512)) DEFAULT_SOURCE\n" +
                        "FROM RDB$FIELDS F ORDER BY RDB$FIELD_NAME"))
                {
                    monitor.subTask("Load Firebird domain types");
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
                            int fieldScale = Math.abs(JDBCUtils.safeGetInt(dbResult, "RDB$FIELD_SCALE")); // For some reason, FireBird returns the negative value in the scale field.
                            int fieldPrecision = JDBCUtils.safeGetInt(dbResult, "RDB$FIELD_PRECISION");
                            int fieldType = JDBCUtils.safeGetInt(dbResult, "RDB$FIELD_TYPE");
                            int fieldSubType = JDBCUtils.safeGetInt(dbResult, "RDB$FIELD_SUB_TYPE");
                            int charLength = JDBCUtils.safeGetInt(dbResult, "RDB$CHARACTER_LENGTH");
                            int charsetId = JDBCUtils.safeGetInt(dbResult, "RDB$CHARACTER_SET_ID");
                            String validationSource = JDBCUtils.safeGetString(dbResult, "VALIDATION_SOURCE"); // ?
                            String computedSource = JDBCUtils.safeGetString(dbResult, "COMPUTED_SOURCE"); // ?
                            String typeDescription = JDBCUtils.safeGetString(dbResult, "RDB$DESCRIPTION");
                            String defaultSource = JDBCUtils.safeGetString(dbResult, "DEFAULT_SOURCE");

                            FireBirdFieldType fieldDT = FireBirdFieldType.getById(fieldType, fieldSubType);
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
