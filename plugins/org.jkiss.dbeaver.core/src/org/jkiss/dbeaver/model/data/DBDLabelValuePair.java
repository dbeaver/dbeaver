/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

/**
 * Label value pair
 */
public class DBDLabelValuePair {

    private final String label;
    private final Object value;

    public DBDLabelValuePair(String label, Object value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public Object getValue() {
        return value;
    }
}
