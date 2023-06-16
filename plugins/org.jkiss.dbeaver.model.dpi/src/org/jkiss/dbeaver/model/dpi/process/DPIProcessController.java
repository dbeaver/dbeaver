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
import org.jkiss.dbeaver.model.dpi.app.DPIApplication;
import org.jkiss.dbeaver.model.exec.DBCFeatureNotSupportedException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Detached process controller
 */
public class DPIProcessController implements DPIController {

    private static final Log log = Log.getLog(DPIProcessController.class);

    public static final int PROCESS_PAWN_TIMEOUT = 10000;
    private int dpiServerPort;

    public static DPIController detachDatabaseProcess(DBRProgressMonitor monitor, DBPDataSourceContainer dataSourceContainer) throws IOException {
        try {
            BundleProcessConfig processConfig = BundleConfigGenerator.generateBundleConfig(monitor, dataSourceContainer);
            return new DPIProcessController(processConfig);
        } catch (Exception e) {
            throw new IOException("Error generating osgi process from datasource configuration", e);
        }
    }

    private final BundleProcessConfig processConfig;
    private final Process process;

    public DPIProcessController(BundleProcessConfig processConfig) throws IOException {
        this.processConfig = processConfig;

        log.debug("Starting detached database application");

        Path serverConfigFile = processConfig.getConfigurationFolder().resolve(DPIApplication.SERVER_INI_FILE);
        if (Files.exists(serverConfigFile)) {
            Files.delete(serverConfigFile);
        }

        this.process = processConfig.startProcess();

        // Wait till process will start and flush server configuration file
        long startTime = System.currentTimeMillis();
        while (process.isAlive()) {
            if (Files.exists(serverConfigFile)) {
                Map<String, String> props = ConfigUtils.readPropertiesFromFile(serverConfigFile);
                dpiServerPort = CommonUtils.toInt(props.get(DPIApplication.PARAM_SERVER_PORT));
                if (dpiServerPort == 0) {
                    // Maybe it was incomplete config file
                    continue;
                } else {
                    break;
                }
            }
            if (System.currentTimeMillis() - startTime > PROCESS_PAWN_TIMEOUT) {
                // Timeout
                terminateChildProcess();
                throw new IOException("Error starting child DPI process. Timeout (" + PROCESS_PAWN_TIMEOUT + ") exceeded.");
            }
            RuntimeUtils.pause(50);
        }

        if (!process.isAlive()) {
            throw new IOException("Child DPI process start is failed (" + process.exitValue() + ")");
        }

        try {
            validateResetClient();
        } catch (IOException e) {
            terminateChildProcess();
            throw new IOException("Error connecting to DPI Server", e);
        }
    }

    private void terminateChildProcess() {
        this.process.destroyForcibly();
    }

    private void validateResetClient() throws IOException {
        RuntimeUtils.pause(50);

        URL dpiServerURL = new URL("http://localhost:" + dpiServerPort + "/");
        log.debug("Connect to DPI Server " + dpiServerURL);
        HttpURLConnection dpiConnection = (HttpURLConnection) dpiServerURL.openConnection();
        try {
            dpiConnection.setRequestMethod("POST");
            dpiConnection.setRequestProperty("content-type", "text/json");
            dpiConnection.setDoOutput(true);
            dpiConnection.connect();
            dpiConnection.getOutputStream().write("{ 'test': 1 }".getBytes());
            Object response = dpiConnection.getContent();
            log.debug(response);
        }
        catch (Throwable e) {
            throw new IOException("Error pinging DPI server", e);
        }
        finally {
            dpiConnection.disconnect();
        }
    }

    @Override
    public DPISession openSession() throws DBCFeatureNotSupportedException {
        throw new DBCFeatureNotSupportedException();
    }

    @NotNull
    @Override
    public DBPDataSource openDataSource(@NotNull DPISession session, @NotNull DBPDataSourceContainer container) throws DBException {
        throw new DBCFeatureNotSupportedException();
    }

    @Override
    public void closeSession(DPISession session) {

    }

    @Override
    public void close() throws Exception {
        if (this.process != null) {
            if (this.process.isAlive()) {
                terminateChildProcess();
            }
        }
    }
}
