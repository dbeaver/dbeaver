/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.tree.DBXTreeNode;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.ICommandIds;

import java.util.ArrayList;
import java.util.List;

/**
 * DBNDataSource
 */
public class DBNProject extends DBNNode implements IAdaptable
{
    private IProject project;

    public DBNProject(DBNRoot parentNode, IProject project)
    {
        super(parentNode);
        this.project = project;
        this.getModel().addNode(this, true);
    }

    protected void dispose(boolean reflect)
    {
        this.getModel().removeNode(this, true);
        this.project = null;
        super.dispose(reflect);
    }

    public DataSourceDescriptor getObject()
    {
        return null;
    }

    public Object getValueObject()
    {
        return project;
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

    public Object getAdapter(Class adapter) {
        if (adapter == DBNProject.class) {
            return this;
        } else if (DBSDataSourceContainer.class.isAssignableFrom(adapter)) {
            return project;
        }
        return null;
    }

    public boolean supportsRename()
    {
        return true;
    }

    public void rename(DBRProgressMonitor monitor, String newName) throws DBException
    {
        try {
            final IProjectDescription description = project.getDescription();
            description.setName(newName);
            project.move(description, true, monitor.getNestedMonitor());
        } catch (CoreException e) {
            throw new DBException(e);
        }
    }

}
