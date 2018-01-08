/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.model.runtime.load.ILoadService;
import org.jkiss.dbeaver.model.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;

import java.lang.reflect.InvocationTargetException;

public class LoadingJob<RESULT>  extends AbstractJob {

    private static final Log log = Log.getLog(LoadingJob.class);

    public static final Object LOADING_FAMILY = new Object();
    private boolean loadFinished;

    public static <RESULT> LoadingJob<RESULT> createService(
        ILoadService<RESULT> loadingService,
        ILoadVisualizer<RESULT> visualizer)
    {
        return new LoadingJob<>(loadingService, visualizer);
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
        if (this.loadingService instanceof AbstractLoadService) {
            ((AbstractLoadService) this.loadingService).initService(monitor, this);
        }

        LoadingUIJob<RESULT> updateUIJob = new LoadingUIJob<>(this);
        updateUIJob.schedule();
        Throwable error = null;
        RESULT result = null;
        try {
            result = this.loadingService.evaluate(monitor);
        }
        catch (InvocationTargetException e) {
            error = e.getTargetException();
        }
        catch (InterruptedException e) {
            return new Status(Status.CANCEL, DBeaverCore.PLUGIN_ID, "Loading interrupted");
        }
        finally {
            loadFinished = true;
            DBeaverUI.asyncExec(new LoadFinisher(result, error));
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
        run(new VoidProgressMonitor(), false);
    }

    private class LoadFinisher implements Runnable {
        private final RESULT innerResult;
        private final Throwable innerError;

        LoadFinisher(RESULT innerResult, Throwable innerError)
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
                DBUserInterface.getInstance().showError(
                        getName(),
                    null,
                    innerError);
            }
        }
    }

    class LoadingUIJob<RESULT> extends AbstractUIJob {

        private static final long DELAY = 200;

        private ILoadVisualizer<RESULT> visualizer;

        LoadingUIJob(LoadingJob<RESULT> loadingJob) {
            super(loadingJob.getName());
            this.visualizer = loadingJob.getVisualizer();
            setSystem(true);
        }

        @Override
        public IStatus runInUIThread(DBRProgressMonitor monitor) {
                if (!visualizer.isCompleted() && !loadFinished) {
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