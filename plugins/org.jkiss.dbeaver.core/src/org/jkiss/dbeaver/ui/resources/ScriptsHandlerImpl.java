/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.resources;

import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.Log;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorInput;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Scripts handler
 */
public class ScriptsHandlerImpl extends AbstractResourceHandler {

    static final Log log = Log.getLog(ScriptsHandlerImpl.class);

    public static final String SCRIPT_FILE_EXTENSION = "sql"; //$NON-NLS-1$

    public static IFolder getScriptsFolder(IProject project, boolean forceCreate) throws CoreException
    {
    	if (project == null) {
    		IStatus status = new Status(IStatus.ERROR, DBeaverCore.getCorePluginID(), "No active project to locate Script Folder");
			throw new CoreException(status);
		}
        final IFolder scriptsFolder = DBeaverCore.getInstance().getProjectRegistry().getResourceDefaultRoot(project, ScriptsHandlerImpl.class);
        if (!scriptsFolder.exists() && forceCreate) {
            scriptsFolder.create(true, true, new NullProgressMonitor());
        }
        return scriptsFolder;
    }

    @Nullable
    public static IFile findRecentScript(IProject project, @Nullable DBSDataSourceContainer container) throws CoreException
    {
        List<IFile> scripts = new ArrayList<IFile>();
        findRecentScripts(ScriptsHandlerImpl.getScriptsFolder(project, false), container, scripts);
        long recentTimestamp = 0l;
        IFile recentFile = null;
        for (IFile file : scripts) {
            if (file.getLocalTimeStamp() > recentTimestamp) {
                recentTimestamp = file.getLocalTimeStamp();
                recentFile = file;
            }
        }
        return recentFile;
    }

    public static void findRecentScripts(IFolder folder, @Nullable DBSDataSourceContainer container, List<IFile> result)
    {
        try {
            for (IResource resource : folder.members()) {
                if (resource instanceof IFile) {
                    if (SQLEditorInput.getScriptDataSource((IFile) resource) == container) {
                        result.add((IFile) resource);
                    }
                } else if (resource instanceof IFolder) {
                    findRecentScripts((IFolder) resource, container, result);
                }

            }
        } catch (CoreException e) {
            log.debug(e);
        }
    }

    public static IFile createNewScript(IProject project, @Nullable IFolder folder, @Nullable DBSDataSourceContainer dataSourceContainer) throws CoreException
    {
        final IProgressMonitor progressMonitor = new NullProgressMonitor();

        // Get folder
        IFolder scriptsFolder = folder;
        if (scriptsFolder == null) {
            scriptsFolder = ScriptsHandlerImpl.getScriptsFolder(project, true);
            if (dataSourceContainer != null && dataSourceContainer.getPreferenceStore().getBoolean(DBeaverPreferences.SCRIPT_AUTO_FOLDERS)) {
                IFolder dbFolder = scriptsFolder.getFolder(CommonUtils.escapeFileName(dataSourceContainer.getName()));
                if (dbFolder != null) {
                    if (!dbFolder.exists()) {
                        dbFolder.create(true, true, progressMonitor);
                    }
                    scriptsFolder = dbFolder;
                }
            }
        }

        // Make new script file
        IFile tempFile = ContentUtils.getUniqueFile(scriptsFolder, "Script", SCRIPT_FILE_EXTENSION);
        tempFile.create(new ByteArrayInputStream(new byte[]{}), true, progressMonitor);

        // Save ds container reference
        if (dataSourceContainer != null) {
            SQLEditorInput.setScriptDataSource(tempFile, dataSourceContainer);
        }

        return tempFile;
    }

    @Override
    public int getFeatures(IResource resource)
    {
        if (resource instanceof IFile) {
            return FEATURE_OPEN | FEATURE_DELETE | FEATURE_RENAME;
        }
        return super.getFeatures(resource);
    }

    @Override
    public String getTypeName(IResource resource)
    {
        if (resource instanceof IFolder) {
            return "script folder"; //$NON-NLS-1$
        } else {
            return "script"; //$NON-NLS-1$
        }
    }

    @Override
    public String getResourceDescription(IResource resource)
    {
        if (resource instanceof IFile) {
            return new SQLEditorInput((IFile)resource).getName();
        }
        return super.getResourceDescription(resource);
    }

    @Override
    public DBNResource makeNavigatorNode(DBNNode parentNode, IResource resource) throws CoreException, DBException
    {
        DBNResource node = super.makeNavigatorNode(parentNode, resource);
        if (resource instanceof IFolder) {
            if (resource.getParent() instanceof IProject) {
                node.setResourceImage(UIIcon.SCRIPTS);
            }
        } else {
            node.setResourceImage(UIIcon.SQL_SCRIPT);
        }
        return node;
    }

    @Override
    public void openResource(IResource resource) throws CoreException, DBException
    {
        if (resource instanceof IFile) {
            SQLEditorInput sqlInput = new SQLEditorInput((IFile)resource);
            DBeaverUI.getActiveWorkbenchWindow().getActivePage().openEditor(
                sqlInput,
                SQLEditor.class.getName());
        } else {
            log.warn("Cannot open folder resource: " + resource.getName());
        }
    }

    @Nullable
    @Override
    public Collection<DBSDataSourceContainer> getAssociatedDataSources(IResource resource)
    {
        if (resource instanceof IFile) {
            DBSDataSourceContainer dataSource = SQLEditorInput.getScriptDataSource((IFile) resource);
            return dataSource == null ? null : Collections.singleton(dataSource);
        }
        return null;
    }
}
