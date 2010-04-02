package org.jkiss.dbeaver.runtime;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.progress.UIJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.DBeaverUtils;

/**
 * Abstract Database Job
 */
public abstract class AbstractUIJob extends UIJob
{

    protected AbstractUIJob(String name)
    {
        super(name);
    }

    public IStatus runInUIThread(IProgressMonitor monitor)
    {
        return this.runInUIThread(DBeaverUtils.makeMonitor(monitor));
    }

    protected abstract IStatus runInUIThread(DBRProgressMonitor monitor);

}