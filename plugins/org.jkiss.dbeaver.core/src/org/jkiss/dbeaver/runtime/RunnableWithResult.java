/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime;

/**
 * Runnable which stores some result
 */
public abstract class RunnableWithResult<RESULT_TYPE> implements Runnable {

    protected RESULT_TYPE result;

    public RESULT_TYPE getResult()
    {
        return result;
    }
}
