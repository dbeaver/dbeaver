/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.model.navigator;

/**
 * Navigator model event
 */
public class DBNEvent {
    public enum Action
    {
        ADD,
        REMOVE,
        UPDATE,
    }

    public enum NodeChange {
        LOAD,
        UNLOAD,
        REFRESH,
        STRUCT_REFRESH,
        LOCK,
        UNLOCK,
    }

    private Object source;
    private Action action;
    private NodeChange nodeChange;
    private DBNNode node;

    public DBNEvent(Object source, Action action, DBNNode node)
    {
        this(source, action, NodeChange.REFRESH, node);
        this.action = action;
        this.node = node;
    }

    public DBNEvent(Object source, Action action, NodeChange nodeChange, DBNNode node)
    {
        this.source = source;
        this.action = action;
        this.nodeChange = nodeChange;
        this.node = node;
    }

    public Object getSource()
    {
        return source;
    }

    public Action getAction()
    {
        return action;
    }

    public NodeChange getNodeChange()
    {
        return nodeChange;
    }

    public DBNNode getNode()
    {
        return node;
    }
}