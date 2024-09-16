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
package org.jkiss.dbeaver.dpi.model.client;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.dpi.model.DPIConstants;
import org.jkiss.dbeaver.dpi.model.DPIContext;
import org.jkiss.dbeaver.dpi.model.adapters.DPISerializer;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.dpi.DPIController;
import org.jkiss.dbeaver.model.dpi.DPIProcessController;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.rest.RestClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Detached process controller
 */
public class DPIProcessControllerImpl implements DPIProcessController {

    private static final Log log = Log.getLog(DPIProcessControllerImpl.class);

    public static final int PROCESS_PAWN_TIMEOUT = 100000;
    private DPIController dpiRestClient;
    private int dpiServerPort;
    private final Process process;


    public DPIProcessControllerImpl(DBPDataSourceContainer dataSourceContainer, BundleProcessConfig processConfig) throws IOException {
        DPIContext dpiContext = new DPIContext(new LoggingProgressMonitor(log), dataSourceContainer, false);

        log.debug("Starting detached database application");

        Path serverConfigFile = processConfig.getConfigurationFolder().resolve(DPIConstants.SERVER_INI_FILE);
        if (Files.exists(serverConfigFile)) {
            Files.delete(serverConfigFile);
        }

        this.process = processConfig.startProcess();

        // Wait till process will start and flush server configuration file
        long startTime = System.currentTimeMillis();
        while (process.isAlive()) {
            if (Files.exists(serverConfigFile)) {
                Map<String, String> props = ConfigUtils.readPropertiesFromFile(serverConfigFile);
                dpiServerPort = CommonUtils.toInt(props.get(DPIConstants.PARAM_SERVER_PORT));
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
            dpiRestClient = RestClient
                .builder(getRemoteEndpoint(), DPIController.class)
                .setGson(DPISerializer.createClientSerializer(dpiContext))
                .setUserAgent(GeneralUtils.getProductTitle())
                .create();

            validateRestClient();
        } catch (Throwable e) {
            terminateChildProcess();
            throw new IOException("Error connecting to DPI Server", e);
        }
        dpiContext.setController(dpiRestClient);
    }

    private void terminateChildProcess() {
        if (dpiRestClient != null) {
            try {
                dpiRestClient.close();
            } catch (Exception e) {
                log.debug(e);
            } finally {
                dpiRestClient = null;
            }
        }
        if (this.process != null && this.process.isAlive()) {
            this.process.destroyForcibly();
        }
    }

    private void validateRestClient() throws DBException {
        RuntimeUtils.pause(50);

        dpiRestClient.ping();
    }

    @NotNull
    private URI getRemoteEndpoint() {
        String endpoint = "http://localhost:" + dpiServerPort + "/";
        try {
            return new URI(endpoint);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Wrong REST URI: " + endpoint, e);
        }
    }

    public DPIController getClient() {
        return dpiRestClient;
    }

    @Override
    public void close() {
        if (this.process != null) {
            terminateChildProcess();
        }
    }
}
