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
package org.jkiss.dbeaver.core;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.internal.image.FileFormat;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jkiss.awt.injector.ProxyInjector;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.features.DBRFeatureRegistry;
import org.jkiss.dbeaver.ui.browser.BrowsePeerMethods;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.awt.*;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * The activator class controls the plug-in life cycle
 */
public class DBeaverActivator extends AbstractUIPlugin {

    // The shared instance
    private static DBeaverActivator instance;
    private ResourceBundle pluginResourceBundle, coreResourceBundle;
    private PrintStream debugWriter;
    private DBPPreferenceStore preferences;

    private static final String PLUGINS_FOLDER = ".plugins";
    private static final String CORE_RESOURCES_PLUGIN_FOLDER = "org.eclipse.core.resources";
    private static final String STARTUP_ACTIONS_FILE = "dbeaver-startup-actions.properties";

    private static final String RESET_USER_PREFERENCES = "reset_user_preferences";
    private static final String RESET_WORKSPACE_CONFIGURATION = "reset_workspace_configuration";

    private static final Log log = Log.getLog(DBeaverActivator.class);
    
    public DBeaverActivator() {
    }

    public static DBeaverActivator getInstance() {
        return instance;
    }

    @Override
    public void start(BundleContext context)
        throws Exception {
        super.start(context);

        instance = this;

        Bundle bundle = getBundle();
        resetSettingsStartupActions(Platform.getInstanceLocation());
        ModelPreferences.setMainBundle(bundle);
        preferences = new BundlePreferenceStore(bundle);

        DBRFeatureRegistry.getInstance().registerFeatures(CoreFeatures.class);

        try {
            coreResourceBundle = ResourceBundle.getBundle(CoreMessages.BUNDLE_NAME);
            pluginResourceBundle = Platform.getResourceBundle(bundle);
        } catch (MissingResourceException x) {
            coreResourceBundle = null;
        }
        if (getPreferenceStore().getBoolean(DBeaverPreferences.UI_USE_EMBEDDED_AUTH)) {
            try {
                if (Desktop.isDesktopSupported()) {
                    injectProxyPeer();
                } else {
                    getLog().warn("Desktop interface not available");
                    getPreferenceStore().setValue(DBeaverPreferences.UI_USE_EMBEDDED_AUTH, false);
                }
            } catch (Throwable e) {
                getLog().warn(e.getMessage());
                getPreferenceStore().setValue(DBeaverPreferences.UI_USE_EMBEDDED_AUTH, false);
            }
        }

        try {
            injectSvgFileFormat();
        } catch (Throwable e) {
            getLog().error("Unable to inject SVG file format support", e);
        }
    }

    private void injectProxyPeer() throws NoSuchFieldException, IllegalAccessException {
        ProxyInjector proxyInjector = new ProxyInjector();
        proxyInjector.injectBrowseInteraction(BrowsePeerMethods::canBrowseInSWTBrowser, BrowsePeerMethods::browseInSWTBrowser);
    }

    /**
     * Registers {@code SVGFileFormat} as a file format for SWT {@link org.eclipse.swt.graphics.ImageLoader}.
     *
     * @throws Throwable if the registration fails
     */
    @SuppressWarnings("JavaReflectionInvocation")
    private static void injectSvgFileFormat() throws Throwable {
        Field FileFormat_formats = FileFormat.class.getDeclaredField("FORMATS"); //$NON-NLS-1$
        FileFormat_formats.setAccessible(true);

        String[] formats = (String[]) FileFormat_formats.get(null);
        String[] patched = ArrayUtils.add(String.class, formats, "SVG");

        Class<?> Unsafe = Class.forName("sun.misc.Unsafe"); //$NON-NLS-1$
        Method Unsafe_staticFieldBase = Unsafe.getDeclaredMethod("staticFieldBase", Field.class); //$NON-NLS-1$
        Method Unsafe_staticFieldOffset = Unsafe.getDeclaredMethod("staticFieldOffset", Field.class); //$NON-NLS-1$
        Method Unsafe_putObject = Unsafe.getDeclaredMethod("putObject", Object.class, long.class, Object.class); //$NON-NLS-1$

        Field theUnsafe = Unsafe.getDeclaredField("theUnsafe"); //$NON-NLS-1$
        theUnsafe.setAccessible(true);
        Object unsafe = theUnsafe.get(null);

        Unsafe_putObject.invoke(
            unsafe,
            Unsafe_staticFieldBase.invoke(unsafe, FileFormat_formats),
            Unsafe_staticFieldOffset.invoke(unsafe, FileFormat_formats),
            patched
        );
    }

    @Override
    public void stop(BundleContext context)
        throws Exception {
        this.shutdownUI();
        this.shutdownCore();

        if (debugWriter != null) {
            debugWriter.close();
            debugWriter = null;
        }
        instance = null;

        super.stop(context);
    }

    private void shutdownUI() {
        DesktopUI.disposeUI();
    }

    /**
     * Returns the plugin's resource bundle,
     *
     * @return core resource bundle
     */
    public static ResourceBundle getCoreResourceBundle() {
        return getInstance().coreResourceBundle;
    }

    public static ResourceBundle getPluginResourceBundle() {
        return getInstance().pluginResourceBundle;
    }

    public DBPPreferenceStore getPreferences() {
        return preferences;
    }

    private void shutdownCore() {
        try {
            // Dispose core
            if (DesktopPlatform.instance != null) {
                DesktopPlatform.instance.dispose();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.err.println("Internal error after shutdown process:" + e.getMessage()); //$NON-NLS-1$
        }
    }

    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(DesktopPlatform.PLUGIN_ID, path);
    }

    private void resetSettingsStartupActions(@NotNull Location instanceLoc) {
        Path path;
        try {
            path = RuntimeUtils.getLocalPathFromURL(Platform.getInstanceLocation().getURL())
                .resolve(DBPWorkspace.METADATA_FOLDER)
                .resolve(STARTUP_ACTIONS_FILE);
        } catch (Exception e) {
            return;
        }

        if (Files.notExists(path)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            final Properties properties = new Properties();
            properties.load(reader);

            if (!properties.isEmpty()) {
                processResetSettings(instanceLoc, path, properties.stringPropertyNames());
            }
        } catch (Exception e) {
            log.error("Unable to read startup actions", e);
        } finally {
            try {
                Files.delete(path);
            } catch (IOException e) {
                log.error("Unable to delete startup actions file: " + e.getMessage());
            }
        }
    }

    private void processResetSettings(
        @NotNull Location instanceLoc,
        @NotNull Path instancePath,
        @NotNull Set<String> actions
    ) throws Exception {
        final boolean resetUserPreferences = actions.contains(RESET_USER_PREFERENCES);
        final boolean resetWorkspaceConfiguration = actions.contains(RESET_WORKSPACE_CONFIGURATION);

        if (!resetUserPreferences && !resetWorkspaceConfiguration || !instanceLoc.isSet()) {
            return;
        }
        Path path = instancePath.resolve(PLUGINS_FOLDER);
        if (Files.notExists(path) || !Files.isDirectory(path)) {
            return;
        }

        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                log.trace("Deleting " + file);

                try {
                    Files.delete(file);
                } catch (IOException e) {
                    log.trace("Unable to delete " + file + ":" + e.getMessage());
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (dir.endsWith(PLUGINS_FOLDER)) {
                    return FileVisitResult.CONTINUE;
                }

                final Path relative = path.relativize(dir);

                if (resetUserPreferences && !relative.startsWith(CORE_RESOURCES_PLUGIN_FOLDER)) {
                    return FileVisitResult.CONTINUE;
                }

                if (resetWorkspaceConfiguration && relative.startsWith(CORE_RESOURCES_PLUGIN_FOLDER)) {
                    return FileVisitResult.CONTINUE;
                }

                return FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                log.trace("Deleting " + dir);

                try {
                    Files.delete(dir);
                } catch (IOException e) {
                    log.trace("Unable to delete " + dir + ":" + e.getMessage());
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }
}
