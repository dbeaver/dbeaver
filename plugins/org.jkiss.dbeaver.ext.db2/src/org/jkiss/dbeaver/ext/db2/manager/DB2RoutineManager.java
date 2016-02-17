/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
import org.jkiss.dbeaver.ext.db2.model.DB2Routine;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;

/**
 * DB2 Routine Manager
 * 
 * @author Denis Forveille
 */
public class DB2RoutineManager extends DB2AbstractDropOnlyManager<DB2Routine, DB2Schema> {

    private static final String SQL_DROP_FUNCTION = "DROP SPECIFIC FUNCTION %s";
    private static final String SQL_DROP_METHOD = "DROP SPECIFIC METHOD %s";
    private static final String SQL_DROP_PROCEDURE = "DROP SPECIFIC PROCEDURE %s";

    @Override
    public String buildDropStatement(DB2Routine db2Routine)
    {
        String fullyQualifiedName = db2Routine.getFullQualifiedName();
        switch (db2Routine.getType()) {
        case F:
            return String.format(SQL_DROP_FUNCTION, fullyQualifiedName);
        case M:
            return String.format(SQL_DROP_METHOD, fullyQualifiedName);
        case P:
            return String.format(SQL_DROP_PROCEDURE, fullyQualifiedName);
        default:
            throw new IllegalStateException(db2Routine.getType() + " not suppoted");
        }
    }

    @Nullable
    @Override
    public DBSObjectCache<DB2Schema, DB2Routine> getObjectsCache(DB2Routine db2Routine)
    {
        switch (db2Routine.getType()) {
        case F:
            return db2Routine.getSchema().getUdfCache();
        case M:
            return db2Routine.getSchema().getMethodCache();
        case P:
            return db2Routine.getSchema().getProcedureCache();
        default:
            throw new IllegalStateException(db2Routine.getType() + " is not a supported DB2RoutineType");
        }

    }

}
