/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.model.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.exec.DBCSession;

import java.util.Collection;

/**
 * DBSAttributeEnumerable
 */
public interface DBSAttributeEnumerable extends DBSEntityAttribute
{
    /**
     * Gets enumeration values
     * @param session        session
     * @param valuePattern   pattern for enumeration values. If null or empty then returns full enumration set
     * @param maxResults     maximum enumeration values in result set
     * @return statement with result set which contains valid enumeration values.
     **/
    @NotNull
    Collection<DBDLabelValuePair> getValueEnumeration(
        @NotNull DBCSession session,
        @Nullable Object valuePattern,
        int maxResults)
        throws DBException;

}