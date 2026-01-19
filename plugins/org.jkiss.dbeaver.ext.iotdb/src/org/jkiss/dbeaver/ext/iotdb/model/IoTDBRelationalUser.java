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

package org.jkiss.dbeaver.ext.iotdb.model;

import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.ArrayList;
import java.util.List;

public class IoTDBRelationalUser extends IoTDBAbstractUser {

    private static final Log log = Log.getLog(IoTDBRelationalUser.class);

    private List<IoTDBDatabase> databases;      // All databases and tables with permissions
    private IoTDBDatabase allDatabase;

    public IoTDBRelationalUser(IoTDBDataSource dataSource,
                               String userName,
                               DBRProgressMonitor monitor) throws DBException {
        super(dataSource, userName);

        try {
            loadDatabases(monitor);
        } catch (DBException e) {
            log.error("Error loading databases and tables", e);
            throw new DBDatabaseException(e, this.getDataSource());
        }
    }

    public IoTDBDatabase getDatabaseAll() {
        if (allDatabase == null) {
            List<String> tables = new ArrayList<>();
            tables.add("(ALL)");
            allDatabase = new IoTDBDatabase("(ALL)", tables);
        }
        return allDatabase;
    }

    /**
     * Get the list of databases
     *
     * @return List of IoTDBDatabase
     */
    public List<IoTDBDatabase> getDatabases() {
        return databases;
    }

    /**
     * Load databases and tables
     *
     * @throws DBException if an error occurs
     */
    public void loadDatabases(DBRProgressMonitor monitor) throws DBException {
        databases = new ArrayList<>();
        boolean isTree = dataSource.isTree();

        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Databases and Tables Info")) {
            String sql = "show databases"; // use this instead of select * from information_schema to prevent permission issues
            JDBCStatement stmt = session.createStatement();
            JDBCResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String currentDatabase = rs.getString("Database");
                List<String> currentTables = new ArrayList<>();

                sql = isTree ? ("show devices " + currentDatabase + ".**") : ("show tables in " + currentDatabase);
                JDBCStatement stmt2 = session.createStatement();
                JDBCResultSet rs2 = stmt2.executeQuery(sql);
                if (isTree) {
                    int prefixLength = currentDatabase.length() + 1;
                    while (rs2.next()) {
                        currentTables.add(rs2.getString("Device").substring(prefixLength));
                    }
                } else {
                    while (rs2.next()) {
                        currentTables.add(rs2.getString("TableName"));
                    }
                }
                IoTDBDatabase newDatabase = new IoTDBDatabase(currentDatabase, currentTables);
                databases.add(newDatabase);
            }
        } catch (Exception e) {
            log.error("Error loading databases and tables", e);
        }
    }

    public static class IoTDBDatabase {

        public final String name;
        public final List<String> tables;

        public IoTDBDatabase(String name, List<String> tables) {
            this.name = name;
            this.tables = tables;
        }
    }
}
