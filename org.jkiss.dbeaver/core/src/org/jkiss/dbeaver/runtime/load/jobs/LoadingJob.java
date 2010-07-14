/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.load.jobs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.AbstractJob;
import org.jkiss.dbeaver.runtime.load.ILoadService;
import org.jkiss.dbeaver.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.ui.DBeaverConstants;

import java.lang.reflect.InvocationTargetException;

public class LoadingJob<RESULT>  extends AbstractJob {

    static final Log log = LogFactory.getLog(LoadingJob.class);

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

    protected IStatus run(DBRProgressMonitor monitor)
    {
        LoadingUIJob<RESULT> updateUIJob = new LoadingUIJob<RESULT>(this, monitor);
        updateUIJob.schedule();
        this.loadingService.setProgressMonitor(monitor);
        RESULT result = null;
        Throwable error = null;
        try {
            result = this.loadingService.evaluate();
        }
        catch (InvocationTargetException e) {
            error = e.getTargetException();
            log.error("Loading error", e.getTargetException());
        }
        catch (InterruptedException e) {
            return new Status(Status.CANCEL, DBeaverConstants.PLUGIN_ID, "Loading interrupted");
        }
        finally {
            new LoadingFinishJob<RESULT>(visualizer, result, error).schedule();
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