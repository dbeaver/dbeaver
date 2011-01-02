/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.load.jobs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.AbstractUIJob;
import org.jkiss.dbeaver.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.ui.UIUtils;

class LoadingFinishJob<RESULT> extends AbstractUIJob {

    private ILoadVisualizer<RESULT> visualizer;
    private RESULT result;
    private Throwable error;

    public LoadingFinishJob(String jobName, ILoadVisualizer<RESULT> visualizer, RESULT result, Throwable error)
    {
        super(jobName);
        this.visualizer = visualizer;
        this.result = result;
        this.error = error;
        setRule(new NonConflictingRule());
    }

    public IStatus runInUIThread(DBRProgressMonitor monitor)
    {
        visualizer.completeLoading(result);
        if (error != null) {
            log.debug(error);
            UIUtils.showErrorDialog(
                visualizer.getShell(),
                getName(),
                error.getMessage());
        }
        return Status.OK_STATUS;
    }

    @Override
    protected void canceling()
    {
        super.canceling();
    }
}