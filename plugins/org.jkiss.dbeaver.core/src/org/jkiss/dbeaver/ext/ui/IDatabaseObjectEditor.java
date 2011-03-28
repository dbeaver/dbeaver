/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.ui;

import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditorInput;

/**
 * IDatabaseObjectEditor
 */
public interface IDatabaseObjectEditor extends IObjectEditorPart, IDataSourceProvider, IEditorPart {

    IDatabaseNodeEditorInput getEditorInput();
}
