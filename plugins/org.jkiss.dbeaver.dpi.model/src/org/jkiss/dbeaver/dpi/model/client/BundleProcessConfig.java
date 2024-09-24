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

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.container.ModuleWiring;
import org.eclipse.osgi.storage.BundleInfo;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.dpi.model.DPIConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class BundleProcessConfig {

    private static final Map<String, Integer> START_BUNDLES = Map.of(
        "org.eclipse.osgi", -1,
        "org.eclipse.core.runtime", 4,
        "org.apache.felix.scr", 2,
        "org.eclipse.equinox.common", 2,
        "org.eclipse.equinox.event", 2,
        "org.eclipse.equinox.simpleconfigurator", 1,
        "org.eclipse.update.configurator", 10
    );

    private final Map<String, ModuleWiring> dependencies = new LinkedHashMap<>();
    private final Path dataPath;
    private Path configurationFolder;
    private Path workspaceDir;
    private Path devPropsFile;
    private int serverPort;

    public BundleProcessConfig(DBRProgressMonitor monitor, String processId) throws IOException {
        dataPath = DBWorkbench.getPlatform().getTempFolder(monitor, "dpi").resolve(processId);
        if (!Files.exists(dataPath)) {
            Files.createDirectories(dataPath);
        }
    }

    public Path getDataPath() {
        return dataPath;
    }

    public Path getConfigurationFolder() {
        return configurationFolder;
    }

    public Path getWorkspaceDir() {
        return workspaceDir;
    }

    public int getServerPort() {
        return serverPort;
    }

    void generateApplicationConfiguration() throws IOException {
        configurationFolder = dataPath.resolve("configuration");
        if (!Files.exists(configurationFolder)) {
            Files.createDirectories(configurationFolder);
        }
        Path configIniFile = configurationFolder.resolve("config.ini");

        try (BufferedWriter out = Files.newBufferedWriter(configIniFile, StandardOpenOption.CREATE)) {
            ConfigUtils.storeProperties(out, generateConfigIni());
        }

        Map<String, String> devProps = generateDevProps();
        if (!CommonUtils.isEmpty(devProps)) {
            devPropsFile = configurationFolder.resolve("dev.properties");
            try (BufferedWriter out = Files.newBufferedWriter(devPropsFile, StandardOpenOption.CREATE)) {
                ConfigUtils.storeProperties(out, devProps);
            }
        }

        workspaceDir = DBWorkbench.getPlatform().getWorkspace().getAbsolutePath();
//        if (!Files.exists(workspaceDir)) {
//            Files.createDirectories(workspaceDir);
//        }
    }

    private Map<String, String> generateDevProps() throws IOException {
        Map<Path, ModuleWiring> devFolders = new LinkedHashMap<>();
        for (ModuleWiring wire : dependencies.values()) {
            String bundleReference = getBundleReference(wire, false);
            Path bundlePath = Path.of(bundleReference);
            if (Files.isDirectory(bundlePath)) {
                devFolders.put(bundlePath, wire);
            }
        }
        if (devFolders.isEmpty()) {
            return null;
        }
        Map<String, String> devProps = new LinkedHashMap<>();
        for (Map.Entry<Path, ModuleWiring> devInfo : devFolders.entrySet()) {
            String bundleId = devInfo.getValue().getBundle().getSymbolicName();
            String bundleLibPath = "target/classes";
            // TODO: add embedded jars
            devProps.put(bundleId, bundleLibPath);
        }
        return devProps;
    }

    private Map<String, String> generateConfigIni() throws IOException {
        URL installPathURL = Platform.getInstallLocation().getURL();
        Map<String, String> result = new LinkedHashMap<>();
        result.put("osgi.install.area", getNormalizeFileReference(installPathURL.toString()));
        result.put("osgi.bundles.defaultStartLevel", "4");
        result.put("osgi.configuration.cascaded", Boolean.FALSE.toString());
        result.put("osgi.bundles",
            dependencies.values().stream().map(this::getBundleReference).collect(Collectors.joining(",")));

        ModuleWiring osgiWiring = dependencies.get("org.eclipse.osgi");
        if (osgiWiring != null) {
            result.put("osgi.framework", "file:" + getBundleReference(osgiWiring, false));
        }

        result.put("eclipse.noRegistryCache", "true");

        return result;
    }

    private String getBundleReference(ModuleWiring wiring) {
        return getBundleReference(wiring, true);
    }

    private String getBundleReference(ModuleWiring wiring, boolean reference) {
        String symbolicName = wiring.getBundle().getSymbolicName();

        Object revisionInfo = wiring.getResource().getRevisionInfo();
        if (revisionInfo instanceof BundleInfo.Generation) {
            BundleFile bundleFile = ((BundleInfo.Generation) revisionInfo).getBundleFile();
            File baseFile = bundleFile.getBaseFile();
            String startLevel = "";
            if (START_BUNDLES.containsKey(symbolicName)) {
                startLevel = "@" + START_BUNDLES.get(symbolicName) + ":start";
            }
            String jarPath = fixWindowsPath(baseFile.getAbsoluteFile().getAbsolutePath());
            if (reference) {
                return "reference:file:" + jarPath + startLevel;
            } else {
                return jarPath;
            }
        }
        return fixWindowsPath(wiring.getBundle().getLocation());
    }

    public boolean isValid() {
        return !dependencies.isEmpty();
    }

    public boolean containsWiring(ModuleWiring wiring) {
        return dependencies.containsKey(wiring.getBundle().getSymbolicName());
    }

    public void addWiring(ModuleWiring wiring) {
        this.dependencies.put(wiring.getBundle().getSymbolicName(), wiring);
    }

    private static String fixWindowsPath(String path) {
        return path.replace('\\', '/');
    }

    private static String getNormalizeFileReference(@NotNull String path) {
        path = fixWindowsPath(path);
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (!path.startsWith("file:")) {
            return "file:" + path;
        } else {
            return path;
        }
    }

    public Process startProcess() throws IOException {
        List<String> cmd = new ArrayList<>();

        String javaExePath = resolveJavaExePath();
        cmd.add(javaExePath);

        String launcherApp = "org.eclipse.equinox.launcher.Main";
        ModuleWiring launcherWiring = dependencies.get("org.jkiss.dbeaver.launcher");
        if (launcherWiring != null) {
            String bundleReference = getBundleReference(launcherWiring, false);
            if (Files.isDirectory(Path.of(bundleReference))) {
                // Seems to be dev env
                bundleReference += "/target/classes";
            }
            if (Files.exists(Path.of(bundleReference))) {
                cmd.add("-cp");
                cmd.add(bundleReference);
                launcherApp = "org.jkiss.dbeaver.launcher.DBeaverLauncher";
            }
        }
        String debugParams = System.getProperty("dbeaver.debug.dpi.launch.parameters");
        if (CommonUtils.isNotEmpty(debugParams)) {
            //-Ddbeaver.debug.dpi.launch.parameters=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=localhost:15005
            cmd.add(debugParams);
        }
        cmd.add(launcherApp);

        cmd.add("-application");
        cmd.add("org.jkiss.dbeaver.dpi.app.application");
        cmd.add("-configuration");
        cmd.add("file:" + configurationFolder.toString());
        if (devPropsFile != null) {
            // Dev mode
            cmd.add("-dev");
            cmd.add("file:" + devPropsFile);
        }
        cmd.add("-data");
        cmd.add(workspaceDir.toString());

        cmd.add(DPIConstants.ARG_ENABLE_ENV);
        cmd.add(String.valueOf(!DBWorkbench.getPlatform().getApplication().isHeadlessMode()));

        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(dataPath.toFile());
        pb.command(cmd);
        pb.inheritIO();
        return pb.start();
    }

    private String resolveJavaExePath() throws IOException {
        String javaExePath = System.getProperty("sun.boot.library.path");
        Path javaInstallationDir = Path.of(javaExePath);
        Path exePath = javaInstallationDir.resolve("java");
        if (Files.exists(exePath)) {
            return exePath.toString();
        }

        exePath = javaInstallationDir.getParent().resolve("bin/java");
        if (Files.exists(exePath)) {
            return exePath.toString();
        }
        exePath = javaInstallationDir.getParent().resolve("bin/java.exe");
        if (Files.exists(exePath)) {
            return exePath.toString();
        }

        throw new IOException("Java exe not found");
    }
}
