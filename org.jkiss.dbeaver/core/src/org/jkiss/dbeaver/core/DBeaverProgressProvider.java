package org.jkiss.dbeaver.core;

import org.eclipse.core.runtime.jobs.ProgressProvider;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.internal.progress.ProgressManager;
import org.jkiss.dbeaver.ui.splash.DBeaverSplashHandler;

/**
 * DBeaver progress provider.
 * Used to pass progress monitors to splash screen
 */
public class DBeaverProgressProvider extends ProgressProvider {

    public DBeaverProgressProvider()
    {
        ProgressManager.getInstance();
        Job.getJobManager().setProgressProvider(this);
    }

    public IProgressMonitor createMonitor(Job job)
    {
        IProgressMonitor activeMonitor = DBeaverSplashHandler.getActiveMonitor();
        if (activeMonitor == null) {
            activeMonitor = ProgressManager.getInstance().createMonitor(job);
        }
        return activeMonitor;
    }

    void shutdown()
    {
        Job.getJobManager().setProgressProvider(ProgressManager.getInstance());
    }
}
