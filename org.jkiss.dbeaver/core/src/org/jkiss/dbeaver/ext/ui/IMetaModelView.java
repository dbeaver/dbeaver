package org.jkiss.dbeaver.ext.ui;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.model.meta.DBMModel;

/**
 * INavigatorView
 */
public interface IMetaModelView
{
    DBMModel getMetaModel();
    
    Viewer getViewer();

    IWorkbenchPart getWorkbenchPart();

}
