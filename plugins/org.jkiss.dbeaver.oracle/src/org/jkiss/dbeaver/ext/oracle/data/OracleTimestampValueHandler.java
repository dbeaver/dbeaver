/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDValueHandler2;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCDateTimeValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * Object type support
 */
public class OracleTimestampValueHandler extends JDBCDateTimeValueHandler implements DBDValueHandler2 {

    public OracleTimestampValueHandler(DBDDataFormatterProfile formatterProfile)
    {
        super(formatterProfile);
    }

    @Override
    public String getValueDisplayString(DBSTypedObject column, String format, Object value)
    {
        return super.getValueDisplayString(column, value);
    }
}
