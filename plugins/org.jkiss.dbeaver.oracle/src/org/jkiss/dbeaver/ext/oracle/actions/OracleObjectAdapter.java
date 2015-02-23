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
package org.jkiss.dbeaver.ext.oracle.actions;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jkiss.dbeaver.ext.IDatabaseEditor;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObject;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObjectEx;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorInput;

/**
 * Oracle object adapter
 */
public class OracleObjectAdapter implements IAdapterFactory {

    public OracleObjectAdapter() {
    }

    @Override
    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (OracleSourceObject.class.isAssignableFrom(adapterType)) {
            DBSObject dbObject = null;
            if (adaptableObject instanceof DBNDatabaseNode) {
                dbObject = ((DBNDatabaseNode) adaptableObject).getObject();
            } else if (adaptableObject instanceof IDatabaseEditor) {
                dbObject = ((IDatabaseEditor) adaptableObject).getEditorInput().getDatabaseObject();
            } else if (adaptableObject instanceof DatabaseEditorInput) {
                dbObject = ((DatabaseEditorInput) adaptableObject).getDatabaseObject();
            }
            if (dbObject != null && adapterType.isAssignableFrom(dbObject.getClass())) {
                return dbObject;
            }
        }
        return null;
    }

    @Override
    public Class[] getAdapterList() {
        return new Class[] { OracleSourceObject.class, OracleSourceObjectEx.class };
    }
}
