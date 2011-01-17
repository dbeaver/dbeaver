/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.ui;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.model.navigator.DBNNode;

/**
 * INavigatorView
 */
public interface INavigatorModelView
{
    DBNNode getRootNode();
    
    Viewer getNavigatorViewer();

}
