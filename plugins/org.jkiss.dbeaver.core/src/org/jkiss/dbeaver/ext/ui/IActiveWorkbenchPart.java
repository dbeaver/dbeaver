/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.ui;

import org.eclipse.ui.IWorkbenchPart;

/**
 * Active workbench part
 */
public interface IActiveWorkbenchPart extends IWorkbenchPart
{
    /**
     * Activates editor part.
     * Called when part becomes active (visible).
     */
    void activatePart();

    /**
     * Deactivates editor part.
     * Called when part becomes inactive (invisible).
     */
    void deactivatePart();

}
