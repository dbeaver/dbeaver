package org.jkiss.dbeaver.ext.ui;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.model.meta.DBMModel;

/**
 * IRefreshableView
 */
public interface IRefreshableView
{
    IAction getRefreshAction();
}