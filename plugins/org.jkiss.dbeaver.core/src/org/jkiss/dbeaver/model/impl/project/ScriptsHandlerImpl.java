/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.project;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorInput;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.ByteArrayInputStream;

/**
 * Scripts handler
 */
public class ScriptsHandlerImpl extends AbstractResourceHandler {

    private static final String SCRIPTS_DIR = "Scripts";

    public static final String RES_TYPE_SCRIPTS = "scripts"; //$NON-NLS-1$

    public static IFolder getScriptsFolder(IProject project)
    {
        return project.getFolder(SCRIPTS_DIR);
    }

    public static IFile createNewScript(IProject project, IFolder folder, DBSDataSourceContainer dataSourceContainer) throws CoreException
    {
        final IProgressMonitor progressMonitor = VoidProgressMonitor.INSTANCE.getNestedMonitor();

        // Get folder
        IFolder scriptsFolder = folder;
        if (scriptsFolder == null) {
            scriptsFolder = ScriptsHandlerImpl.getScriptsFolder(project);
            if (dataSourceContainer != null && dataSourceContainer.getPreferenceStore().getBoolean(PrefConstants.SCRIPT_AUTO_FOLDERS)) {
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
        IFile tempFile = ContentUtils.getUniqueFile(scriptsFolder, "Script", "sql");
        tempFile.create(new ByteArrayInputStream(new byte[]{}), true, progressMonitor);
        tempFile.setPersistentProperty(PROP_RESOURCE_TYPE, RES_TYPE_SCRIPTS);

        // Save ds container reference
        if (dataSourceContainer != null) {
            new SQLEditorInput(tempFile, dataSourceContainer);
        }

        return tempFile;
    }

    @Override
    public int getFeatures(IResource resource)
    {
        if (resource instanceof IFolder) {
            if (resource.getParent() instanceof IFolder) {
                return FEATURE_DELETE | FEATURE_MOVE_INTO | FEATURE_RENAME | FEATURE_CREATE_FOLDER;
            }
            return FEATURE_MOVE_INTO | FEATURE_CREATE_FOLDER;
        } else {
            return FEATURE_OPEN | FEATURE_DELETE | FEATURE_RENAME;
        }
    }

    public String getTypeName(IResource resource)
    {
        if (resource instanceof IFolder) {
            return "script folder";
        } else {
            return "script";
        }
    }

    @Override
    public void initializeProject(IProject project, IProgressMonitor monitor) throws CoreException, DBException
    {
        final IFolder scriptsFolder = getScriptsFolder(project);
        if (!scriptsFolder.exists()) {
            scriptsFolder.create(true, true, monitor);
        }
        scriptsFolder.setPersistentProperty(PROP_RESOURCE_TYPE, RES_TYPE_SCRIPTS);
    }

    @Override
    public DBNResource makeNavigatorNode(DBNNode parentNode, IResource resource) throws CoreException, DBException
    {
        DBNResource node = super.makeNavigatorNode(parentNode, resource);
        if (resource instanceof IFolder) {
            if (resource.getParent() instanceof IProject) {
                node.setResourceImage(DBIcon.SCRIPTS.getImage());
            }
        } else {
            node.setResourceImage(DBIcon.SQL_SCRIPT.getImage());
        }
        return node;
    }

    @Override
    public void openResource(IResource resource, IWorkbenchWindow window) throws CoreException, DBException
    {
        if (resource instanceof IFile) {
            SQLEditorInput sqlInput = new SQLEditorInput((IFile)resource);
            window.getActivePage().openEditor(
                sqlInput,
                SQLEditor.class.getName());
        } else {
            throw new DBException("Cannot open folder");
        }
    }
}
