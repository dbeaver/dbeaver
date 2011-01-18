/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.project;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;

import java.io.ByteArrayInputStream;

/**
 * Scripts handler
 */
public class ScriptsHandlerImpl extends AbstractResourceHandler {

    private static final String SCRIPTS_DIR = "Scripts";

    public static final String RES_TYPE_SCRIPTS = "scripts";

    public static IFolder getScriptsFolder(IProject project)
    {
        return project.getFolder(SCRIPTS_DIR);
    }

    public static IFile createNewScript(IProject project) throws CoreException
    {
        final IFolder scriptsFolder = ScriptsHandlerImpl.getScriptsFolder(project);
        IFile tempFile;
        for (int i = 1; ; i++) {
            tempFile = scriptsFolder.getFile("Script " + i);
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
            scriptsFolder.setPersistentProperty(PROP_RESOURCE_TYPE, RES_TYPE_SCRIPTS);
        }
    }

}
