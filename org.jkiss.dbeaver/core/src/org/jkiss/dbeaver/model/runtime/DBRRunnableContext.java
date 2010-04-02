package org.jkiss.dbeaver.model.runtime;

import java.lang.reflect.InvocationTargetException;

/**
 * DBPViewCallback
 */
public interface DBRRunnableContext
{
    public void run(boolean fork, boolean cancelable, DBRRunnableWithProgress runnable)
        throws InvocationTargetException, InterruptedException;
}
