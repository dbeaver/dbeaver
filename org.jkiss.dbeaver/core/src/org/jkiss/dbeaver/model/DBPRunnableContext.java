package org.jkiss.dbeaver.model;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;

import java.lang.reflect.InvocationTargetException;

/**
 * DBPViewCallback
 */
public interface DBPRunnableContext
{
    public void run(boolean fork, boolean cancelable, DBPRunnableWithProgress runnable) 
        throws InvocationTargetException, InterruptedException;
}
