package org.jkiss.dbeaver.runtime.load.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.progress.UIJob;
import org.jkiss.dbeaver.runtime.load.ILoadVisualizer;

class LoadingFinishJob<RESULT> extends UIJob {

    private ILoadVisualizer<RESULT> visualizer;
    private RESULT result;

    public LoadingFinishJob(ILoadVisualizer<RESULT> visualizer, RESULT result)
    {
        super("Remove load load placeholder");
        this.visualizer = visualizer;
        this.result = result;
        setRule(new NonConflictingRule());
    }

    public IStatus runInUIThread(IProgressMonitor monitor)
    {
        visualizer.completeLoading(result);
        return Status.OK_STATUS;
    }

    @Override
    protected void canceling()
    {
        super.canceling();
    }
}