/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.load.jobs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.AbstractUIJob;
import org.jkiss.dbeaver.runtime.load.ILoadService;
import org.jkiss.dbeaver.runtime.load.ILoadVisualizer;

class LoadingUIJob<RESULT> extends AbstractUIJob {
    static final Log log = LogFactory.getLog(LoadingUIJob.class);

    private static final long DELAY = 200;

    private ILoadService<RESULT> loadService;
    private ILoadVisualizer<RESULT> visualizer;
    private DBRProgressMonitor mainMonitor;

    LoadingUIJob(LoadingJob<RESULT> loadingJob, DBRProgressMonitor mainMonitor)
    {
        super(loadingJob.getName());
        this.loadService = loadingJob.getLoadingService();
        this.visualizer = loadingJob.getVisualizer();
        this.mainMonitor = mainMonitor;
        setSystem(true);
        setRule(new NonConflictingRule());
    }

    public IStatus runInUIThread(DBRProgressMonitor monitor)
    {
/*
        if (mainMonitor.isCanceled()) {
            // Try to cancel current load service
            try {
                loadService.cancel();
            }
            catch (InvocationTargetException e) {
                log.warn("Error while canceling service", e.getTargetException());
            }
            return Status.CANCEL_STATUS;
        } else {
*/
            if (!visualizer.isCompleted()) {
                visualizer.visualizeLoading();
                schedule(DELAY);
            }
        //}
        return Status.OK_STATUS;
    }

    public boolean belongsTo(Object family)
    {
        return family == LoadingJob.LOADING_FAMILY;
    }

    @Override
    protected void canceling()
    {
        super.canceling();
    }
}