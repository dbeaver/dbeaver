package org.jkiss.dbeaver.model;

import java.lang.reflect.InvocationTargetException;

/**
 * Database progress monitor.
 * Similar to IProgressMonitor but with DBP specific features
 */
public interface DBPRunnableWithProgress {

    public void run(DBPProgressMonitor monitor)
        throws InvocationTargetException, InterruptedException;

}