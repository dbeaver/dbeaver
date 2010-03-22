package org.jkiss.dbeaver.runtime.load.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.progress.UIJob;
import org.jkiss.dbeaver.runtime.load.ILoadService;
import org.jkiss.dbeaver.runtime.load.ILoadVisualizer;

class LoadingUIJob<RESULT> extends UIJob {

    private static final long DELAY = 200;

    private ILoadService<RESULT> loadService;
    private ILoadVisualizer<RESULT> visualizer;
    private IProgressMonitor mainMonitor;

    LoadingUIJob(LoadingJob<RESULT> loadingJob, IProgressMonitor mainMonitor)
    {
        super(loadingJob.getName());
        this.loadService = loadingJob.getLoadingService();
        this.visualizer = loadingJob.getVisualizer();
        this.mainMonitor = mainMonitor;
        setSystem(true);
        setRule(new NonConflictingRule());
    }

    public IStatus runInUIThread(IProgressMonitor monitor)
    {
        if (mainMonitor.isCanceled()) {
            // Try to cancel current load service
            loadService.cancel();
            return Status.CANCEL_STATUS;
        } else {
            if (!visualizer.isCompleted()) {
                visualizer.visualizeLoading();
                schedule(DELAY);
            }
        }
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