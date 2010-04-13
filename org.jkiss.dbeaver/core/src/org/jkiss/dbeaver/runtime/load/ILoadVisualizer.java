/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.load;

/**
 * Lazy loading visualizer
 */
public interface ILoadVisualizer<RESULT> {

    boolean isCompleted();

    void visualizeLoading();

    void completeLoading(RESULT result);

}