package org.jkiss.dbeaver.runtime.load.tree;

import org.jkiss.dbeaver.runtime.load.AbstractLoadService;

/**
 * TreeLoadService
 */
public abstract class TreeLoadService extends AbstractLoadService<Object[]> {

    private Object parent;

    protected TreeLoadService(String serviceName, Object parent)
    {
        super(serviceName);
        this.parent = parent;
    }

    public Object getParent()
    {
        return parent;
    }

}
