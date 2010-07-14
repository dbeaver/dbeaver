/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.load.jobs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.AbstractUIJob;
import org.jkiss.dbeaver.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.utils.DBeaverUtils;

class LoadingFinishJob<RESULT> extends AbstractUIJob {

    private ILoadVisualizer<RESULT> visualizer;
    private RESULT result;
    private Throwable error;

    public LoadingFinishJob(ILoadVisualizer<RESULT> visualizer, RESULT result, Throwable error)
    {
        super("Remove load load placeholder");
        this.visualizer = visualizer;
        this.result = result;
        this.error = error;
        setRule(new NonConflictingRule());
    }

    public IStatus runInUIThread(DBRProgressMonitor monitor)
    {
        visualizer.completeLoading(result);
        if (error != null) {
            DBeaverUtils.showErrorDialog(
                visualizer.getShell(),
                "Error loading data",
                "Could not load data",
                error);
        }
        return Status.OK_STATUS;
    }

    @Override
    protected void canceling()
    {
        super.canceling();
    }
}