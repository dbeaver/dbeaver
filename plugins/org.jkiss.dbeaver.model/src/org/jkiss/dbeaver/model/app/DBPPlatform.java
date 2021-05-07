/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBPExternalFileManager;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderRegistry;
import org.jkiss.dbeaver.model.data.DBDRegistry;
import org.jkiss.dbeaver.model.edit.DBERegistry;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.qm.QMController;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.OSDescriptor;

import java.io.File;
import java.io.IOException;

/**
 * DBPPlatform
 */
public interface DBPPlatform
{
    @NotNull
    DBPApplication getApplication();

    @NotNull
    DBPWorkspace getWorkspace();

    @NotNull
    DBPResourceHandler getDefaultResourceHandler();

    @NotNull
    DBPPlatformLanguage getLanguage();

    @NotNull
    DBNModel getNavigatorModel();

    @NotNull
    DBPDataSourceProviderRegistry getDataSourceProviderRegistry();

    @NotNull
    OSDescriptor getLocalSystem();

    @NotNull
    QMController getQueryManager();

    @NotNull
    DBDRegistry getValueHandlerRegistry();

    @NotNull
    DBERegistry getEditorsRegistry();

    DBPGlobalEventManager getGlobalEventManager();

    @NotNull
    DBPDataFormatterRegistry getDataFormatterRegistry();

    @NotNull
    DBPPreferenceStore getPreferenceStore();

    @NotNull
    DBACertificateStorage getCertificateStorage();

    @NotNull
    DBASecureStorage getSecureStorage();

    @NotNull
    DBPExternalFileManager getExternalFileManager();

    @NotNull
    File getTempFolder(DBRProgressMonitor monitor, String name) throws IOException;

    @NotNull
    File getApplicationConfiguration();

    @NotNull
    File getConfigurationFile(String fileName);

    @NotNull
    File getCustomDriversHome();

    boolean isReadOnly();

    boolean isShuttingDown();

}
