/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;

/**
 * Column value
 */
public class DBDColumnValue {

    private DBSEntityAttribute attribute;
    private Object value;

    public DBDColumnValue(DBSEntityAttribute attribute, Object value) {
        this.attribute = attribute;
        this.value = value;
    }

    public DBSEntityAttribute getAttribute() {
        return attribute;
    }

    public void setAttribute(DBSTableColumn attribute) {
        this.attribute = attribute;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
