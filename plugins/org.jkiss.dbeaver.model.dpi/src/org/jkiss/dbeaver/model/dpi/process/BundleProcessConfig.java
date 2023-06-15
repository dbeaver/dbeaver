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

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.container.ModuleWiring;
import org.eclipse.osgi.storage.BundleInfo;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
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

    private static final Log log = Log.getLog(BundleProcessConfig.class);
    private static final List<String> START_BUNDLES = List.of(
        "org.eclipse.osgi",
        "org.eclipse.core.runtime",
        "org.apache.felix.scr",
        "org.eclipse.equinox.common",
        "org.eclipse.equinox.event",
        "org.eclipse.equinox.simpleconfigurator",
        "org.eclipse.update.configurator"
        );

    private final Map<String, ModuleWiring> dependencies = new LinkedHashMap<>();
    private final Path dataPath;
    private Path cfgDir;
    private Path workspaceDir;
    private Path devPropsFile;

    public BundleProcessConfig(DBRProgressMonitor monitor, String processId) throws IOException {
        dataPath = DBWorkbench.getPlatform().getTempFolder(monitor, "dpi").resolve(processId);
        if (!Files.exists(dataPath)) {
            Files.createDirectories(dataPath);
        }
    }

    void generateApplicationConfiguration() throws IOException {
        cfgDir = dataPath.resolve("configuration");
        if (!Files.exists(cfgDir)) {
            Files.createDirectories(cfgDir);
        }
        Path configIniFile = cfgDir.resolve("config.ini");

        try (BufferedWriter out = Files.newBufferedWriter(configIniFile, StandardOpenOption.CREATE)) {
            storeProperties(out, generateConfigIni());
        }

        Map<String, String> devProps = generateDevProps();
        if (!CommonUtils.isEmpty(devProps)) {
            devPropsFile = cfgDir.resolve("dev.properties");
            try (BufferedWriter out = Files.newBufferedWriter(devPropsFile, StandardOpenOption.CREATE)) {
                storeProperties(out, devProps);
            }
        }

        workspaceDir = dataPath.resolve("workspace");
        if (!Files.exists(workspaceDir)) {
            Files.createDirectories(workspaceDir);
        }


//        EquinoxFwAdminImpl qa = new EquinoxFwAdminImpl();
//        qa.activate(bundle.getBundleContext());
//        Manipulator manipulator = qa.getManipulator();
//        qa.launch(manipulator, )
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
        result.put("osgi.bundles", dependencies.values().stream().map(this::getBundleReference).collect(Collectors.joining(",")));

        ModuleWiring osgiWiring = dependencies.get("org.eclipse.osgi");
        if (osgiWiring != null) {
            result.put("osgi.framework", "file:" + getBundleReference(osgiWiring, false));
        }

        result.put("eclipse.noRegistryCache", "true");

        return result;
    }

    public static void storeProperties(BufferedWriter bw, @NotNull Map<String, String> properties) throws IOException {
        for (Map.Entry<String, String> e : properties.entrySet()) {
            String key = e.getKey();
            String val = e.getValue();
            bw.write(key + "=" + val);
            bw.newLine();
        }
        bw.flush();
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
            if (START_BUNDLES.contains(symbolicName)) {
                startLevel = "@" + START_BUNDLES.indexOf(symbolicName) + ":start";
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

        String javaExePath = System.getProperty("sun.boot.library.path");
        Path exePath = Path.of(javaExePath).resolve("java");
        cmd.add(exePath.toString());

        ModuleWiring launcherWiring = dependencies.get("org.eclipse.equinox.launcher");
        if (launcherWiring != null) {
            cmd.add("-cp");
            cmd.add(getBundleReference(launcherWiring, false));
        }

        cmd.add("org.eclipse.equinox.launcher.Main");

        cmd.add("-launcher");
        cmd.add(System.getProperty("eclipse.launcher"));
        cmd.add("-application");
        cmd.add("org.jkiss.dbeaver.model.dpi.application");
        cmd.add("-configuration");
        cmd.add("file:" + cfgDir.toString());
        if (devPropsFile != null) {
            // Dev mode
            cmd.add("-dev");
            cmd.add("file:" + devPropsFile.toString());
        }
        cmd.add("-data");
        cmd.add(workspaceDir.toString());

        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(dataPath.toFile());
        pb.command(cmd);
        pb.inheritIO();
        return pb.start();
    }
}
