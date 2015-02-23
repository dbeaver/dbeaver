/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.runtime.load.jobs;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.AbstractUIJob;
import org.jkiss.dbeaver.runtime.load.ILoadService;
import org.jkiss.dbeaver.runtime.load.ILoadVisualizer;

class LoadingUIJob<RESULT> extends AbstractUIJob {
    static final Log log = Log.getLog(LoadingUIJob.class);

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
        return family == LoadingJob.LOADING_FAMILY;
    }

    @Override
    protected void canceling()
    {
        super.canceling();
    }
}