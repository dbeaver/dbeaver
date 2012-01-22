/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDColumnValue;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;

import java.util.Collection;
import java.util.List;

/**
 * Enumerable Constraint.
 * May return possible values for one of constraint's key.
 */
public interface DBSConstraintEnumerable extends DBSTableConstraint
{
    /**
     * Checks that this constrain supports key enumerations.
     * Usually it depends on constraint type (enumerations makes sense only for unique constraints).
     * @return true or false
     */
    boolean supportsEnumeration();

    /**
     * Gets enumeration values
     * @param context
     * @param keyColumn enumeration column.
     * @param keyPattern pattern for enumeration values. If null or empty then returns full enumration set
     * @param preceedingKeys other constrain key values. May be null.
     * @param maxResults maximum enumeration values in result set     @return statement with result set which contains valid enumeration values.
     * */
    Collection<DBDLabelValuePair> getKeyEnumeration(
        DBCExecutionContext context,
        DBSEntityAttribute keyColumn,
        Object keyPattern,
        List<DBDColumnValue> preceedingKeys,
        int maxResults)
        throws DBException;

}