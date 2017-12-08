/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.data.DBDRegistry;
import org.jkiss.dbeaver.model.edit.DBERegistry;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.qm.QMController;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.format.SQLFormatterRegistry;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * DBPPlatform
 */
public interface DBPPlatform
{
    @NotNull
    DBPApplication getApplication();

    @NotNull
    DBNModel getNavigatorModel();

    @NotNull
    IWorkspace getWorkspace();

    @NotNull
    DBPProjectManager getProjectManager();

    @NotNull
    List<IProject> getLiveProjects();

    @NotNull
    QMController getQueryManager();

    @NotNull
    DBDRegistry getValueHandlerRegistry();

    @NotNull
    DBERegistry getEditorsRegistry();

    @NotNull
    SQLFormatterRegistry getSQLFormatterRegistry();

    @NotNull
    DBPPreferenceStore getPreferenceStore();

    @NotNull
    DBACertificateStorage getCertificateStorage();

    @NotNull
    DBASecureStorage getSecureStorage();

    @NotNull
    File getTempFolder(DBRProgressMonitor monitor, String name) throws IOException;

    boolean isShuttingDown();

}
