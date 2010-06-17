/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.dbc;

import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * Execution context
 */
public interface DBCExecutionContext {

    DBPDataSource getDataSource();
    
    void close();

}
