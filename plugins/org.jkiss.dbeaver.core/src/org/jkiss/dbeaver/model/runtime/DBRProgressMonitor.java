/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.runtime;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Database progress monitor.
 * Similar to IProgressMonitor but with DBP specific features
 */
public interface DBRProgressMonitor {

    /**
     * Obtains eclipse progress monitor.
     * Can be used to pass to eclipse API.
     * @return
     */
    public IProgressMonitor getNestedMonitor();

    public void beginTask(String name, int totalWork);

    public void done();

    public void subTask(String name);

    public void worked(int work);

    public boolean isCanceled();

    public void startBlock(DBRBlockingObject object, String taskName);

    public void endBlock();

    public DBRBlockingObject getActiveBlock();

}
