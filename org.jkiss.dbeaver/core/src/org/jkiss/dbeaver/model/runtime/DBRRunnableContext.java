/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

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
