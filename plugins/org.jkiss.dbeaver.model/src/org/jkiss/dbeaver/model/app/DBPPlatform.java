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
import org.jkiss.dbeaver.model.DBConfigurationController;
import org.jkiss.dbeaver.model.DBFileController;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderRegistry;
import org.jkiss.dbeaver.model.data.DBDRegistry;
import org.jkiss.dbeaver.model.edit.DBERegistry;
import org.jkiss.dbeaver.model.fs.DBFRegistry;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.net.DBWHandlerRegistry;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.qm.QMRegistry;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.OSDescriptor;
import org.jkiss.dbeaver.model.sql.SQLDialectMetadataRegistry;
import org.jkiss.dbeaver.model.task.DBTTaskController;

import java.io.IOException;
import java.nio.file.Path;

/**
 * DBPPlatform
 */
public interface DBPPlatform {

    @NotNull
    DBPApplication getApplication();

    @NotNull
    DBPWorkspace getWorkspace();

    @Deprecated // use navigator model from DBPProject
    @Nullable
    DBNModel getNavigatorModel();

    @NotNull
    DBPDataSourceProviderRegistry getDataSourceProviderRegistry();

    @NotNull
    OSDescriptor getLocalSystem();

    /**
     * Returns global QM registry
     */
    @NotNull
    QMRegistry getQueryManager();

    @NotNull
    DBDRegistry getValueHandlerRegistry();

    @NotNull
    DBERegistry getEditorsRegistry();

    @NotNull
    DBFRegistry getFileSystemRegistry();

    @NotNull
    SQLDialectMetadataRegistry getSQLDialectRegistry();

    @NotNull
    DBWHandlerRegistry getNetworkHandlerRegistry();

    @NotNull
    DBPPreferenceStore getPreferenceStore();

    @NotNull
    DBACertificateStorage getCertificateStorage();

    @NotNull
    Path getTempFolder(@NotNull DBRProgressMonitor monitor, @NotNull String name) throws IOException;

    /**
     * Returns platform configuration controller,
     * which keeps configuration which can be shared with other users.
     */
    @NotNull
    DBConfigurationController getConfigurationController();
    
    /**
     * Returns configuration controller,
     * which keeps product configuration which can be shared with other users.
     */
    @NotNull
    DBConfigurationController getProductConfigurationController();
    
    /**
     * Returns configuration controller,
     * which keeps plugin configuration which can be shared with other users.
     */
    @NotNull
    DBConfigurationController getPluginConfigurationController(@Nullable String pluginId);

    /**
     * Local config files are used to store some configuration specific to local machine only.
     */
    @NotNull
    Path getLocalConfigurationFile(String fileName);

    /**
     * File controller allows to read/write binary files (e.g. custom driver libraries)
     */
    @NotNull
    DBFileController getFileController();

    /**
     * Task controller can read and change tasks configuration file
     */
    @NotNull
    DBTTaskController getTaskController();

    @Deprecated
    @NotNull
    Path getApplicationConfiguration();


    @NotNull
    DBPDataFormatterRegistry getDataFormatterRegistry();

    boolean isShuttingDown();

    default boolean isUnitTestMode() {
        return false;
    }
}
