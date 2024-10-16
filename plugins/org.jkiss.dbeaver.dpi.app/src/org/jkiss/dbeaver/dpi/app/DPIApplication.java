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
package org.jkiss.dbeaver.dpi.app;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.internal.location.EquinoxLocations;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.dpi.model.DPIConstants;
import org.jkiss.dbeaver.dpi.model.client.ConfigUtils;
import org.jkiss.dbeaver.dpi.server.DPIRestServer;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.dpi.DBPApplicationDPI;
import org.jkiss.dbeaver.model.impl.app.BaseApplicationImpl;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * DPI application
 */
public class DPIApplication extends BaseApplicationImpl implements DBPApplicationDPI {

    private static final Log log = Log.getLog(DPIApplication.class);

    private final Map<String, String[]> driverLibsLocation = new ConcurrentHashMap<>();

    private boolean environmentVariablesAccessible = false;

    public DPIApplication() {
    }

    @Override
    public boolean isHeadlessMode() {
        return true;
    }

    @Override
    public boolean isDetachedProcess() {
        return true;
    }

    @Override
    public Object start(IApplicationContext context) {
        initializeApplicationServices();
        DBPApplication application = DBWorkbench.getPlatform().getApplication();
        try {
            String enableEnvVariablesArgument = getCommandLineArgument(DPIConstants.ARG_ENABLE_ENV);
            if (CommonUtils.isNotEmpty(enableEnvVariablesArgument)) {
                // allow access to env variables only if the main application has them
                this.environmentVariablesAccessible = Boolean.parseBoolean(enableEnvVariablesArgument);
            }
            runServer(context, application);
        } catch (IOException e) {
            log.error(e);
        }

        log.debug("Exiting DPI application");

        return EXIT_OK;
    }

    private void runServer(IApplicationContext appContext, DBPApplication application) throws IOException {
        int portNumber = IOUtils.findFreePort(20000, 65000);
        DPIRestServer server = new DPIRestServer(application, portNumber);
        saveServerInfo(portNumber);
        try {
            log.debug("Started DPI Server at " + portNumber);
            server.join();
        } finally {
            deleteServerInfo();
        }
    }

    private void saveServerInfo(int portNumber) throws IOException {
        Path serverIniFile = getServerIniFile();
        try (BufferedWriter out = Files.newBufferedWriter(serverIniFile, StandardOpenOption.CREATE)) {
            Map<String, String> props = new LinkedHashMap<>();
            props.put(DPIConstants.PARAM_SERVER_PORT, String.valueOf(portNumber));
            props.put("startTime", new Date().toString());
            ConfigUtils.storeProperties(out, props);
        }
    }

    private void deleteServerInfo() throws IOException {
        Path serverIniFile = getServerIniFile();
        if (Files.exists(serverIniFile)) {
            Files.delete(serverIniFile);
        }
    }

    @NotNull
    private Path getServerIniFile() throws IOException {
        String configPath = System.getProperty(EquinoxLocations.PROP_CONFIG_AREA);
        if (configPath == null) {
            throw new IOException("OSGI configuration area property not set");
        }
        configPath = normalizeFileReference(configPath);
        Path configFolder = Path.of(configPath);
        if (!Files.exists(configFolder)) {
            throw new IOException("Configuration folder '" + configFolder + "' doesn't exists");
        }
        Path serverIniFile = configFolder.resolve(DPIConstants.SERVER_INI_FILE);
        return serverIniFile;
    }

    @NotNull
    private String normalizeFileReference(String configPath) {
        if (configPath.startsWith("file:")) {
            configPath = configPath.substring(configPath.indexOf(':') + 1);
        }
        // Check Windows-specific path (/C:/Temp)
        while (configPath.contains(":/") && configPath.startsWith("/")) {
            configPath = configPath.substring(1);
        }
        return configPath;
    }

    @Override
    public void stop() {
        System.out.println("Stopping DPI application");
        super.stop();
    }

    @Override
    public @Nullable Path getDefaultWorkingFolder() {
        return null;
    }

    @NotNull
    @Override
    public Class<? extends DBPPlatform> getPlatformClass() {
        return DPIPlatform.class;
    }

    @Override
    public boolean isEnvironmentVariablesAccessible() {
        return environmentVariablesAccessible;
    }

    @Override
    public String getDefaultProjectName() {
        return "default";
    }

    @Nullable
    private String getCommandLineArgument(@NotNull String argName) {
        String[] args = Platform.getCommandLineArgs();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(argName) && args.length > i + 1) {
                return args[i + 1];
            }
        }
        return null;
    }

    @NotNull
    @Override
    public synchronized List<Path> getDriverLibsLocation(@NotNull String driverId) {
        return Arrays.stream(driverLibsLocation.getOrDefault(driverId, new String[0]))
            .map(Path::of)
            .collect(Collectors.toList());
    }

    public void addDriverLibsLocation(@NotNull String driverId, @NotNull String[] driverLibsLocation) {
        this.driverLibsLocation.put(driverId, driverLibsLocation);
    }

    @Override
    public long getLastUserActivityTime() {
        return -1;
    }

    @NotNull
    public DBPPreferenceStore getPreferenceStore() {
        return new BundlePreferenceStore(DPIPlatform.PLUGIN_ID);
    }
}
