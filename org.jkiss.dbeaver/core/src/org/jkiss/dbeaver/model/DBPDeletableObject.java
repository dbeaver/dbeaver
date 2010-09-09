/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * DBPDeletableObject
 */
public interface DBPDeletableObject
{
    void deleteObject(IWorkbenchWindow workbenchWindow);
}
