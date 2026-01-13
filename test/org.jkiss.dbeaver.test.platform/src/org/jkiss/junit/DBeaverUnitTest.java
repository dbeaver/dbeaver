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
package org.jkiss.junit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.osgi.annotation.RunWithProduct;
import org.junit.Assert;
import org.mockito.Mock;


@RunWithProduct("DBeaverUnitTest.product")
public abstract class DBeaverUnitTest extends ApplicationUnitTest {

    @Mock
    protected DBRProgressMonitor monitor;

    protected final String lineBreak = System.lineSeparator();

    @NotNull
    protected DBPDataSourceContainer configureTestContainer(@NotNull String driverID) {
        DBPDriver driver = DBWorkbench.getPlatform().getDataSourceProviderRegistry().findDriver(driverID);
        Assert.assertNotNull(driver);
        DBPProject mockProject = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        Assert.assertNotNull(mockProject);

        DBPConnectionConfiguration connectionConfiguration = new DBPConnectionConfiguration();
        DBPDataSourceContainer dataSourceContainer = new DataSourceDescriptor(
            mockProject.getDataSourceRegistry(),
            "test-datasource",
            driver,
            connectionConfiguration);
        dataSourceContainer.setName("Test DS");


        return dataSourceContainer;
    }

}
