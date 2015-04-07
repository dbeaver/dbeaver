/*
 * Copyright (C) 2010-2015 Serge Rieder
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
package org.jkiss.dbeaver.model.navigator;

import org.eclipse.ui.IEditorDescriptor;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.registry.tree.DBXTreeObject;
import org.jkiss.utils.CommonUtils;

/**
 * DBNDatabaseObject
 */
public class DBNDatabaseObject extends DBNDatabaseNode implements DBSObject
{
    private DBXTreeObject meta;
    private IEditorDescriptor editorDescriptor;

    DBNDatabaseObject(DBNNode parent, DBXTreeObject meta)
    {
        super(parent);
        this.meta = meta;
        DBNModel.getInstance().addNode(this);
        this.editorDescriptor = DBeaverUI.getActiveWorkbenchWindow().getWorkbench().getEditorRegistry().findEditor(meta.getEditorId());
    }

    @Override
    protected void dispose(boolean reflect)
    {
        DBNModel.getInstance().removeNode(this, reflect);
        super.dispose(reflect);
    }

    public IEditorDescriptor getEditorDescriptor()
    {
        return editorDescriptor;
    }

    @Override
    public DBXTreeObject getMeta()
    {
        return meta;
    }

    @Override
    protected void reloadObject(DBRProgressMonitor monitor, DBSObject object) {
        // do nothing
    }

    @Override
    public DBSObject getObject()
    {
        return this;
    }

    @Override
    public Object getValueObject()
    {
        return this;
    }

    @Override
    public String getNodeFullName()
    {
        StringBuilder pathName = new StringBuilder();
        for (DBNNode parent = getParentNode(); parent != null; parent = parent.getParentNode()) {
            if (parent instanceof DBNDatabaseFolder) {
                // skip folders
                continue;
            }
            String parentName = parent.getNodeName();
            if (!CommonUtils.isEmpty(parentName)) {
                if (pathName.length() > 0) {
                    pathName.insert(0, '.');
                }
                pathName.insert(0, parentName);
            }
        }
        pathName.insert(0, getNodeName() + " (");
        pathName.append(")");
        return pathName.toString();
    }


    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return meta.getNodeType(getDataSource());
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return meta.getDescription();
    }

    @Override
    public DBSObject getParentObject()
    {
        return getParentNode() instanceof DBNDatabaseNode ? ((DBSWrapper)getParentNode()).getObject() : null;
    }

    @Nullable
    @Override
    public DBPDataSource getDataSource()
    {
        DBSObject parentObject = getParentObject();
        return parentObject == null ? null : parentObject.getDataSource();
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

}
