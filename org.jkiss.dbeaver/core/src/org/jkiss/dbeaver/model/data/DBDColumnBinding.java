/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;

/**
 * Column value binding info
 */
public class DBDColumnBinding {
    private final DBCColumnMetaData metaData;
    private final DBDValueHandler valueHandler;
    private DBDValueLocator valueLocator;

    public DBDColumnBinding(DBCColumnMetaData metaData, DBDValueHandler valueHandler) {
        this.metaData = metaData;
        this.valueHandler = valueHandler;
    }

    public DBCColumnMetaData getMetaData() {
        return metaData;
    }

    public DBDValueHandler getValueHandler() {
        return valueHandler;
    }

    public DBDValueLocator getValueLocator() {
        return valueLocator;
    }

    public void setValueLocator(DBDValueLocator valueLocator) {
        this.valueLocator = valueLocator;
    }
}
