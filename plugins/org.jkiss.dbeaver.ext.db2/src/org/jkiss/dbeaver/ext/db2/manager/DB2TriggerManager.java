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
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Trigger;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;

/**
 * DB2 Trigger Manager
 * 
 * @author Denis Forveille
 */
public class DB2TriggerManager extends DB2AbstractDropOnlyManager<DB2Trigger, DB2Schema> {

    private static final String SQL_DROP = "DROP TRIGGER %s";

    @Override
    public String buildDropStatement(DB2Trigger db2Trigger)
    {
        String fullyQualifiedName = db2Trigger.getFullQualifiedName();
        return String.format(SQL_DROP, fullyQualifiedName);
    }

    @Nullable
    @Override
    public DBSObjectCache<DB2Schema, DB2Trigger> getObjectsCache(DB2Trigger db2Trigger)
    {
        return db2Trigger.getSchema().getTriggerCache();
    }

}