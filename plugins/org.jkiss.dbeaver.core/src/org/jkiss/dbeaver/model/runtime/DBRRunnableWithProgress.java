/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.runtime;

import java.lang.reflect.InvocationTargetException;

/**
 * Database progress monitor.
 * Similar to IProgressMonitor but with DBP specific features
 */
public interface DBRRunnableWithProgress {

    public void run(DBRProgressMonitor monitor)
        throws InvocationTargetException, InterruptedException;

}