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
package org.jkiss.dbeaver.ext.db2.manager;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2Tablespace;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;

/**
 * DB2 Tablespace Manager
 * 
 * @author Denis Forveille
 */
public class DB2TablespaceManager extends DB2AbstractDropOnlyManager<DB2Tablespace, DB2DataSource> {

    private static final String SQL_DROP = "DROP TABLESPACE %s";

    @Override
    public String buildDropStatement(DB2Tablespace db2Tablespace)
    {
        String name = db2Tablespace.getName();
        return String.format(SQL_DROP, name);
    }

    @Nullable
    @Override
    public DBSObjectCache<DB2DataSource, DB2Tablespace> getObjectsCache(DB2Tablespace db2Tablespace)
    {
        return db2Tablespace.getDataSource().getTablespaceCache();
    }
}
