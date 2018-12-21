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
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.DBeaverIcons;

/**
 * NodeEditorInput
 */
public class NodeEditorInput implements INavigatorEditorInput, IPersistableElement
{
    private DBNNode node;
    public NodeEditorInput(DBNNode node)
    {
        this.node = node;
    }

    @Override
    public DBNNode getNavigatorNode() {
        return node;
    }

    public void setNavigatorNode(DBNNode node) {
        this.node = node;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return DBeaverIcons.getImageDescriptor(node.getNodeIcon());
    }

    @Override
    public String getName() {
        return node.getNodeName();
    }

    @Override
    public IPersistableElement getPersistable() {
        return this;
    }

    @Override
    public String getToolTipText() {
        return node.getNodeDescription();
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBNNode.class) {
            return adapter.cast(node);
        }
        return null;
    }

    @Override
    public String getFactoryId()
    {
        return NodeEditorInputFactory.ID_FACTORY;
    }

    @Override
    public void saveState(IMemento memento)
    {
        NodeEditorInputFactory.saveState(memento, this);
    }

}
