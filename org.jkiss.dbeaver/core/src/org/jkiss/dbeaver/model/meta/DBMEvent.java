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
        CHANGE,
        REFRESH,
        REMOVE
    }

    private Action action;
    private DBMNode node;

    public DBMEvent(Object source, Action action, DBMNode node)
    {
        super(source);
        this.action = action;
        this.node = node;
    }

    public Action getAction()
    {
        return action;
    }

    public DBMNode getNode()
    {
        return node;
    }
}