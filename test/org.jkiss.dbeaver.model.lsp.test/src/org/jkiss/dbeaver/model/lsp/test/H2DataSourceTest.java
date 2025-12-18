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
package org.jkiss.dbeaver.model.lsp.test;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Before;

import java.nio.file.Path;
import java.sql.SQLException;

public abstract class H2DataSourceTest extends DBeaverUnitTest {

    protected DataSourceDescriptor dataSourceDescriptor;
    protected DBPProject project;
    protected JDBCSession databaseSession;
    protected final DBRProgressMonitor monitor = new LoggingProgressMonitor();

    @Before
    public void setUp() throws DBException {
        PrefUtils.setDefaultPreferenceValue(
            DBWorkbench.getPlatform().getPreferenceStore(),
            ModelPreferences.UI_DRIVERS_HOME,
            Path.of("../../../dbeaver-resources-drivers-jdbc/binaries")
        );

        dataSourceDescriptor = DocumentServiceTestUtils.createDataSource(monitor);
        databaseSession = DBUtils.openUtilSession(monitor, dataSourceDescriptor, "Internal test session");
        project = DBWorkbench.getPlatform().getWorkspace().getProjects().getFirst();
        project.getDataSourceRegistry().addDataSource(dataSourceDescriptor);

        try (JDBCStatement stmt = databaseSession.createStatement()) {
            Assert.assertFalse(stmt.execute("CREATE TABLE TEST_TABLE1 (id IDENTITY NOT NULL PRIMARY KEY, a VARCHAR, b INT)"));
            Assert.assertFalse(stmt.execute("CREATE TABLE TEST_TABLE2 (id IDENTITY NOT NULL PRIMARY KEY, a VARCHAR, b INT)"));
            for (int i = 0; i < 100; i++) {
                Assert.assertFalse(stmt.execute("INSERT INTO TEST_TABLE1 (a, b) VALUES ('test" + i + "', " + i + ")"));
                Assert.assertFalse(stmt.execute("INSERT INTO TEST_TABLE2 (a, b) VALUES ('test" + i + "', " + i + ")"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
