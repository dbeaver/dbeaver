/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDValueClonable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.PreparedStatement;

/**
 * JDBCBLOB
 *
 * @author Serge Rider
 */
public abstract class JDBCContentAbstract implements DBDContent, DBDValueClonable {

    public abstract void bindParameter(DBCExecutionContext context, PreparedStatement preparedStatement, DBSTypedObject columnType, int paramIndex)
        throws DBCException;

}