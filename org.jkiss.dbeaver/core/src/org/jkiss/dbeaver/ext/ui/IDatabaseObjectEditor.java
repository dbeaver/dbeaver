/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.ui;

import org.eclipse.ui.IEditorPart;

/**
 * IDatabaseObjectEditor
 */
public interface IDatabaseObjectEditor<OBJECT_TYPE> extends IEmbeddedWorkbenchPart, IEditorPart {

    void initObjectEditor(IDatabaseObjectManager<OBJECT_TYPE> manager);

}
