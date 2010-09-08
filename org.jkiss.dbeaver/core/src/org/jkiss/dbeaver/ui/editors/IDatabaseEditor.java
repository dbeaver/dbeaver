/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors;

import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.model.navigator.DBNEvent;

/**
 * IDatabaseEditor
 */
public interface IDatabaseEditor extends IEditorPart
{
    IDatabaseEditorInput getEditorInput();

    void refreshDatabaseContent(DBNEvent event);
}