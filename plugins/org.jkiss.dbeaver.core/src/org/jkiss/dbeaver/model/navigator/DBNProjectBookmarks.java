/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.DBIcon;

import java.util.List;

/**
 * DBNProjectBookmarks
 */
public class DBNProjectBookmarks extends DBNNode implements DBNContainer
{
    private IProject project;

    public DBNProjectBookmarks(DBNNode parentNode, IProject project)
    {
        super(parentNode);
        this.project = project;
    }

    void dispose(boolean reflect)
    {
        super.dispose(reflect);
    }

    public Object getValueObject()
    {
        return project;
    }

    public String getItemsLabel()
    {
        return "Bookmark";
    }

    public Class<IResource> getItemsClass()
    {
        return IResource.class;
    }

    public DBNNode addChildItem(DBRProgressMonitor monitor, Object childObject) throws DBException
    {
        throw new IllegalArgumentException("Only data source descriptors could be added to root node");
    }

    public void removeChildItem(DBNNode item) throws DBException
    {
        throw new IllegalArgumentException("Only data source descriptors could be removed from root node");
    }

    public String getNodeName()
    {
        return "Bookmarks";
    }

    public String getNodeDescription()
    {
        return project.getName() + " bookmarks";
    }

    public Image getNodeIcon()
    {
        return DBIcon.BOOKMARK_FOLDER.getImage();
    }

    public boolean hasChildren()
    {
        return false;
    }

    @Override
    public boolean hasNavigableChildren()
    {
        return hasChildren();
    }

    public List<? extends DBNNode> getChildren(DBRProgressMonitor monitor)
    {
        return null;
    }

    public String getDefaultCommandId()
    {
        return null;
    }

}
