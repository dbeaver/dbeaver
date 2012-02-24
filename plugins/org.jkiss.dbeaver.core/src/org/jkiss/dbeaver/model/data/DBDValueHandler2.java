/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * DBD Value Handler extension.
 */
public interface DBDValueHandler2
{

    /**
     * Converts value to specified format.
     * @param column column
     * @param format format name
     * @param value value  @return string representation
     * @return formatted string
     */
    String getValueDisplayString(
        DBSTypedObject column,
        String format,
        Object value);

}
