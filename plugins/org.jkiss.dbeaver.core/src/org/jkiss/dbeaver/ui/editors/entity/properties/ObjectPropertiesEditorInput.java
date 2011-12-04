/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity.properties;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.MultiEditorInput;
import org.eclipse.ui.views.properties.IPropertySource2;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * ObjectPropertiesEditorInput
 */
public class ObjectPropertiesEditorInput extends MultiEditorInput implements IDatabaseEditorInput {

    private final IDatabaseEditorInput mainInput;

    /**
     * Constructs a new MultiEditorInput.
     */
    public ObjectPropertiesEditorInput(
        IDatabaseEditorInput mainInput)
    {
        super(new String[]{}, new IEditorInput[] {});
        this.mainInput = mainInput;
    }

    public DBNDatabaseNode getTreeNode()
    {
        return mainInput.getTreeNode();
    }

    public DBSObject getDatabaseObject()
    {
        return mainInput.getDatabaseObject();
    }

    public String getDefaultPageId()
    {
        return mainInput.getDefaultPageId();
    }

    public String getDefaultFolderId()
    {
        return mainInput.getDefaultFolderId();
    }

    public DBECommandContext getCommandContext()
    {
        return mainInput.getCommandContext();
    }

    public IPropertySource2 getPropertySource()
    {
        return mainInput.getPropertySource();
    }

    public DBPDataSource getDataSource()
    {
        return mainInput.getDataSource();
    }
}
