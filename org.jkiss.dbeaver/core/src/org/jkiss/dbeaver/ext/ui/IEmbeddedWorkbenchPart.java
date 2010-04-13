/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.ui;

import org.eclipse.ui.IWorkbenchPart;

/**
 * IEmbeddedWorkbenchPart
 */
public interface IEmbeddedWorkbenchPart extends IWorkbenchPart
{
    void activatePart();

    void deactivatePart();
}
