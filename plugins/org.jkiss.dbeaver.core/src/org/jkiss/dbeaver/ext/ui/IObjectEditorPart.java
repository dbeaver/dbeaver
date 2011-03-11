/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.ui;

import org.eclipse.ui.IWorkbenchPart;

/**
 * Embedded workbench part
 */
public interface IObjectEditorPart extends IWorkbenchPart
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
