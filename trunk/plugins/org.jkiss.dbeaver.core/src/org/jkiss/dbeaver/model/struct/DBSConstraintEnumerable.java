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
package org.jkiss.dbeaver.model.struct;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDAttributeValue;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.exec.DBCSession;

import java.util.Collection;
import java.util.List;

/**
 * Enumerable Constraint.
 * May return possible values for one of constraint's key.
 */
public interface DBSConstraintEnumerable extends DBSEntityConstraint
{
    /**
     * Checks that this constrain supports key enumerations.
     * Usually it depends on constraint type (enumerations makes sense only for unique constraints).
     * @return true or false
     */
    boolean supportsEnumeration();

    /**
     * Gets enumeration values
     * @param session
     * @param keyColumn enumeration column.
     * @param keyPattern pattern for enumeration values. If null or empty then returns full enumration set
     * @param preceedingKeys other constrain key values. May be null.
     * @param maxResults maximum enumeration values in result set     @return statement with result set which contains valid enumeration values.
     * */
    Collection<DBDLabelValuePair> getKeyEnumeration(
        DBCSession session,
        DBSEntityAttribute keyColumn,
        Object keyPattern,
        @Nullable List<DBDAttributeValue> preceedingKeys,
        int maxResults)
        throws DBException;

}