/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.old.layout;

import java.util.List;

/**
 * Abstract factory to create a Layouter.
 */
public abstract class LayouterFactory {

    /**
     * Create the layouter
     *
     * @param figs The Figs to add to the Layouter
     */
    public abstract Layouter createLayouter(List figs);
    
    /**
     * Get the name of the layouter this factory creates.
     * @return the layout name
     */
    public abstract String getName();
}
