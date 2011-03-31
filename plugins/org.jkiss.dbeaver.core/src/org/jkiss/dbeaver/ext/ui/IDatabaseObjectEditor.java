/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.ui;

import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditorInput;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;

/**
 * IDatabaseObjectEditor
 */
public interface IDatabaseObjectEditor extends IObjectEditorPart, IDataSourceProvider, IEditorPart {

    IDatabaseNodeEditorInput getEditorInput();

    ProgressPageControl getProgressControl();

}
