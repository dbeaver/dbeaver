/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDValueClonable;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.PreparedStatement;

/**
 * JDBCBLOB
 *
 * @author Serge Rider
 */
public abstract class JDBCContentAbstract implements DBDContent, DBDValueClonable {

    public abstract void bindParameter(DBRProgressMonitor monitor, PreparedStatement preparedStatement, DBSTypedObject columnType, int paramIndex)
        throws DBCException;

}