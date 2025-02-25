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

package org.jkiss.dbeaver.ext.iotdb.model.meta;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBStructUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

public class IoTDBMetaModel extends GenericMetaModel {

    private static final Log log = Log.getLog(IoTDBMetaModel.class);

    public IoTDBMetaModel() {
        super();
    }

    /**
     * @param monitor to create session or to read metadata
     * @param sourceObject source object with required name and parents info
     * @param options for generated DDL
     * @return "test" for temporary
     */
    @Override
    public String getTableDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericTableBase sourceObject,
                              @NotNull Map<String, Object> options) throws DBException {

        DBSEntity table = (DBSEntity) sourceObject;
        String device = table.getParentObject().getName();
        String timeseries = table.getName();
        String commonPrefix = "create timeseries " + device + "." + timeseries + ".";

        StringBuilder ddl = new StringBuilder(200);
        for (DBSEntityAttribute column : CommonUtils.safeCollection(table.getAttributes(monitor))) {
            String columnName = column.getName();
            String columnType = column.getFullTypeName();
            ddl.append(commonPrefix).append(columnName).append(" WITH DATATYPE=").append(columnType).append(";\n");
        }

        return ddl.toString();

    }

    /**
     * @return true to trim extra spaces around columns, tables, objects names
     */
    @Override
    public boolean isTrimObjectNames() {
        return true;
    }
}