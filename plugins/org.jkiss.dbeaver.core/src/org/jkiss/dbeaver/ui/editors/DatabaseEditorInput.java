/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.views.properties.IPropertySource2;
import org.jkiss.dbeaver.ext.IDataSourceContainerProvider;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandContextImpl;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.properties.PropertySourceEditable;

/**
 * DatabaseEditorInput
 */
public abstract class DatabaseEditorInput<NODE extends DBNDatabaseNode> implements IDatabaseEditorInput, IDataSourceContainerProvider
{
    private final NODE node;
    private final DBECommandContext commandContext;
    private String defaultPageId;
    private String defaultFolderId;
    private PropertySourceEditable propertySource;

    protected DatabaseEditorInput(NODE node)
    {
        this(node, null);
    }

    protected DatabaseEditorInput(NODE node, DBECommandContext commandContext)
    {
        this.node = node;
        this.commandContext = commandContext != null ?
            commandContext :
            new DBECommandContextImpl(node.getObject().getDataSource().getContainer());
    }

    @Override
    public boolean exists()
    {
        return false;
    }

    @Override
    public ImageDescriptor getImageDescriptor()
    {
        return ImageDescriptor.createFromImage(node.getNodeIconDefault());
    }

    @Override
    public String getName()
    {
        return node.getNodePathName();
    }

    @Override
    public IPersistableElement getPersistable()
    {
        return null;
    }

    @Override
    public String getToolTipText()
    {
        return node.getNodeDescription();
    }

    @Override
    public Object getAdapter(Class adapter)
    {
        if (IWorkbenchAdapter.class.equals(adapter)) {
            return new WorkbenchAdapter() {
                @Override
                public ImageDescriptor getImageDescriptor(Object object) {
                    return ImageDescriptor.createFromImage(node.getNodeIconDefault());
                }
                @Override
                public String getLabel(Object o) {
                    return node.getName();
                }
                @Override
                public Object getParent(Object o) {
                    return node.getParentNode();
                }
            };
        }

        return null;
    }

    @Override
    public DBSDataSourceContainer getDataSourceContainer()
    {
        final DBPDataSource dbpDataSource = getDataSource();
        return dbpDataSource == null ? null : dbpDataSource.getContainer();
    }

    @Override
    public DBPDataSource getDataSource() {
        DBSObject object = node.getObject();
        return object == null ? null : object.getDataSource();
    }

    @Override
    public NODE getTreeNode()
    {
        return node;
    }

    @Override
    public DBSObject getDatabaseObject()
    {
        return node.getObject();
    }

    @Override
    public String getDefaultPageId()
    {
        return defaultPageId;
    }

    @Override
    public String getDefaultFolderId()
    {
        return defaultFolderId;
    }

    @Override
    public DBECommandContext getCommandContext()
    {
        return commandContext;
    }

    public void setDefaultPageId(String defaultPageId)
    {
        this.defaultPageId = defaultPageId;
    }

    public void setDefaultFolderId(String defaultFolderId)
    {
        this.defaultFolderId = defaultFolderId;
    }

    @Override
    public IPropertySource2 getPropertySource()
    {
        if (propertySource == null) {
            propertySource = new PropertySourceEditable(
                getCommandContext(),
                getTreeNode(),
                getDatabaseObject());
            propertySource.collectProperties();
        }
        return propertySource;
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj == this ||
            (obj instanceof DatabaseEditorInput && ((DatabaseEditorInput<?>)obj).node.equals(node));
    }

}
