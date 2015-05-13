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
import org.jkiss.dbeaver.ext.db2.model.DB2Alias;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;

/**
 * DB2 Alias Manager
 * 
 * @author Denis Forveille
 */
public class DB2AliasManager extends DB2AbstractDropOnlyManager<DB2Alias, DB2Schema> {

    private static final String SQL_DROP_MODULE = "DROP ALIAS %s FOR MODULE";
    private static final String SQL_DROP_SEQUENCE = "DROP ALIAS %s FOR SEQUENCE";
    private static final String SQL_DROP_TABLE = "DROP ALIAS %s FOR TABLE";

    @Override
    public String buildDropStatement(DB2Alias db2Alias)
    {
        String fullyQualifiedName = db2Alias.getFullQualifiedName();

        switch (db2Alias.getType()) {
        case MODULE:
            return String.format(SQL_DROP_MODULE, fullyQualifiedName);
        case SEQUENCE:
            return String.format(SQL_DROP_SEQUENCE, fullyQualifiedName);
        case TABLE:
            return String.format(SQL_DROP_TABLE, fullyQualifiedName);
        default:
            throw new IllegalArgumentException(db2Alias.getType() + " as DB2AliasType is not supported");
        }
    }

    @Nullable
    @Override
    public DBSObjectCache<DB2Schema, DB2Alias> getObjectsCache(DB2Alias db2Alias)
    {
        return db2Alias.getSchema().getAliasCache();
    }

}
