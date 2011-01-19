/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.project;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.ui.DBIcon;

/**
 * Bookmarks handler
 */
public class BookmarksHandlerImpl extends AbstractResourceHandler {

    private static final String BOOKMARKS_DIR = "Bookmarks";

    public static final String RES_TYPE_BOOKMARKS = "bookmarks"; //$NON-NLS-1$

    public static IFolder getBookmarksFolder(IProject project)
    {
        return project.getFolder(BOOKMARKS_DIR);
    }

    @Override
    public void initializeProject(IProject project, IProgressMonitor monitor) throws CoreException, DBException
    {
        final IFolder bookmarksFolder = getBookmarksFolder(project);
        if (!bookmarksFolder.exists()) {
            bookmarksFolder.create(true, true, monitor);
        }
        bookmarksFolder.setPersistentProperty(PROP_RESOURCE_TYPE, RES_TYPE_BOOKMARKS);
    }

    @Override
    public DBNResource makeNavigatorNode(DBNNode parentNode, IResource resource) throws CoreException, DBException
    {
        DBNResource node = super.makeNavigatorNode(parentNode, resource);
        if (resource instanceof IFolder) {
            node.setResourceImage(DBIcon.BOOKMARK_FOLDER.getImage());
        } else {
            node.setResourceImage(DBIcon.BOOKMARK.getImage());
        }
        return node;
    }


}
