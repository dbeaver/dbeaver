/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.ext.ui.IRefreshablePart;
import org.jkiss.dbeaver.model.DBPDataSourceUser;

/**
 * IDatabaseNodeEditor
 */
public interface IDatabaseNodeEditor extends IEditorPart, DBPDataSourceUser, IDataSourceProvider, IRefreshablePart
{

    IDatabaseNodeEditorInput getEditorInput();

}