/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.dpi.process;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.dpi.api.DPIController;
import org.jkiss.dbeaver.model.dpi.api.DPISession;
import org.jkiss.dbeaver.model.exec.DBCFeatureNotSupportedException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.IOException;

/**
 * Detached process controller
 */
public class DPIProcessController implements DPIController {

    private static final Log log = Log.getLog(DPIProcessController.class);

    public static DPIController detachDatabaseProcess(DBRProgressMonitor monitor, DBPDataSourceContainer dataSourceContainer) {
        try {
            BundleProcessConfig processConfig = BundleConfigGenerator.generateBundleConfig(monitor, dataSourceContainer);
            if (processConfig != null) {
                return new DPIProcessController(processConfig);
            }
        } catch (Exception e) {
            log.debug("Error generating osgi process from datasource configuration", e);
        }

        return null;
    }

    private final BundleProcessConfig processConfig;
    private final Process process;

    public DPIProcessController(BundleProcessConfig processConfig) throws IOException {
        this.processConfig = processConfig;

        log.debug("Starting detached database application");
        this.process = processConfig.startProcess();

        try {
            int result = this.process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public DPISession openSession() {
        return null;
    }

    @NotNull
    @Override
    public DBPDataSource openDataSource(@NotNull DPISession session, @NotNull DBPDataSourceContainer container) throws DBException {
        throw new DBCFeatureNotSupportedException();
    }

    @Override
    public void closeSession(DPISession session) {

    }
}
