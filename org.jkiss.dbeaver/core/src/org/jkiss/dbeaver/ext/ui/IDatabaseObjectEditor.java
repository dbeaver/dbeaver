/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.ui;

import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.ext.IDatabaseObjectManager;
import org.jkiss.dbeaver.model.DBPObject;

/**
 * IDatabaseObjectEditor
 */
public interface IDatabaseObjectEditor<OBJECT_MANAGER extends IDatabaseObjectManager<? extends DBPObject>> extends IEmbeddedWorkbenchPart, IEditorPart {

    void initObjectEditor(OBJECT_MANAGER manager);

}
