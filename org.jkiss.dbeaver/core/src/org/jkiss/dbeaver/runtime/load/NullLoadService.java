package org.jkiss.dbeaver.runtime.load;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.IProgressMonitor;

import java.lang.reflect.InvocationTargetException;

/**
 * NullLoadService
 */
public class NullLoadService extends AbstractLoadService<Object> {

    public static final IProgressMonitor NULL_MONITOR = new NullProgressMonitor();

    public NullLoadService()
    {
        super("NULL");
        setProgressMonitor(NULL_MONITOR);
    }

    public Object evaluate()
        throws InvocationTargetException, InterruptedException
    {
        return null;
    }
}
