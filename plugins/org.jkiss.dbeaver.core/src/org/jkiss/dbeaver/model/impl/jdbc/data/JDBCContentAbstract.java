/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDValueCloneable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * JDBCBLOB
 *
 * @author Serge Rider
 */
public abstract class JDBCContentAbstract implements DBDContent, DBDValueCloneable {

    public abstract void bindParameter(JDBCExecutionContext context, JDBCPreparedStatement preparedStatement, DBSTypedObject columnType, int paramIndex)
        throws DBCException;

    @Override
    public void resetContents()
    {
        // do nothing
    }
}