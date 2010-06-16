/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.load;

import org.eclipse.jface.window.IShellProvider;

/**
 * Lazy loading visualizer
 */
public interface ILoadVisualizer<RESULT> extends IShellProvider {

    boolean isCompleted();

    void visualizeLoading();

    void completeLoading(RESULT result);

}