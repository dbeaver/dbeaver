/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.struct.DBSTableColumn;

/**
 * Column value
 */
public class DBDColumnValue {

    private DBSTableColumn column;
    private Object value;

    public DBDColumnValue(DBSTableColumn column, Object value) {
        this.column = column;
        this.value = value;
    }

    public DBSTableColumn getColumn() {
        return column;
    }

    public void setColumn(DBSTableColumn column) {
        this.column = column;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
