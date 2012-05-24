/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model.source;

import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.model.struct.DBSObjectStateful;

/**
 * OracleStatefulObject
 */
public interface OracleStatefulObject extends DBSObjectStateful
{
    @Override
    OracleDataSource getDataSource();

    OracleSchema getSchema();

}
