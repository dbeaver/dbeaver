/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.ui;

import org.eclipse.ui.IWorkbenchPart;

/**
 * IRefreshablePart
 */
public interface IRefreshablePart extends IWorkbenchPart
{
    void refreshPart(Object source);
}