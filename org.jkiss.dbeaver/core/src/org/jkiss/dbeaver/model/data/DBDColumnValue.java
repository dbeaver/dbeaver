/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.struct.DBSColumnBase;

/**
 * Column value
 */
public class DBDColumnValue {

    private DBSColumnBase column;
    private Object value;

    public DBDColumnValue(DBSColumnBase column, Object value) {
        this.column = column;
        this.value = value;
    }

    public DBSColumnBase getColumn() {
        return column;
    }

    public void setColumn(DBSColumnBase column) {
        this.column = column;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
