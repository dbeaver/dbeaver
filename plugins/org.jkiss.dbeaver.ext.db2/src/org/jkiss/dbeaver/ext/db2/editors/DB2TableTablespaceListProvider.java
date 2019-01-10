/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        List<DB2Tablespace> validTablespaces = new ArrayList<>(colTablespaces.size());

        for (DB2Tablespace db2Tablespace : colTablespaces) {
            if (db2Tablespace.getDataType().isValidForUserTables()) {
                validTablespaces.add(db2Tablespace);
            }
        }
        return validTablespaces.toArray(new DB2Tablespace[validTablespaces.size()]);
    }
}