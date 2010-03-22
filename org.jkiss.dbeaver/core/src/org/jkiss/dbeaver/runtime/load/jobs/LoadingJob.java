package org.jkiss.dbeaver.runtime.load.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.dbeaver.ui.DBeaverConstants;
import org.jkiss.dbeaver.runtime.load.ILoadService;
import org.jkiss.dbeaver.runtime.load.ILoadVisualizer;

import java.lang.reflect.InvocationTargetException;

public class LoadingJob<RESULT>  extends Job {

    public static final Object LOADING_FAMILY = new Object();

    private ILoadService<RESULT> loadingService;
    private ILoadVisualizer<RESULT> visualizer;

    public LoadingJob(ILoadService<RESULT> loadingService, ILoadVisualizer<RESULT> visualizer)
    {
        super(loadingService.getServiceName());
        this.loadingService = loadingService;
        this.visualizer = visualizer;
        setUser(false);
        setRule(new NonConflictingRule());
    }

    public ILoadService<RESULT> getLoadingService()
    {
        return loadingService;
    }

    public ILoadVisualizer<RESULT> getVisualizer()
    {
        return visualizer;
    }

    protected IStatus run(IProgressMonitor monitor)
    {
        LoadingUIJob<RESULT> updateUIJob = new LoadingUIJob<RESULT>(this, monitor);
        updateUIJob.schedule();
        this.loadingService.setProgressMonitor(monitor);
        RESULT result = null;
        try {
            result = this.loadingService.evaluate();
        }
        catch (InvocationTargetException e) {
            return new Status(Status.ERROR, DBeaverConstants.PLUGIN_ID, "Loading error", e.getTargetException());
        }
        catch (InterruptedException e) {
            return new Status(Status.CANCEL, DBeaverConstants.PLUGIN_ID, "Loading interrupted", e);
        }
        finally {
            new LoadingFinishJob<RESULT>(visualizer, result).schedule();
        }
        return Status.OK_STATUS;
    }

    public boolean belongsTo(Object family)
    {
        return family == LOADING_FAMILY;
    }

    protected void canceling()
    {
        // TODO: guess what?
    }

}