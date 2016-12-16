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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.editors.entity.NodeEditorInput;

public class NodeEditorInputFactory implements IElementFactory
{
    private static final Log log = Log.getLog(NodeEditorInputFactory.class);

    public static final String ID_FACTORY = NodeEditorInputFactory.class.getName(); //$NON-NLS-1$

    private static final String TAG_NODE = "node"; //$NON-NLS-1$


    public NodeEditorInputFactory()
    {
    }

    @Override
    public IAdaptable createElement(IMemento memento)
    {
        // Get the node path.
        final String nodePath = memento.getString(TAG_NODE);

        final DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();

        try {
            final DBNNode node = navigatorModel.getNodeByPath(VoidProgressMonitor.INSTANCE, nodePath);
            if (node != null) {
                return new NodeEditorInput(node);
            }
        } catch (DBException e) {
            log.error("Error opening node '" + nodePath + "'", e);
            return null;
        }
        return null;
    }

    public static void saveState(IMemento memento, NodeEditorInput input)
    {
        final DBNNode node = input.getNavigatorNode();
        memento.putString(TAG_NODE, node.getNodeItemPath());
    }

}