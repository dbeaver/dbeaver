/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.load;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.lang.reflect.InvocationTargetException;

/**
 * Lazy loading service
 * @param <RESULT> result type
 */
public interface ILoadService<RESULT> {

    String getServiceName();

    DBRProgressMonitor getProgressMonitor();

    void setProgressMonitor(DBRProgressMonitor monitor);

    RESULT evaluate() throws InvocationTargetException, InterruptedException;

    boolean cancel() throws InvocationTargetException;

    Object getFamily();
}
