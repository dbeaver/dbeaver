package org.jkiss.dbeaver.ext.oracle.model.source;

import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.model.struct.DBSObjectStateful;

/**
 * OracleStatefulObject
 */
public interface OracleStatefulObject extends DBSObjectStateful
{
    OracleDataSource getDataSource();

    OracleSchema getSchema();

}
