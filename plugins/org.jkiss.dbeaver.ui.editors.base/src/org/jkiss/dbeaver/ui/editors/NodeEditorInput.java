/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;

/**
 * NodeEditorInput
 */
public class NodeEditorInput implements INavigatorEditorInput, IPersistableElement {
    private volatile DBNNode node;
    private String nodePath;

    public NodeEditorInput(DBNNode node) {
        this.node = node;
    }

    public NodeEditorInput(String nodePath) {
        this.nodePath = nodePath;
    }

    @Override
    public DBNNode getNavigatorNode() {
        if (node == null) {
            if (nodePath == null) {
                throw new IllegalStateException("Invalid node input");
            }
            try {
                final DBNModel navigatorModel = DBWorkbench.getPlatform().getNavigatorModel();
                node = navigatorModel.getNodeByPath(new VoidProgressMonitor(), nodePath);
                if (node == null) {
                    throw new IllegalStateException("Navigator node '" + nodePath + "' not found");
                }
            } catch (DBException e) {
                throw new IllegalStateException("Cannot find navigator node '" + nodePath + "'", e);
            }
        }
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
        return DBeaverIcons.getImageDescriptor(getNavigatorNode().getNodeIcon());
    }

    @Override
    public String getName() {
        return node == null ? nodePath : node.getNodeDisplayName();
    }

    @Override
    public IPersistableElement getPersistable() {
        return node == null || node.isDisposed() || !node.isPersisted() ? null : this;
    }

    @Override
    public String getToolTipText() {
        return node == null ? null : node.getNodeDescription();
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBNNode.class) {
            return adapter.cast(getNavigatorNode());
        }
        return null;
    }

    @Override
    public String getFactoryId() {
        return NodeEditorInputFactory.ID_FACTORY;
    }

    @Override
    public void saveState(IMemento memento) {
        if (node == null || node.isDisposed() || !node.isPersisted()) {
            return;
        }
        NodeEditorInputFactory.saveState(memento, this);
    }

}
