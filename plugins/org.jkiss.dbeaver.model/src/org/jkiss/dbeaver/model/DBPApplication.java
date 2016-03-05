/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.model;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDRegistry;
import org.jkiss.dbeaver.model.edit.DBERegistry;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.qm.QMController;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * DBPApplication
 */
public interface DBPApplication
{
    @NotNull
    DBNModel getNavigatorModel();

    @NotNull
    IWorkspace getWorkspace();

    @NotNull
    DBPProjectManager getProjectManager();
    @NotNull
    Collection<IProject> getLiveProjects();

    @NotNull
    QMController getQueryManager();

    @NotNull
    DBDRegistry getValueHandlerRegistry();

    @NotNull
    DBERegistry getEditorsRegistry();

    @NotNull
    DBPPreferenceStore getPreferenceStore();

    @NotNull
    DBPSecurityManager getSecurityManager();

    @NotNull
    File getTempFolder(DBRProgressMonitor monitor, String name) throws IOException;

    @NotNull
    DBRRunnableContext getRunnableContext();
}
