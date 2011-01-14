/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.ICommandIds;

import java.util.ArrayList;
import java.util.List;

/**
 * DBNProjectFolder
 */
public class DBNProjectFolder extends DBNNode
{
    public enum FolderType {
        DATABASES,
        SCRIPTS,
        BOOKMARKS
    }


    private IProject project;
    private FolderType folderType;

    public DBNProjectFolder(DBNNode parentNode, IProject project, FolderType folderType)
    {
        super(parentNode);
        this.project = project;
        this.folderType = folderType;
    }

    protected void dispose(boolean reflect)
    {
        this.project = null;
        super.dispose(reflect);
    }

    public String getNodeName()
    {
        return project.getName();
    }

    public String getNodeDescription()
    {
        try {
            return project.getDescription().getComment();
        } catch (CoreException e) {
            log.error(e);
            return null;
        }
    }

    @Override
    public Image getNodeIcon()
    {
        return DBIcon.PROJECT.getImage();
    }

    @Override
    public boolean hasChildren()
    {
        return true;
    }

    @Override
    public boolean hasNavigableChildren()
    {
        return true;
    }

    @Override
    public List<? extends DBNNode> getChildren(DBRProgressMonitor monitor) throws DBException
    {
        return new ArrayList<DBNNode>();
    }

    public String getDefaultCommandId()
    {
        return ICommandIds.CMD_OBJECT_OPEN;
    }

    public boolean isLazyNode()
    {
        return false;
    }

    public boolean isManagable()
    {
        return true;
    }

}
