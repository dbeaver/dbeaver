/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.edit;

import org.eclipse.ui.IWorkbenchWindow;

/**
 * Private editor.
 * Doesn't participates in common object edit framework. All object's modifications are done
 * by some external dialogs/tools/whatever.
 */
public interface DBEPrivateObjectEditor {

    void editObject(IWorkbenchWindow workbenchWindow);

}