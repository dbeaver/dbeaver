/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.load.jobs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.AbstractJob;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.load.ILoadService;
import org.jkiss.dbeaver.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.ui.DBeaverConstants;
import org.jkiss.dbeaver.ui.UIUtils;

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
        return run(monitor, true);
    }

    private IStatus run(DBRProgressMonitor monitor, boolean lazy)
    {
        monitor = visualizer.overwriteMonitor(monitor);

        LoadingUIJob<RESULT> updateUIJob = new LoadingUIJob<RESULT>(this, monitor);
        updateUIJob.schedule();
        this.loadingService.setProgressMonitor(monitor);
        Throwable error = null;
        RESULT result = null;
        try {
            result = this.loadingService.evaluate();
        }
        catch (InvocationTargetException e) {
            log.error(e.getTargetException());
            error = e.getTargetException();
        }
        catch (InterruptedException e) {
            return new Status(Status.CANCEL, DBeaverConstants.PLUGIN_ID, "Loading interrupted");
        }
        finally {
            final RESULT innerResult = result;
            final Throwable innerError = error;
            Display.getDefault().syncExec(new Runnable() {
                public void run()
                {
                    visualizer.completeLoading(innerResult);

                    if (innerError != null) {
                        log.debug(innerError);
                        UIUtils.showErrorDialog(
                            visualizer.getShell(),
                            getName(),
                            innerError.getMessage());
                    }
                }
            });
//            final LoadingFinishJob<RESULT> finisher = new LoadingFinishJob<RESULT>(getName(), visualizer, result, error);
//            if (lazy) {
//                finisher.schedule();
//            } else {
//                finisher.runInUIThread(monitor);
//            }
        }
        return Status.OK_STATUS;
    }

    public boolean belongsTo(Object family)
    {
        return family == loadingService.getFamily();
    }

    public void syncRun()
    {
        run(VoidProgressMonitor.INSTANCE, false);
    }

}