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

package org.jkiss.dbeaver.model.app;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;

import java.nio.file.Path;

/**
 * DB application.
 * Application implementors may redefine core app behavior and/or settings.
 */
public interface DBPApplication {

    default void beforeWorkspaceInitialization() {}

    boolean isStandalone();

    /**
     * Primary instance if the first instance of application which locked the workspace.
     * Other instances can be run over the same workspace but they can't lock it.
     */
    boolean isPrimaryInstance();

    /**
     * Headless mode - console interface or server-side mode
     */
    boolean isHeadlessMode();

    /**
     * Shared mode is enabled when dbeaver called thru CLI interface. It is a headless mode.
     * Shared sessions are usually short-term, they launched to execute some particular command and quit.
     * Many UI-specific functions are disabled
     */
    boolean isExclusiveMode();

    /**
     * Multiple users can login into the app at the same time
     */
    boolean isMultiuser();

    /**
     * Distributed application requires remote server.
     */
    boolean isDistributed();

    boolean isDetachedProcess();

    /**
     * Application information details.
     * Like license info or some custom produce info
     */
    String getInfoDetails(DBRProgressMonitor monitor);

    /**
     * Default project name, e.g. 'General'.
     */
    @Nullable
    String getDefaultProjectName();

    @Nullable
    String getProductProperty(@NotNull String propName);

    boolean hasProductFeature(@NotNull String featureName);

    /**
     * @return null if not found, otherwise returns default workspace path
     */
    @Nullable
    Path getDefaultWorkingFolder();

    /**
     * Unique application instance identifier.
     * Generated on every application launch.
     */
    @NotNull
    String getApplicationRunId();

    /**
     * Application start time
     */
    long getApplicationStartTime();

    /**
     * Platform implementation class.
     * Platform instance can be obtained thru {@link DBWorkbench#getPlatform()}.
     * Platform is initialized in a lazy way during application startup.
     */
    @NotNull
    Class<? extends DBPPlatform> getPlatformClass();

    /**
     * Platform UI implementation class.
     * If not specified then base console UI is used.
     */
    @Nullable
    default Class<? extends DBPPlatformUI> getPlatformUIClass() {
        return null;
    }

    /**
     * enables the use of environment variables while the application is in use
     * for example, in a script template
     */
    boolean isEnvironmentVariablesAccessible();

    /**
     * The boolean flag in order identify community application
     *
     * @return - true if community application
     */
    default boolean isCommunity() {
        return false;
    }

    /**
     * Whether the user is allowed to switch workspaces or the default workspace is enforced
     */
    default boolean isWorkspaceSwitchingAllowed() {
        return true;
    }

    /**
     * Whether the statistics collection is enforced or can be disabled by the user
     */
    default boolean isStatisticsCollectionRequired() {
        return false;
    }


    /**
     * Returns last user activity time
     * @return -1 by default
     */
    default long getLastUserActivityTime() {
        return -1;
    }

}
