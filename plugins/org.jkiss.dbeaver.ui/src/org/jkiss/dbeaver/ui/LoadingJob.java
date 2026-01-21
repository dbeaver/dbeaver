/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.model.runtime.load.ILoadService;
import org.jkiss.dbeaver.model.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.model.runtime.load.ILoadVisualizerExt;
import org.jkiss.dbeaver.runtime.DBInterruptedException;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.internal.UIActivator;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;

public class LoadingJob<RESULT> extends AbstractJob {

    private static final Log log = Log.getLog(LoadingJob.class);

    public static final Object LOADING_FAMILY = new Object();
    private boolean loadFinished;
    private boolean showErrors = true;

    public static <RESULT> LoadingJob<RESULT> createService(
        @NotNull ILoadService<RESULT> loadingService,
        @NotNull ILoadVisualizer<RESULT> visualizer
    ) {
        return new LoadingJob<>(loadingService, visualizer);
    }

    private final ILoadService<RESULT> loadingService;
    private final ILoadVisualizer<RESULT> visualizer;

    public LoadingJob(@NotNull ILoadService<RESULT> loadingService, @NotNull ILoadVisualizer<RESULT> visualizer) {
        super(loadingService.getServiceName());
        this.loadingService = loadingService;
        this.visualizer = visualizer;
        setUser(false);
    }

    public ILoadService<RESULT> getLoadingService() {
        return loadingService;
    }

    @Override
    public boolean isForceCancel() {
        return loadingService.isForceCancel();
    }

    @NotNull
    public ILoadVisualizer<RESULT> getVisualizer() {
        return visualizer;
    }

    public boolean isShowErrors() {
        return showErrors;
    }

    public void setShowErrors(boolean showErrors) {
        this.showErrors = showErrors;
    }

    @NotNull
    @Override
    protected IStatus run(@NotNull DBRProgressMonitor monitor) {
        monitor = visualizer.overwriteMonitor(monitor);
        if (this.loadingService instanceof AbstractLoadService<?> als) {
            als.initService(monitor, this);
        }

        LoadingUIJob<RESULT> updateUIJob = new LoadingUIJob<>(this);
        updateUIJob.schedule();
        Throwable error = null;
        RESULT result = null;
        monitor.beginTask(getName(), 1);
        try {
            result = this.loadingService.evaluate(monitor);
        } catch (InvocationTargetException e) {
            error = e.getTargetException();
        } catch (InterruptedException e) {
            return new Status(Status.CANCEL, UIActivator.PLUGIN_ID, "Loading interrupted");
        } finally {
            loadFinished = true;
            UIUtils.asyncExec(new LoadFinisher(result, error));
            monitor.done();
        }
        return Status.OK_STATUS;
    }

    @Override
    public boolean belongsTo(@NotNull Object family) {
        return family == loadingService.getFamily();
    }

    public void syncRun() {
        run(new VoidProgressMonitor());
    }

    private class LoadFinisher implements Runnable {
        private final RESULT innerResult;
        private final Throwable innerError;

        LoadFinisher(@NotNull RESULT innerResult, @Nullable Throwable innerError) {
            this.innerResult = innerResult;
            this.innerError = innerError;
        }

        @Override
        public void run() {
            try {
                visualizer.completeLoading(innerResult);
            } catch (Throwable e) {
                log.debug(e);
            }

            if (innerError != null && !(innerError instanceof DBInterruptedException)) {
                if (showErrors) {
                    DBWorkbench.getPlatformUI().showError(
                        getName(),
                        null,
                        innerError
                    );
                } else {
                    log.debug(CommonUtils.getAllExceptionMessages(innerError));
                }
            }
            if (visualizer instanceof ILoadVisualizerExt lve) {
                lve.finalizeLoading();
            }
        }
    }

    class LoadingUIJob<JOB_RESULT> extends AbstractUIJob {

        private static final long DELAY = 100;

        private final ILoadVisualizer<JOB_RESULT> visualizer;

        LoadingUIJob(@NotNull LoadingJob<JOB_RESULT> loadingJob) {
            super(loadingJob.getName());
            this.visualizer = loadingJob.getVisualizer();
            setSystem(true);
        }

        @NotNull
        @Override
        public IStatus runInUIThread(@NotNull DBRProgressMonitor monitor) {
            if (!visualizer.isCompleted() && !loadFinished) {
                visualizer.visualizeLoading();
                schedule(DELAY);
            }
            //}
            return Status.OK_STATUS;
        }

        @Override
        public boolean belongsTo(Object family) {
            return family == LOADING_FAMILY;
        }

        @Override
        protected void canceling() {
            super.canceling();
        }
    }
}