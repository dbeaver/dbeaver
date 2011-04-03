/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.ui;

import org.eclipse.ui.IWorkbenchPart;

/**
 * IFolderedPart
 */
public interface IFolderedPart extends IWorkbenchPart
{
    void switchFolder(String folderId);
}