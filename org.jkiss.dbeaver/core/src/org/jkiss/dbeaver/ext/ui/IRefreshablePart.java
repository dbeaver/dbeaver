package org.jkiss.dbeaver.ext.ui;

import org.eclipse.ui.IWorkbenchPart;

/**
 * IRefreshablePart
 */
public interface IRefreshablePart extends IWorkbenchPart
{
    void refreshPart(Object source);
}