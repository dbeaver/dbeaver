/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.PropertySourceEditable;
import org.jkiss.dbeaver.ui.DBeaverIcons;

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

    protected DatabaseEditorInput(@Nullable NODE node)
    {
        this(node, null);
    }

    protected DatabaseEditorInput(@Nullable NODE node, @Nullable DBECommandContext commandContext)
    {
        this.node = node;
        DBSObject object = node == null ? null : node.getObject();
        if (object != null) {
            this.executionContext = DBUtils.getDefaultContext(object, false);
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
        if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(DatabaseEditorPreferences.PROP_TITLE_SHOW_FULL_NAME)) {
            return node.getNodeFullName();
        } else {
            return node.getName();
        }
    }

    @Override
    public IPersistableElement getPersistable()
    {
        if (getExecutionContext() == null ||
            getDatabaseObject() == null ||
            !getDatabaseObject().isPersisted() ||
            !DBWorkbench.getPlatform().getPreferenceStore().getBoolean(DatabaseEditorPreferences.PROP_SAVE_EDITORS_STATE))
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
    public <T> T getAdapter(Class<T> adapter)
    {
        if (IWorkbenchAdapter.class.equals(adapter)) {
            return adapter.cast(new WorkbenchAdapter() {
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
            });
        } else if (DBSObject.class.equals(adapter)) {
            return adapter.cast(getDatabaseObject());
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
        PropertySourceEditable propertySource = new PropertySourceEditable(
            getCommandContext(),
            getNavigatorNode(),
            getDatabaseObject());
        propertySource.collectProperties();

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
