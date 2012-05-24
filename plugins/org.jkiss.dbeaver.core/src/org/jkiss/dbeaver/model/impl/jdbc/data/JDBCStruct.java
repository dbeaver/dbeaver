/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.data.DBDValueCloneable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.sql.SQLException;
import java.sql.Struct;

/**
 * Struct holder
 */
public class JDBCStruct implements DBDValue, DBDValueCloneable {

    static final Log log = LogFactory.getLog(JDBCStruct.class);

    private Struct contents;
    private JDBCArrayType type;

    public JDBCStruct(Struct contents)
    {
        this.contents = contents;
    }

    public Struct getValue() throws DBCException
    {
        return contents;
    }

    @Override
    public DBDValueCloneable cloneValue(DBRProgressMonitor monitor)
    {
        return new JDBCStruct(contents);
    }

    @Override
    public boolean isNull()
    {
        return contents == null;
    }

    @Override
    public DBDValue makeNull()
    {
        return new JDBCStruct(null);
    }

    @Override
    public void release()
    {
    }

    public String toString()
    {
        if (isNull()) {
            return DBConstants.NULL_VALUE_LABEL;
        } else {
            try {
                return makeStructString();
            } catch (SQLException e) {
                log.error(e);
                return contents.toString();
            }
        }
    }

    public String getTypeName()
    {
        try {
            return contents == null ? null : contents.getSQLTypeName();
        } catch (SQLException e) {
            log.error(e);
            return null;
        }
    }

    public String makeStructString() throws SQLException
    {
        if (isNull()) {
            return null;
        }
        if (contents == null) {
            return null;
        }
        StringBuilder str = new StringBuilder(200);
        String typeName = getTypeName();
        if (typeName != null) {
            str.append(typeName);
        }
        str.append("(");
        final Object[] attributes = contents.getAttributes();
        for (int i = 0, attributesLength = attributes.length; i < attributesLength; i++) {
            if (i > 0) str.append(',');
            str.append('\'');
            Object item = attributes[i];
            str.append(item == null ? DBConstants.NULL_VALUE_LABEL : item.toString());
            str.append('\'');
        }
        str.append(")");
        return str.toString();
    }

}
