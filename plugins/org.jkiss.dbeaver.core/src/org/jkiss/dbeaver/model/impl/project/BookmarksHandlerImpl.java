/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.project;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jkiss.dbeaver.DBException;

/**
 * Bookmarks handler
 */
public class BookmarksHandlerImpl extends AbstractResourceHandler {

    private static final String BOOKMARKS_DIR = "Bookmarks";

    public static final String RES_TYPE_BOOKMARKS = "bookmarks";

    public static IFolder getBookmarksFolder(IProject project)
    {
        return project.getFolder(BOOKMARKS_DIR);
    }

/*
    public static IFile createNewScript(IProject project) throws CoreException
    {
        final IFolder scriptsFolder = BookmarksHandlerImpl.getBookmarksFolder(project);
        IFile tempFile;
        for (int i = 1; ; i++) {
            tempFile = scriptsFolder.getFile("Script " + i + ".sql");
            if (tempFile.exists()) {
                continue;
            }
            tempFile.create(new ByteArrayInputStream(new byte[]{}), true, VoidProgressMonitor.INSTANCE.getNestedMonitor());
            break;
        }
        tempFile.setPersistentProperty(PROP_RESOURCE_TYPE, RES_TYPE_BOOKMARKS);
        return tempFile;
    }

*/
    @Override
    public void initializeProject(IProject project, IProgressMonitor monitor) throws CoreException, DBException
    {
        final IFolder bookmarksFolder = getBookmarksFolder(project);
        if (!bookmarksFolder.exists()) {
            bookmarksFolder.create(true, true, monitor);
        }
        bookmarksFolder.setPersistentProperty(PROP_RESOURCE_TYPE, RES_TYPE_BOOKMARKS);
    }

}
