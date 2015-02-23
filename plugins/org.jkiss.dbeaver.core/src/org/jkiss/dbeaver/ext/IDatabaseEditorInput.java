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

package org.jkiss.dbeaver.ext;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.views.properties.IPropertySource2;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;

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

    Collection<String> getAttributeNames();

    Object getAttribute(String name);

    Object setAttribute(String name, Object value);
}
