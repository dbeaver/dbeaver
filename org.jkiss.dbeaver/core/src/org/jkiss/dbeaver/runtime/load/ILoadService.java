package org.jkiss.dbeaver.runtime.load;

import org.jkiss.dbeaver.model.DBPProgressMonitor;

import java.lang.reflect.InvocationTargetException;

/**
 * Lazy loading service
 * @param <RESULT> result type
 */
public interface ILoadService<RESULT> {

    String getServiceName();

    DBPProgressMonitor getProgressMonitor();

    void setProgressMonitor(DBPProgressMonitor monitor);

    void setNestedService(ILoadService nested);

    void clearNestedService();

    RESULT evaluate() throws InvocationTargetException, InterruptedException;

    boolean cancel();

}
