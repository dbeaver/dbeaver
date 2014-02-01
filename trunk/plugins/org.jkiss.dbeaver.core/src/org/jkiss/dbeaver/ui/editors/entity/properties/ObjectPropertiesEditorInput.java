/*
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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

    @Override
    public DBNDatabaseNode getTreeNode()
    {
        return mainInput.getTreeNode();
    }

    @Override
    public DBSObject getDatabaseObject()
    {
        return mainInput.getDatabaseObject();
    }

    @Override
    public String getDefaultPageId()
    {
        return mainInput.getDefaultPageId();
    }

    @Override
    public String getDefaultFolderId()
    {
        return mainInput.getDefaultFolderId();
    }

    @Override
    public DBECommandContext getCommandContext()
    {
        return mainInput.getCommandContext();
    }

    @Override
    public IPropertySource2 getPropertySource()
    {
        return mainInput.getPropertySource();
    }

    @Override
    public DBPDataSource getDataSource()
    {
        return mainInput.getDataSource();
    }
}
