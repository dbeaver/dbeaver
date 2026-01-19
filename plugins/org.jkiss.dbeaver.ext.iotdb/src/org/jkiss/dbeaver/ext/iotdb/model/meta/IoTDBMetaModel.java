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
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Map;

public class IoTDBMetaModel extends GenericMetaModel {

    private static final Log log = Log.getLog(IoTDBMetaModel.class);

    /**
     * Get DDL for table.
     *
     * @param monitor to create session or to read metadata
     * @param sourceObject source object with required name and parents info
     * @param options for generated DDL
     *
     * @return "test" for temporary
     */
    @Override
    public String getTableDDL(@NotNull DBRProgressMonitor monitor,
                              @NotNull GenericTableBase sourceObject,
                              @NotNull Map<String, Object> options)  {

        String device1 = ((DBSEntity) sourceObject).getParentObject().getName();
        String device2 = ((DBSEntity) sourceObject).getName();
        String device = device1 + "." + device2;

        boolean isAligned = false;
        StringBuilder ddl = new StringBuilder(200);

        try (JDBCSession session = DBUtils.openMetaSession(monitor, (DBSObject) sourceObject, "Execute SQL for IoTDB-tree")) {
            String sql = String.format("show devices %s", device);
            JDBCStatement stmt = session.createStatement();
            JDBCResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                isAligned = (rs.getString("IsAligned")).equals("true");
            }
        } catch (Exception e) {
            log.error("Error executing sql", e);
        }

        try (JDBCSession session = DBUtils.openMetaSession(monitor, (DBSObject) sourceObject, "Execute SQL for IoTDB-tree")) {
            String sql = String.format("show timeseries %s.**", device);
            JDBCStatement stmt = session.createStatement();
            JDBCResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                ddl.append("delete timeseries ").append(rs.getString("Timeseries")).append(";\n");
            }
        } catch (Exception e) {
            log.error("Error executing sql", e);
        }

        ddl.append("\n");

        try (JDBCSession session = DBUtils.openMetaSession(monitor, (DBSObject) sourceObject, "Execute SQL for IoTDB-tree")) {
            String sql = String.format("show timeseries %s.**", device);
            JDBCStatement stmt = session.createStatement();
            JDBCResultSet rs = stmt.executeQuery(sql);
            if (isAligned) {
                String prefix = device + ".";
                ddl.append("create aligned timeseries ").append(device).append("(");
                while (rs.next()) {
                    String timeseries = rs.getString("Timeseries").replaceFirst("^" + prefix, "");
                    ddl.append(timeseries).append(" ");
                    ddl.append(rs.getString("DataType")).append(" ");
                    ddl.append("encoding=").append(rs.getString("Encoding")).append(" ");
                    ddl.append("compressor=").append(rs.getString("Compression")).append(", ");
                }
                ddl.setLength(ddl.length() - 2);
                ddl.append(");\n");
            } else {
                while (rs.next()) {
                    ddl.append("create timeseries ").append(rs.getString("Timeseries"));
                    ddl.append(" with datatype=").append(rs.getString("DataType"));
                    ddl.append(", encoding=").append(rs.getString("Encoding")).append(";\n");
                }
            }
        } catch (Exception e) {
            log.error("Error executing sql", e);
        }

        return ddl.toString();
    }

    /**
     * Check if object names should be trimmed.
     *
     * @return true to trim extra spaces around columns, tables, objects names
     */
    @Override
    public boolean isTrimObjectNames() {
        return true;
    }
}