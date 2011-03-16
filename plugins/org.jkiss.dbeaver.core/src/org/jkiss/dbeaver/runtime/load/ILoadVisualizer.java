/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.load;

import org.eclipse.jface.window.IShellProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Lazy loading visualizer
 */
public interface ILoadVisualizer<RESULT> extends IShellProvider {

    /**
     * Allows visualizer to overwrite monitor by its own implementation.
     * By default returns passed one
     * @param monitor monitor
     * @return new or original monitor
     */
    DBRProgressMonitor overwriteMonitor(DBRProgressMonitor monitor);

    boolean isCompleted();

    void visualizeLoading();

    void completeLoading(RESULT result);

}