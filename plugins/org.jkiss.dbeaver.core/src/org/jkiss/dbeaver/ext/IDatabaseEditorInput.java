/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.views.properties.IPropertySource2;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * IDatabaseEditorInput
 */
public interface IDatabaseEditorInput extends IEditorInput, IDataSourceProvider {

    DBNDatabaseNode getTreeNode();

    DBSObject getDatabaseObject();

    /**
     * Default editor page ID
     * @return page ID or null
     */
    String getDefaultPageId();

    /**
     * Default editor folder (tab) ID
     * @return folder ID or null
     */
    String getDefaultFolderId();

    /**
     * Command context
     * @return command context
     */
    DBECommandContext getCommandContext();

    /**
     * Underlying object's property source
     * @return property source
     */
    IPropertySource2 getPropertySource();
}
