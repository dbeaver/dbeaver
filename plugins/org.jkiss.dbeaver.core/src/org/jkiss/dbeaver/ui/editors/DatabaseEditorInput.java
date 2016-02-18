/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPPropertySource;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.SimpleCommandContext;
import org.jkiss.dbeaver.runtime.properties.PropertySourceEditable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DatabaseEditorInput
 */
public abstract class DatabaseEditorInput<NODE extends DBNDatabaseNode> implements IPersistableElement, IDatabaseEditorInput, IDataSourceContainerProvider
{
    private final NODE node;
    private final DBCExecutionContext executionContext;
    private final DBECommandContext commandContext;
    private String defaultPageId;
    private String defaultFolderId;
    private Map<String, Object> attributes = new LinkedHashMap<>();
    private PropertySourceEditable propertySource;

    protected DatabaseEditorInput(NODE node)
    {
        this(node, null);
    }

    protected DatabaseEditorInput(NODE node, DBECommandContext commandContext)
    {
        this.node = node;
        DBPDataSource dataSource = node.getDataSource();
        if (dataSource != null) {
            this.executionContext = dataSource.getDefaultContext(false);
            this.commandContext = commandContext != null ?
                commandContext :
                new SimpleCommandContext(
                    this.executionContext,
                    false);
        } else {
            this.executionContext = null;
            this.commandContext = null;
        }
    }

    @Override
    public boolean exists()
    {
        return false;
    }

    @Override
    public ImageDescriptor getImageDescriptor()
    {
        return DBeaverIcons.getImageDescriptor(node.getNodeIconDefault());
    }

    @Override
    public String getName()
    {
        if (DBeaverCore.getGlobalPreferenceStore().getBoolean(DBeaverPreferences.NAVIGATOR_EDITOR_FULL_NAME)) {
            return node.getNodeFullName();
        } else {
            return node.getName();
        }
    }

    @Override
    public IPersistableElement getPersistable()
    {
        if (getExecutionContext() == null ||
            !DBeaverCore.getGlobalPreferenceStore().getBoolean(DBeaverPreferences.UI_KEEP_DATABASE_EDITORS))
        {
            return null;
        }

        return this;
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
                    return DBeaverIcons.getImageDescriptor(node.getNodeIconDefault());
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
    public DBPDataSourceContainer getDataSourceContainer()
    {
        if (executionContext != null) {
            return executionContext.getDataSource().getContainer();
        } else if (node instanceof DBNDataSource) {
            return node.getDataSourceContainer();
        } else {
            return null;
        }
    }

    @Override
    public DBCExecutionContext getExecutionContext() {
        return executionContext;
    }

    @Override
    public NODE getNavigatorNode()
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

    @Nullable
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
    public Collection<String> getAttributeNames() {
        return new ArrayList<>(attributes.keySet());
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Object setAttribute(String name, Object value) {
        if (value == null) {
            return attributes.remove(name);
        } else {
            return attributes.put(name, value);
        }
    }

    @Override
    public DBPPropertySource getPropertySource()
    {
        if (propertySource == null) {
            propertySource = new PropertySourceEditable(
                getCommandContext(),
                getNavigatorNode(),
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

    @Override
    public String getFactoryId()
    {
        return DatabaseEditorInputFactory.ID_FACTORY;
    }

    @Override
    public void saveState(IMemento memento)
    {
        DatabaseEditorInputFactory.saveState(memento, this);
    }
}
