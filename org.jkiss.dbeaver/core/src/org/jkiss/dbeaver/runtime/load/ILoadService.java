package org.jkiss.dbeaver.runtime.load;

import org.eclipse.core.runtime.IProgressMonitor;

import java.lang.reflect.InvocationTargetException;

/**
 * Lazy loading service
 * @param <RESULT> result type
 */
public interface ILoadService<RESULT> {

    String getServiceName();

    IProgressMonitor getProgressMonitor();

    void setProgressMonitor(IProgressMonitor monitor);

    void setNestedService(ILoadService nested);

    void clearNestedService();

    RESULT evaluate() throws InvocationTargetException, InterruptedException;

    boolean cancel();

}
