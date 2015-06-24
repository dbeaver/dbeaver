/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.db2.editors;

import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2Tablespace;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Provides a list of DB2 Tablespaces for DB2 Table editors
 * 
 * @author Denis Forveille
 * 
 */
public class DB2TableTablespaceListProvider implements IPropertyValueListProvider<DB2Table> {

    @Override
    public boolean allowCustomValue()
    {
        return false;
    }

    @Override
    public Object[] getPossibleValues(DB2Table db2Table)
    {
        Collection<DB2Tablespace> colTablespaces = db2Table.getDataSource().getTablespaceCache().getCachedObjects();
        List<DB2Tablespace> validTablespaces = new ArrayList<DB2Tablespace>(colTablespaces.size());

        for (DB2Tablespace db2Tablespace : colTablespaces) {
            if (db2Tablespace.getDataType().isValidForUserTables()) {
                validTablespaces.add(db2Tablespace);
            }
        }
        return validTablespaces.toArray(new DB2Tablespace[validTablespaces.size()]);
    }
}