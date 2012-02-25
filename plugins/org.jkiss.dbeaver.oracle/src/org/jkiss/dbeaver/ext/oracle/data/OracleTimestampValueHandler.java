/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.data;

import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCDateTimeValueHandler;

/**
 * Object type support
 */
public class OracleTimestampValueHandler extends JDBCDateTimeValueHandler {

    public OracleTimestampValueHandler(DBDDataFormatterProfile formatterProfile)
    {
        super(formatterProfile);
    }

}
