/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDColumnValue;
import org.jkiss.dbeaver.model.dbc.DBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;

/**
 * Enumerable Constraint.
 * May return possible values for one of constraint's key.
 */
public interface DBSConstraintEnumerable extends DBSConstraint
{
    /**
     * Checks that this constrain supports key enumerations.
     * Usually it depends on constraint type (enumerations makes sense only for unique constraints).
     * @return true or false
     */
    boolean supportsEnumeration();

    /**
     * Gets enumeration values
     * @param sampleValue enumeration column. Value of this column is a pattern for enumeration values.
     *  If sample column value is null or empty then returns full enumration set
     * @param otherKeys other constrain key values. May be null.
     * @return statement with result set which contains valid enumeration values.
     */
    DBCStatement getKeyEnumeration(
        DBRProgressMonitor monitor,
        DBDColumnValue sampleValue,
        List<DBDColumnValue> otherKeys,
        int maxResults)
        throws DBException;

}