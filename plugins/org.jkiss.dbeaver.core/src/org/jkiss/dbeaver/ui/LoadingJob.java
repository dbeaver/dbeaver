/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui;

import org.jkiss.dbeaver.Log;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.ILoadService;
import org.jkiss.dbeaver.model.runtime.load.ILoadVisualizer;

import java.lang.reflect.InvocationTargetException;

public class LoadingJob<RESULT>  extends AbstractJob {

    private static final Log log = Log.getLog(LoadingJob.class);

    public static final Object LOADING_FAMILY = new Object();

    public static <RESULT> LoadingJob<RESULT> createService(
        ILoadService<RESULT> loadingService,
        ILoadVisualizer<RESULT> visualizer)
    {
        return new LoadingJob<RESULT>(loadingService, visualizer);
    }

    private ILoadService<RESULT> loadingService;
    private ILoadVisualizer<RESULT> visualizer;

    public LoadingJob(ILoadService<RESULT> loadingService, ILoadVisualizer<RESULT> visualizer)
    {
        super(loadingService.getServiceName());
        this.loadingService = loadingService;
        this.visualizer = visualizer;
        setUser(false);
    }

    public ILoadService<RESULT> getLoadingService()
    {
        return loadingService;
    }

    public ILoadVisualizer<RESULT> getVisualizer()
    {
        return visualizer;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {
        return run(monitor, true);
    }

    private IStatus run(DBRProgressMonitor monitor, boolean lazy)
    {
        monitor = visualizer.overwriteMonitor(monitor);

        LoadingUIJob<RESULT> updateUIJob = new LoadingUIJob<>(this, monitor);
        updateUIJob.schedule();
        this.loadingService.setProgressMonitor(monitor);
        Throwable error = null;
        RESULT result = null;
        try {
            result = this.loadingService.evaluate();
        }
        catch (InvocationTargetException e) {
//            log.error(e.getTargetException());
            error = e.getTargetException();
        }
        catch (InterruptedException e) {
            return new Status(Status.CANCEL, DBeaverCore.PLUGIN_ID, "Loading interrupted");
        }
        finally {
            UIUtils.runInUI(null, new LoadFinisher(result, error));
        }
        return Status.OK_STATUS;
    }

    @Override
    public boolean belongsTo(Object family)
    {
        return family == loadingService.getFamily();
    }

    public void syncRun()
    {
        run(VoidProgressMonitor.INSTANCE, false);
    }

    private class LoadFinisher implements Runnable {
        private final RESULT innerResult;
        private final Throwable innerError;

        public LoadFinisher(RESULT innerResult, Throwable innerError)
        {
            this.innerResult = innerResult;
            this.innerError = innerError;
        }

        @Override
        public void run()
        {
            visualizer.completeLoading(innerResult);

            if (innerError != null) {
                log.debug(innerError);
                UIUtils.showErrorDialog(
                    null,
                    getName(),
                    null,
                    innerError);
            }
        }
    }

    static class LoadingUIJob<RESULT> extends AbstractUIJob {
        private static final Log log = Log.getLog(LoadingUIJob.class);

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
        }

        @Override
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

        @Override
        public boolean belongsTo(Object family)
        {
            return family == LOADING_FAMILY;
        }

        @Override
        protected void canceling()
        {
            super.canceling();
        }
    }
}