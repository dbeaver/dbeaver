package org.jkiss.dbeaver.model.meta;

import org.jkiss.dbeaver.registry.event.RegistryEvent;

/**
 * DataSourceEvent
 */
public class DBMEvent extends RegistryEvent
{
    public enum Action
    {
        ADD,
        REFRESH,
        REMOVE
    }

    public enum NodeChange {
        LOADED,
        UNLOADED,
        CHANGED
    }

    private Action action;
    private NodeChange nodeChange;
    private DBMNode node;

    public DBMEvent(Object source, Action action, DBMNode node)
    {
        this(source, action, NodeChange.CHANGED, node);
        this.action = action;
        this.node = node;
    }

    public DBMEvent(Object source, Action action, NodeChange nodeChange, DBMNode node)
    {
        super(source);
        this.action = action;
        this.nodeChange = nodeChange;
        this.node = node;
    }

    public Action getAction()
    {
        return action;
    }

    public NodeChange getNodeChange()
    {
        return nodeChange;
    }

    public DBMNode getNode()
    {
        return node;
    }
}