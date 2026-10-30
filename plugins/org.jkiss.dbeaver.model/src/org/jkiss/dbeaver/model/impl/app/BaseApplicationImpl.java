/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl.app;

import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

/**
 * Base application implementation
 */
public abstract class BaseApplicationImpl extends AbstractApplication {
    private static final Log log = Log.getLog(BaseApplicationImpl.class);

    public static final String DBEAVER_DATA_DIR = "DBeaverData";
    private static final String INSTALL_DATA_DIR = "install-data";
    public static final String DEFAULT_WORKSPACE_FOLDER = "workspace6";
    public static final String SECURE_DATA_FOLDER = "secure";

    public static final String ECLIPSE_EXIT_DATA = "eclipse.exitdata";

    private final Path workingDirectory;
    private Path workspacePath;

    protected BaseApplicationImpl() {
        this(DBEAVER_DATA_DIR, DEFAULT_WORKSPACE_FOLDER);
    }

    protected BaseApplicationImpl(
        @NotNull String defaultWorkspaceLocation,
        @NotNull String defaultAppWorkspaceName
    ) {
        workingDirectory = Path.of(RuntimeUtils.getWorkingDirectory(defaultWorkspaceLocation));
        cleanInstallCache();
        // Workspace dir
        workspacePath = RuntimeUtils.getWorkspacePath(workingDirectory.toString(), defaultAppWorkspaceName);
    }

    // Clean old versions of this product only when its configuration lives in DBeaverData/install-data.
    private void cleanInstallCache() {
        try {
            Path cacheRoot = workingDirectory.resolve(INSTALL_DATA_DIR).toAbsolutePath().normalize();
            Path currentVersion = RuntimeUtils.getLocalPathFromURL(Platform.getConfigurationLocation().getURL())
                .toAbsolutePath().normalize();
            Path productDirectory = currentVersion.getParent();
            if (productDirectory == null || !cacheRoot.equals(productDirectory.getParent()) || !Files.isDirectory(cacheRoot)) {
                return;
            }
            // Use a non-daemon thread so short-lived CLI processes can complete the cleanup as well.
            Thread.ofPlatform().name("DBeaver install cache cleanup").start(() ->
                InstallCacheCleaner.clean(cacheRoot, currentVersion, Instant.now().minus(Duration.ofDays(7)), log::warn)
            );
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Unable to locate install cache", e);
        }
    }

    @NotNull
    @Override
    public Path getGlobalDataPath() {
        return workingDirectory;
    }

    @NotNull
    public Path getWorkspacePath() {
        return workspacePath;
    }

    protected void setWorkspacePath(@NotNull Path workspacePath) {
        this.workspacePath = workspacePath;
    }
}
