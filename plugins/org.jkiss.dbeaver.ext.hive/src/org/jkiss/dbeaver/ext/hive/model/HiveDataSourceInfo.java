/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.hive.model;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceInfo;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.IOUtils;

import java.io.InputStream;
import java.util.Properties;

class HiveDataSourceInfo extends JDBCDataSourceInfo {

    private static final Log log = Log.getLog(HiveDataSourceInfo.class);

    private String serverVersion;
    private String clientVersion;

    HiveDataSourceInfo(DBRProgressMonitor monitor, DBPDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super(metaData);

        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Read Hive version")) {
            serverVersion = JDBCUtils.queryString(session, "SELECT version()");
        } catch (Exception e) {
            log.debug("Error reading Hive version: " + e.getMessage());
        }
        try {
            ClassLoader classLoader = dataSource.getContainer().getDriver().getClassLoader();
            if (classLoader != null) {
                InputStream propsStream = classLoader.getResourceAsStream("common-version-info.properties");
                if (propsStream != null) {
                    try {
                        Properties props = new Properties();
                        props.load(propsStream);
                        clientVersion = props.getProperty("version");
                    } finally {
                        IOUtils.close(propsStream);
                    }
                }
            }

        } catch (Throwable e) {
            log.debug("Error getting Hive client version");
        }
    }

    @Override
    public String getDatabaseProductVersion() {
        return serverVersion == null ? super.getDatabaseProductVersion() : serverVersion;
    }

    @Override
    public String getDriverVersion() {
        if (clientVersion != null) {
            return clientVersion;
        }
        return super.getDriverVersion();
    }
}
