/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.ui;

import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.edit.DBOManager;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * IDatabaseObjectEditor
 */
public interface IDatabaseObjectEditor<OBJECT_MANAGER extends DBOManager<? extends DBSObject>> extends IObjectEditorPart, IDataSourceProvider, IEditorPart {

    OBJECT_MANAGER getObjectManager();

    /**
     * Initializes object manager
     * @param manager object manager
     */
    void initObjectEditor(OBJECT_MANAGER manager);

}
