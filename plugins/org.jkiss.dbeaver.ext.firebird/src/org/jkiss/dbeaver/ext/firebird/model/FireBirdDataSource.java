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
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IntKeyMap;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FireBirdDataSource extends GenericDataSource {

    private static final Log log = Log.getLog(FireBirdDataSource.class);

    private static class MetaFieldInfo {
        int type;
        String name;
        String description;

        MetaFieldInfo(int type, String name, String description) {
            this.type = type;
            this.name = name;
            this.description = description;
        }

        @Override
        public String toString() {
            return name + ":" + type;
        }
    }

    private Map<String, IntKeyMap<MetaFieldInfo>> metaFields = new HashMap<>();

    public FireBirdDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, GenericMetaModel metaModel)
        throws DBException
    {
        super(monitor, container, metaModel, new GenericSQLDialect());
    }

    public String getMetaFieldValue(String name, int type) {
        IntKeyMap<MetaFieldInfo> fieldMap = metaFields.get(name);
        if (fieldMap != null) {
            MetaFieldInfo info = fieldMap.get(type);
            if (info != null) {
                return info.name;
            }
        }
        return null;
    }

    @Override
    public void initialize(DBRProgressMonitor monitor) throws DBException {
        // Read metadata
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read generic metadata")) {
            // Read metadata
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM RDB$TYPES")) {
                monitor.subTask("Load FireBird types");
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        String fieldName = JDBCUtils.safeGetString(dbResult, "RDB$FIELD_NAME");
                        int fieldType = JDBCUtils.safeGetInt(dbResult, "RDB$TYPE");
                        String typeName = JDBCUtils.safeGetString(dbResult, "RDB$TYPE_NAME");
                        String fieldDescription = JDBCUtils.safeGetString(dbResult, "RDB$SYSTEM_FLAG");
                        IntKeyMap<MetaFieldInfo> metaFields = this.metaFields.get(fieldName);
                        if (metaFields == null) {
                            metaFields = new IntKeyMap<>();
                            this.metaFields.put(fieldName, metaFields);
                        }
                        metaFields.put(fieldType, new MetaFieldInfo(fieldType, typeName, fieldDescription));
                    }
                }
            }

        } catch (SQLException ex) {
            log.error("Error reading FB metadata", ex);
        }


        // Init
        super.initialize(monitor);
    }
}
