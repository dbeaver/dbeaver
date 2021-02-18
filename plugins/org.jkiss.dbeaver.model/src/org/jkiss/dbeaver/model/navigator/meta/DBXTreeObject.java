/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.navigator.meta;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;

/**
 * DBXTreeObject
 */
public class DBXTreeObject extends DBXTreeNode
{
    private String label;
    private String description;
    private String editorId;

    public DBXTreeObject(AbstractDescriptor source, DBXTreeNode parent, IConfigurationElement config, String visibleIf, String label, String description, String editorId)
    {
        super(source, parent, config, true, false, false, false, visibleIf, null);
        this.label = label;
        this.description = description;
        this.editorId = editorId;
    }

    DBXTreeObject(AbstractDescriptor source, DBXTreeNode parent, DBXTreeObject object)
    {
        super(source, parent, object);
        this.label = object.label;
        this.description = object.description;
        this.editorId = object.editorId;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String getNodeTypeLabel(@Nullable DBPDataSource dataSource, @Nullable String locale)
    {
        return label;
    }

    @Override
    public String getChildrenTypeLabel(@Nullable DBPDataSource dataSource, String locale)
    {
        return label;
    }

    @Override
    public String toString() {
        return "Object " + label;
    }

    public String getDescription()
    {
        return description;
    }

    public String getEditorId()
    {
        return editorId;
    }
}