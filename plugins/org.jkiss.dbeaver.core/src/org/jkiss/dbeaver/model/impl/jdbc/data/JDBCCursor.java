/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.data.DBDCursor;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.api.JDBCResultSetImpl;

import java.sql.ResultSet;

/**
 * Result set holder
 */
public class JDBCCursor extends JDBCResultSetImpl implements DBDCursor {

    static final Log log = LogFactory.getLog(JDBCCursor.class);

    public JDBCCursor(JDBCExecutionContext context, ResultSet original, String description)
    {
        super(context, original, description);
    }

    @Override
    public boolean isNull()
    {
        return false;
    }

    @Override
    public DBDValue makeNull()
    {
        throw new IllegalStateException(CoreMessages.model_jdbc_cant_create_null_cursor);
    }

    @Override
    public void release()
    {
        super.close();
    }

    @Override
    public String toString()
    {
        return getStatement().getDescription();
    }

}
