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

package org.jkiss.dbeaver.model.navigator;

import org.jkiss.code.NotNull;

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
    @NotNull
    private DBNNode node;

    public DBNEvent(Object source, Action action, @NotNull DBNNode node)
    {
        this(source, action, NodeChange.REFRESH, node);
        this.action = action;
        this.node = node;
    }

    public DBNEvent(Object source, Action action, NodeChange nodeChange, @NotNull DBNNode node)
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

    @NotNull
    public DBNNode getNode()
    {
        return node;
    }
}