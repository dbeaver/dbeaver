package org.jkiss.dbeaver.runtime.load;

import org.jkiss.dbeaver.model.DBPProgressMonitor;
import org.jkiss.dbeaver.runtime.NullProgressMonitor;

import java.lang.reflect.InvocationTargetException;

/**
 * NullLoadService
 */
public class NullLoadService extends AbstractLoadService<Object> {

    public static final DBPProgressMonitor NULL_MONITOR = new NullProgressMonitor();

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
