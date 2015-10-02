/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.runtime.load.jobs;

import org.jkiss.dbeaver.Log;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.load.ILoadService;
import org.jkiss.dbeaver.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;

public class LoadingJob<RESULT>  extends AbstractJob {

    static final Log log = Log.getLog(LoadingJob.class);

    public static final Object LOADING_FAMILY = new Object();

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
}