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

package org.jkiss.dbeaver.model.navigator;

import org.jkiss.code.NotNull;

/**
 * Navigator model event
 */
public class DBNEvent {

    public static final Object FORCE_REFRESH = new Object();
    public static final Object UPDATE_ON_SAVE = new Object();

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
        SELECT,
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

    @Override
    public String toString() {
        return action + ":" + nodeChange + ":" + node;
    }
}