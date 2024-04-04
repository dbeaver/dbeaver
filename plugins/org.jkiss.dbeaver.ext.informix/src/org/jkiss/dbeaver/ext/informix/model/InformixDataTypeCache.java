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
package org.jkiss.dbeaver.ext.informix.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericDataType;
import org.jkiss.dbeaver.ext.generic.model.GenericDataTypeCache;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

class InformixDataTypeCache extends GenericDataTypeCache {

    private static final Log log = Log.getLog(InformixDataTypeCache.class);

    public InformixDataTypeCache(GenericStructContainer container) {
        super(container);
    }

    @Override
    protected void addCustomObjects(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericStructContainer owner,
        @NotNull List<GenericDataType> genericDataTypes
    ) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, owner, "Load informix data types")) {
            beforeCacheLoading(session, owner);
            try (JDBCStatement dbStat = session.prepareStatement("SELECT * FROM informix.sysxtdtypes")) {
                monitor.subTask("Load Informix data types");
                dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
                dbStat.executeStatement();
                try (JDBCResultSet dbResult = dbStat.getResultSet()) {
                    if (dbResult != null) {
                        while (dbResult.next()) {
                            if (monitor.isCanceled()) {
                                return;
                            }
                            String typeName = JDBCUtils.safeGetString(dbResult, "name");
                            if (CommonUtils.isEmpty(typeName)) {
                                continue;
                            }
                            if (DBUtils.findObject(genericDataTypes, typeName) != null) {
                                log.debug("Duplicate data type: " + typeName);
                                continue;
                            }
                            int dataType = JDBCUtils.safeGetInt(dbResult, "type");
                            long length = JDBCUtils.safeGetLong(dbResult, "length");
                            long maxLength = JDBCUtils.safeGetLong(dbResult, "maxlen");
                            String mode = JDBCUtils.safeGetString(dbResult, "mode");
                            GenericDataType dt = new GenericDataType(
                                owner,
                                Types.VARCHAR,
                                typeName,
                                null,
                                false,
                                false,
                                (int) maxLength,
                                0,
                                0);
                            genericDataTypes.add(dt);
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            throw new DBException(ex, owner.getDataSource());
        }
    }

}
