/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.project.BookmarkStorage;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.InputStream;

/**
 * DBNBookmark
 */
public class DBNBookmark extends DBNResource
{
    private BookmarkStorage storage;

    public DBNBookmark(DBNNode parentNode, IResource resource, DBPResourceHandler handler) throws DBException, CoreException
    {
        super(parentNode, resource, handler);
        storage = new BookmarkStorage((IFile)resource, true);
    }

    protected void dispose(boolean reflect)
    {
        storage.dispose();
        super.dispose(reflect);
    }

    public String getNodeName()
    {
        return storage.getTitle();
    }

    public String getNodeDescription()
    {
        return storage.getDescription();
    }

    public Image getNodeIcon()
    {
        return storage.getImage();
    }

    @Override
    public void rename(DBRProgressMonitor monitor, String newName) throws DBException
    {
        try {
            storage.setTitle(newName);
            InputStream data = storage.serialize();
            ((IFile)getResource()).setContents(data, true, false, monitor.getNestedMonitor());
        } catch (Exception e) {
            throw new DBException(e);
        }
    }
}
