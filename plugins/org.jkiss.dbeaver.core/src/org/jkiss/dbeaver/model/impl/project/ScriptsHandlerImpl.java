/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.project;

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
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorInput;

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

    public static IFile createNewScript(IProject project) throws CoreException
    {
        final IFolder scriptsFolder = ScriptsHandlerImpl.getScriptsFolder(project);
        IFile tempFile;
        for (int i = 1; ; i++) {
            tempFile = scriptsFolder.getFile("Script " + i + ".sql");
            if (tempFile.exists()) {
                continue;
            }
            tempFile.create(new ByteArrayInputStream(new byte[]{}), true, VoidProgressMonitor.INSTANCE.getNestedMonitor());
            break;
        }
        tempFile.setPersistentProperty(PROP_RESOURCE_TYPE, RES_TYPE_SCRIPTS);
        return tempFile;
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
            node.setResourceImage(DBIcon.SCRIPTS.getImage());
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
