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

package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.INavigatorEditorInput;

/**
 * NodeEditorInput
 */
public class NodeEditorInput implements INavigatorEditorInput, IPersistableElement
{
    private final DBNNode node;
    public NodeEditorInput(DBNNode node)
    {
        this.node = node;
    }

    @Override
    public DBNNode getNavigatorNode() {
        return node;
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
        return null;
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
    public String getFactoryId() {
        return null;
    }

    @Override
    public void saveState(IMemento memento) {

    }

}
