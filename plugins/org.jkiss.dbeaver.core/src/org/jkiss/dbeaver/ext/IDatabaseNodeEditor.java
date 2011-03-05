/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.model.navigator.DBNEvent;

/**
 * IDatabaseNodeEditor
 */
public interface IDatabaseNodeEditor extends IEditorPart
{
    IDatabaseNodeEditorInput getEditorInput();

    void refreshDatabaseContent(DBNEvent event);
}