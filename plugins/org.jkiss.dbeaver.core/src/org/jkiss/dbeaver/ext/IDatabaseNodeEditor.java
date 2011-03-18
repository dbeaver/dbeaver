/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.model.DBPDataSourceUser;
import org.jkiss.dbeaver.model.navigator.DBNEvent;

/**
 * IDatabaseNodeEditor
 */
public interface IDatabaseNodeEditor extends IEditorPart, DBPDataSourceUser
{
    IDatabaseNodeEditorInput getEditorInput();

    void refreshDatabaseContent(DBNEvent event);
}